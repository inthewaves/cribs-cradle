package org.welbodipartnership.cradle5.facilities.details

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.patients.details.BaseDetailsCard
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone

/**
 * Shows the details for a [Patient]
 */
@Composable
fun FacilityCard(facility: Facility, modifier: Modifier = Modifier) {
  BaseDetailsCard(
    title = stringResource(R.string.facility_card_title),
    modifier = modifier
  ) {
    val spacerHeight = 4.dp
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrNone(
      label = stringResource(R.string.facility_card_id_label),
      value = facility.id.toString()
    )
    Spacer(modifier = Modifier.height(spacerHeight))
    LabelAndValueOrNone(
      label = stringResource(R.string.facility_card_name_label),
      value = facility.name,
    )
  }
}
