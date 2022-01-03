package org.welbodipartnership.cradle5

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.auth.AuthState
import org.welbodipartnership.cradle5.util.appinit.AppInitManager
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
  appInitManager: AppInitManager,
  authRepository: AuthRepository,
) : ViewModel() {

  val appState: StateFlow<AppInitManager.AppState> = appInitManager.appStateFlow

  val authState: Flow<AuthState> = authRepository.authStateFlow

  override fun onCleared() {
    super.onCleared()
    Log.d(TAG, "onCleared()")
  }

  companion object {
    private const val TAG = "MainActivityViewModel"
  }
}
