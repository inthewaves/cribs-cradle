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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.auth.AuthState
import java.util.concurrent.CancellationException
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
    object Done : SubmissionState()
  }

  private val submissionState: MutableStateFlow<SubmissionState> =
    MutableStateFlow(SubmissionState.Waiting(null))

  private val _screenState: Flow<ScreenState> = combine(
    submissionState,
    authRepository.authStateFlow
  ) { submissionState, authState ->
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
      SubmissionState.Done -> ScreenState.Initializing
    }
  }
  val screenState: Flow<ScreenState> = _screenState

  sealed class ChannelAction {
    class Login(val username: String, val password: String) : ChannelAction()
    class Reauthenticate(val password: String) : ChannelAction()
    object Logout : ChannelAction()
    object Reset : ChannelAction()
  }

  private val _loginMessagesFlow: MutableStateFlow<String> = MutableStateFlow("")
  val loginMessagesFlow: StateFlow<String> = _loginMessagesFlow

  private val authChannel = viewModelScope.actor<ChannelAction>(capacity = Channel.RENDEZVOUS) {
    for (action in channel) {
      Log.d(TAG, "authChannel received an action")
      submissionState.value = SubmissionState.Submitting
      try {
        when (action) {
          is ChannelAction.Login -> {
            val newValue = coroutineScope {
              val loginEventChannel = actor<String>(capacity = Channel.CONFLATED) {
                consumeEach {
                  Log.d(TAG, "Login action: $it")
                  _loginMessagesFlow.value = it
                }
              }

              when (
                val loginResult = authRepository.login(action.username, action.password, loginEventChannel)
              ) {
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
                AuthRepository.LoginResult.Success -> SubmissionState.Done
              }.also {
                loginEventChannel.close()
              }
            }
            submissionState.value = newValue
          }
          is ChannelAction.Reauthenticate -> {
            val authSuccess = authRepository.reauthForLockscreen(action.password)
            submissionState.value = if (authSuccess) {
              SubmissionState.Done
            } else {
              SubmissionState.Waiting("Wrong password")
            }
          }
          ChannelAction.Logout, ChannelAction.Reset -> {
            Log.d(TAG, "action channel has reset")
            submissionState.value = SubmissionState.Waiting(null)
            _loginMessagesFlow.value = ""
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e(TAG, "failed to handle auth event", e)
        submissionState.value = SubmissionState.Waiting(
          "Failed to handle authentication (${e::class.java.simpleName}): ${e.localizedMessage}"
        )
      } finally {
        if (submissionState.value is SubmissionState.Submitting) {
          submissionState.value = SubmissionState.Waiting(null)
        }
      }
    }
  }

  fun reset() {
    viewModelScope.launch {
      authChannel.send(ChannelAction.Reset)
    }
  }

  fun submitAction(action: ChannelAction) {
    authChannel.trySend(action).also {
      Log.d(TAG, "submitAction result for submitting ${action::class.java.simpleName}: $it")
    }
  }

  override fun onCleared() {
    super.onCleared()
    Log.d(TAG, "onCleared()")
  }

  companion object {
    private const val TAG = "AuthViewModel"
  }
}
