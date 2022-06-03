package org.welbodipartnership.cradle5.patients.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.resultentities.PatientFacilityDistrictOutcomes
import org.welbodipartnership.cradle5.patients.PatientPreviewClasses
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrUnknown
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

/**
 * Shows the details for a [Patient]
 */
@Composable
fun PatientCard(
  patientAndRelatedInfo: PatientFacilityDistrictOutcomes,
  modifier: Modifier = Modifier
) {
  val (
    patient: Patient,
    facility: Facility?,
    referralFromDistrict: District?,
    referralFromFacility: Facility?,
    referralToDistrict: District?,
    referralToFacility: Facility?,
  ) = patientAndRelatedInfo

  BaseDetailsCard(title = stringResource(R.string.patient_registration_card_title), modifier = modifier) {
    val spacerHeight = 6.dp
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

    Spacer(modifier = Modifier.height(spacerHeight * 2))

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
    LabelAndValueOrNone(
      label = stringResource(R.string.patient_registration_registration_date_label),
      value = patient.registrationDate.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.patient_registration_presentation_date_label),
      value = patient.presentationDate?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.patient_registration_age_label),
      value = patient.dateOfBirth?.getAgeInYearsFromNow()?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.patient_address_label),
      value = patient.address?.ifBlank { null },
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.patient_registration_healthcare_facility_label),
      value = facility?.name,
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.patient_referral_checkbox_label),
      value = stringResource(
        if (
          patient.referralInfo != null || patient.referralInfoTouched.nullEnabledState == true
        ) R.string.yes else R.string.no
      ),
    )
    if (patient.referralInfo != null || patient.referralInfoTouched.nullEnabledState == true) {
      Spacer(modifier = Modifier.height(spacerHeight))
      LabelAndValueOrUnknown(
        label = stringResource(R.string.patient_referral_info_from_district_label),
        value = referralFromDistrict?.name,
      )
      Spacer(modifier = Modifier.height(spacerHeight))
      LabelAndValueOrUnknown(
        label = stringResource(R.string.patient_referral_info_from_facility_label),
        value = if (referralFromDistrict?.isOther == true && patient.referralInfo != null) {
          patient.referralInfo!!.fromFacilityText
        } else {
          referralFromFacility?.name
        },
      )
      Spacer(modifier = Modifier.height(spacerHeight))
      LabelAndValueOrUnknown(
        label = stringResource(R.string.patient_referral_info_to_district_label),
        value = referralToDistrict?.name,
      )
      Spacer(modifier = Modifier.height(spacerHeight))
      LabelAndValueOrUnknown(
        label = stringResource(R.string.patient_referral_info_to_facility_label),
        value = if (referralToDistrict?.isOther == true && patient.referralInfo != null) {
          patient.referralInfo!!.toFacilityText
        } else {
          referralToFacility?.name
        },
      )
    }
  }
}

@Preview
@Composable
fun PatientCardPreview() {
  CradleTrialAppTheme {
    val scrollState = rememberScrollState()
    PatientCard(
      PatientFacilityDistrictOutcomes(
        PatientPreviewClasses.createTestPatient(),
        null,
        null,
        null,
        null,
        null,
        null
      ),
      modifier = Modifier.verticalScroll(scrollState)
    )
  }
}
