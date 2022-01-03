package org.welbodipartnership.cradle5.util.foreground

import android.app.Application
import org.welbodipartnership.cradle5.util.appinit.AppInitTask
import javax.inject.Inject

class AppForegroundedObserverSetupTask @Inject internal constructor(
  private val appForegroundedObserver: AppForegroundedObserver
) : AppInitTask {
  override val order: ULong = 0u

  override suspend fun init(application: Application) {
    appForegroundedObserver.setup()
  }
}