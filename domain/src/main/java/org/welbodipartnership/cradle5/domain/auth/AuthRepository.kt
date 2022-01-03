package org.welbodipartnership.cradle5.domain.auth

import android.util.Log
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import org.welbodipartnership.api.ApiAuthToken
import org.welbodipartnership.cradle5.data.cryptography.PasswordHasher
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.data.settings.AuthToken
import org.welbodipartnership.cradle5.data.settings.PasswordHash
import org.welbodipartnership.cradle5.domain.NetworkResult
import org.welbodipartnership.cradle5.domain.RestApi
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp
import org.welbodipartnership.cradle5.util.foreground.AppForegroundedObserver
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

private val AUTH_TIMEOUT: Duration = 1.days

@Singleton
class AuthRepository @Inject internal constructor(
  private val restApi: RestApi,
  private val appValuesStore: AppValuesStore,
  private val passwordHasher: PasswordHasher,
  appForegroundedObserver: AppForegroundedObserver
) {
  val authStateFlow = combine(
    // Put this here so that it is
    appForegroundedObserver.isForegrounded,
    appValuesStore.authTokenFlow,
    appValuesStore.lastTimeAuthedFlow
  ) { _, authToken: AuthToken?, lastTimeAuthed: UnixTimestamp? ->
    if (authToken == null) {
      AuthState.LoggedOut
    } else {
      val username = authToken.username
      // fall back to 0 to always force authentication if there is last authed time for some reason
      val lastTimeAuthedForComparison = lastTimeAuthed ?: UnixTimestamp(0)
      val expiryTime: UnixTimestamp = UnixTimestamp
        .fromDateTimeString(authToken.expires, ApiAuthToken.dateTimeFormatter)
      val now = UnixTimestamp.now()

      when {
        now >= expiryTime -> AuthState.TokenExpired(username)
        lastTimeAuthedForComparison durationBetween now >= AUTH_TIMEOUT -> {
          AuthState.LoggedInLocked(username)
        }
        else -> AuthState.LoggedInUnlocked(username)
      }
    }
  }

  sealed class LoginResult {
    object Success : LoginResult()
    class Invalid(val errorMessage: String?, val errorCode: Int) : LoginResult()
    class Exception(val errorMessage: String?) : LoginResult()
  }

  suspend fun login(username: String, password: String): LoginResult {
    val token: AuthToken = when (val loginResult = restApi.login(username, password)) {
      is NetworkResult.Success -> loginResult.value
      is NetworkResult.Failure -> {
        return LoginResult.Invalid(
          loginResult.errorValue?.errorDescription,
          loginResult.statusCode
        )
      }
      is NetworkResult.NetworkException -> {
        return LoginResult.Exception(
          "(${loginResult.cause::class.java.simpleName}) ${loginResult.cause.localizedMessage}"
        )
      }
    }
    val hash = passwordHasher.hashPassword(password)
    appValuesStore.insertLoginDetails(authToken = token, hash)
    return LoginResult.Success
  }

  suspend fun reauthForLockscreen(password: String): Boolean {
    val existingHash: PasswordHash? = appValuesStore.passwordHashFlow.firstOrNull()
    if (existingHash == null) {
      Log.w(TAG, "trying to reauthenticate, but there is no stored hash")
      return false
    }

    val correctHash = passwordHasher.verifyPassword(password, existingHash)
    Log.d(TAG, "reauthForLockscreen() -> $correctHash")
    if (correctHash) {
      appValuesStore.
    }

    return correctHash

  }

  suspend fun logout() {

  }

  companion object {
    private const val TAG = "AuthRepository"
  }
}
