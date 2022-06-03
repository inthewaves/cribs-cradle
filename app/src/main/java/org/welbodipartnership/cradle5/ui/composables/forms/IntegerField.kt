package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import org.welbodipartnership.cradle5.compose.forms.state.TextFieldState

@Composable
fun IntegerField(
  field: TextFieldState,
  label: String,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  textStyle: TextStyle = MaterialTheme.typography.body2,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = true,
  maxLines: Int = Int.MAX_VALUE,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  shape: Shape = MaterialTheme.shapes.small,
  colors: TextFieldColors = darkerDisabledOutlinedTextFieldColors()
) {
  OutlinedTextFieldWithErrorHint(
    value = field.stateValue,
    onValueChange = { newValue -> field.stateValue = newValue },
    modifier = modifier,
    textFieldModifier = textFieldModifier.then(field.createFocusChangeModifier()),
    enabled = enabled,
    readOnly = readOnly,
    textStyle = textStyle,
    label = { RequiredText(label, required = field.isMandatory) },
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    errorHint = field.getError(),
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number),
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    maxLines,
    interactionSource,
    shape,
    colors
  )
}
