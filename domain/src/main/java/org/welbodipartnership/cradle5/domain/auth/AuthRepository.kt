package org.welbodipartnership.cradle5.domain.auth

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.selects.SelectClause2
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.welbodipartnership.api.ApiAuthToken
import org.welbodipartnership.api.cradle5.HealthcareFacilitySummary
import org.welbodipartnership.cradle5.data.cryptography.ArgonHasher
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.data.settings.AuthToken
import org.welbodipartnership.cradle5.data.settings.ArgonHash
import org.welbodipartnership.cradle5.domain.NetworkResult
import org.welbodipartnership.cradle5.domain.RestApi
import org.welbodipartnership.cradle5.domain.UrlProvider
import org.welbodipartnership.cradle5.domain.districts.DistrictRepository
import org.welbodipartnership.cradle5.domain.enums.EnumRepository
import org.welbodipartnership.cradle5.domain.facilities.FacilityRepository
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.domain.util.launchWithPermit
import org.welbodipartnership.cradle5.util.ApplicationCoroutineScope
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp
import org.welbodipartnership.cradle5.util.foreground.AppForegroundedObserver
import org.welbodipartnership.cradle5.util.net.NetworkObserver
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

private val AUTH_TIMEOUT: Duration = 1.days

/**
 * If the time since the token was issued is less than this threshold, then we do not perform
 * opportunistic token refreshes. Note: The tokens currently expire in 14 days.
 *
 * Note that after a certain number of failures, the app will also start using the server as a
 * source of truth.
 */
private val AUTO_REFRESH_THRESHOLD: Duration = 1.days

