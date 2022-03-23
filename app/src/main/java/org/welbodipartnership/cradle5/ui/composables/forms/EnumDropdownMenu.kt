package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.patients.form.withRequiredStar
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@Composable
fun EnumDropdownMenuIdOnly(
  currentSelection: EnumSelection.IdOnly?,
  onSelect: (EnumSelection.IdOnly?) -> Unit,
  modifier: Modifier = Modifier,
  textModifier: Modifier = Modifier,
  label: @Composable () -> Unit,
  serverEnum: ServerEnum,
  errorHint: String? = null,
  enabled: Boolean = true,
) {
  BaseEnumDropdownMenu(
    currentSelection = currentSelection,
    onSelect = { selectionId ->
      onSelect(selectionId?.let { EnumSelection.IdOnly(it) })
    },
    modifier = modifier,
    dropdownTextModifier = textModifier,
    label = label,
    errorHint = errorHint,
    serverEnum = serverEnum,
    enabled = enabled,
  )
}

@Composable
fun EnumDropdownMenuWithOther(
  currentSelection: EnumSelection.WithOther?,
  onSelect: (EnumSelection.WithOther?) -> Unit,
  modifier: Modifier = Modifier,
  dropdownTextModifier: Modifier = Modifier,
  otherTextModifier: Modifier = Modifier,
  label: @Composable () -> Unit,
  errorHint: String?,
  showErrorHintOnOtherField: Boolean = true,
  serverEnum: ServerEnum,
  enabled: Boolean = true,
  spacerHeight: Dp = 8.dp,
) {
  BaseEnumDropdownMenu(
    currentSelection = currentSelection,
    onSelect = { selectionId ->
      onSelect(selectionId?.let { EnumSelection.WithOther(it) })
    },
    modifier = modifier,
    dropdownTextModifier = dropdownTextModifier,
    label = label,
    errorHint = if (!showErrorHintOnOtherField) errorHint else null,
    serverEnum = serverEnum,
    enabled = enabled,
  ) { currentEntry ->
    Spacer(Modifier.height(spacerHeight))

    val isOtherEnabled = enabled &&
      currentEntry?.id == serverEnum.otherEntry?.id &&
      currentEntry?.id != null
    OutlinedTextFieldWithErrorHint(
      value = currentSelection?.otherString ?: "",
      onValueChange = {
        if (currentSelection != null) {
          onSelect(currentSelection.copy(otherString = it))
        }
      },
      label = {
        if (isOtherEnabled) {
          Text(stringResource(R.string.other_enum_label).withRequiredStar())
        } else {
          Text(stringResource(R.string.other_enum_label))
        }
      },
      enabled = isOtherEnabled,
      errorHint = if (
        isOtherEnabled &&
        showErrorHintOnOtherField &&
        errorHint != null
      ) {
        errorHint
      } else {
        null
      },
      textFieldModifier = otherTextModifier
    )
  }
}

@Composable
private fun <T : EnumSelection> BaseEnumDropdownMenu(
  currentSelection: T?,
  onSelect: (Int?) -> Unit,
  modifier: Modifier = Modifier,
  dropdownTextModifier: Modifier = Modifier,
  label: @Composable () -> Unit,
  errorHint: String?,
  serverEnum: ServerEnum,
  enabled: Boolean = true,
  dropdownColors: TextFieldColors = darkerDisabledOutlinedTextFieldColors(),
  extraContent: @Composable (ColumnScope.(currentEntry: ServerEnum.Entry?) -> Unit)? = null,
) {
  var expanded by remember { mutableStateOf(false) }
  val currentEntry = currentSelection?.selectionId?.let { serverEnum.get(it) }

  Column(modifier) {

    val onClick = {
      expanded = if (enabled) {
        // ensure that tapping on the menu again closes it
        !expanded
      } else {
        expanded
      }
    }
    FixLongPressExposedDropdownMenuBox(
      expanded = enabled && expanded,
      onExpandedChange = {
        // ensure that tapping on the menu again closes it
        expanded = if (expanded) false else it
      },
      enabled = enabled,
    ) {
      OutlinedTextFieldWithErrorHint(
        readOnly = true,
        value = currentEntry?.name ?: "",
        onValueChange = {},
        label = label,
        maxLines = 2,
        enabled = enabled,
        trailingIcon = {
          ExposedDropdownMenuDefaults.TrailingIcon(
            expanded = enabled && expanded,
            onIconClick = onClick
          )
        },
        errorHint = errorHint,
        colors = dropdownColors,
        textFieldModifier = dropdownTextModifier,
        interactionSource = remember { MutableInteractionSource() }
          .also { interactionSource ->
            if (enabled) {
              LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect {
                  if (it is PressInteraction.Release) {
                    onClick()
                  }
                }
              }
            }
          }
      )
      ExposedDropdownMenu(
        expanded = enabled && expanded,
        onDismissRequest = { expanded = false },
        modifier = dropdownTextModifier,
      ) {
        serverEnum.sortedValuesWithEmptyResponse.forEach { selectionOption ->
          DropdownMenuItem(
            onClick = {
              expanded = false
              val selection = when (selectionOption) {
                // the web app has an empty option apparently
                ServerEnum.EmptyResponseEntry -> null
                is ServerEnum.Entry -> selectionOption.id
              }
              onSelect(selection)
            },
          ) {
            val text = when (selectionOption) {
              ServerEnum.EmptyResponseEntry -> null
              is ServerEnum.Entry -> selectionOption.name
            }
            Text(text = text ?: "")
          }
        }
      }
    }
    extraContent?.invoke(this@Column, currentEntry)
  }
}

@Preview
@Composable
fun EnumDropdownMenuPreview() {
  CradleTrialAppTheme {
    Surface {
      var currentSelection: EnumSelection.IdOnly? by remember { mutableStateOf(null) }
      EnumDropdownMenuIdOnly(
        currentSelection = currentSelection,
        onSelect = { currentSelection = it },
        serverEnum = ServerEnumCollection.defaultInstance[DropdownType.Place]!!,
        label = { Text("my label") }
      )
    }
  }
}

@Preview
@Composable
fun EnumDropdownMenuWithOtherPreview() {
  CradleTrialAppTheme {
    Surface {
      val enum = ServerEnumCollection.defaultInstance[DropdownType.CauseForHduOrItuAdmission]!!
      var selection: EnumSelection.WithOther? by remember {
        mutableStateOf(
          EnumSelection.WithOther(
            enum.otherEntry!!.id,
            "My other string"
          )
        )
      }
      EnumDropdownMenuWithOther(
        currentSelection = selection,
        onSelect = { selection = it },
        label = { Text("hi") },
        serverEnum = enum,
        modifier = Modifier.fillMaxWidth(),
        dropdownTextModifier = Modifier.fillMaxWidth(),
        errorHint = "My error"
      )
    }
  }
}
