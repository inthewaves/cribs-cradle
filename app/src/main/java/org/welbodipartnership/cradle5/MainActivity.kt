package org.welbodipartnership.cradle5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.welbodipartnership.cradle5.ui.theme.TestJetpackComposeApplicationTheme


class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      TestJetpackComposeApplicationTheme {

      }
    }
  }
}
