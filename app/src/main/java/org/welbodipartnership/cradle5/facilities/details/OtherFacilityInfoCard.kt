package org.welbodipartnership.cradle5.facilities.details

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.cradleform.details.BaseDetailsCard
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone

@Composable
fun OtherFacilityInfoCard(
  hasVisited: Boolean,
  localNotes: String?,
  onEditOtherInfoButtonClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BaseDetailsCard(title = null, modifier = modifier) {
    val spacerHeight = 8.dp
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
      Text(stringResource(R.string.facility_other_info_card_description))
    }

    LabelAndValueOrNone(
      label = stringResource(R.string.facility_has_visited_label),
      value = stringResource(if (hasVisited) R.string.yes else R.string.no)
    )
    Spacer(modifier = Modifier.height(spacerHeight))

    LabelAndValueOrNone(
      label = stringResource(R.string.local_notes_label),
      value = localNotes?.ifBlank { null }
    )
    Spacer(modifier = Modifier.height(spacerHeight))

    TextButton(
      onClick = onEditOtherInfoButtonClick,
      modifier = Modifier.align(Alignment.End)
    ) {
      Text(stringResource(R.string.edit_button))
    }
  }
}
