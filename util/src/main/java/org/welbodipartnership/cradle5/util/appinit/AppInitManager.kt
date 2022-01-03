package org.welbodipartnership.cradle5.util.appinit

import android.app.Application
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class AppInitManager @Inject constructor(
  private val application: Application,
  /**
   * Set of initialization tasks. This is provided by `@IntoSet` annotations
   * https://dagger.dev/dev-guide/multibindings.html
   */
  private val initializationTasks: Set<@JvmSuppressWildcards AppInitTask>,
  private val coroutineDispatchers: AppCoroutineDispatchers
) {
  sealed class AppState {
    object Ready : AppState()
    object Initializing : AppState()
    class FailedToInitialize(
      val classOfTaskThatFailed: KClass<out AppInitTask>,
      val cause: Exception
    ) : AppState()
  }

  private val _appState: MutableStateFlow<AppState> = MutableStateFlow(AppState.Initializing)
  val appStateFlow: StateFlow<AppState> = _appState

  private var initMutex: Mutex? = Mutex()

  suspend fun init() {
    val mutex = initMutex
    if (mutex == null) {
      Log.w(TAG, "Already initialized!")
      return
    }
    mutex.withLock {
      if (initMutex == null) {
        Log.w(TAG, "Already initialized!")
        return@withLock
      }

      // Sort tasks by their name so that there is some deterministic order.
      val sortedTasks = withContext(coroutineDispatchers.default) {
        initializationTasks.sortedBy { it::class.java.simpleName }
      }
      Log.d(
        TAG,
        "Tasks for app initialization: ${
          sortedTasks.joinToString { it::class.java.canonicalName ?: it.toString() }
        }"
      )

      for (task in sortedTasks) {
        try {
          coroutineScope { task.init(application) }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to initialize ${task::class.java.canonicalName}", e)
          _appState.value = AppState.FailedToInitialize(
            classOfTaskThatFailed = task::class,
            cause = e
          )
          return
        }
      }

      Log.w(TAG, "Done initializing!")
      _appState.value = AppState.Ready
      initMutex = null
    }
  }

  companion object {
    private const val TAG = "AppInitializerManager"
  }
}