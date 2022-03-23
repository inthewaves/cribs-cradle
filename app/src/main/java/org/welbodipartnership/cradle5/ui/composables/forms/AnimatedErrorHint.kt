package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun AnimatedErrorHint(
  errorHint: String?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  var previousErrorHint by remember { mutableStateOf(errorHint) }
  val showError = enabled && errorHint != null
  LaunchedEffect(errorHint) {
    previousErrorHint = if (errorHint != null) {
      errorHint
    } else {
      // wait until it hides so that the error hint remains while it is fading away
      delay(500L)
      null
    }
  }
  AnimatedVisibility(
    modifier = modifier,
    visible = showError,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
  ) {
    Text(
      previousErrorHint ?: "",
      color = MaterialTheme.colors.error,
      fontSize = MaterialTheme.typography.caption.fontSize
    )
  }
}
