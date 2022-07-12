package org.welbodipartnership.cradle5.appmigrations

import android.app.Application
import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.welbodipartnership.cradle5.data.cryptography.ArgonHasher
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import javax.inject.Inject

class AppMigrations @Inject internal constructor(
  private val appValuesStore: AppValuesStore,
  private val hasher: ArgonHasher,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  private val syncRepository: SyncRepository,
) {
  companion object {
    private const val TAG = "AppMigrations"

    const val CURRENT_VERSION = 2
  }

  private object Version {
    const val USERNAME_HASH = 1
    const val OTHER_FACILITY_SYNC = 2
  }

  suspend fun runMigrations(application: Application) {
    val lastSeenVersion = appValuesStore.lastAppVersionFlow.firstOrNull() ?: 0
    Log.d(TAG, "lastSeenVersion = $lastSeenVersion, current = $CURRENT_VERSION")

    if (lastSeenVersion < Version.USERNAME_HASH) {
      val authToken = appValuesStore.authTokenFlow.firstOrNull()
      val passwordHash = appValuesStore.passwordHashFlow.firstOrNull()
      if (authToken != null && passwordHash != null) {
        Log.d(TAG, "inserting hash of username")
        appValuesStore.insertLoginDetails(
          authToken,
          hashOfSuccessfulPassword = passwordHash,
          hashOfUsername = withContext(appCoroutineDispatchers.default) {
            hasher.hash(forPassword = false, authToken.username)
          }
        )
      }
    }
    if (lastSeenVersion < Version.OTHER_FACILITY_SYNC) {
      syncRepository.enqueueDownloadSyncJob()
    }

    appValuesStore.setLastAppMigrationVersion(CURRENT_VERSION)
  }
}
