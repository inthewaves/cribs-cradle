package org.welbodipartnership.cradle5

import android.app.Application
import android.app.NotificationManager
import android.app.job.JobInfo
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.acra.ACRA
import org.acra.config.httpSender
import org.acra.config.limiter
import org.acra.config.notification
import org.acra.config.scheduler
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import org.welbodipartnership.cradle5.appmigrations.AppMigrations
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.util.ApplicationCoroutineScope
import org.welbodipartnership.cradle5.util.appinit.AppInitManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class Cradle5Application : Application(), Configuration.Provider {

  @Inject @ApplicationCoroutineScope
  lateinit var appCoroutineScope: CoroutineScope

  @Inject
  lateinit var appInitManager: AppInitManager

  @Inject
  lateinit var workerFactory: HiltWorkerFactory

  @Inject
  lateinit var appMigrations: AppMigrations

  @Inject
  lateinit var authRepository: AuthRepository

  override fun attachBaseContext(base: Context?) {
    super.attachBaseContext(base)

    initAcra {
      buildConfigClass = BuildConfig::class.java
      reportFormat = StringFormat.JSON

      // alsoReportToAndroidFramework = true
      notification {
        //required
        title = getString(R.string.error_notification_title__s_has_encountered_an_error, getString(R.string.app_name))
        //required
        text = getString(R.string.error_notification_body)
        //required
        channelName = getString(R.string.error_notification_channel_name)
        //optional channel description
        channelDescription = getString(R.string.error_notification_channel_description)
        //defaults to NotificationManager.IMPORTANCE_HIGH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          channelImportance = NotificationManager.IMPORTANCE_HIGH
        }
        resIcon = android.R.drawable.stat_sys_warning
        //defaults to android.R.string.ok
        sendButtonText = getString(R.string.error_notification_send_button)
        //defaults to false
        sendOnClick = false
        this.enabled = true
      }
      httpSender {
        uri = BuildConfig.DEFAULT_ACRA_URL
        basicAuthLogin = BuildConfig.ACRA_USER
        // Note: This app is open source (and if it wasn't, this can be found by reverse engineering)
        basicAuthPassword = BuildConfig.ACRA_PASS
        httpMethod = HttpSender.Method.POST
        compress = true
      }
      scheduler {
        enabled = true
        restartAfterCrash = false
        requiresNetworkType = JobInfo.NETWORK_TYPE_ANY
      }
      limiter {
        enabled = true
        period = 30
        periodUnit = TimeUnit.MINUTES
        exceptionClassLimit = 30
        ignoredCrashToast = getString(R.string.s_encountered_error_toast)
      }
    }
  }

  override fun onCreate() {
    super.onCreate()

    // https://github.com/ACRA/acra/issues/855#issuecomment-855434695
    // "You need this if(ACRA.isACRASenderServiceProcess()) return; before any non-acra
    // configuration in both attachBaseContext and onCreate. acra configuration should be in
    // attachBaseContext."
    if (ACRA.isACRASenderServiceProcess()) {
      Log.w("Cradle5Application", "Bailing out of onCreate due to isACRASenderServiceProcess")
      return
    }

    runBlocking {
      appInitManager.init()
      appMigrations.runMigrations(this@Cradle5Application)
    }

    appCoroutineScope.launch {
      authRepository.authStateFlow.collect { authState ->
        // this will include username
        ACRA.errorReporter.putCustomData("authstate", authState.toString())
      }
    }
  }

  override fun getWorkManagerConfiguration(): Configuration {
    Log.d("Cradle5Application", "getWorkManagerConfiguration")
    return Configuration.Builder()
      .setWorkerFactory(workerFactory)
      .build()
  }
}
