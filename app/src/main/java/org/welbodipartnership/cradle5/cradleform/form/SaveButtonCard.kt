package org.welbodipartnership.cradle5.cradleform.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@Composable
fun SaveButtonCard(
  onSaveButtonClick: () -> Unit,
  text: String,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
) {
  Card(
    elevation = 4.dp,
    shape = MaterialTheme.shapes.small,
    modifier = modifier.padding(36.dp)
  ) {
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp),
      onClick = onSaveButtonClick,
      enabled = isEnabled,
    ) {
      Text(text)
    }
  }
}

@Preview
@Composable
fun SaveButtonCardPreview() {
  CradleTrialAppTheme {
    Scaffold {
      SaveButtonCard(onSaveButtonClick = {}, text = "Save")
    }
  }
}
