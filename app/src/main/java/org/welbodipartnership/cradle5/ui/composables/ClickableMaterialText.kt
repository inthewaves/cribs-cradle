package org.welbodipartnership.cradle5.ui.composables

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle

@Composable
fun ClickableMaterialText(
  text: AnnotatedString,
  modifier: Modifier = Modifier,
  style: TextStyle = TextStyle.Default,
  onTextLayout: (TextLayoutResult) -> Unit = {},
  onClick: (Int) -> Unit
) {
  val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
  val pressIndicator = Modifier.pointerInput(onClick) {
    detectTapGestures { pos ->
      layoutResult.value?.let { layoutResult ->
        onClick(layoutResult.getOffsetForPosition(pos))
      }
    }
  }

  Text(
    text = text,
    modifier = modifier.then(pressIndicator),
    style = style,
    onTextLayout = {
      layoutResult.value = it
      onTextLayout(it)
    }
  )
}
