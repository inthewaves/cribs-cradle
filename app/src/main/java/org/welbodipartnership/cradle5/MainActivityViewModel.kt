package org.welbodipartnership.cradle5

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.auth.AuthState
import org.welbodipartnership.cradle5.util.ApplicationCoroutineScope
import org.welbodipartnership.cradle5.util.appinit.AppInitManager
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
  appInitManager: AppInitManager,
  private val authRepository: AuthRepository,
  private val appValuesStore: AppValuesStore,
  @ApplicationCoroutineScope private val applicationCoroutineScope: CoroutineScope
) : ViewModel() {

  val appState: StateFlow<AppInitManager.AppState> = appInitManager.appStateFlow

  val authState: Flow<AuthState> = authRepository.authStateFlow

  val serverEnumCollection: StateFlow<ServerEnumCollection> = appValuesStore.serverEnumCollection

  val districtName: Flow<String?> = appValuesStore.districtNameFlow

  fun clearWarningMessage() {
    applicationCoroutineScope.launch { appValuesStore.clearWarningMessage() }
  }

  fun logout() {
    applicationCoroutineScope.launch {
      authRepository.logout()
    }
  }

  fun forceLockScreen() {
    applicationCoroutineScope.launch {
      authRepository.forceLockscreen()
    }
  }

  override fun onCleared() {
    super.onCleared()
    Log.d(TAG, "onCleared()")
  }

  companion object {
    private const val TAG = "MainActivityViewModel"
  }
}
