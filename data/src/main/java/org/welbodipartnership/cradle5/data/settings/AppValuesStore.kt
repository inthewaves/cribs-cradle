package org.welbodipartnership.cradle5.data.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.util.ApplicationCoroutineScope
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppValuesStore @Inject internal constructor(
  private val encryptedSettings: EncryptedSettingsManager,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  @ApplicationCoroutineScope private val applicationCoroutineScope: CoroutineScope,
) {

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

  val syncIdFlow: Flow<UUID?> = encryptedSettings.encryptedSettingsFlow()
    .map { settings ->
      settings.currentSyncId
        .takeIf { settings.hasCurrentSyncId() }
        ?.let { UUID.fromString(it) }
    }
    .distinctUntilChanged()
    .conflate()

  val lastSyncCompletedTimestamp: Flow<UnixTimestamp?> = encryptedSettings.encryptedSettingsFlow()
    .map { settings ->
      settings.lastSyncCompletedTimestamp
        .takeIf { settings.hasLastSyncCompletedTimestamp() }
        ?.let { UnixTimestamp(it) }
    }
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

  suspend fun clearAllData() {
    encryptedSettings.updateData {
      EncryptedSettings.getDefaultInstance()
    }
  }

  val serverEnumCollection: StateFlow<ServerEnumCollection> = encryptedSettings
    .encryptedSettingsFlow()
    .map { settings ->
      settings.enumsList
        .ifEmpty { null }
        ?.let { ServerEnumCollection(it) }
        ?: ServerEnumCollection.defaultInstance
    }
    .stateIn(
      applicationCoroutineScope,
      SharingStarted.WhileSubscribed(),
      ServerEnumCollection.defaultInstance
    )

  suspend fun insertSyncUuid(workId: UUID) {
    encryptedSettings.updateData { settings ->
      settings.toBuilder().setCurrentSyncId(workId.toString()).build()
    }
  }

  suspend fun setLastTimeSyncCompletedToNow() {
    encryptedSettings.updateData { settings ->
      settings.toBuilder().setLastSyncCompletedTimestamp(UnixTimestamp.now().timestamp).build()
    }
  }

  suspend fun replaceEnums(enums: Iterable<DynamicServerEnum>) {
    encryptedSettings.updateData { settings ->
      settings.toBuilder()
        .clearEnums()
        .addAllEnums(enums)
        .build()
    }
  }
}
