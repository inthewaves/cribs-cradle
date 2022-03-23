package org.welbodipartnership.cradle5

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import org.welbodipartnership.cradle5.appmigrations.AppMigrations
import org.welbodipartnership.cradle5.util.appinit.AppInitManager
import javax.inject.Inject

@HiltAndroidApp
class Cradle5Application : Application(), Configuration.Provider {

  @Inject
  lateinit var appInitManager: AppInitManager

  @Inject
  lateinit var workerFactory: HiltWorkerFactory

  @Inject
  lateinit var appMigrations: AppMigrations

  override fun onCreate() {
    super.onCreate()

    runBlocking {
      appInitManager.init()
      appMigrations.runMigrations(this@Cradle5Application)
    }
  }

  override fun getWorkManagerConfiguration(): Configuration {
    Log.d("Cradle5Application", "getWorkManagerConfiguration")
    return Configuration.Builder()
      .setWorkerFactory(workerFactory)
      .build()
  }
}
