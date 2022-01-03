package org.welbodipartnership.cradle5

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import org.welbodipartnership.cradle5.domain.auth.AuthState
import org.welbodipartnership.cradle5.home.LoggedInHome
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.appinit.AppInitManager

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  private val viewModel by viewModels<MainActivityViewModel>()


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
        if (authState is AuthState.LoggedInUnlocked) {
          LoggedInHome(navController)
        } else {
          val authViewModel: AuthViewModel = hiltViewModel()
          val authScreenState by authViewModel.screenState
            .collectAsState(AuthViewModel.ScreenState.Initializing)

          Surface {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              authScreenState.let { state ->
                when (state) {
                  AuthViewModel.ScreenState.Done -> {
                    CircularProgressIndicator()
                  }
                  AuthViewModel.ScreenState.Initializing -> {
                    CircularProgressIndicator()
                  }
                  AuthViewModel.ScreenState.Submitting -> {
                    CircularProgressIndicator()
                  }
                  is AuthViewModel.ScreenState.WaitingForLogin -> {
                    Text("AuthViewModel.ScreenState.WaitingForLogin")

                    var username by remember {
                      mutableStateOf("")
                    }
                    var password by remember {
                      mutableStateOf("")
                    }

                    OutlinedTextField(value = username, onValueChange = { username = it })

                    OutlinedTextField(value = password, onValueChange = { password = it })

                    Button(
                      onClick = { authViewModel.submitAction(AuthViewModel.ChannelAction.Login(username, password)) }
                    ) {
                      Text("Login")
                    }
                  }
                  is AuthViewModel.ScreenState.WaitingForReauth -> {
                    Text("AuthViewModel.ScreenState.WaitingForReauth")
                  }
                  is AuthViewModel.ScreenState.WaitingForTokenRefreshLogin -> {
                    Text("AuthViewModel.ScreenState.WaitingForTokenRefreshLogin")
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
