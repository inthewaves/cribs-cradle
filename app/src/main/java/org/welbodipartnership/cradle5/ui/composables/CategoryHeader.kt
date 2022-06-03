package org.welbodipartnership.cradle5.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.ui.composables.forms.MoreInfoIconButton
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@Composable
fun CategoryHeader(
  text: String,
  modifier: Modifier = Modifier,
  moreInfoText: String? = null,
  textModifier: Modifier = Modifier
) {
  Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = text,
      style = MaterialTheme.typography.h6,
      modifier = textModifier.weight(1f),
    )
    moreInfoText?.let {
      MoreInfoIconButton(
        moreInfoText = it,
      )
    }
  }
}

@Preview
@Composable
fun CategoryHeaderPreview() {
  CradleTrialAppTheme {
    Surface {
      Column {
        CategoryHeader(
          text = stringResource(R.string.outcomes_age_at_delivery_label),
          moreInfoText = "More info"
        )

        CategoryHeader(
          text = stringResource(R.string.outcomes_eclampsia_label),
        )

        CategoryHeader(
          text = stringResource(R.string.outcomes_maternal_death_label),
          moreInfoText = stringResource(R.string.outcomes_maternal_death_more_info),
        )
      }
    }
  }
}
