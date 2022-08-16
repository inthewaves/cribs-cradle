package org.welbodipartnership.cradle5.cradleform.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.cradleform.CradleFormPreviewClasses
import org.welbodipartnership.cradle5.cradleform.form.ChecklistMissedList
import org.welbodipartnership.cradle5.cradleform.form.PowerSupplyList
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.resultentities.CradleTrainingFormFacilityDistrict
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrUnknown
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import java.time.ZoneId

/**
 * Shows the details for a [CradleTra]
 */
@Composable
fun CradleFormCard(
  formAndRelatedInfo: CradleTrainingFormFacilityDistrict,
  modifier: Modifier = Modifier
) {
  val (
    form: CradleTrainingForm,
    facility: Facility?,
    district: District?
  ) = formAndRelatedInfo

  BaseDetailsCard(title = stringResource(R.string.cradle_form_title), modifier = modifier) {
    val spacerHeight = 6.dp
    Spacer(modifier = Modifier.height(spacerHeight))
    val serverErrorMessage = form.serverErrorMessage
    if (!form.isUploadedToServer && serverErrorMessage != null) {
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.error) {
        LabelAndValueOrNone(
          label = stringResource(R.string.errors_from_sync_label),
          value = serverErrorMessage
        )
      }
    }

    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_record_last_updated_label),
      value = form.recordLastUpdated
        ?.withZoneSameInstant(ZoneId.systemDefault())
        ?.format(CradleTrainingForm.friendlyDateFormatterForRecordLastUpdated),
    )

    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_district_label),
      value = district?.name,
    )

    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_healthcare_facility_label),
      value = facility?.name,
    )

    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_date_of_training_label),
      value = form.dateOfTraining?.toString(),
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(
      stringResource(R.string.cradle_form_today_during_the_cradle_training_subtitle),
      style = MaterialTheme.typography.h6,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_number_of_functioning_bp_devices_label),
      value = form.numOfBpDevicesFunctioning?.toString(),
    )

    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_number_of_functioning_cradle_devices_label),
      value = form.numOfCradleDevicesFunctioning?.toString(),
    )

    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_number_of_broken_cradle_devices_label),
      value = form.numOfCradleDevicesBroken?.toString(),
    )

    Spacer(modifier = Modifier.height(spacerHeight))
    Column {
      Text(
        stringResource(R.string.cradle_form_power_supply_label),
        style = MaterialTheme.typography.subtitle1,
      )
      PowerSupplyList(powerSupply = form.powerSupply, onPowerSupplyChanged = {}, enabled = false)
    }

    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_staff_working_at_facility_label),
      value = form.totalStaffWorking?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_staff_providing_maternity_services_at_facility_label),
      value = form.totalStaffProvidingMaternityServices?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_staff_trained_today_label),
      value = form.totalStaffTrainedToday?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_doctors_trained_today_label),
      value = form.totalStaffTrainedTodayDoctors?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_midwives_trained_today_label),
      value = form.totalStaffTrainedTodayMidwives?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_SACHOS_trained_today_label),
      value = form.totalStaffTrainedTodaySACHOS?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_SRNs_trained_today_label),
      value = form.totalStaffTrainedTodaySRNs?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_SECHNs_trained_today_label),
      value = form.totalStaffTrainedTodaySECHNs?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_CHOs_trained_today_label),
      value = form.totalStaffTrainedTodayCHOs?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_CHAs_trained_today_label),
      value = form.totalStaffTrainedTodayCHAs?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_MCH_aides_trained_today_label),
      value = form.totalStaffTrainedTodayMCHAides?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_TBA_trained_today_label),
      value = form.totalStaffTrainedTodayTBA?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_volunteers_trained_today_label),
      value = form.totalStaffTrainedTodayVolunteers?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_trained_before_label),
      value = form.totalStaffTrainedBefore?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_staff_observed_and_scored),
      value = form.totalStaffObservedAndScored?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrUnknown(
      label = stringResource(R.string.cradle_form_total_trained_score_more_than_8_label),
      value = form.totalStaffTrainedScoredMoreThan14?.toString(),
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    ChecklistMissedList(
      checklistMissed = form.checklistMissed,
      onChecklistMissedChanged = {},
      enabled = false,
    )
  }
}

@Preview
@Composable
fun CradleFormCardPreview() {
  CradleTrialAppTheme {
    val scrollState = rememberScrollState()
    CradleFormCard(
      CradleTrainingFormFacilityDistrict(
        CradleFormPreviewClasses.createTestCradleForm(),
        CradleFormPreviewClasses.createTestFacility(),
        CradleFormPreviewClasses.createTestDistrict(),
      ),
      modifier = Modifier.verticalScroll(scrollState)
    )
  }
}