@Singleton
class AuthRepository @Inject internal constructor(
  private val restApi: RestApi,
  private val appValuesStore: AppValuesStore,
  private val hasher: ArgonHasher,
  private val networkObserver: NetworkObserver,
  private val dbWrapper: CradleDatabaseWrapper,
  private val dispatchers: AppCoroutineDispatchers,
  private val syncRepository: SyncRepository,
  private val facilityRepository: FacilityRepository,
  private val districtRepository: DistrictRepository,
  private val enumRepository: EnumRepository,
  private val urlProvider: UrlProvider,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  @ApplicationContext private val context: Context,
  @ApplicationCoroutineScope private val applicationCoroutineScope: CoroutineScope,
  appForegroundedObserver: AppForegroundedObserver
) {

  val nextExpiryTimeFlow: Flow<UnixTimestamp?> = appValuesStore.lastTimeAuthedFlow
    .map { lastTimeAuthed ->
      if (lastTimeAuthed != null) {
        lastTimeAuthed + AUTH_TIMEOUT
      } else {
        null
      }
    }

  /**
   * Represents the authentication state of the user.
   */
  val authStateFlow: Flow<AuthState> = combine(
    // Put this here so that it is refreshed when the app's foreground state changes. We want
    appForegroundedObserver.isForegrounded as Flow<Boolean>,
    appValuesStore.authTokenFlow as Flow<AuthToken?>,
    appValuesStore.lastTimeAuthedFlow as Flow<UnixTimestamp?>,
    appValuesStore.loginCompleteFlow as Flow<Boolean>,
    appValuesStore.warningMessageFlow as Flow<String?>,
    appValuesStore.forceReauthFlow as Flow<Boolean>,
  ) { args ->
    var i = 1
    val authToken: AuthToken? = args[i++] as AuthToken?
    val lastTimeAuthed: UnixTimestamp? = args[i++] as UnixTimestamp?
    val isLoginComplete: Boolean = args[i++] as Boolean
    val warningMessage: String? = args[i++] as String?
    val forceReauth: Boolean = args[i++] as Boolean

    if (!warningMessage.isNullOrBlank()) {
      AuthState.BlockingWarningMessage(warningMessage)
    } else if (
      authToken == null ||
      !authToken.isInitialized ||
      authToken == AuthToken.getDefaultInstance() ||
      !isLoginComplete
    ) {
      if (authToken != null && !isLoginComplete) {
        Log.d(TAG, "authStateFlow: token present but login not complete")
        AuthState.LoggingIn
      } else {
        AuthState.LoggedOut
      }
    } else {
      val username = authToken.username
      // fall back to 0 to always force authentication if there is no last authed time for some
      // reason
      val lastTimeAuthedForComparison = lastTimeAuthed ?: UnixTimestamp(0)
      val tokenExpiryTime: UnixTimestamp = UnixTimestamp
        .fromDateTimeString(authToken.expires, ApiAuthToken.dateTimeFormatter)
      val now = UnixTimestamp.now()

      when {
        forceReauth -> AuthState.ForcedRelogin(username)
        now >= tokenExpiryTime -> AuthState.TokenExpired(username)
        lastTimeAuthedForComparison durationBetween now >= AUTH_TIMEOUT -> {
          AuthState.LoggedInLocked(username)
        }
        else -> AuthState.LoggedInUnlocked(username)
      }
    }
  }.stateIn(
    scope = applicationCoroutineScope,
    started = SharingStarted.WhileSubscribed(
      stopTimeoutMillis = 5000L,
      replayExpirationMillis = Long.MAX_VALUE
    ),
    AuthState.Initializing
  )

  @Immutable
  sealed class LoginResult {
    /**
     * Whether in the login process, we reached the point where we were able to store a fresh
     * auth token from the server. If logging in for the first time, the auth token is not
     * guaranteed to be stored.
     *
     * If this is true but the result is not a [Success], this means something went wrong during
     * the facility, districts, enums download.
     */
    abstract val gotTokenFromServer: Boolean
    abstract val isLocalPasswordCheckSuccess: Boolean

    abstract fun withSuccessfulLocalPasswordCheck(): LoginResult

    object Success : LoginResult() {
      override val gotTokenFromServer: Boolean = true
      override val isLocalPasswordCheckSuccess: Boolean = true
      override fun withSuccessfulLocalPasswordCheck(): LoginResult = this
    }

    /**
     * Server sent non-successful HTTP response
     */
    data class Invalid(
      override val gotTokenFromServer: Boolean,
      val errorMessage: String?,
      val errorCode: Int?,
      val errorType: String? = null,
      override val isLocalPasswordCheckSuccess: Boolean = false,
    ) : LoginResult() {
      val isFromBadCredentials: Boolean get() = errorCode == 400 && errorType == "invalid_grant"
      override fun withSuccessfulLocalPasswordCheck(): LoginResult =
        this.copy(isLocalPasswordCheckSuccess = true)
    }
    data class Exception(
      override val gotTokenFromServer: Boolean,
      val errorMessage: String?,
      override val isLocalPasswordCheckSuccess: Boolean = false,
    ) : LoginResult() {
      override fun withSuccessfulLocalPasswordCheck(): LoginResult =
        this.copy(isLocalPasswordCheckSuccess = true)
    }
  }

  suspend fun login(
    username: String,
    password: String,
    loginEventMessagesChannel: SendChannel<String>?,
    isForTokenRefresh: Boolean,
  ): LoginResult {
    /*
    if (!networkObserver.isNetworkAvailable()) {
      Log.d(TAG, "login(): network not available")
      return LoginResult.Exception(context.getString(R.string.login_error_no_network))
    }
     */

    appValuesStore.markLoginIncomplete()
    var success = false
    try {
      loginEventMessagesChannel?.trySend(
        if (isForTokenRefresh) {
          "Checking credentials with server"
        } else {
          "Submitting credentials to server"
        }
      )
      val token: AuthToken = when (val loginResult = restApi.login(username, password)) {
        is NetworkResult.Success -> loginResult.value
        is NetworkResult.Failure -> {
          return LoginResult.Invalid(
            gotTokenFromServer = false,
            errorMessage = loginResult.errorValue?.errorDescription,
            errorCode = loginResult.statusCode,
            errorType = loginResult.errorValue?.error
          )
        }
        is NetworkResult.NetworkException -> {
          return LoginResult.Exception(
            gotTokenFromServer = false,
            loginResult.formatErrorMessage(context)
          )
        }
      }
      Log.d(TAG, "login(): successfully obtained token")
      if (!isForTokenRefresh) {
        loginEventMessagesChannel?.trySend("Setting up lockscreen")
      }
      val passwordHash = hasher.hash(forPassword = true, password)
      val usernameHash = hasher.hash(forPassword = false, username)
      // We have to insert the token here for RestApi to be able to authenticate.
      // Note: This function is also used for token refreshes, so the hash will be updated
      // redundantly?
      appValuesStore.insertLoginDetails(authToken = token, passwordHash, usernameHash)

      val result = doLoginInfoSync(
        loginEventMessagesChannel?.let { InfoSyncProgressReceiver.StringMessages(it) }
      )
      success = result is LoginResult.Success
      return result
    } finally {
      if (!success) {
        if (!isForTokenRefresh) {
          appValuesStore.clearAuthToken()
        }
      } else {
        appValuesStore.setForceReauth(false)
      }
      appValuesStore.markLoginComplete()
      loginEventMessagesChannel?.close()
    }
  }

  data class InfoSyncProgress(
    val stage: InfoSyncStage,
    val text: String
  )

  sealed class InfoSyncProgressReceiver {
    abstract val stringChannel: SendChannel<String>
    abstract fun sendProgress(infoString: String)
    abstract fun sendProgress(stage: InfoSyncStage, infoString: String)
    abstract fun sendProgress(stage: InfoSyncStage)
    class StringMessages(
      private val channel: SendChannel<String>
    ) : InfoSyncProgressReceiver() {
      override fun sendProgress(stage: InfoSyncStage, infoString: String) {
        channel.trySend(infoString)
      }

      override fun sendProgress(stage: InfoSyncStage) {
        channel.trySend(stage.logString)
      }

      override val stringChannel: SendChannel<String> get() = channel
      override fun sendProgress(infoString: String) {
        channel.trySend(infoString)
      }
    }
    class StageAndStringMessages(private val channel: SendChannel<InfoSyncProgress>) : InfoSyncProgressReceiver() {
      var previousStage: InfoSyncStage = InfoSyncStage.GETTING_USER_INFO

      override fun sendProgress(stage: InfoSyncStage) {
        previousStage = stage
        channel.trySend(InfoSyncProgress(stage, stage.logString))
      }
      override fun sendProgress(stage: InfoSyncStage, infoString: String) {
        previousStage = stage
        channel.trySend(InfoSyncProgress(stage, infoString))
      }
      override fun sendProgress(infoString: String) {
        channel.trySend(InfoSyncProgress(previousStage, infoString))
      }

      override val stringChannel: SendChannel<String> = object : SendChannel<String> {
        @ExperimentalCoroutinesApi
        override val isClosedForSend: Boolean
          get() = channel.isClosedForSend
        override val onSend: SelectClause2<String, SendChannel<String>>
          get() = error("Not used")

        override fun close(cause: Throwable?): Boolean {
          return channel.close(cause)
        }

        @ExperimentalCoroutinesApi
        override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) {
          channel.invokeOnClose(handler)
        }

        override suspend fun send(element: String) {
          channel.send(InfoSyncProgress(previousStage, element))
        }

        override fun trySend(element: String): ChannelResult<Unit> {
          return channel.trySend(InfoSyncProgress(previousStage, element))
        }
      }
    }
  }

  enum class InfoSyncStage(val logString: String) {
    GETTING_USER_INFO("Getting user information"),
    GETTING_USER_DISTRICT("Getting user's district"),
    DOWNLOADING_DISTRICTS("Downloading districts"),
    DOWNLOADING_FACILITIES("Downloading facilities"),
    DOWNLOADING_DROPDOWN_VALUES("Downloading dropdown values"),
  }

  suspend fun doLoginInfoSync(
    progressReceiver: InfoSyncProgressReceiver?
  ): LoginResult {
    // Try to get the userId from the index menu items. If we can't get it, we will fail,
    // because certain forms (like GPS coordinates) require us to input a userId.
    progressReceiver?.sendProgress(InfoSyncStage.GETTING_USER_INFO)
    when (val indexResult = restApi.getIndexEntries()) {
      is NetworkResult.Success -> {
        val userDataItem = indexResult.value.asReversed().find { it.title == "User data" }
        if (userDataItem != null) {
          userDataItem.url.substringAfterLast('/', "")
            .toIntOrNull()
            ?.let { userId -> appValuesStore.insertUserId(userId) }
            ?: run {
              val errorMessage =
                "Unable to get userId: ${userDataItem.url} from API index doesn't end in a number"
              progressReceiver?.apply {
                sendProgress(errorMessage)
                delay(2.seconds)
              }
              return LoginResult.Exception(gotTokenFromServer = true, errorMessage)
            }
        } else {
          val errorMessage =
            "Unable to get userId: Missing user data index tab " +
              "(available tabs: ${indexResult.value})"
          progressReceiver?.apply {
            sendProgress(errorMessage)
            delay(2.seconds)
          }
          return LoginResult.Exception(gotTokenFromServer = true, errorMessage)
        }
      }
      is NetworkResult.Failure -> {
        val message = indexResult.errorValue.decodeToString()
        val errorMessage =
          "Unable to get userId: HTTP ${indexResult.statusCode} error (message: $message)"
        progressReceiver?.apply {
          sendProgress(errorMessage)
          delay(800L)
        }
        return LoginResult.Invalid(
          gotTokenFromServer = true,
          errorMessage,
          indexResult.statusCode
        )
      }
      is NetworkResult.NetworkException -> {
        val errorMessage = "Unable to get userId: ${indexResult.formatErrorMessage(context)}"
        progressReceiver?.sendProgress(errorMessage)
        delay(800L)
        return LoginResult.Exception(gotTokenFromServer = true, errorMessage)
      }
    }

    // Try to get the user's district
    progressReceiver?.sendProgress(InfoSyncStage.GETTING_USER_DISTRICT)
    val districtName: String? = when (
      val result = restApi.getFormTitle<HealthcareFacilitySummary>()
    ) {
      is NetworkResult.Success -> {
        val title = result.value
        val name = title.substringAfter("Healthcare Facility Summary - ")
          .trim()
          .ifBlank { null }
        Log.d(TAG, "doLoginInfoSync(): parsed title $title to get name $name")
        if (name != null) {
          appValuesStore.setDistrictName(name)
        } else {
          progressReceiver?.sendProgress("User not associated with a district")
          delay(1.seconds)
        }
        name
      }
      is NetworkResult.Failure -> {
        val message = result.errorValue.decodeToString()
        progressReceiver?.sendProgress(
          "Unable to get district: HTTP ${result.statusCode} error (message: $message)"
        )
        null
      }
      is NetworkResult.NetworkException -> {
        progressReceiver?.sendProgress(
          "Unable to get district: ${result.formatErrorMessage(context)}"
        )
        null
      }
    }

    progressReceiver?.sendProgress(InfoSyncStage.DOWNLOADING_DISTRICTS)
    when (val result = districtRepository.downloadAndSaveDistricts(progressReceiver?.stringChannel)) {
      DistrictRepository.DownloadResult.Success -> {}
      is DistrictRepository.DownloadResult.Exception -> {
        return LoginResult.Exception(gotTokenFromServer = true, result.errorMessage)
      }
      is DistrictRepository.DownloadResult.Invalid -> {
        return LoginResult.Invalid(gotTokenFromServer = true, result.errorMessage, result.errorCode)
      }
    }

    districtName
      ?.let { dbWrapper.districtDao().getDistrictByName(districtName).firstOrNull() }
      ?.let { district ->
        appValuesStore.setDistrictId(district.id)
        district.id
      }

    // Try to get the facilities associated with this district

    progressReceiver?.sendProgress(InfoSyncStage.DOWNLOADING_FACILITIES, "Getting facilities for our district")
    when (val result = facilityRepository.downloadAndSaveFacilities(progressReceiver?.stringChannel)) {
      FacilityRepository.DownloadResult.Success -> {}
      is FacilityRepository.DownloadResult.Exception -> {
        return LoginResult.Exception(gotTokenFromServer = true, result.errorMessage)
      }
      is FacilityRepository.DownloadResult.Invalid -> {
        return LoginResult.Invalid(gotTokenFromServer = true, result.errorMessage, result.errorCode)
      }
    }

    val workSemaphore = Semaphore(permits = 3)
    try {
      withContext(appCoroutineDispatchers.io.limitedParallelism(3)) {
        dbWrapper.districtDao().getAllDistricts().forEach { district ->
          launchWithPermit(workSemaphore) {
            progressReceiver?.sendProgress("Getting facilities for district ${district.name}")
            when (
              val result = facilityRepository.downloadAndSaveFacilities(
                progressReceiver?.stringChannel,
                districtId = district.id
              )
            ) {
              FacilityRepository.DownloadResult.Success -> {}
              is FacilityRepository.DownloadResult.Exception -> {
                throw FacilityParallelDownloadException(
                  LoginResult.Exception(gotTokenFromServer = true, result.errorMessage)
                )
              }
              is FacilityRepository.DownloadResult.Invalid -> {
                throw FacilityParallelDownloadException(
                  LoginResult.Invalid(
                    gotTokenFromServer = true,
                    result.errorMessage,
                    result.errorCode
                  )
                )
              }
            }
          }
        }
      }
    } catch (e: FacilityParallelDownloadException) {
      Log.e(TAG, "Facility download failed: ${e.result}")
      return e.result
    }

    progressReceiver?.sendProgress(InfoSyncStage.DOWNLOADING_DROPDOWN_VALUES)
    when (val result = enumRepository.downloadAndSaveEnumsFromServer(progressReceiver?.stringChannel)) {
      EnumRepository.DownloadResult.Success -> {}
      is EnumRepository.DownloadResult.Exception -> {
        return LoginResult.Exception(gotTokenFromServer = true, result.errorMessage)
      }
      is EnumRepository.DownloadResult.Invalid -> {
        return LoginResult.Invalid(gotTokenFromServer = true, result.errorMessage, result.errorCode)
      }
    }

    return LoginResult.Success
  }

  suspend fun reauthForLockscreen(
    password: String,
    forceServerRefresh: Boolean,
    eventMessagesChannel: SendChannel<String>?,
  ): LoginResult {
    val result = reauthForLockscreenInner(password, forceServerRefresh, eventMessagesChannel)
    when (result) {
      LoginResult.Success -> {
        Log.w(TAG, "reauth(): successful reauthentication")
      }
      is LoginResult.Exception -> {
        if (!result.gotTokenFromServer) {
          appValuesStore.setWarningMessage(
            "Got an issue during reauthentication with the server: ${result.errorMessage}"
          )
        }
      }
      is LoginResult.Invalid -> {
        if (!result.gotTokenFromServer && result.isLocalPasswordCheckSuccess) {
          appValuesStore.setWarningMessage(
            buildString {
              append("Got an issue during reauthentication with the server ")
              append("(error code ${result.errorCode ?: "unknown"}, ")
              append("error type ${result.errorType ?: "unknown"})")
              if (!result.errorMessage.isNullOrBlank()) {
                append(":")
                appendLine()
                appendLine()
                append(result.errorMessage)
              }
            }
          )
        }
      }
    }

    if (result.isLocalPasswordCheckSuccess || result is LoginResult.Success) {
      Log.w(TAG, "reauth(): local password check was successful")
      appValuesStore.apply {
        setLastTimeAuthenticatedToNow()
        setForceReauth(false)
      }
    }

    return result
  }

  private suspend fun reauthForLockscreenInner(
    password: String,
    forceServerRefresh: Boolean,
    eventMessagesChannel: SendChannel<String>?
  ): LoginResult {

    val existingHash: ArgonHash? = appValuesStore.passwordHashFlow.firstOrNull()
    if (existingHash == null) {
      Log.w(TAG, "reauth(): trying to reauthenticate, but there is no stored hash")
      return LoginResult.Exception(gotTokenFromServer = false, "Missing existing login details")
    }

    eventMessagesChannel?.trySend("Verifying password")
    val isLocalPasswordMatch: Boolean = hasher.verifyPassword(password, existingHash)
    Log.d(
      TAG,
      "reauthForLockscreen(forceServerRefresh = $forceServerRefresh) " +
        "-> isLocalPasswordMatch: $isLocalPasswordMatch"
    )
    // We might have to force a server refresh if they changed password on MedSciNet but their
    // phone still has the old password hash stored.
    if (isLocalPasswordMatch || forceServerRefresh) {
      if (forceServerRefresh) {
        Log.d(TAG, "doing forced token refresh")
        return refreshAuthTokenAndUserInfoAndFacilities(
          password = password,
          skipTimeChecks = true,
          isLocalPasswordIncorrect = !isLocalPasswordMatch,
          eventMessagesChannel = eventMessagesChannel,
        ).let { result ->
          if (isLocalPasswordMatch) result.withSuccessfulLocalPasswordCheck() else result
        }
      } else {
        Log.d(TAG, "doing opportunistic token refresh")

        val result = refreshAuthTokenAndUserInfoAndFacilities(
          password = password,
          skipTimeChecks = false,
          isLocalPasswordIncorrect = !isLocalPasswordMatch,
          eventMessagesChannel = eventMessagesChannel,
        ).let { result ->
          if (isLocalPasswordMatch) result.withSuccessfulLocalPasswordCheck() else result
        }
        Log.d(TAG, "opportunistic token refresh result: $result")

        if (result is LoginResult.Invalid && result.isFromBadCredentials) {
          if (isLocalPasswordMatch) {
            appValuesStore.setWarningMessage(
              "The password you entered was correct before, but the MedSciNet server " +
                "returned the following error: ${result.errorMessage} (error code " +
                "${result.errorCode}).\n\nYour password may have been changed or your account " +
                "may have been disabled. Please try to login on the website " +
                "(website link can be found in the settings in the top-right button) to check " +
                "the account status."
            )
          }
        }
      }
    }

    return if (isLocalPasswordMatch) {
      LoginResult.Success
    } else {
      LoginResult.Invalid(
        gotTokenFromServer = false,
        "Invalid password",
        errorCode = null,
        isLocalPasswordCheckSuccess = false
      )
    }
  }

  /**
   * Refreshes the auth token, user information, and facilities list. Since the API doesn't offer
   * any refresh tokens, we require the user's password as user input.
   */
  private suspend fun refreshAuthTokenAndUserInfoAndFacilities(
    password: String,
    skipTimeChecks: Boolean,
    isLocalPasswordIncorrect: Boolean,
    eventMessagesChannel: SendChannel<String>?,
  ): LoginResult {
    Log.d(
      TAG,
      "refreshAuthToken(): skipTimeChecks = $skipTimeChecks, " +
        "isLocalPasswordIncorrect = $isLocalPasswordIncorrect"
    )
    // try to login again
    val token = appValuesStore.authTokenFlow.firstOrNull()
    val username = token?.username
    if (token == null || username.isNullOrBlank()) {
      Log.w(TAG, "refreshAuthToken(): trying to reauthenticate when there is no token")
      return LoginResult.Exception(gotTokenFromServer = false, "No authentication token present")
    } else {
      val issued = UnixTimestamp.fromDateTimeString(token.issued, ApiAuthToken.dateTimeFormatter)
      val expires = UnixTimestamp.fromDateTimeString(token.expires, ApiAuthToken.dateTimeFormatter)
      val now = UnixTimestamp.now()

      if (now >= expires) {
        Log.w(TAG, "refreshAuthToken(): token already expired; forcing refresh")
      } else if (!skipTimeChecks) {
        val durationUntilExpiry = now durationBetween expires
        Log.d(
          TAG,
          "refreshAuthToken(): token expires at ${token.expires}; " +
            "$durationUntilExpiry until token expires"
        )
        val durationSinceIssued = now durationBetween issued
        Log.d(
          TAG,
          "refreshAuthToken(): token issued at ${token.issued}; " +
            "$durationSinceIssued since the token was issued"
        )

        if (durationSinceIssued < AUTO_REFRESH_THRESHOLD) {
          Log.d(
            TAG,
            "refreshAuthToken(): $durationSinceIssued since token was issued; this is less " +
              "than the threshold of $AUTO_REFRESH_THRESHOLD, so ignoring refresh attempt"
          )
          return LoginResult.Exception(
            gotTokenFromServer = false,
            "$durationSinceIssued since token was issued; less than $AUTO_REFRESH_THRESHOLD, " +
              "so ignoring"
          )
        }
      }
    }

    if (!networkObserver.isNetworkAvailable()) {
      Log.w(TAG, "refreshAuthToken(): network not available")
      // return LoginResult.Exception("Network not available")
    }

    eventMessagesChannel?.trySend(
      "Preparing to re-verify credentials and user info with the server"
    )
    delay(1.seconds)

    return login(
      username = username,
      password = password,
      loginEventMessagesChannel = eventMessagesChannel,
      isForTokenRefresh = true
    )

    /*
    val freshAuthToken: AuthToken = when (val loginResult = restApi.login(username, password)) {
      is NetworkResult.Success -> loginResult.value
      is NetworkResult.Failure -> {
        Log.w(
          TAG,
          "refreshAuthToken(): got HTTP code ${loginResult.statusCode}; user's password " +
            "doesn't seem to work, error is ${loginResult.errorValue}"
        )
        return false
      }
      is NetworkResult.NetworkException -> {
        Log.w(
          TAG,
          "refreshAuthToken(): got exception ${loginResult.cause.stackTraceToString()}"
        )
        return false
      }
    }

    Log.d(TAG, "refreshAuthToken(): successful refresh; inserting fresh token into store")
    appValuesStore.insertFreshAuthToken(freshAuthToken)

     */
  }

  suspend fun forceLockscreen() {
    Log.d(TAG, "forceLockscreen()")
    val currentAuthTime = appValuesStore.lastTimeAuthedFlow.first() ?: UnixTimestamp(0)
    val expiredTime: UnixTimestamp = currentAuthTime - AUTH_TIMEOUT
    appValuesStore.setLastTimeAuthenticated(expiredTime)
  }

  suspend fun logout() {
    Log.d(TAG, "logout()")
    withContext(dispatchers.io) {
      syncRepository.apply {
        cancelAllSyncWork()
        pruneAllWork()
      }

      dbWrapper.database?.clearAllTables()
      // This will clear the auth token, which will signal to the app state flow above that
      // we are logged out.
      appValuesStore.clearAllDataExceptVersionsAndOverrides()
    }
  }

  companion object {
    private const val TAG = "AuthRepository"
  }
}
