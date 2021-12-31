package org.welbodipartnership.cradle5

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import org.welbodipartnership.cradle5.home.Home
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.appinit.AppInitManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val viewModel by viewModels<MainActivityViewModel>()

  @Inject
  lateinit var appInitManager: AppInitManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    WindowCompat.setDecorFitsSystemWindows(window, false)
    setContent {
      ProvideWindowInsets(consumeWindowInsets = false) {
        CradleTrialAppTheme {
          val systemUiController = rememberSystemUiController()
          val useDarkIcons = MaterialTheme.colors.isLight
          SideEffect {
            systemUiController.setSystemBarsColor(
              color = Color.Transparent,
              darkIcons = useDarkIcons
            )
          }

          val appState by appInitManager.appState.collectAsState()

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
                    Text("Loading...")
                  }
                }
              }
              is AppInitManager.AppState.Ready -> {
                Home()
              }
            }
          }
        }
      }
    }
  }
}
