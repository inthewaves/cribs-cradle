package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable

const val DARKER_DISABLED_ALPHA = 0.15f

@Composable
fun darkerDisabledOutlinedTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
  disabledTextColor = LocalContentColor.current.copy(DARKER_DISABLED_ALPHA),
  disabledBorderColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED_ALPHA),
  disabledLabelColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED_ALPHA),
  disabledLeadingIconColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED_ALPHA),
  disabledPlaceholderColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED_ALPHA),
  disabledTrailingIconColor = MaterialTheme.colors.onSurface.copy(alpha = DARKER_DISABLED_ALPHA),
)
