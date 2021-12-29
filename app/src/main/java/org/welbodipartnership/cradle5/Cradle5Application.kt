package org.welbodipartnership.cradle5

import android.app.Application
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Cradle5Application : Application() {
  override fun onCreate() {
    super.onCreate()

    StreamingAeadConfig.register()
  }
}