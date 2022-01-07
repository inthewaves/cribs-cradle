package org.welbodipartnership.cradle5.patients.details

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
import org.welbodipartnership.cradle5.domain.patients.PatientsManager
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone

@Composable
fun OtherInfoCard(
  editState: PatientsManager.FormEditState?,
  isPatientUploadedToServer: Boolean,
  isDraft: Boolean,
  localNotes: String?,
  onEditOtherInfoButtonClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BaseDetailsCard(title = null, modifier = modifier) {
    val spacerHeight = 8.dp
    if (!isPatientUploadedToServer) {
      LabelAndValueOrNone(
        label = stringResource(R.string.marked_as_draft_label),
        value = stringResource(if (isDraft) R.string.yes else R.string.no)
      )
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
          stringResource(
            if (isDraft) {
              R.string.mark_as_draft_yes_patient_will_not_be_synced
            } else {
              R.string.mark_as_draft_no_patient_will_be_synced
            }
          )
        )
      }
      Spacer(modifier = Modifier.height(spacerHeight))
    }
    LabelAndValueOrNone(
      label = stringResource(R.string.local_notes_label),
      value = localNotes?.ifBlank { null }
    )
    Spacer(modifier = Modifier.height(spacerHeight))

    val canEdit = if (isPatientUploadedToServer) {
      true
    } else {
      editState?.canEdit == true
    }
    TextButton(
      onClick = onEditOtherInfoButtonClick,
      enabled = canEdit,
      modifier = Modifier.align(Alignment.End)
    ) {
      Text(stringResource(R.string.edit_button))
    }
  }
}
