package org.welbodipartnership.cradle5.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.ui.formatters.unknownSelectionFormatter

@Composable
fun LabelAndValueOrNone(
  label: String,
  value: String?,
  modifier: Modifier = Modifier,
  textModifier: Modifier = Modifier,
) {
  LabelAndValue(
    label = label,
    value = value ?: stringResource(R.string.none),
    modifier = modifier,
    textModifier = textModifier
  )
}

@Composable
fun LabelAndValueOrUnknown(
  label: String,
  value: String?,
  modifier: Modifier = Modifier,
  textModifier: Modifier = Modifier,
) {
  LabelAndValue(
    label = label,
    value = value ?: stringResource(R.string.unknown),
    modifier = modifier,
    textModifier = textModifier
  )
}

@Composable
private fun LabelAndValue(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  textModifier: Modifier = Modifier,
) {
  Column(modifier) {
    Text(
      text = label,
      style = MaterialTheme.typography.subtitle1,
      modifier = textModifier
    )
    Text(
      text = value,
      style = MaterialTheme.typography.body1,
      modifier = textModifier
    )
  }
}

@Composable
fun LabelAndValueForDropdownOrUnknown(
  dropdownType: DropdownType,
  label: String,
  enumValue: EnumSelection?,
  enumCollection: ServerEnumCollection,
  modifier: Modifier = Modifier,
  textModifier: Modifier = Modifier,
) {
  val value: String? = when (enumValue) {
    is EnumSelection.IdOnly -> {
      enumValue.getSelectionString(enumCollection[dropdownType]) { unknownSelectionFormatter(it) }
    }
    is EnumSelection.WithOther -> {
      enumValue.getSelectionString(enumCollection[dropdownType]) { unknownSelectionFormatter(it) }
    }
    null -> null
  }
  LabelAndValueOrUnknown(
    label = label,
    value = value,
    modifier = modifier,
    textModifier = textModifier
  )
}
