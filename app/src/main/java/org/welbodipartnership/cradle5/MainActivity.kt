package org.welbodipartnership.cradle5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import org.welbodipartnership.cradle5.data.settings.ServerType
import org.welbodipartnership.cradle5.domain.UrlProvider
import org.welbodipartnership.cradle5.domain.auth.AuthState
import org.welbodipartnership.cradle5.home.LoggedInHome
import org.welbodipartnership.cradle5.ui.composables.LocalUrlProvider
import org.welbodipartnership.cradle5.ui.composables.PrivacyPolicyButton
import org.welbodipartnership.cradle5.ui.composables.UsingServerText
import org.welbodipartnership.cradle5.ui.composables.forms.BringIntoViewOutlinedTextField
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.appinit.AppInitManager
import javax.inject.Inject

private const val MAX_LOCKSCREEN_ATTEMPTS_BEFORE_TRYING_SERVER = 2
private const val MAX_ICON_TAPS_BEFORE_SHOWING_SERVER_OVERRIDE_DIALOG = 10

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  private val viewModel by viewModels<MainActivityViewModel>()

  @Inject
  lateinit var urlProvider: UrlProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      CompositionLocalProvider(LocalUrlProvider provides urlProvider) {
        ProvideWindowInsets(consumeWindowInsets = false, windowInsetsAnimationsEnabled = true) {
          CradleTrialAppTheme {
            MainApp(
              viewModel,
              onOpenSettingsForApp = {
                startActivity(
                  Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                  )
                )
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MainApp(viewModel: MainActivityViewModel, onOpenSettingsForApp: () -> Unit,) {
  val systemUiController = rememberSystemUiController()
  val useDarkIcons = MaterialTheme.colors.isLight
  SideEffect {
    systemUiController.setSystemBarsColor(
      color = Color.Transparent,
      darkIcons = useDarkIcons
    )
  }

  val appState by viewModel.appState.collectAsState()

  LaunchedEffect(appState) {
    Log.d("MainActivity", "New app state: ${appState::class.java.simpleName}")
  }

  appState.let { state ->
    when (state) {
      is AppInitManager.AppState.FailedToInitialize -> {
        Surface(Modifier.padding(12.dp)) {
          val horizontalScrollState = rememberScrollState()
          val verticalScrollState = rememberScrollState()

          SelectionContainer {
            Column(Modifier.verticalScroll(verticalScrollState)) {
              Text("Failed to initialize the app (${state.classOfTaskThatFailed.java.canonicalName}")
              Text(
                state.cause.stackTraceToString(),
                modifier = Modifier.horizontalScroll(horizontalScrollState)
              )
            }
          }
        }
      }
      is AppInitManager.AppState.Initializing -> {
        Surface {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator()
          }
        }
      }
      is AppInitManager.AppState.Ready -> {
        val navController = rememberAnimatedNavController()

        val authState by viewModel.authState.collectAsState(AuthState.Initializing)
        LaunchedEffect(authState) {
          Log.d("MainActivity", "New auth state: ${authState::class.java.simpleName}")
        }

        authState.let { currentAuthState ->
          when (currentAuthState) {
            is AuthState.LoggedInUnlocked -> {
              val serverEnums by viewModel.serverEnumCollection.collectAsState()
              val districtName by viewModel.districtName.collectAsState(initial = null)
              CompositionLocalProvider(LocalServerEnumCollection provides serverEnums) {
                LoggedInHome(
                  navController,
                  currentAuthState,
                  districtName,
                  onLogout = { viewModel.logout() },
                  onLock = { viewModel.forceLockScreen() },
                  onOpenSettingsForApp = onOpenSettingsForApp
                )
              }
            }
            is AuthState.LoggedInLocked, AuthState.Initializing, AuthState.LoggedOut,
            is AuthState.TokenExpired, AuthState.LoggingIn -> {
              LoginOrLockscreen(currentAuthState)
            }
            is AuthState.BlockingWarningMessage -> {
              val scrollState = rememberScrollState()
              Surface {
                Column(
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.Start,
                  modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .navigationBarsWithImePadding()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 24.dp)
                ) {
                  Text(stringResource(R.string.warning_title), style = MaterialTheme.typography.h4)
                  Spacer(Modifier.height(12.dp))

                  Text(text = currentAuthState.warningMessage)
                  Spacer(Modifier.height(24.dp))

                  Button(
                    onClick = viewModel::clearWarningMessage,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(stringResource(android.R.string.ok))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun LoginOrLockscreen(authState: AuthState) {
  val authViewModel: AuthViewModel = hiltViewModel()
  val authScreenState by authViewModel.screenState
    .collectAsState(AuthViewModel.ScreenState.Initializing)

  DisposableEffect(null) {
    // getting around viewmodels not scoped to composables
    onDispose {
      Log.d("MainActivity", "disposing and resetting login screen")
      authViewModel.reset()
    }
  }

  // use boolean as key to ensure these gets cleared if logging out from the lockscreen
  val (username, setUsername) = rememberSaveable(authState is AuthState.LoggedInUnlocked) {
    mutableStateOf("")
  }
  val (password, setPassword) = rememberSaveable(authState is AuthState.LoggedInUnlocked) {
    mutableStateOf("")
  }
  var attemptCount by rememberSaveable(authState is AuthState.LoggedOut) { mutableStateOf(0) }
  var iconTaps by rememberSaveable(authState is AuthState.LoggedOut) { mutableStateOf(0) }

  var isServerDialogShowing by rememberSaveable(authState is AuthState.LoggedInUnlocked) {
    mutableStateOf(false)
  }

  var hasClickedForgotPassword by rememberSaveable(authState is AuthState.LoggedInUnlocked) {
    mutableStateOf(false)
  }

  if (isServerDialogShowing) {
    val currentOption by authViewModel.serverUrlOption.collectAsState()
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(currentOption) }
    AlertDialog(
      onDismissRequest = { isServerDialogShowing = false },
      confirmButton = {
        TextButton(
          onClick = {
            isServerDialogShowing = false
            authViewModel.setServerTypeOverride(selectedOption)
          }
        ) {
          Text("Select")
        }
      },
      title = { Text("Select a server") },
      text = {
        // https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#RadioButton(kotlin.Boolean,kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.material.RadioButtonColors)
        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
        Column(Modifier.selectableGroup()) {
          listOf(ServerType.UNSET, ServerType.PRODUCTION, ServerType.TEST).forEach { currentOpt ->
            Row(
              Modifier
                .fillMaxWidth()
                .height(48.dp)
                .selectable(
                  selected = currentOpt == selectedOption,
                  onClick = { onOptionSelected(currentOpt) },
                  role = Role.RadioButton
                )
                .padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = currentOpt == selectedOption,
                onClick = null // null recommended for accessibility with screen readers
              )
              Text(
                text = when (currentOpt) {
                  ServerType.PRODUCTION -> "Main / production server"
                  ServerType.TEST -> "Test server"
                  ServerType.UNRECOGNIZED, ServerType.UNSET -> "Default"
                },
                style = MaterialTheme.typography.body1.merge(),
                modifier = Modifier.padding(start = 16.dp)
              )
            }
          }
        }
      }
    )
  }

  Surface {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      authScreenState.let { state ->
        when (state) {
          AuthViewModel.ScreenState.Done,
          AuthViewModel.ScreenState.Initializing,
          AuthViewModel.ScreenState.Submitting -> {
            val loginInfo by authViewModel.loginMessagesFlow.collectAsState()
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(loginInfo, textAlign = TextAlign.Center)
          }
          is AuthViewModel.ScreenState.UserInputNeeded -> {
            val extraMessage: String?
            val loginType = when (state) {
              is AuthViewModel.ScreenState.UserInputNeeded.WaitingForLogin -> {
                extraMessage = null
                LoginType.NewLogin { username, password ->
                  authViewModel.submitAction(AuthViewModel.ChannelAction.Login(username, password))
                  attemptCount++
                }
              }
              is AuthViewModel.ScreenState.UserInputNeeded.WaitingForReauth,
              is AuthViewModel.ScreenState.UserInputNeeded.WaitingForTokenRefreshLogin -> {
                val isRefreshNeeded =
                  state is AuthViewModel.ScreenState.UserInputNeeded.WaitingForTokenRefreshLogin ||
                    attemptCount > MAX_LOCKSCREEN_ATTEMPTS_BEFORE_TRYING_SERVER ||
                    hasClickedForgotPassword
                extraMessage = if (isRefreshNeeded) {
                  "Internet access is required in order to refresh credentials with MedSciNet"
                } else {
                  null
                }

                val currentUsername by authViewModel.usernameFlow.collectAsState(initial = null)

                LoginType.Lockscreen(currentUsername ?: "") { password ->
                  Log.d(
                    "MainActivity",
                    "submitting lockscreen, forceRefresh = $isRefreshNeeded, " +
                      "attempt count = $attemptCount"
                  )
                  authViewModel.submitAction(
                    AuthViewModel.ChannelAction.Reauthenticate(
                      password,
                      forceTokenRefresh = isRefreshNeeded
                    )
                  )
                  attemptCount++
                }
              }
            }

            var isForgotPasswordInfoDialogShowing by rememberSaveable { mutableStateOf(false) }
            if (isForgotPasswordInfoDialogShowing) {
              AlertDialog(
                onDismissRequest = { isForgotPasswordInfoDialogShowing = false },
                title = { Text("Forgot password") },
                text = {
                  Text(
                    "Please contact the administrator to reset your password, and then try the new password with an internet connection. Alternatively, you can reset your password on the website if your account has an email associated with it.\n\nAdministrator email can be found on the website."
                  )
                },
                confirmButton = {
                  TextButton(onClick = { isForgotPasswordInfoDialogShowing = false }) {
                    Text(stringResource(android.R.string.ok))
                  }
                }
              )
            }

            LoginForm(
              loginType = loginType,
              username = username,
              onUsernameChange = setUsername,
              password = password,
              onPasswordChange = setPassword,
              onIconTap = {
                iconTaps++
                if (iconTaps >= MAX_ICON_TAPS_BEFORE_SHOWING_SERVER_OVERRIDE_DIALOG) {
                  isServerDialogShowing = true
                }
              },
              hasClickedForgotPassword = hasClickedForgotPassword,
              onForgotPasswordClicked = {
                hasClickedForgotPassword = true
                isForgotPasswordInfoDialogShowing = true
              },
              errorMessage = state.errorMessage,
              currentAttempts = attemptCount,
              extraMessage = extraMessage
            )
          }
        }
      }
    }
  }
}

@Stable
private sealed class LoginType {
  @Stable
  class NewLogin(
    val onSubmit: (username: String, password: String) -> Unit
  ) : LoginType()

  @Stable
  class Lockscreen(
    val username: String,
    val onSubmit: (password: String) -> Unit
  ) : LoginType()
}

@Composable
private fun LoginForm(
  loginType: LoginType,
  username: String,
  onUsernameChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  onIconTap: () -> Unit,
  hasClickedForgotPassword: Boolean,
  onForgotPasswordClicked: () -> Unit,
  errorMessage: String?,
  currentAttempts: Int,
  modifier: Modifier = Modifier,
  extraMessage: String? = null,
) {

  val onSubmit: () -> Unit = {
    when (loginType) {
      is LoginType.Lockscreen -> loginType.onSubmit(password)
      is LoginType.NewLogin -> loginType.onSubmit(username, password)
    }
  }

  val scrollState = rememberScrollState()
  Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start,
    modifier = modifier
      .verticalScroll(scrollState)
      .navigationBarsWithImePadding()
      .widthIn(max = 600.dp)
      .padding(horizontal = 24.dp)
  ) {

    Column(
      Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painterResource(R.mipmap.ic_launcher_foreground),
        stringResource(R.string.app_icon_cd),
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 200.dp)
          .then(
            if (loginType is LoginType.NewLogin) {
              Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onIconTap
              )
            } else {
              Modifier
            }
          )
      )
      Text(
        stringResource(R.string.app_name), style = MaterialTheme.typography.h4,
        textAlign = TextAlign.Center
      )
    }

    Spacer(Modifier.height(12.dp))

    if (loginType is LoginType.Lockscreen) {
      Text(stringResource(R.string.lockscreen_title), style = MaterialTheme.typography.h5)
      Spacer(Modifier.height(12.dp))
      Text(stringResource(R.string.lockscreen_subtitle_logged_in_as_s, loginType.username))
      Spacer(Modifier.height(12.dp))
    }

    if (errorMessage != null) {
      Text(errorMessage, color = MaterialTheme.colors.error)
      Spacer(Modifier.height(12.dp))
      if (
        loginType is LoginType.Lockscreen &&
        currentAttempts > MAX_LOCKSCREEN_ATTEMPTS_BEFORE_TRYING_SERVER
      ) {
        Text(
          "If password has changed on MedSciNet, enter your new password instead",
          color = MaterialTheme.colors.error
        )
        Spacer(Modifier.height(12.dp))
      }
    }

    if (extraMessage != null) {
      Text(extraMessage)
      Spacer(Modifier.height(12.dp))
    }

    UsingServerText(LocalUrlProvider.current)
    Spacer(Modifier.height(12.dp))

    if (loginType is LoginType.NewLogin) {
      BringIntoViewOutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.username_label)) },
        keyboardOptions = KeyboardOptions(
          autoCorrect = false,
          imeAction = ImeAction.Next,
        ),
        maxLines = 1,
      )
    }

    BringIntoViewOutlinedTextField(
      value = password,
      onValueChange = onPasswordChange,
      modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(R.string.password_label)) },
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(
        autoCorrect = false,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
      ),
      keyboardActions = KeyboardActions(onDone = { onSubmit() }),
      maxLines = 1,
    )

    Spacer(Modifier.height(12.dp))

    Button(
      onClick = onSubmit,
      modifier = Modifier.fillMaxWidth()
    ) {
      when (loginType) {
        is LoginType.Lockscreen -> Text(stringResource(R.string.unlock_button))
        is LoginType.NewLogin -> Text(stringResource(R.string.login_button))
      }
    }

    Spacer(Modifier.height(12.dp))

    TextButton(onClick = onForgotPasswordClicked, modifier = Modifier.fillMaxWidth()) {
      Text("Forgot password")
    }

    Spacer(Modifier.height(2.dp))

    PrivacyPolicyButton(Modifier.align(Alignment.CenterHorizontally))
  }
}

@Preview
@Composable
fun LoginFormLockscreenPreview() {
  CradleTrialAppTheme {
    Surface {
      LoginForm(
        loginType = LoginType.Lockscreen("TestUser") {},
        username = "",
        onUsernameChange = {},
        password = "password",
        onPasswordChange = {},
        onIconTap = {},
        hasClickedForgotPassword = true,
        onForgotPasswordClicked = {},
        errorMessage = "My error message",
        currentAttempts = 0
      )
    }
  }
}

@Preview(device = Devices.PIXEL_C)
@Composable
fun LoginFormNewLoginPreview() {
  CradleTrialAppTheme {
    Surface {
      LoginForm(
        loginType = LoginType.NewLogin { _, _ -> },
        username = "testuser",
        onUsernameChange = {},
        password = "password",
        onPasswordChange = {},
        onIconTap = {},
        hasClickedForgotPassword = false,
        onForgotPasswordClicked = {},
        errorMessage = "My error message",
        currentAttempts = 0
      )
    }
  }
}
