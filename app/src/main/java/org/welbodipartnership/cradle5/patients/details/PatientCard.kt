package org.welbodipartnership.cradle5.patients.details

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate

/**
 * Shows the details for a [Patient]
 */
@Composable
fun PatientCard(patient: Patient, facility: Facility?, modifier: Modifier = Modifier) {
  BaseDetailsCard(title = stringResource(R.string.patient_registration_card_title), modifier = modifier) {
    val spacerHeight = 4.dp
    LabelAndValueOrNone(
      label = stringResource(R.string.patient_registration_card_id_label),
      value = patient.serverPatientId?.toString() ?:
      stringResource(R.string.patient_registration_server_id_not_available),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrNone(
      label = stringResource(R.string.patient_registration_initials_label),
      value = patient.initials,
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrNone(
      label = stringResource(R.string.patient_registration_presentation_date_label),
      value = patient.presentationDate.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrNone(
      label = stringResource(R.string.patient_registration_date_of_birth_label),
      value = patient.dateOfBirth.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrNone(
      label = stringResource(R.string.patient_registration_age_label),
      value = patient.dateOfBirth.getAgeInYearsFromNow().toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrNone(
      label = stringResource(R.string.patient_registration_healthcare_facility_label),
      value = facility?.name ?: stringResource(R.string.unknown),
    )
  }
}

@Preview
@Composable
fun PatientCardPreview() {
  CradleTrialAppTheme {
    // Scaffold {
    val scrollState = rememberScrollState()
    PatientCard(
      patient = Patient(
        initials = "AA",
        serverInfo = null,
        presentationDate = FormDate(day = 10, month = 2, year = 1995),
        dateOfBirth = FormDate(day = 19, month = 8, year = 1989),
        healthcareFacilityId = 50L,
        lastUpdatedTimestamp = 162224953
      ),
      facility = null,
      modifier = Modifier.verticalScroll(scrollState)
    )
    // }
  }
}
