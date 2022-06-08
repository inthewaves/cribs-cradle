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

    const val CURRENT_VERSION = 0
  }

  private object Version {
    // const val SOMETHING = 1
  }

  suspend fun runMigrations(application: Application) {
    val lastSeenVersion = appValuesStore.lastAppVersionFlow.firstOrNull() ?: 0
    Log.d(TAG, "lastSeenVersion = $lastSeenVersion, current = $CURRENT_VERSION")

    // if (lastSeenVersion < Version.SOMETHING) {
    // }
    appValuesStore.setLastAppMigrationVersion(CURRENT_VERSION)
  }
}
