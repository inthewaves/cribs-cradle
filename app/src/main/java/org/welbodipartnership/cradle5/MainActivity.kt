package org.welbodipartnership.cradle5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import dagger.hilt.android.AndroidEntryPoint
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      CradleTrialAppTheme {
        Text("Hello world")
      }
    }
  }
}
