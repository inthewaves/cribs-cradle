package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.ui.composables.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@Composable
fun EnumDropdownMenu(
  currentSelection: ServerEnum.Entry?,
  onSelect: (ServerEnum.Entry) -> Unit,
  modifier: Modifier = Modifier,
  textModifier: Modifier = Modifier,
  label: @Composable () -> Unit,
  serverEnum: ServerEnum,
  enabled: Boolean = true,
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = enabled && expanded,
    onExpandedChange = { expanded = it },
    modifier
  ) {
    OutlinedTextField(
      readOnly = true,
      value = currentSelection?.name ?: "",
      onValueChange = { },
      modifier = textModifier,
      label = label,
      enabled = enabled,
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(
          expanded = enabled && expanded
        )
      },
      colors = TextFieldDefaults.outlinedTextFieldColors()
    )
    ExposedDropdownMenu(
      expanded = enabled && expanded,
      onDismissRequest = { expanded = false }
    ) {
      serverEnum.sortedValues.forEach { selectionOption ->
        DropdownMenuItem(
          onClick = {
            expanded = false
            onSelect(selectionOption)
          }
        ) {
          Text(text = selectionOption.name)
        }
      }
    }
  }
}

@Composable
fun EnumDropdownMenuWithOther(
  currentSelection: EnumSelection.WithOther?,
  onSelect: (EnumSelection.WithOther) -> Unit,
  modifier: Modifier = Modifier,
  dropdownTextModifier: Modifier = Modifier,
  otherTextModifier: Modifier = Modifier,
  label: @Composable () -> Unit,
  errorHint: String?,
  serverEnum: ServerEnum,
  enabled: Boolean = true,
  spacerHeight: Dp = 8.dp,
) {
  var expanded by remember { mutableStateOf(false) }
  val currentEntry = currentSelection?.selectionId?.let { serverEnum.getValueFromId(it) }

  Column(modifier) {
    ExposedDropdownMenuBox(
      expanded = enabled && expanded,
      onExpandedChange = { expanded = it }
    ) {
      OutlinedTextField(
        readOnly = true,
        value = currentEntry?.name ?: "",
        onValueChange = {},
        label = label,
        maxLines = 2,
        enabled = enabled,
        trailingIcon = {
          ExposedDropdownMenuDefaults.TrailingIcon(
            expanded = enabled && expanded
          )
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(),
        modifier = dropdownTextModifier
      )
      ExposedDropdownMenu(
        expanded = enabled && expanded,
        onDismissRequest = { expanded = false },
        modifier = dropdownTextModifier,
      ) {
        serverEnum.sortedValues.forEach { selectionOption ->
          DropdownMenuItem(
            onClick = {
              expanded = false
              onSelect(EnumSelection.WithOther(selectionOption.id))
            },
          ) {
            Text(text = selectionOption.name)
          }
        }
      }
    }
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
      label = { Text("If Other, please specify") },
      enabled = isOtherEnabled,
      errorHint = errorHint,
      textFieldModifier = otherTextModifier
    )
  }
}

@Preview
@Composable
fun EnumDropdownMenuPreview() {
  CradleTrialAppTheme {
    Surface {
      var currentSelection: ServerEnum.Entry? by remember { mutableStateOf(null) }
      EnumDropdownMenu(
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
      var selection by remember {
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
