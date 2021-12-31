package org.welbodipartnership.cradle5.patients.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.date.FormDate

/**
 * Shows the details for a [Patient]
 */
@Composable
fun PatientCard(patient: Patient, modifier: Modifier = Modifier) {
  Card(modifier) {
    Column(Modifier.fillMaxWidth()) {
      val spacerHeight = 4.dp
      val padding = Modifier.padding(horizontal = 16.dp)
      LabelAndValue(
        label = stringResource(R.string.patient_card_id_label),
        value = patient.id.toString(),
        modifier = padding
      )
      Spacer(modifier = Modifier.height(spacerHeight))
      LabelAndValue(
        label = stringResource(R.string.patient_card_initials_label),
        value = patient.initials,
        textModifier = padding
      )
      Spacer(modifier = Modifier.height(spacerHeight))
      LabelAndValue(
        label = stringResource(R.string.patient_card_presentation_date_label),
        value = patient.presentationDate.toString(),
        textModifier = padding
      )
      Spacer(modifier = Modifier.height(spacerHeight))
      LabelAndValue(
        label = stringResource(R.string.patient_card_presentation_date_label),
        value = patient.presentationDate.toString(),
        textModifier = padding
      )
    }
  }
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
      style = MaterialTheme.typography.subtitle2,
      modifier = textModifier
    )
    Text(
      text = value,
      style = MaterialTheme.typography.body1,
      modifier = textModifier
    )
  }
}

@Preview
@Composable
fun PatientDetailsPreview() {
  CradleTrialAppTheme {
    // Scaffold {
    PatientCard(
      patient = Patient(
        initials = "AA",
        presentationDate = FormDate(day = 10, month = 2, year = 1995),
        dateOfBirth = FormDate(day = 19, month = 8, year = 1989),
        isExactDateOfBirth = false,
        lastUpdatedTimestamp = 162224953
      )
    )
    // }
  }
}
