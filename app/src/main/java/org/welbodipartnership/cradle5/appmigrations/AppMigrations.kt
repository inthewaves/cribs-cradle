package org.welbodipartnership.cradle5.appmigrations

import android.app.Application
import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.welbodipartnership.cradle5.data.cryptography.ArgonHasher
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import javax.inject.Inject

class AppMigrations @Inject internal constructor(
  private val appValuesStore: AppValuesStore,
  private val hasher: ArgonHasher,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {
  companion object {
    private const val TAG = "AppMigrations"

    const val CURRENT_VERSION = 3
  }

  private object Version {
    const val FACILITY_LIST_FULL_DOWNLOAD_AND_DISTRICTS = 1
    const val NEW_OTHER_DISTRICT = 2
    const val USERNAME_HASH = 3
  }

  suspend fun runMigrations(application: Application) {
    val lastSeenVersion = appValuesStore.lastAppVersionFlow.firstOrNull() ?: 0
    Log.d(TAG, "lastSeenVersion = $lastSeenVersion, current = $CURRENT_VERSION")

    if (lastSeenVersion < Version.FACILITY_LIST_FULL_DOWNLOAD_AND_DISTRICTS) {
      if (appValuesStore.authTokenFlow.firstOrNull() != null) {
        Log.d(TAG, "Forcing reauth for FACILITY_LIST_FULL_DOWNLOAD_AND_DISTRICTS")
        appValuesStore.setWarningMessage("You will need internet to reconnect in order to download all facilities and districts.")
        appValuesStore.setForceReauth(true)
      }
    }

    if (lastSeenVersion < Version.NEW_OTHER_DISTRICT) {
      if (appValuesStore.authTokenFlow.firstOrNull() != null) {
        Log.d(TAG, "Forcing reauth for NEW_OTHER_DISTRICT")
        appValuesStore.setWarningMessage("You will need internet to reconnect in order to update district information.")
        appValuesStore.setForceReauth(true)
      }
    }

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

    appValuesStore.setLastAppMigrationVersion(CURRENT_VERSION)
  }
}
