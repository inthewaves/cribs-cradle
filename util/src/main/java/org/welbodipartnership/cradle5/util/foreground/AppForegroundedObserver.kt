package org.welbodipartnership.cradle5.util.foreground

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppForegroundedObserver @Inject internal constructor(
  val appCoroutineDispatchers: AppCoroutineDispatchers
) {
  private val _isForegrounded = MutableStateFlow(false)
  val isForegrounded: StateFlow<Boolean> = _isForegrounded

  internal suspend fun setup() {
    withContext(appCoroutineDispatchers.main.immediate) {
      ProcessLifecycleOwner.get().lifecycle.addObserver(
        object : DefaultLifecycleObserver {
          override fun onStart(owner: LifecycleOwner) {
            _isForegrounded.value = true
            Log.d(TAG, "foregrounded")
          }

          override fun onStop(owner: LifecycleOwner) {
            _isForegrounded.value = false
            Log.d(TAG, "backgrounded")
          }
        }
      )
    }
  }

  companion object {
    private const val TAG = "AppForegroundedObserver"
  }
}