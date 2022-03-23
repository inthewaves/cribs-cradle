package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun OutlinedTextFieldWithErrorHint(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  textStyle: TextStyle = MaterialTheme.typography.body2,
  label: @Composable (() -> Unit)? = null,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  errorHint: String? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = true,
  maxLines: Int = Int.MAX_VALUE,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  shape: Shape = MaterialTheme.shapes.small,
  colors: TextFieldColors = darkerDisabledOutlinedTextFieldColors()
) {
  Column(modifier) {
    BringIntoViewOutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = textFieldModifier,
      enabled = enabled,
      readOnly = readOnly,
      textStyle = textStyle,
      label = label,
      placeholder = placeholder,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      isError = enabled && errorHint != null,
      visualTransformation = visualTransformation,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      singleLine = singleLine,
      maxLines = maxLines,
      interactionSource = interactionSource,
      shape = shape,
      colors = colors
    )
    AnimatedErrorHint(errorHint, enabled = enabled)
  }
}
