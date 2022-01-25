package org.welbodipartnership.cradle5.domain.auth

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.welbodipartnership.api.ApiAuthToken
import org.welbodipartnership.api.cradle5.HealthcareFacilitySummary
import org.welbodipartnership.cradle5.data.cryptography.PasswordHasher
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.data.settings.AuthToken
import org.welbodipartnership.cradle5.data.settings.PasswordHash
import org.welbodipartnership.cradle5.domain.NetworkResult
import org.welbodipartnership.cradle5.domain.ObjectId
import org.welbodipartnership.cradle5.domain.RestApi
import org.welbodipartnership.cradle5.domain.UrlProvider
import org.welbodipartnership.cradle5.domain.enums.EnumRepository
import org.welbodipartnership.cradle5.domain.facilities.FacilityRepository
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
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
  private val passwordHasher: PasswordHasher,
  private val networkObserver: NetworkObserver,
  private val dbWrapper: CradleDatabaseWrapper,
  private val dispatchers: AppCoroutineDispatchers,
  private val syncRepository: SyncRepository,
  private val facilityRepository: FacilityRepository,
  private val enumRepository: EnumRepository,
  private val urlProvider: UrlProvider,
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
    appForegroundedObserver.isForegrounded,
    appValuesStore.authTokenFlow,
    appValuesStore.lastTimeAuthedFlow,
    appValuesStore.loginCompleteFlow,
    appValuesStore.warningMessageFlow,
  ) { _, authToken, lastTimeAuthed, isLoginComplete, warningMessage ->

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
      }
      AuthState.LoggedOut
    } else {
      val username = authToken.username
      // fall back to 0 to always force authentication if there is no last authed time for some
      // reason
      val lastTimeAuthedForComparison = lastTimeAuthed ?: UnixTimestamp(0)
      val tokenExpiryTime: UnixTimestamp = UnixTimestamp
        .fromDateTimeString(authToken.expires, ApiAuthToken.dateTimeFormatter)
      val now = UnixTimestamp.now()

      when {
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

  sealed class LoginResult {
    object Success : LoginResult()

    /**
     * Server sent non-successful HTTP response
     */
    data class Invalid(
      val errorMessage: String?,
      val errorCode: Int?,
      val errorType: String? = null,
    ) : LoginResult() {
      val isFromBadCredentials: Boolean get() = errorCode == 400 && errorType == "invalid_grant"
    }
    data class Exception(val errorMessage: String?) : LoginResult()
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
            errorMessage = loginResult.errorValue?.errorDescription,
            errorCode = loginResult.statusCode,
            errorType = loginResult.errorValue?.error
          )
        }
        is NetworkResult.NetworkException -> {
          return LoginResult.Exception(loginResult.formatErrorMessage(context))
        }
      }
      Log.d(TAG, "login(): successfully obtained token")
      if (!isForTokenRefresh) {
        loginEventMessagesChannel?.trySend("Setting up lockscreen")
      }
      val hash = passwordHasher.hashPassword(password)
      // We have to insert the token here for RestApi to be able to authenticate.
      // Note: This function is also used for token refreshes, so the hash will be updated
      // redundantly?
      appValuesStore.insertLoginDetails(authToken = token, hash)

      // Try to get the userId from the index menu items. If we can't get it, we will fail,
      // because certain forms (like GPS coordinates) require us to input a userId.
      loginEventMessagesChannel?.trySend("Getting user information")
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
                loginEventMessagesChannel?.apply {
                  trySend(errorMessage)
                  delay(2.seconds)
                }
                return LoginResult.Exception(errorMessage)
              }
          } else {
            val errorMessage =
              "Unable to get userId: Missing user data index tab " +
                "(available tabs: ${indexResult.value})"
            loginEventMessagesChannel?.apply {
              trySend(errorMessage)
              delay(2.seconds)
            }
            return LoginResult.Exception(errorMessage)
          }
        }
        is NetworkResult.Failure -> {
          val message = indexResult.errorValue.decodeToString()
          val errorMessage = "Unable to get userId: HTTP ${indexResult.statusCode} error (message: $message)"
          loginEventMessagesChannel?.apply {
            trySend(errorMessage)
            delay(800L)
          }
          return LoginResult.Invalid(errorMessage, errorCode = indexResult.statusCode)
        }
        is NetworkResult.NetworkException -> {
          val errorMessage = "Unable to get userId: ${indexResult.formatErrorMessage(context)}"
          loginEventMessagesChannel?.trySend(errorMessage)
          delay(800L)
          return LoginResult.Exception(errorMessage)
        }
      }

      // Try to get the user's district
      loginEventMessagesChannel?.trySend("Getting user district")
      when (
        val result = restApi.getFormData<HealthcareFacilitySummary>(objectId = ObjectId.QUERIES)
      ) {
        is NetworkResult.Success -> {
          val districtName = result.value.districtName
          if (districtName != null) {
            appValuesStore.setDistrictName(districtName)
          } else {
            loginEventMessagesChannel?.trySend("User not associated with a district")
            delay(1.seconds)
          }
        }
        is NetworkResult.Failure -> {
          val message = result.errorValue.decodeToString()
          loginEventMessagesChannel?.trySend(
            "Unable to get district: HTTP ${result.statusCode} error (message: $message)"
          )
        }
        is NetworkResult.NetworkException -> {
          loginEventMessagesChannel?.trySend(
            "Unable to get district: ${result.formatErrorMessage(context)}"
          )
        }
      }

      // Try to get the facilities associated with this district
      loginEventMessagesChannel?.trySend("Getting facilities")
      when (val result = facilityRepository.downloadAndSaveFacilities(loginEventMessagesChannel)) {
        FacilityRepository.DownloadResult.Success -> {}
        is FacilityRepository.DownloadResult.Exception -> {
          return LoginResult.Exception(result.errorMessage)
        }
        is FacilityRepository.DownloadResult.Invalid -> {
          return LoginResult.Invalid(result.errorMessage, result.errorCode)
        }
      }

      loginEventMessagesChannel?.trySend("Getting dropdown values")
      when (val result = enumRepository.downloadAndSaveEnumsFromServer(loginEventMessagesChannel)) {
        EnumRepository.DownloadResult.Success -> {}
        is EnumRepository.DownloadResult.Exception -> {
          return LoginResult.Exception(result.errorMessage)
        }
        is EnumRepository.DownloadResult.Invalid -> {
          return LoginResult.Invalid(result.errorMessage, result.errorCode)
        }
      }

      return LoginResult.Success
    } finally {
      appValuesStore.markLoginComplete()
      loginEventMessagesChannel?.close()
    }
  }

  suspend fun reauthForLockscreen(
    password: String,
    forceServerRefresh: Boolean,
    eventMessagesChannel: SendChannel<String>?,
  ): LoginResult {
    val result = reauthForLockscreenInner(password, forceServerRefresh, eventMessagesChannel)
    if (result is LoginResult.Success) {
      Log.w(TAG, "reauth(): successful reauthentication; setting last auth time to now")
      appValuesStore.setLastTimeAuthenticatedToNow()
    }
    return result
  }

  private suspend fun reauthForLockscreenInner(
    password: String,
    forceServerRefresh: Boolean,
    eventMessagesChannel: SendChannel<String>?
  ): LoginResult {
    val existingHash: PasswordHash? = appValuesStore.passwordHashFlow.firstOrNull()
    if (existingHash == null) {
      Log.w(TAG, "reauth(): trying to reauthenticate, but there is no stored hash")
      return LoginResult.Exception("Missing existing login details")
    }

    eventMessagesChannel?.trySend("Verifying password")
    val isLocalPasswordMatch: Boolean = passwordHasher.verifyPassword(password, existingHash)
    Log.d(
      TAG,
      "reauthForLockscreen(forceServerRefresh = $forceServerRefresh) -> $isLocalPasswordMatch"
    )
    if (isLocalPasswordMatch || forceServerRefresh) {
      if (forceServerRefresh) {
        Log.d(TAG, "doing forced token refresh")
        return refreshAuthTokenAndUserInfoAndFacilities(
          password = password,
          skipTimeChecks = true,
          isLocalPasswordIncorrect = !isLocalPasswordMatch,
          eventMessagesChannel = eventMessagesChannel,
        )
      } else {
        Log.d(TAG, "doing opportunistic token refresh")

        val result = refreshAuthTokenAndUserInfoAndFacilities(
          password = password,
          skipTimeChecks = false,
          isLocalPasswordIncorrect = !isLocalPasswordMatch,
          eventMessagesChannel = eventMessagesChannel,
        )
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
      LoginResult.Invalid("Invalid password", errorCode = null)
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
      return LoginResult.Exception("No authentication token present")
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
      appValuesStore.clearAllData()
    }
  }

  companion object {
    private const val TAG = "AuthRepository"
  }
}
