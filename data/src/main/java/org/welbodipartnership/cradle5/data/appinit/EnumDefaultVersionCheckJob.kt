package org.welbodipartnership.cradle5.data.appinit

import android.app.Application
import android.util.Log
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.data.settings.EncryptedSettingsManager
import org.welbodipartnership.cradle5.util.appinit.AppInitTask
import javax.inject.Inject

class EnumDefaultVersionCheckJob @Inject internal constructor(
  private val encryptedSettingsManager: EncryptedSettingsManager
) : AppInitTask {
  override val order: ULong
    get() = 2u

  override suspend fun init(application: Application) {
    encryptedSettingsManager.updateData { settings ->
      val enumCount = settings.enumsList.size
      Log.d(TAG, "Current stored enum count: $enumCount")
      if (enumCount > 0) {
        Log.d(
          TAG,
          if (settings.hasDefaultDropdownVersion()) {
            "Stored default enum settings version: ${settings.defaultDropdownVersion}"
          } else {
            "No stored default enum settings version"
          }
        )
        Log.d(
          TAG, "Current default enum settings version: ${ServerEnumCollection.DROPDOWN_VERSION}"
        )

        if (
          !settings.hasDefaultDropdownVersion() ||
          settings.defaultDropdownVersion < ServerEnumCollection.DROPDOWN_VERSION
        ) {

          Log.d(
            TAG,
            "Migrating enums to version ${ServerEnumCollection.DROPDOWN_VERSION} by " +
              "deleting stored enums and setting stored default enum version to current"
          )

          settings.toBuilder()
            .clearEnums()
            .setDefaultDropdownVersion(ServerEnumCollection.DROPDOWN_VERSION)
            .build()
        } else {
          Log.d(TAG, "Skipping migration; already current default version.")
          settings
        }
      } else {
        Log.d(TAG, "Skipping migration; no enums are stored.")
        settings
      }
    }
  }

  companion object {
    private const val TAG = "EnumDefaultVersionCheckJob"
  }
}
