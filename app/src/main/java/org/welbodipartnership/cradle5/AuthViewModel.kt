package org.welbodipartnership.cradle5

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.data.settings.ServerType
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.auth.AuthState
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val authRepository: AuthRepository,
  private val appValuesStore: AppValuesStore,
) : ViewModel() {

  @Immutable
  sealed class ScreenState {
    object Initializing : ScreenState()
    object Done : ScreenState()
    sealed class UserInputNeeded : ScreenState() {
      abstract val errorMessage: String?
      @Immutable
      data class WaitingForTokenRefreshLogin(override val errorMessage: String?) : UserInputNeeded()
      @Immutable
      data class WaitingForLogin(override val errorMessage: String?) : UserInputNeeded()
      @Immutable
      data class WaitingForReauth(override val errorMessage: String?) : UserInputNeeded()
    }
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
            ScreenState.UserInputNeeded.WaitingForReauth(submissionState.errorMessage)
          }
          AuthState.LoggedOut -> {
            ScreenState.UserInputNeeded.WaitingForLogin(submissionState.errorMessage)
          }
          is AuthState.TokenExpired, is AuthState.ForcedRelogin -> {
            ScreenState.UserInputNeeded.WaitingForTokenRefreshLogin(submissionState.errorMessage)
          }
          AuthState.Initializing, is AuthState.BlockingWarningMessage -> ScreenState.Initializing
          AuthState.LoggingIn -> ScreenState.Submitting
        }
      }
      SubmissionState.Done -> ScreenState.Initializing
    }
  }
  val screenState: Flow<ScreenState> = _screenState

  sealed class ChannelAction {
    class Login(val username: String, val password: String) : ChannelAction()
    class Reauthenticate(
      val password: String,
      val forceTokenRefresh: Boolean,
    ) : ChannelAction()
    object Logout : ChannelAction()
    object Reset : ChannelAction()
    object ClearWarning : ChannelAction()
  }

  private val _loginMessagesFlow: MutableStateFlow<String> = MutableStateFlow("")
  val loginMessagesFlow: StateFlow<String> = _loginMessagesFlow

  val usernameFlow: Flow<String?> = appValuesStore.authTokenFlow
    .map { it?.username }

  private val authChannel = viewModelScope.actor<ChannelAction>(capacity = Channel.RENDEZVOUS) {
    for (action in channel) {
      Log.d(TAG, "authChannel received an action")
      submissionState.value = SubmissionState.Submitting
      val loginEventChannel: SendChannel<String> by lazy {
        actor(capacity = Channel.UNLIMITED) {
          consumeEach {
            Log.d(TAG, "Login action: $it")
            _loginMessagesFlow.value = it
          }
        }
      }
      try {
        when (action) {
          is ChannelAction.Login -> {
            val newValue = coroutineScope {
              when (
                val loginResult = authRepository.login(
                  action.username,
                  action.password,
                  loginEventChannel,
                  isForTokenRefresh = false,
                )
              ) {
                is AuthRepository.LoginResult.Exception -> {
                  SubmissionState.Waiting(loginResult.errorMessage)
                }
                is AuthRepository.LoginResult.Invalid -> {
                  val errorMessage = context.getString(
                    R.string.login_error_error_code_format_s_s,
                    loginResult.errorMessage ?: "",
                    loginResult.errorCode?.toString() ?: ""
                  )
                  SubmissionState.Waiting(errorMessage)
                }
                AuthRepository.LoginResult.Success -> SubmissionState.Done
              }
            }
            submissionState.value = newValue
          }
          is ChannelAction.Reauthenticate -> {
            submissionState.value = when (
              val reauthResult = authRepository.reauthForLockscreen(
                action.password,
                forceServerRefresh = action.forceTokenRefresh,
                eventMessagesChannel = loginEventChannel,
              )
            ) {
              AuthRepository.LoginResult.Success -> {
                SubmissionState.Done
              }
              is AuthRepository.LoginResult.Exception ->
                SubmissionState.Waiting(reauthResult.errorMessage)
              is AuthRepository.LoginResult.Invalid ->
                SubmissionState.Waiting(reauthResult.errorMessage)
            }
          }
          ChannelAction.Logout, ChannelAction.Reset -> {
            Log.d(TAG, "action channel has reset")
            submissionState.value = SubmissionState.Waiting(null)
            _loginMessagesFlow.value = ""
          }
          ChannelAction.ClearWarning -> {
            Log.d(TAG, "clearing warning")
            appValuesStore.clearWarningMessage()
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e(TAG, "failed to handle auth event", e)
        submissionState.value = SubmissionState.Waiting(
          "Failed to handle authentication (${e::class.java.simpleName}): ${e.localizedMessage}"
        )
      } finally {
        loginEventChannel.close()
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

  val serverUrlOption: StateFlow<ServerType> = appValuesStore.serverUrlOverrideFlow
    .map { it ?: ServerType.UNSET }
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 3000L),
      ServerType.UNSET
    )

  fun setServerTypeOverride(serverType: ServerType) {
    viewModelScope.launch {
      appValuesStore.setServerTypeOverride(serverType)
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
