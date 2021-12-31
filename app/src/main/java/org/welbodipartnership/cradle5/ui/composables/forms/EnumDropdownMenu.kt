package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@Composable
fun EnumDropdownMenu(
  currentSelection: ServerEnum.Entry?,
  onSelect: (ServerEnum.Entry) -> Unit,
  serverEnum: ServerEnum,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier) {
    OutlinedTextField(
      readOnly = true,
      value = currentSelection?.name ?: "",
      onValueChange = { },
      label = { Text("Label") },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(
          expanded = expanded
        )
      },
      colors = ExposedDropdownMenuDefaults.textFieldColors()
    )
    ExposedDropdownMenu(
      expanded = expanded,
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

@Preview
@Composable
fun EnumDropdownMenuPreview() {
  CradleTrialAppTheme {
    Surface {
      var currentSelection: ServerEnum.Entry? by remember { mutableStateOf(null) }
      EnumDropdownMenu(
        currentSelection = currentSelection,
        onSelect = { currentSelection = it },
        serverEnum = ServerEnumCollection.defaultInstance[DropdownType.Place]!!
      )
    }
  }
}