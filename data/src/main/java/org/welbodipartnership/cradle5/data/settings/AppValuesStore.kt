package org.welbodipartnership.cradle5.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppValuesStore @Inject internal constructor(
  private val encryptedSettings: EncryptedSettingsManager
) {
  val authTokenFlow: Flow<AuthToken?> = encryptedSettings.dateStoreFlow()
    .map { settings -> settings.token.takeIf { settings.hasToken() } }

  val lastTimeAuthedFlow: Flow<UnixTimestamp?> = encryptedSettings.dateStoreFlow()
    .map { settings ->
      settings.lastTimeAuthenticated
        .takeIf { settings.hasLastTimeAuthenticated() }
        ?.let { UnixTimestamp(it) }
    }

  /**
   * When login is successful, the server returns a [authToken].
   *
   * This information should be stored in the app so that the token can be used for authentication
   * to the server and that the hashed password can be used for a lockscreen to the app.
   *
   * [hashOfSuccessfulPassword] is the hash of the password used to correctly logged
   */
  suspend fun insertLoginDetails(authToken: AuthToken, hashOfSuccessfulPassword: PasswordHash) {
    encryptedSettings.updateData { settings ->
      settings.toBuilder()
        .setToken(authToken)
        .setLastTimeAuthenticated(UnixTimestamp.now().timestamp)
        .setPasswordHash(hashOfSuccessfulPassword)
        .build()
    }
  }

  suspend fun insertFreshAuthToken(newerAuthToken: AuthToken) {
    encryptedSettings.updateData { settings ->
      settings.toBuilder()
        .setToken(newerAuthToken)
        .build()
    }
  }

  fun getServerEnumCollection() = ServerEnumCollection.defaultInstance
}
