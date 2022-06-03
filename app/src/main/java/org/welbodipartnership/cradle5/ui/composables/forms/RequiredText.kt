package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
@ReadOnlyComposable
fun String.withRequiredStar() = buildAnnotatedString {
  append(this@withRequiredStar)
  withStyle(SpanStyle(color = MaterialTheme.colors.error)) {
    append('*')
  }
}

@Composable
fun RequiredText(
  text: String,
  modifier: Modifier = Modifier,
  required: Boolean = true,
  enabled: Boolean = true
) {
  val previous = LocalContentAlpha.current
  CompositionLocalProvider(LocalContentAlpha provides if (enabled) previous else ContentAlpha.disabled) {
    if (required && enabled) {
      Text(text.withRequiredStar(), modifier = modifier)
    } else {
      Text(text, modifier = modifier)
    }
  }
}