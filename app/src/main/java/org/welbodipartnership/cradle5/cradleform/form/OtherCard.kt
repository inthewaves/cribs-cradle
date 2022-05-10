package org.welbodipartnership.cradle5.cradleform.form

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.cradleform.details.BaseDetailsCard
import org.welbodipartnership.cradle5.ui.composables.forms.BooleanRadioButtonRow
import org.welbodipartnership.cradle5.ui.composables.forms.BringIntoViewOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.MoreInfoIconButton
import org.welbodipartnership.cradle5.ui.composables.forms.darkerDisabledOutlinedTextFieldColors

@Composable
fun OtherCard(
  hideDraft: Boolean,
  isDraft: Boolean?,
  onIsDraftChange: (Boolean) -> Unit,
  localNotes: String,
  onLocalNotesChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  BaseDetailsCard(title = null, modifier) {
    if (!hideDraft) {
      RequiredText(stringResource(R.string.mark_as_draft_label))
      Row {
        BooleanRadioButtonRow(isTrue = isDraft, onBooleanChange = onIsDraftChange)
        MoreInfoIconButton(stringResource(R.string.other_card_draft_more_info))
      }
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(stringResource(R.string.mark_as_draft_description))
      }
      Spacer(Modifier.height(8.dp))
    }

    BringIntoViewOutlinedTextField(
      value = localNotes,
      onValueChange = onLocalNotesChange,
      modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(R.string.local_notes_label)) },
      enabled = true,
      colors = darkerDisabledOutlinedTextFieldColors()
    )
    Spacer(Modifier.height(4.dp))
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
      Text(stringResource(R.string.local_notes_description))
    }
  }
}
