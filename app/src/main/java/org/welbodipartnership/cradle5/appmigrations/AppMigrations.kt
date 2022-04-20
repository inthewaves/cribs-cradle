package org.welbodipartnership.cradle5.appmigrations

import android.app.Application
import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import javax.inject.Inject

class AppMigrations @Inject internal constructor(
  private val appValuesStore: AppValuesStore,
  private val authRepository: AuthRepository,
) {
  companion object {
    private const val TAG = "AppMigrations"

    const val CURRENT_VERSION = 2
  }

  private object Version {
    const val FACILITY_LIST_FULL_DOWNLOAD_AND_DISTRICTS = 1
    const val NEW_OTHER_DISTRICT = 2
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

    appValuesStore.setLastAppMigrationVersion(CURRENT_VERSION)
  }
}
