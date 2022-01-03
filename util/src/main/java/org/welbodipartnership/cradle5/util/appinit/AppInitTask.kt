package org.welbodipartnership.cradle5.util.appinit

import android.app.Application

interface AppInitTask {
  /**
   * Tasks will be run in increasing order of this [order] property.
   */
  val order: ULong

  suspend fun init(application: Application)
}