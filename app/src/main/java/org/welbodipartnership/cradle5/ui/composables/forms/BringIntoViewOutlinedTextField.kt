package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BringIntoViewOutlinedTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  textStyle: TextStyle = LocalTextStyle.current,
  label: @Composable (() -> Unit)? = null,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  isError: Boolean = false,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = false,
  maxLines: Int = Int.MAX_VALUE,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  shape: Shape = MaterialTheme.shapes.small,
  colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(),
  bringIntoViewRequester: BringIntoViewRequester = remember { BringIntoViewRequester() },
  delayMillis: Long = 200
) {
  val scope = rememberCoroutineScope()
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier
      .bringIntoViewRequester(bringIntoViewRequester)
      .onFocusEvent {
        if (it.isFocused) {
          scope.launch {
            delay(delayMillis)
            bringIntoViewRequester.bringIntoView()
          }
        }
      },
    enabled = enabled,
    readOnly = readOnly,
    textStyle = textStyle,
    label = label,
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    isError = isError,
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    maxLines = maxLines,
    interactionSource = interactionSource,
    shape = shape,
    colors = colors
  )
}
