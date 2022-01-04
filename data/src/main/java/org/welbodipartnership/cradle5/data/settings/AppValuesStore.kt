package org.welbodipartnership.cradle5.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppValuesStore @Inject internal constructor(
  private val encryptedSettings: EncryptedSettingsManager
) {
  val encryptedSettingsFlow: Flow<EncryptedSettings> = encryptedSettings.encryptedSettingsFlow()

  val loginCompleteFlow: Flow<Boolean> = encryptedSettings.encryptedSettingsFlow()
    .map { settings ->
      settings.isLoginComplete.takeIf { settings.hasIsLoginComplete() }
        ?: false
    }
    .distinctUntilChanged()
    .conflate()

  val authTokenFlow: Flow<AuthToken?> = encryptedSettings.encryptedSettingsFlow()
    .map { settings -> settings.token.takeIf { settings.hasToken() } }
    .distinctUntilChanged()
    .conflate()

  val lastTimeAuthedFlow: Flow<UnixTimestamp?> = encryptedSettings.encryptedSettingsFlow()
    .map { settings ->
      settings.lastTimeAuthenticated
        .takeIf { settings.hasLastTimeAuthenticated() }
        ?.let { UnixTimestamp(it) }
    }
    .distinctUntilChanged()
    .conflate()

  val passwordHashFlow: Flow<PasswordHash?> = encryptedSettings.encryptedSettingsFlow()
    .map { settings -> settings.passwordHash.takeIf { settings.hasPasswordHash() } }
    .distinctUntilChanged()
    .conflate()

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

  suspend fun markLoginComplete() {
    encryptedSettings.updateData { settings ->
      settings.toBuilder()
        .setIsLoginComplete(true)
        .build()
    }
  }

  suspend fun insertUserId(userId: Int) {
    encryptedSettings.updateData { settings ->
      settings.toBuilder()
        .setUserInfo(
          settings.userInfo.toBuilder()
            .setUserId(userId)
            .build()
        )
        .build()
    }
  }

  suspend fun setDistrictName(districtName: String) {
    encryptedSettings.updateData { settings ->
      settings.toBuilder()
        .setUserInfo(
          settings.userInfo.toBuilder()
            .setDistrictName(districtName)
            .build()
        )
        .build()
    }
  }

  suspend fun setLastTimeAuthenticatedToNow() {
    setLastTimeAuthenticated(UnixTimestamp.now())
  }

  suspend fun setLastTimeAuthenticated(newTimestamp: UnixTimestamp) {
    encryptedSettings.updateData { settings ->
      settings.toBuilder().setLastTimeAuthenticated(newTimestamp.timestamp).build()
    }
  }

  suspend fun insertFreshAuthToken(newerAuthToken: AuthToken) {
    encryptedSettings.updateData { settings ->
      settings.toBuilder()
        .setToken(newerAuthToken)
        .build()
    }
  }

  suspend fun clearAllDataExceptEnums() {
    encryptedSettings.updateData { settings ->
      val preservedEnums = settings.enumsList
      encryptedSettings {
        enums.addAll(preservedEnums)
      }
    }
  }

  fun getServerEnumCollection() = ServerEnumCollection.defaultInstance
}
