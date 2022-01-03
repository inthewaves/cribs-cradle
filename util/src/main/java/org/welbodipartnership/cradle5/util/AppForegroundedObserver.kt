package org.welbodipartnership.cradle5.util

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppForegroundedObserver @Inject constructor() {
  private val isForegrounded = MutableStateFlow(false)
}