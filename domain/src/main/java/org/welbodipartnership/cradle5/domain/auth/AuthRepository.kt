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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.welbodipartnership.api.ApiAuthToken
import org.welbodipartnership.api.cradle5.HealthcareFacilityLookupEntry
import org.welbodipartnership.api.cradle5.HealthcareFacilitySummary
import org.welbodipartnership.cradle5.data.cryptography.PasswordHasher
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.data.settings.AuthToken
import org.welbodipartnership.cradle5.data.settings.PasswordHash
import org.welbodipartnership.cradle5.domain.ControlId
import org.welbodipartnership.cradle5.domain.FormId
import org.welbodipartnership.cradle5.domain.NetworkResult
import org.welbodipartnership.cradle5.domain.ObjectId
import org.welbodipartnership.cradle5.domain.RestApi
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
 * The tokens expire in 14 days
 */
private val AUTO_REFRESH_THRESHOLD: Duration = 5.days

@Singleton
class AuthRepository @Inject internal constructor(
  private val restApi: RestApi,
  private val appValuesStore: AppValuesStore,
  private val passwordHasher: PasswordHasher,
  private val networkObserver: NetworkObserver,
  private val dbWrapper: CradleDatabaseWrapper,
  private val dispatchers: AppCoroutineDispatchers,
  @ApplicationContext private val context: Context,
  @ApplicationCoroutineScope applicationCoroutineScope: CoroutineScope,
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
  ) { _, authToken, lastTimeAuthed ->
    if (authToken == null || !authToken.isInitialized || authToken == AuthToken.getDefaultInstance()) {
      AuthState.LoggedOut
    } else {
      val username = authToken.username
      // fall back to 0 to always force authentication if there is last authed time for some reason
      val lastTimeAuthedForComparison = lastTimeAuthed ?: UnixTimestamp(0)
      val expiryTime: UnixTimestamp = UnixTimestamp
        .fromDateTimeString(authToken.expires, ApiAuthToken.dateTimeFormatter)
      val now = UnixTimestamp.now()

      Log.d(TAG, "duration between: ${lastTimeAuthedForComparison durationBetween now}")
      when {
        now >= expiryTime -> AuthState.TokenExpired(username)
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
    class Invalid(val errorMessage: String?, val errorCode: Int) : LoginResult()
    class Exception(val errorMessage: String?) : LoginResult()
  }

  suspend fun login(
    username: String,
    password: String,
    loginEventMessagesChannel: SendChannel<String>?
  ): LoginResult {
    /*
    if (!networkObserver.isNetworkAvailable()) {
      Log.d(TAG, "login(): network not available")
      return LoginResult.Exception(context.getString(R.string.login_error_no_network))
    }
     */

    try {
      loginEventMessagesChannel?.trySend("Submitting credentials to server")
      val token: AuthToken = when (val loginResult = restApi.login(username, password)) {
        is NetworkResult.Success -> loginResult.value
        is NetworkResult.Failure -> {
          return LoginResult.Invalid(
            loginResult.errorValue?.errorDescription,
            loginResult.statusCode
          )
        }
        is NetworkResult.NetworkException -> {
          return LoginResult.Exception(loginResult.formatErrorMessage(context))
        }
      }
      Log.d(TAG, "login(): success")
      loginEventMessagesChannel?.trySend("Setting up lockscreen")
      val hash = passwordHasher.hashPassword(password)
      appValuesStore.insertLoginDetails(authToken = token, hash)

      // Try to get the userId from the index menu items
      loginEventMessagesChannel?.trySend("Getting user id")
      when (val indexResult = restApi.getIndexEntries()) {
        is NetworkResult.Success -> {
          val userDataItem = indexResult.value.asReversed().find { it.title == "User data" }
          if (userDataItem != null) {
            userDataItem.url.substringAfterLast('/', "")
              .toIntOrNull()
              ?.let { userId -> appValuesStore.insertUserId(userId) }
              ?: loginEventMessagesChannel?.trySend(
                "Unable to get userId: ${userDataItem.url} doesn't end in a number"
              )
          } else {
            loginEventMessagesChannel?.trySend(
              "Unable to get userId: Missing user data index tab"
            )
            delay(800L)
          }
        }
        is NetworkResult.Failure -> {
          val message = indexResult.errorValue.decodeToString()
          loginEventMessagesChannel?.trySend(
            "Unable to get userId: HTTP ${indexResult.statusCode} error (message: $message)"
          )
          delay(800L)
        }
        is NetworkResult.NetworkException -> {
          loginEventMessagesChannel?.trySend(
            "Unable to get userId: ${indexResult.formatErrorMessage(context)}"
          )
          delay(800L)
        }
      }

      // Try to get the user's district
      loginEventMessagesChannel?.trySend("Getting user district")
      when (
        val result = restApi.getFormData<HealthcareFacilitySummary>(FormId(113), ObjectId.QUERIES)
      ) {
        is NetworkResult.Success -> appValuesStore.setDistrictName(result.value.districtName)
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

      // Try to get the user's district
      loginEventMessagesChannel?.trySend("Getting facilities")
      when (
        val result = restApi
          .getDynamicLookupData<HealthcareFacilityLookupEntry>(
            ControlId("Control2092"),
            FormId(63),
            ObjectId.QUERIES
          )
      ) {
        is NetworkResult.Success -> {
          dbWrapper.withTransaction { db ->
            loginEventMessagesChannel?.trySend("Inserting ${result.value.totalNumberOfRecords} facilities")
            val dao = db.facilitiesDao()
            result.value.results.forEach { dao.upsert(Facility(it.id, it.name)) }
          }
        }
        is NetworkResult.Failure -> {
          val message = result.errorValue.decodeToString()
          return LoginResult.Invalid(
            "Unable to get facilities: HTTP ${result.statusCode} error (message: $message)",
            result.statusCode
          )
        }
        is NetworkResult.NetworkException -> {
          return LoginResult.Exception(result.formatErrorMessage(context))
        }
      }

      return LoginResult.Success
    } finally {
      loginEventMessagesChannel?.close()
    }
  }

  suspend fun reauthForLockscreen(password: String): Boolean {
    val existingHash: PasswordHash? = appValuesStore.passwordHashFlow.firstOrNull()
    if (existingHash == null) {
      Log.w(TAG, "reauth(): trying to reauthenticate, but there is no stored hash")
      return false
    }

    val correctHash = passwordHasher.verifyPassword(password, existingHash)
    Log.d(TAG, "reauthForLockscreen() -> $correctHash")
    if (correctHash) {
      appValuesStore.setLastTimeAuthenticatedToNow()
      Log.d(TAG, "doing opportunistic token refresh")
      withTimeoutOrNull(10.seconds) {
        refreshAuthToken(password)
      }
    }

    return correctHash
  }

  /**
   * Refreshes the auth token. Since the API doesn't offer any refresh tokens, we require
   * the user's password.
   */
  suspend fun refreshAuthToken(password: String, skipTimeChecks: Boolean = false): Boolean {
    Log.d(TAG, "refreshAuthToken(): skipTimeChecks = $skipTimeChecks")
    // try to login again
    val token = appValuesStore.authTokenFlow.firstOrNull()
    val username = token?.username
    if (token == null || username.isNullOrBlank()) {
      Log.w(TAG, "refreshAuthToken(): trying to reauthenticate when there is no token")
      return false
    } else {
      val expires = UnixTimestamp
        .fromDateTimeString(token.expires, ApiAuthToken.dateTimeFormatter)
      val now = UnixTimestamp.now()
      if (!skipTimeChecks) {
        if (now >= expires) {
          Log.w(TAG, "refreshAuthToken(): token already expired")
          return false
        }

        val durationUntilExpiry = now durationBetween expires
        if (durationUntilExpiry > AUTO_REFRESH_THRESHOLD) {
          Log.d(TAG, "refreshAuthToken(): $durationUntilExpiry until token expires; ignoring")
          return false
        }
      }
    }

    if (!networkObserver.isNetworkAvailable()) {
      Log.d(TAG, "refreshAuthToken(): network not available")
      return false
    }

    yield()

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
    return true
  }

  suspend fun forceLockscreen() {
    Log.d(TAG, "forceLockscreen()")
    val currentAuthTime = appValuesStore.lastTimeAuthedFlow.first() ?: UnixTimestamp(0)
    val expiredTime: UnixTimestamp = currentAuthTime + (AUTH_TIMEOUT * 2)
    appValuesStore.setLastTimeAuthenticated(expiredTime)
  }

  suspend fun logout() {
    withContext(dispatchers.io) {
      appValuesStore.clearAllDataExceptEnums()
      dbWrapper.database?.clearAllTables()
    }
  }

  companion object {
    private const val TAG = "AuthRepository"
  }
}
