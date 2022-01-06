package org.welbodipartnership.cradle5

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.auth.AuthState
import org.welbodipartnership.cradle5.home.LoggedInHome
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.appinit.AppInitManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  private val viewModel by viewModels<MainActivityViewModel>()

  @Inject
  lateinit var authRepository: AuthRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      ProvideWindowInsets(consumeWindowInsets = false, windowInsetsAnimationsEnabled = true) {
        CradleTrialAppTheme {
          MainApp(viewModel)
        }
      }
    }
  }
}

@Composable
private fun MainApp(viewModel: MainActivityViewModel) {
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

        authState.let { currentAuthState ->
          if (currentAuthState is AuthState.LoggedInUnlocked) {
            val serverEnums by viewModel.serverEnumCollection.collectAsState()
            val districtName by viewModel.districtName.collectAsState(initial = null)
            CompositionLocalProvider(LocalServerEnumCollection provides serverEnums) {
              LoggedInHome(
                navController,
                currentAuthState,
                districtName,
                onLogout = { viewModel.logout() },
                onLock = { viewModel.forceLockScreen() }
              )
            }
          } else {
            LoginOrLockscreen(currentAuthState)
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
  val (username, setUsername) = rememberSaveable(authState is AuthState.LoggedOut) {
    mutableStateOf("")
  }
  val (password, setPassword) = rememberSaveable(authState is AuthState.LoggedOut) {
    mutableStateOf("")
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
          is AuthViewModel.ScreenState.WaitingForLogin -> {
            LoginForm(
              LoginType.NewLogin { username, password ->
                authViewModel.submitAction(AuthViewModel.ChannelAction.Login(username, password))
              },
              username = username,
              onUsernameChange = setUsername,
              password = password,
              onPasswordChange = setPassword,
              errorMessage = state.errorMessage
            )
          }
          is AuthViewModel.ScreenState.WaitingForReauth -> {
            LoginForm(
              LoginType.Lockscreen { password ->
                authViewModel.submitAction(AuthViewModel.ChannelAction.Reauthenticate(password))
              },
              username = username,
              onUsernameChange = setUsername,
              password = password,
              onPasswordChange = setPassword,
              errorMessage = state.errorMessage
            )
          }
          is AuthViewModel.ScreenState.WaitingForTokenRefreshLogin -> {
            Text("AuthViewModel.ScreenState.WaitingForTokenRefreshLogin")
          }
        }
      }
    }
  }
}

@Stable
private sealed class LoginType {
  class NewLogin(
    val onSubmit: (username: String, password: String) -> Unit
  ) : LoginType()
  class Lockscreen(val onSubmit: (password: String) -> Unit) : LoginType()
}

@Composable
private fun LoginForm(
  loginType: LoginType,
  username: String,
  onUsernameChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  errorMessage: String?,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start,
    modifier = modifier
      .navigationBarsWithImePadding()
      .widthIn(max = 600.dp)
      .padding(horizontal = 24.dp)
  ) {
    if (loginType is LoginType.Lockscreen) {
      item {
        Text(stringResource(R.string.lockscreen_title), style = MaterialTheme.typography.h4)
      }
      item {
        Spacer(Modifier.height(12.dp))
      }
    }

    if (errorMessage != null) {
      item {
        Text(errorMessage, color = MaterialTheme.colors.error)
        Spacer(Modifier.height(12.dp))
      }
    }
    item {
      if (loginType is LoginType.NewLogin) {
        OutlinedTextField(
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
    }

    item {
      OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.password_label)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
          autoCorrect = false,
          keyboardType = KeyboardType.Password,
          imeAction = ImeAction.Done
        ),
        maxLines = 1,
      )
    }

    item { Spacer(Modifier.height(12.dp)) }

    item {
      Button(
        onClick = {
          when (loginType) {
            is LoginType.Lockscreen -> loginType.onSubmit(password)
            is LoginType.NewLogin -> loginType.onSubmit(username, password)
          }
        },
        modifier = Modifier.fillMaxWidth()
      ) {
        when (loginType) {
          is LoginType.Lockscreen -> Text(stringResource(R.string.unlock_button))
          is LoginType.NewLogin -> Text(stringResource(R.string.login_button))
        }
      }
    }
  }
}

@Preview
@Composable
fun LoginFormPreview() {
  CradleTrialAppTheme {
    Surface {
      LoginForm(
        loginType = LoginType.Lockscreen {},
        username = "",
        onUsernameChange = {},
        password = "password",
        onPasswordChange = {},
        errorMessage = "My error message"
      )
    }
  }
}
