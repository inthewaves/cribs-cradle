package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@Composable
fun BooleanRadioButtonRow(
  isTrue: Boolean?,
  onBooleanChange: (newBoolean: Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val focusManager = LocalFocusManager.current
  val previous = LocalContentAlpha.current
  CompositionLocalProvider(
    LocalContentAlpha provides if (enabled) previous else ContentAlpha.disabled
  ) {
    Row(
      modifier,
      verticalAlignment = Alignment.CenterVertically
    ) {
      RadioButton(
        selected = isTrue == true,
        onClick = {
          onBooleanChange(true)
          focusManager.clearFocus()
        },
        enabled = enabled,
      )
      Text(stringResource(R.string.yes))
      Spacer(Modifier.width(8.dp))
      RadioButton(
        selected = isTrue == false,
        onClick = {
          onBooleanChange(false)
          focusManager.clearFocus()
        },
        enabled = enabled,
      )
      Text(stringResource(R.string.no))
      // radio button radius is 10
      Spacer(Modifier.width(15.dp))
    }
  }
}

@Preview
@Composable
fun BooleanRadioButtonRowPreview() {
  CradleTrialAppTheme {
    Surface {
      Column {
        BooleanRadioButtonRow(isTrue = true, onBooleanChange = {})
        BooleanRadioButtonRow(isTrue = false, onBooleanChange = {})
        BooleanRadioButtonRow(isTrue = null, onBooleanChange = {})
      }
    }
  }
}
