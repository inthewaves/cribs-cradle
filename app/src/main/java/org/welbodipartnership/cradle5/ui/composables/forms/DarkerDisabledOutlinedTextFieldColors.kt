package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable

private const val DARKER_DISABLED = 0.15f

@Composable
fun darkerDisabledOutlinedTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
  disabledTextColor = LocalContentColor.current.copy(DARKER_DISABLED),
  disabledBorderColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED),
  disabledLabelColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED),
  disabledLeadingIconColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED),
  disabledPlaceholderColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED),
  disabledTrailingIconColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED),
)
