package org.welbodipartnership.cradle5.util.appinit

import android.app.Application

interface AppInitTask {
  suspend fun init(application: Application)
}