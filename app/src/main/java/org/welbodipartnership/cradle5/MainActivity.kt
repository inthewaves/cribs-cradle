package org.welbodipartnership.cradle5

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
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

    setContent {
      CradleTrialAppTheme {
        val appState by appInitManager.appState.collectAsState()

        LaunchedEffect(appState) {
          Log.d("MainActivity", "New app state: $appState")
        }

        appState.let { state ->
          when (state) {
            is AppInitManager.AppState.FailedToInitialize -> {
              Surface {
                Column {
                  Text("Failed to initialize the app (${state.classOfTaskThatFailed.java.canonicalName}")
                  Text(state.cause.stackTraceToString())
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
              Surface {
                Column(
                  modifier = Modifier.fillMaxSize(),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("Wat...")
                }
              }
            }
          }
        }
      }
    }
  }
}
