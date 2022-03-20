package org.welbodipartnership.cradle5.patients.details

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.PatientReferralInfo
import org.welbodipartnership.cradle5.data.database.entities.TouchedState
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.patients.PatientPreviewClasses
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrUnknown
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate

/**
 * Shows the details for a [Patient]
 */
@Composable
fun PatientCard(patient: Patient, facility: Facility?, modifier: Modifier = Modifier) {
  BaseDetailsCard(title = stringResource(R.string.patient_registration_card_title), modifier = modifier) {
    val spacerHeight = 4.dp
    Spacer(modifier = Modifier.height(spacerHeight))
    val serverErrorMessage = patient.serverErrorMessage
    if (serverErrorMessage != null) {
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.error) {
        LabelAndValueOrNone(
          label = stringResource(R.string.errors_from_sync_label),
          value = serverErrorMessage
        )
      }
    }

    LabelAndValueOrNone(
      label = stringResource(R.string.patient_registration_card_id_label),
      value = patient.serverPatientId?.toString()
        ?: stringResource(R.string.patient_registration_server_id_not_available),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrNone(
      label = stringResource(R.string.patient_registration_initials_label),
      value = patient.initials,
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.patient_registration_presentation_date_label),
      value = patient.presentationDate?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.patient_registration_date_of_birth_label),
      value = patient.dateOfBirth?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.patient_registration_age_label),
      value = patient.dateOfBirth?.getAgeInYearsFromNow()?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.patient_registration_healthcare_facility_label),
      value = facility?.name,
    )
  }
}

@Preview
@Composable
fun PatientCardPreview() {
  CradleTrialAppTheme {
    val scrollState = rememberScrollState()
    PatientCard(
      patient = PatientPreviewClasses.createTestPatient(),
      facility = null,
      modifier = Modifier.verticalScroll(scrollState)
    )
  }
}
