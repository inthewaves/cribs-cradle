package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@Composable
fun BooleanRadioButtonRow(
  isTrue: Boolean?,
  onBooleanChange: (newBoolean: Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      selected = isTrue == true,
      onClick = { onBooleanChange(true) },
    )
    Text(stringResource(R.string.yes))
    Spacer(Modifier.width(8.dp))
    RadioButton(
      selected = isTrue == false,
      onClick = { onBooleanChange(false) },
    )
    Text(stringResource(R.string.no))
    // radio button radius is 10
    Spacer(Modifier.width(15.dp))
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