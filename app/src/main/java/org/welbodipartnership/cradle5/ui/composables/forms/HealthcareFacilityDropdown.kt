package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import org.welbodipartnership.cradle5.data.database.entities.Facility

@Composable
fun HealthcareFacilityDropdown(
  facility: Facility,
  onFacilitySelected: (Facility) -> Unit,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  label: @Composable (() -> Unit)? = null,
  errorHint: String?,
) {
  var showDialog by remember { mutableStateOf(false) }
  if (showDialog) {
    Dialog(onDismissRequest = { showDialog = false }) {
    }
  }

  OutlinedTextFieldWithErrorHint(
    readOnly = true,
    value = facility.name ?: "",
    onValueChange = {},
    label = label,
    maxLines = 2,
    enabled = enabled,
    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = enabled && showDialog) },
    errorHint = errorHint,
    modifier = modifier,
    textFieldModifier = textFieldModifier,
    interactionSource = remember { MutableInteractionSource() }
      .also { interactionSource ->
        LaunchedEffect(interactionSource) {
          interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
              showDialog = true
            }
          }
        }
      }
  )
}
