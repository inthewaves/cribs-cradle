package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.compose.forms.state.DistrictState
import org.welbodipartnership.cradle5.compose.forms.state.HealthcareFacilityState
import org.welbodipartnership.cradle5.compose.forms.state.NonEmptyTextState
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.patients.form.DistrictListDropdown
import org.welbodipartnership.cradle5.patients.form.FacilityListDropdown

@Composable
fun DistrictAndFacilityFormPair(
  districtState: DistrictState,
  districtLabel: @Composable() (() -> Unit)?,
  facilityState: HealthcareFacilityState,
  facilityCustomTextState: NonEmptyTextState?,
  facilityLabel: @Composable() (() -> Unit)?,
  districtPagingFlow: Flow<PagingData<District>>,
  facilityPagingFlowGetter: (District?) -> Flow<PagingData<Facility>>,
  textFieldToTextFieldHeight: Dp,
  enabled: Boolean = true,
) {
  DistrictListDropdown(
    state = districtState,
    pagingItemFlow = districtPagingFlow,
    label = districtLabel,
    modifier = Modifier.fillMaxWidth(),
    textFieldModifier = Modifier
      .fillMaxWidth()
      .then(districtState.createFocusChangeModifier()),
    extraOnItemSelected = { old, new ->
      if (old != new) {
        facilityState.reset()
        facilityCustomTextState?.reset()
      }
    },
    enabled = enabled
  )

  Spacer(Modifier.height(textFieldToTextFieldHeight))

  if (facilityCustomTextState != null && districtState.stateValue?.district?.isOther == true) {
    OutlinedTextFieldWithErrorHint(
      value = facilityCustomTextState.stateValue ?: "",
      onValueChange = { facilityCustomTextState.stateValue = it },
      modifier = Modifier.fillMaxWidth(),
      label = facilityLabel,
      colors = darkerDisabledOutlinedTextFieldColors(),
      errorHint = facilityCustomTextState.getError(),
      enabled = enabled
    )
  } else {
    val fromFacilityFlow = remember(districtState.stateValue) {
      facilityPagingFlowGetter(districtState.stateValue?.district)
    }

    FacilityListDropdown(
      state = facilityState,
      pagingItemFlow = fromFacilityFlow,
      label = facilityLabel,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier
        .fillMaxWidth()
        .then(facilityState.createFocusChangeModifier()),
      enabled = enabled
    )
  }
}
