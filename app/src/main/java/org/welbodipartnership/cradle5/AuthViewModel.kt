package org.welbodipartnership.cradle5

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.auth.AuthState
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val authRepository: AuthRepository,
) : ViewModel() {

  sealed class ScreenState {
    object Initializing : ScreenState()
    object Done : ScreenState()
    class WaitingForTokenRefreshLogin(val errorMessage: String?) : ScreenState()
    class WaitingForLogin(val errorMessage: String?) : ScreenState()
    class WaitingForReauth(val errorMessage: String?) : ScreenState()
    object Submitting : ScreenState()
  }

  private sealed class SubmissionState {
    object Submitting : SubmissionState()
    class Waiting(val errorMessage: String?) : SubmissionState()
  }

  private val submissionState: MutableStateFlow<SubmissionState> =
    MutableStateFlow(SubmissionState.Waiting(null))

  private val _screenState: Flow<ScreenState> = combine(
    submissionState,
    authRepository.authStateFlow
  ){ submissionState, authState ->
    when (submissionState) {
      SubmissionState.Submitting -> ScreenState.Submitting
      is SubmissionState.Waiting -> {
        when (authState) {
          is AuthState.LoggedInUnlocked -> {
            ScreenState.Done
          }
          is AuthState.LoggedInLocked -> {
            ScreenState.WaitingForReauth(submissionState.errorMessage)
          }
          AuthState.LoggedOut -> {
            ScreenState.WaitingForLogin(submissionState.errorMessage)
          }
          is AuthState.TokenExpired -> {
            ScreenState.WaitingForTokenRefreshLogin(submissionState.errorMessage)
          }
          AuthState.Initializing -> ScreenState.Initializing
        }
      }
    }
  }
  val screenState: Flow<ScreenState> = _screenState

  sealed class ChannelAction {
    class Login(val username: String, val password: String) : ChannelAction()
    class Reauthenticate(val password: String) : ChannelAction()
    object Logout : ChannelAction()
  }

  private val authChannel = viewModelScope.actor<ChannelAction>(capacity = Channel.RENDEZVOUS) {
    consumeEach { action ->
      submissionState.value = SubmissionState.Submitting
      try {
        when (action) {
          is ChannelAction.Login -> {
            submissionState.value =
              when (val loginResult = authRepository.login(action.username, action.password)) {
                is AuthRepository.LoginResult.Exception -> {
                  SubmissionState.Waiting(loginResult.errorMessage)
                }
                is AuthRepository.LoginResult.Invalid -> {
                  val errorMessage = context.getString(
                    R.string.login_error_error_code_format_s_d,
                    loginResult.errorMessage ?: "",
                    loginResult.errorCode
                  )
                  SubmissionState.Waiting(loginResult.errorMessage)
                }
                AuthRepository.LoginResult.Success -> SubmissionState.Waiting(null)
              }
          }
          is ChannelAction.Reauthenticate -> {

          }
          ChannelAction.Logout -> {

          }
        }
      } finally {
        if (submissionState.value is SubmissionState.Submitting) {
          submissionState.value = SubmissionState.Waiting(null)
        }
      }
    }
  }
  fun submitAction(action: ChannelAction) {
    authChannel.trySend(action)
  }

  override fun onCleared() {
    super.onCleared()
    Log.d(TAG, "onCleared()")
  }

  companion object {
    private const val TAG = "AuthViewModel"
  }
}