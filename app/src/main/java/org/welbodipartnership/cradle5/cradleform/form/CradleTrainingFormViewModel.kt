package org.welbodipartnership.cradle5.cradleform.form

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.LeafScreen
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.compose.SavedStateMutableState
import org.welbodipartnership.cradle5.compose.createMutableState
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.PowerSupply
import org.welbodipartnership.cradle5.data.database.resultentities.CradleTrainingFormFacilityDistrict
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.ui.composables.forms.TextFieldState
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import java.time.ZonedDateTime
import javax.inject.Inject

private const val PATIENT_MAX_INITIALS_LENGTH = 5

private val AGE_RANGE = 0L..60L

private val VALID_LENGTH_OF_ITU_HDU_STAY = 1L..100L

private val VALID_DEVICES_NUMBER_RANGE = 0..50
private val VALID_STAFF_NUMBER_RANGE = 0..50

data class FieldError(@StringRes val fieldTitle: Int, val errorMessage: String)

@HiltViewModel
class CradleTrainingFormViewModel @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val handle: SavedStateHandle,
  private val valuesStore: AppValuesStore,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  private val dbWrapper: CradleDatabaseWrapper
) : ViewModel() {
  companion object {
    private const val TAG = "PatientFormViewModel"
  }

  val database = dbWrapper.database!!

  private val existingCradleFormPrimaryKey: Long? =
    handle[LeafScreen.PatientEdit.ARG_PATIENT_PRIMARY_KEY]

  val isExistingEdit = existingCradleFormPrimaryKey != null

  val existingCradleTrainingForm: Flow<CradleTrainingFormFacilityDistrict?> =
    existingCradleFormPrimaryKey?.let { pk ->
      database.cradleTrainingFormDao().getFormFlow(pk)
    } ?: flowOf(null)

  val districtsPagerFlow: Flow<PagingData<District>> = Pager(
    PagingConfig(pageSize = 60, enablePlaceholders = true, maxSize = 200)
  ) { dbWrapper.districtDao().districtsPagingSourceNoOther() }
    .flow
    .cachedIn(viewModelScope)

  val facilitiesForSelfDistrictPagerFlow: Flow<PagingData<Facility>> = valuesStore.districtIdFlow
    .flatMapLatest { districtId ->
      Pager(PagingConfig(pageSize = 60, enablePlaceholders = true, maxSize = 200)) {
        if (districtId != null) {
          dbWrapper.facilitiesDao().facilitiesPagingSource(districtId)
        } else {
          dbWrapper.facilitiesDao().facilitiesPagingSource()
        }
      }.flow
    }.cachedIn(viewModelScope)

  fun getFacilitiesPagingDataForDistrict(district: District?): Flow<PagingData<Facility>> {
    district ?: return emptyFlow()

    return Pager(PagingConfig(pageSize = 60, enablePlaceholders = true, maxSize = 200)) {
      dbWrapper.facilitiesDao().facilitiesPagingSource(districtId = district.id)
    }.flow
  }

  sealed class FormState {
    val isForFormEdit get() = (this as? Ready)?.existingInfo != null

    object Loading : FormState()
    class Ready(val existingInfo: CradleTrainingFormFacilityDistrict?) : FormState()
    object Saving : FormState()
    class SavedNewPatient(val primaryKeyOfForm: Long) : FormState()
    class SavedEditsToExistingPatient(val primaryKeyOfForm: Long) : FormState()
    class FailedException(val exception: Exception) : FormState()
    class FailedValidation(val errorsBySectionStringId: Map<Int, List<FieldError>>) : FormState()
    class FailedLoading(val message: String) : FormState()
  }

  private val _formState: MutableStateFlow<FormState> = MutableStateFlow(FormState.Loading)
  val formState: StateFlow<FormState> = _formState

  private val isFormDraftState = enabledState("isFormDraft")

  /**
   * putting this outside so that additional validation can work
   */
  private val totalNumberOfStaffWorkingAtThisFacilityState = limitedIntState(
    key = "totalNumberStaffAtThisFacility",
    isMandatory = false,
    range = VALID_STAFF_NUMBER_RANGE
  )

  private val totalNumberOfStaffTrainedState = limitedIntState(
    key = "totalNumberStaffTrained",
    isMandatory = false,
    range = VALID_STAFF_NUMBER_RANGE,
    upperBoundState = totalNumberOfStaffWorkingAtThisFacilityState,
    upperBoundErrorString = R.string.staff_working_error_total_trained_too_large
  )

  val formFieldsNew = CradleTrainingFormFields(
    district = DistrictState(
      isMandatory = true,
      handle.createMutableState("district", null),
      isFormDraftState = isFormDraftState
    ),
    facility = HealthcareFacilityState(
      isMandatory = true,
      handle.createMutableState("facility", null),
      isFormDraftState = isFormDraftState
    ),
    dateOfTraining = dateState(
      key = "dateOfTraining",
      isMandatory = false,
      areApproximateDatesAcceptable = false
    ),
    numOfBpDevicesFunction = limitedIntState(
      key = "numOfBpDevicesFunction",
      isMandatory = false,
      range = VALID_DEVICES_NUMBER_RANGE
    ),
    numOfCradleDevicesFunctioning = limitedIntState(
      key = "numOfCradleDevicesFunction",
      isMandatory = false,
      range = VALID_DEVICES_NUMBER_RANGE
    ),
    numOfCradleDevicesBroken = limitedIntState(
      key = "numOfCradleDevicesBroken",
      isMandatory = false,
      range = VALID_DEVICES_NUMBER_RANGE
    ),
    powerSupply = parcelableState("powerSupply"),
    totalStaffWorking = totalNumberOfStaffWorkingAtThisFacilityState,
    totalStaffProvidingMaternityServices = limitedIntState(
      key = "staffProvidingMatServices",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffWorkingAtThisFacilityState,
      upperBoundErrorString = R.string.staff_working_error_maternity_providers_too_large
    ),
    totalStaffTrainedToday = totalNumberOfStaffTrainedState,
    totalStaffTrainedTodayDoctors = limitedIntState(
      key = "doctorsTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_doctors_too_large
    ),
    totalStaffTrainedTodayMidwives = limitedIntState(
      key = "midwivesTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_midwives_too_large
    ),
    totalStaffTrainedTodaySACHOS = limitedIntState(
      key = "SACHOSTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_SACHOS_too_large
    ),
    totalStaffTrainedTodaySECHNMidwives = limitedIntState(
      key = "SECHNMidwivesTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_SECHN_midwives_too_large
    ),
    totalStaffTrainedTodaySRNs = limitedIntState(
      key = "SRNsTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_SRNs_too_large
    ),
    totalStaffTrainedTodayCHOs = limitedIntState(
      key = "CHOsTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_CHOs_too_large
    ),
    totalStaffTrainedTodayCHAs = limitedIntState(
      key = "CHAsTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_CHAs_too_large
    ),
    totalStaffTrainedTodayCSECHNs = limitedIntState(
      key = "CSECHNsTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_CSECHNs_too_large
    ),
    totalStaffTrainedTodayMCHAides = limitedIntState(
      key = "MCHAidesTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_MCH_aides_too_large
    ),
    totalStaffTrainedTodayTBA = limitedIntState(
      key = "TBAAidesTrained",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_TBA_aides_too_large
    ),
    totalStaffTrainedBefore = limitedIntState(
      key = "totalStaffTrainedBefore",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_trained_in_cradle_before_too_large
    ),
    totalStaffTrainedScoredMoreThan8 = limitedIntState(
      key = "totalStaffTrainedScored8",
      isMandatory = false,
      range = VALID_STAFF_NUMBER_RANGE,
      upperBoundState = totalNumberOfStaffTrainedState,
      upperBoundErrorString = R.string.staff_working_error_scored_8_or_higher_too_large
    ),
    localNotes = handle.createMutableState("formLocalNotes", ""),
    isDraft = isFormDraftState
  )

  init {
    Log.d(TAG, "initializing with pk = $existingCradleFormPrimaryKey")
    viewModelScope.launch(appCoroutineDispatchers.main) {
      if (existingCradleFormPrimaryKey != null) {

        val cradleFormAndDependencies: CradleTrainingFormFacilityDistrict? = database.cradleTrainingFormDao()
          .getFormFacilityDistrict(existingCradleFormPrimaryKey)
        _formState.value = if (cradleFormAndDependencies == null) {
          Log.w(TAG, "Unable to find form with pk $existingCradleFormPrimaryKey")
          FormState.FailedLoading(
            context.getString(R.string.cradle_form_failed_to_load_patient_with_pk_d)
          )
        } else if (cradleFormAndDependencies.form.serverInfo != null) {
          Log.w(TAG, "trying to edit a form with server info")
          FormState.FailedLoading(
            context.getString(R.string.cradle_form_cannot_edit_already_on_server)
          )
        } else {
          val (
            cradleForm: CradleTrainingForm,
            formFacility: Facility?,
            formDistrict: District?,
          ) = cradleFormAndDependencies
          Log.d(TAG, "Setting up form for edit")

          with(formFieldsNew) {
            formDistrict?.let { district ->
              this.district.stateValue = DistrictAndPosition(
                district,
                district.id
                  .let { id -> dbWrapper.districtDao().getDistrictIndexWhenOrderedById(id) }
                  ?.coerceAtLeast(0)
              )
            }

            val facilityPosition = formFacility?.id
              ?.let { facilityId ->
                database.facilitiesDao().getFacilityIndexWhenOrderedByName(
                  facilityId = facilityId,
                  districtId = valuesStore.districtIdFlow.firstOrNull() ?: Facility.DEFAULT_DISTRICT_ID
                )
              }?.toInt()?.coerceAtLeast(0)
            facility.stateValue = formFacility?.let { FacilityAndPosition(it, facilityPosition) }

            dateOfTraining.setStateFromFormDate(cradleForm.dateOfTraining)
            numOfBpDevicesFunction.backingState.value = cradleForm.numOfBpDevicesFunctioning?.toString() ?: ""
            numOfCradleDevicesFunctioning.backingState.value = cradleForm.numOfCradleDevicesFunctioning?.toString() ?: ""
            numOfCradleDevicesBroken.backingState.value = cradleForm.numOfCradleDevicesBroken?.toString() ?: ""
            powerSupply.value = cradleForm.powerSupply

            totalStaffWorking.backingState.value = cradleForm.totalStaffWorking?.toString() ?: ""
            totalStaffProvidingMaternityServices.backingState.value = cradleForm.totalStaffProvidingMaternityServices?.toString() ?: ""
            totalStaffTrainedToday.backingState.value = cradleForm.totalStaffTrainedToday?.toString() ?: ""
            totalStaffTrainedTodayDoctors.backingState.value = cradleForm.totalStaffTrainedTodayDoctors?.toString() ?: ""
            totalStaffTrainedTodayMidwives.backingState.value = cradleForm.totalStaffTrainedTodayMidwives?.toString() ?: ""
            totalStaffTrainedTodaySACHOS.backingState.value = cradleForm.totalStaffTrainedTodaySACHOS?.toString() ?: ""
            totalStaffTrainedTodaySECHNMidwives.backingState.value = cradleForm.totalStaffTrainedTodaySECHNMidwives?.toString() ?: ""
            totalStaffTrainedTodaySRNs.backingState.value = cradleForm.totalStaffTrainedTodaySRNs?.toString() ?: ""
            totalStaffTrainedTodayCHOs.backingState.value = cradleForm.totalStaffTrainedTodayCHOs?.toString() ?: ""
            totalStaffTrainedTodayCHAs.backingState.value = cradleForm.totalStaffTrainedTodayCHAs?.toString() ?: ""
            totalStaffTrainedTodayCSECHNs.backingState.value = cradleForm.totalStaffTrainedTodayCSECHNs?.toString() ?: ""
            totalStaffTrainedTodayMCHAides.backingState.value = cradleForm.totalStaffTrainedTodayMCHAides?.toString() ?: ""
            totalStaffTrainedTodayTBA.backingState.value = cradleForm.totalStaffTrainedTodayTBA?.toString() ?: ""
            totalStaffTrainedBefore.backingState.value = cradleForm.totalStaffTrainedBefore?.toString() ?: ""
            totalStaffTrainedScoredMoreThan8.backingState.value = cradleForm.totalStaffTrainedScoredMoreThan8?.toString() ?: ""

            localNotes.value = cradleForm.localNotes ?: ""
            isDraft.value = cradleForm.isDraft
          }

          FormState.Ready(cradleFormAndDependencies)
        }
      } else {
        valuesStore.districtIdFlow.firstOrNull()?.let { districtPk ->
          dbWrapper.districtDao().getDistrict(districtPk)?.let { district ->
            formFieldsNew.district.stateValue = DistrictAndPosition(
              district,
              district.id
                .let { id -> dbWrapper.districtDao().getDistrictIndexWhenOrderedById(id) }
                ?.coerceAtLeast(0)
            )
          }
        }
        _formState.value = FormState.Ready(null)
      }
    }
  }

  fun save() {
    Log.d(TAG, "save()")
    saveRequestChannel.trySend(Unit)
  }

  fun isDraft() = formFieldsNew.isDraft.value == true

  private val saveRequestChannel = viewModelScope.actor<Unit>(
    context = appCoroutineDispatchers.default,
    capacity = Channel.RENDEZVOUS,
  ) {
    for (saveTick in channel) {
      Log.d(TAG, "Handling save request")
      _formState.value = FormState.Saving
      val isDraft = isDraft()

      formFieldsNew.forceAllErrors()

      val fieldToErrorMap = linkedMapOf<Int, List<FieldError>>()
      fun LinkedHashMap<Int, List<FieldError>>.addFieldError(
        @StringRes categoryTitle: Int,
        @StringRes fieldLabel: Int,
        errorMessage: String
      ) {
        (getOrPut(categoryTitle) { ArrayList() } as ArrayList<FieldError>).add(
          FieldError(fieldLabel, errorMessage)
        )
      }

      _formState.value = try {
        val existingForm: CradleTrainingFormFacilityDistrict? = existingCradleTrainingForm.first()
        require(
          existingCradleFormPrimaryKey == null ||
            existingForm?.form?.id == existingCradleFormPrimaryKey
        ) {
          "Existing form doesn't match primary key"
        }

        val newForm = with(formFieldsNew) {
          if (!district.isValid) {
            fieldToErrorMap.addFieldError(
              categoryTitle = R.string.cradle_form_title,
              fieldLabel = R.string.cradle_form_district_label,
              errorMessage = district.errorFor(context, district.stateValue)
            )
          }
          if (!facility.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_title,
              R.string.cradle_form_healthcare_facility_label,
              facility.errorFor(context, facility.stateValue)
            )
          }
          if (!dateOfTraining.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_title,
              R.string.cradle_form_date_of_training_label,
              dateOfTraining.errorFor(context, dateOfTraining.stateValue)
            )
          }
          if (!numOfBpDevicesFunction.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_title,
              R.string.cradle_form_number_of_functioning_bp_devices_label,
              numOfBpDevicesFunction.errorFor(context, numOfBpDevicesFunction.stateValue)
            )
          }
          if (!numOfCradleDevicesFunctioning.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_title,
              R.string.cradle_form_number_of_functioning_cradle_devices_label,
              numOfCradleDevicesFunctioning.errorFor(context, numOfCradleDevicesFunctioning.stateValue)
            )
          }
          if (!numOfCradleDevicesBroken.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_title,
              R.string.cradle_form_number_of_broken_cradle_devices_label,
              numOfCradleDevicesBroken.errorFor(context, numOfCradleDevicesBroken.stateValue)
            )
          }
          // powerSupply always valid
          if (!totalStaffWorking.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_title,
              R.string.cradle_form_total_staff_working_at_facility_label,
              totalStaffWorking.errorFor(context, totalStaffWorking.stateValue)
            )
          }
          if (!totalStaffProvidingMaternityServices.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_title,
              R.string.cradle_form_total_staff_providing_maternity_services_at_facility_label,
              totalStaffProvidingMaternityServices.errorFor(context, totalStaffProvidingMaternityServices.stateValue)
            )
          }

          if (!totalStaffTrainedToday.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_staff_trained_today_label,
              totalStaffTrainedToday.errorFor(context, totalStaffTrainedToday.stateValue)
            )
          }
          if (!totalStaffTrainedTodayDoctors.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_doctors_trained_today_label,
              totalStaffTrainedTodayDoctors.errorFor(context, totalStaffTrainedTodayDoctors.stateValue)
            )
          }
          if (!totalStaffTrainedTodayMidwives.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_midwives_trained_today_label,
              totalStaffTrainedTodayMidwives.errorFor(context, totalStaffTrainedTodayMidwives.stateValue)
            )
          }
          if (!totalStaffTrainedTodaySACHOS.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_SACHOS_trained_today_label,
              totalStaffTrainedTodaySACHOS.errorFor(context, totalStaffTrainedTodaySACHOS.stateValue)
            )
          }
          if (!totalStaffTrainedTodaySECHNMidwives.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_SECHN_midwives_trained_today_label,
              totalStaffTrainedTodaySECHNMidwives.errorFor(context, totalStaffTrainedTodaySECHNMidwives.stateValue)
            )
          }
          if (!totalStaffTrainedTodaySRNs.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_SRNs_trained_today_label,
              totalStaffTrainedTodaySRNs.errorFor(context, totalStaffTrainedTodaySRNs.stateValue)
            )
          }
          if (!totalStaffTrainedTodayCHOs.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_CHOs_trained_today_label,
              totalStaffTrainedTodayCHOs.errorFor(context, totalStaffTrainedTodayCHOs.stateValue)
            )
          }
          if (!totalStaffTrainedTodayCHAs.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_CHAs_trained_today_label,
              totalStaffTrainedTodayCHAs.errorFor(context, totalStaffTrainedTodayCHAs.stateValue)
            )
          }
          if (!totalStaffTrainedTodayCSECHNs.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_CSECHNs_trained_today_label,
              totalStaffTrainedTodayCSECHNs.errorFor(context, totalStaffTrainedTodayCSECHNs.stateValue)
            )
          }
          if (!totalStaffTrainedTodayMCHAides.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_MCH_aides_trained_today_label,
              totalStaffTrainedTodayMCHAides.errorFor(context, totalStaffTrainedTodayMCHAides.stateValue)
            )
          }
          if (!totalStaffTrainedTodayTBA.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_TBA_trained_today_label,
              totalStaffTrainedTodayTBA.errorFor(context, totalStaffTrainedTodayTBA.stateValue)
            )
          }
          if (!totalStaffTrainedBefore.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_trained_before_label,
              totalStaffTrainedBefore.errorFor(context, totalStaffTrainedBefore.stateValue)
            )
          }
          if (!totalStaffTrainedScoredMoreThan8.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.cradle_form_staff_trained_title,
              R.string.cradle_form_total_trained_score_more_than_8_label,
              totalStaffTrainedScoredMoreThan8.errorFor(context, totalStaffTrainedScoredMoreThan8.stateValue)
            )
          }

          runCatching {
            CradleTrainingForm(
              id = existingForm?.form?.id ?: 0L,
              serverInfo = existingForm?.form?.serverInfo,
              serverErrorMessage = null,
              recordLastUpdated = ZonedDateTime.now(),
              district = district.stateValue?.district?.id,
              healthcareFacility = facility.stateValue?.facility?.id,
              dateOfTraining = dateOfTraining.dateFromStateOrNull(),
              numOfBpDevicesFunctioning = numOfBpDevicesFunction.stateValue.toIntOrNull(),
              numOfCradleDevicesFunctioning = numOfCradleDevicesFunctioning.stateValue.toIntOrNull(),
              numOfCradleDevicesBroken = numOfCradleDevicesBroken.stateValue.toIntOrNull(),
              powerSupply = powerSupply.value,
              totalStaffWorking = totalStaffWorking.stateValue.toIntOrNull(),
              totalStaffProvidingMaternityServices = totalStaffProvidingMaternityServices.stateValue.toIntOrNull(),
              totalStaffTrainedToday = totalStaffTrainedToday.stateValue.toIntOrNull(),
              totalStaffTrainedTodayDoctors = totalStaffTrainedTodayDoctors.stateValue.toIntOrNull(),
              totalStaffTrainedTodayMidwives = totalStaffTrainedTodayMidwives.stateValue.toIntOrNull(),
              totalStaffTrainedTodaySACHOS = totalStaffTrainedTodaySACHOS.stateValue.toIntOrNull(),
              totalStaffTrainedTodaySECHNMidwives = totalStaffTrainedTodaySECHNMidwives.stateValue.toIntOrNull(),
              totalStaffTrainedTodaySRNs = totalStaffTrainedTodaySRNs.stateValue.toIntOrNull(),
              totalStaffTrainedTodayCHOs = totalStaffTrainedTodayCHOs.stateValue.toIntOrNull(),
              totalStaffTrainedTodayCHAs = totalStaffTrainedTodayCHAs.stateValue.toIntOrNull(),
              totalStaffTrainedTodayCSECHNs = totalStaffTrainedTodayCSECHNs.stateValue.toIntOrNull(),
              totalStaffTrainedTodayMCHAides = totalStaffTrainedTodayMCHAides.stateValue.toIntOrNull(),
              totalStaffTrainedTodayTBA = totalStaffTrainedTodayTBA.stateValue.toIntOrNull(),
              totalStaffTrainedBefore = totalStaffTrainedBefore.stateValue.toIntOrNull(),
              totalStaffTrainedScoredMoreThan8 = totalStaffTrainedScoredMoreThan8.stateValue.toIntOrNull(),
              localNotes = localNotes.value,
              isDraft = isDraft
            )
          }
        }

        with(formFieldsNew) {
          if (this.isDraft.value == null) {
            fieldToErrorMap.addFieldError(
              R.string.other_card_title,
              R.string.mark_as_draft_label,
              context.getString(R.string.mark_as_draft_not_selected_error)
            )
          }
        }

        if (fieldToErrorMap.isNotEmpty()) {
          Log.d(TAG, "Errors: $fieldToErrorMap")
          FormState.FailedValidation(fieldToErrorMap)
        } else {
          Log.d(TAG, "Attempting to save new form")
          val primaryKey = database.cradleTrainingFormDao().upsert(newForm.getOrThrow())
          if (existingCradleFormPrimaryKey != null) {
            assert(existingCradleFormPrimaryKey == primaryKey)
            FormState.SavedEditsToExistingPatient(primaryKey)
          } else {
            FormState.SavedNewPatient(primaryKey)
          }.also {
            channel.close()
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to save form", e)
        FormState.FailedException(e)
      }
      ensureActive()
    }
  }

  private fun <T : Parcelable> parcelableState(key: String): SavedStateMutableState<T?> =
    handle.createMutableState(key, null)

  private fun enabledState(key: String): SavedStateMutableState<Boolean?> =
    handle.createMutableState(key, null)

  private fun toggleState(key: String, isMandatory: Boolean) = NullableToggleState(
    handle.createMutableState(key, null),
    isFormDraftState = isFormDraftState,
    isMandatory = isMandatory,
  )

  private fun nonNullBooleanState(key: String, defaultValue: Boolean): SavedStateMutableState<Boolean> =
    handle.createMutableState(key, defaultValue)

  private fun dateState(
    key: String,
    isMandatory: Boolean,
    areApproximateDatesAcceptable: Boolean,
  ): NoFutureDateState = NoFutureDateState(
    isMandatory,
    areApproximateDatesAcceptable = areApproximateDatesAcceptable,
    backingState = handle.createMutableState(key, ""),
    isFormDraftState = isFormDraftState,
  )

  private fun limitedIntState(
    key: String,
    isMandatory: Boolean,
    range: IntRange,
  ) = LimitedIntState(
    isMandatory = isMandatory,
    limit = range,
    handle.createMutableState(key, ""),
    isFormDraftState = isFormDraftState,
  )

  private fun limitedIntState(
    key: String,
    isMandatory: Boolean,
    range: IntRange,
    upperBoundState: TextFieldState,
    @StringRes upperBoundErrorString: Int
  ) = LimitedIntStateWithStateUpperBound(
    isMandatory = isMandatory,
    limit = range,
    handle.createMutableState(key, ""),
    isFormDraftState = isFormDraftState,
    stateUpperBound = upperBoundState,
    upperBoundErrorString = upperBoundErrorString
  )

  private fun enumIdOnlyState(
    key: String,
    dropdownType: DropdownType,
    isMandatory: Boolean,
  ) = EnumIdOnlyState(
    valuesStore.serverEnumCollection.value[dropdownType],
    isMandatory = isMandatory,
    handle.createMutableState(key, null),
    isFormDraftState
  )

  private fun enumWithOtherState(
    key: String,
    dropdownType: DropdownType,
    isMandatory: Boolean,
  ) = EnumWithOtherState(
    enum = valuesStore.serverEnumCollection.value[dropdownType],
    isMandatory = isMandatory,
    backingState = handle.createMutableState(key, null),
    isFormDraftState = isFormDraftState,
  )

  @Stable
  data class CradleTrainingFormFields(
    val district: DistrictState,
    val facility: HealthcareFacilityState,
    val dateOfTraining: NoFutureDateState,

    val numOfBpDevicesFunction: LimitedIntState,
    val numOfCradleDevicesFunctioning: LimitedIntState,
    val numOfCradleDevicesBroken: LimitedIntState,
    val powerSupply: MutableState<PowerSupply?>,

    val totalStaffWorking: LimitedIntState,
    val totalStaffProvidingMaternityServices: LimitedIntStateWithStateUpperBound,

    val totalStaffTrainedToday: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodayDoctors: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodayMidwives: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodaySACHOS: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodaySECHNMidwives: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodaySRNs: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodayCHOs: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodayCHAs: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodayCSECHNs: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodayMCHAides: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedTodayTBA: LimitedIntStateWithStateUpperBound,
    /**
     * How many of the staff trained today had ever been trained in CRADLE before?
     */
    val totalStaffTrainedBefore: LimitedIntStateWithStateUpperBound,
    val totalStaffTrainedScoredMoreThan8: LimitedIntStateWithStateUpperBound,
    val localNotes: MutableState<String>,
    val isDraft: SavedStateMutableState<Boolean?>,
  ) {
    fun forceAllErrors() {
      facility.enableShowErrors(force = true)
      dateOfTraining.enableShowErrors(force = true)
      numOfBpDevicesFunction.enableShowErrors(force = true)
      numOfCradleDevicesFunctioning.enableShowErrors(force = true)
      numOfCradleDevicesBroken.enableShowErrors(force = true)
      // powerSupply: MutableState<PowerSupply?> has no errors
      totalStaffWorking.enableShowErrors(force = true)
      totalStaffProvidingMaternityServices.enableShowErrors(force = true)
      totalStaffTrainedToday.enableShowErrors(force = true)
      totalStaffTrainedTodayDoctors.enableShowErrors(force = true)
      totalStaffTrainedTodayMidwives.enableShowErrors(force = true)
      totalStaffTrainedTodaySACHOS.enableShowErrors(force = true)
      totalStaffTrainedTodaySECHNMidwives.enableShowErrors(force = true)
      totalStaffTrainedTodaySRNs.enableShowErrors(force = true)
      totalStaffTrainedTodayCHOs.enableShowErrors(force = true)
      totalStaffTrainedTodayCHAs.enableShowErrors(force = true)
      totalStaffTrainedTodayCSECHNs.enableShowErrors(force = true)
      totalStaffTrainedTodayMCHAides.enableShowErrors(force = true)
      totalStaffTrainedTodayTBA.enableShowErrors(force = true)
      totalStaffTrainedBefore.enableShowErrors(force = true)
      totalStaffTrainedScoredMoreThan8.enableShowErrors(force = true)
    }
  }
}
