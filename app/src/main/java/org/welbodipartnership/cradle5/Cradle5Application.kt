package org.welbodipartnership.cradle5

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import org.welbodipartnership.cradle5.data.settings.EncryptedSettingsManager
import org.welbodipartnership.cradle5.util.appinit.AppInitManager
import javax.inject.Inject

@HiltAndroidApp
class Cradle5Application : Application() {

  @Inject
  lateinit var appInitManager: AppInitManager

  @Inject
  lateinit var encryptedSettingsManager: EncryptedSettingsManager

  override fun onCreate() {
    super.onCreate()

    runBlocking {
      appInitManager.init()
    }
  }
}
