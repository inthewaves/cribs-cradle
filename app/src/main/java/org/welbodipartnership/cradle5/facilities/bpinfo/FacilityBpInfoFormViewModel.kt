package org.welbodipartnership.cradle5.facilities.bpinfo

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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.LeafScreen
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.compose.SavedStateMutableState
import org.welbodipartnership.cradle5.compose.createMutableState
import org.welbodipartnership.cradle5.compose.forms.state.DistrictState
import org.welbodipartnership.cradle5.compose.forms.state.HealthcareFacilityState
import org.welbodipartnership.cradle5.compose.forms.state.LimitedIntState
import org.welbodipartnership.cradle5.compose.forms.state.NoFutureDateState
import org.welbodipartnership.cradle5.compose.forms.state.TextFieldState
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.FacilityBpInfo
import org.welbodipartnership.cradle5.data.database.resultentities.BpInfoFacilityDistrict
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.patients.form.FieldError
import org.welbodipartnership.cradle5.util.DistrictAndPosition
import org.welbodipartnership.cradle5.util.FacilityAndPosition
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import org.welbodipartnership.cradle5.util.datetime.FormDate
import java.time.ZonedDateTime
import javax.inject.Inject

private val FACILITY_BP_COUNT_RANGE = 0..999

@HiltViewModel
class FacilityBpInfoFormViewModel @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val handle: SavedStateHandle,
  private val valuesStore: AppValuesStore,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  private val dbWrapper: CradleDatabaseWrapper
) : ViewModel() {
  companion object {
    private const val TAG = "FacilityBpInfoFormVM"
  }

  val database = dbWrapper.database!!

  private val existingFormPrimaryKey: Long? =
    handle[LeafScreen.FacilityBpInfoEdit.ARG_FORM_PRIMARY_KEY]

  /**
   * Primary key of the facility if the user opened from a facility details screen
   */
  private val initialFacilityPk: Long? =
    handle.get<String?>(LeafScreen.FacilityBpInfoCreate.OPTIONAL_ARG_FACILITY_PK)?.toLongOrNull()

  val isExistingEdit = existingFormPrimaryKey != null

  val canChangeFacility = !isExistingEdit && initialFacilityPk == null

  val existingForm: Flow<BpInfoFacilityDistrict?> =
    existingFormPrimaryKey
      ?.let { pk -> database.bpInfoDao().getFormFlow(pk) }
      ?: flowOf(null)

  val districtsPagerFlow: Flow<PagingData<District>> = Pager(
    PagingConfig(pageSize = 60, enablePlaceholders = true, maxSize = 200)
  ) { dbWrapper.districtDao().districtsPagingSourceNoOther() }
    .flow
    .cachedIn(viewModelScope)

  fun getFacilitiesPagingDataForDistrict(district: District?): Flow<PagingData<Facility>> {
    district ?: return emptyFlow()

    return Pager(PagingConfig(pageSize = 60, enablePlaceholders = true, maxSize = 200)) {
      dbWrapper.facilitiesDao().facilitiesPagingSource(districtId = district.id)
    }.flow
  }

  sealed class FormState {
    val isForFormEdit get() = (this as? Ready)?.existingInfo != null

    object Loading : FormState()
    class Ready(val existingInfo: BpInfoFacilityDistrict?) : FormState()
    object Saving : FormState()
    class SavedNewForm(val primaryKeyOfForm: Long, val primaryKeyOfFacility: Long) : FormState()
    class SavedEditsToExistingForm(val primaryKeyOfForm: Long, val primaryKeyOfFacility: Long) : FormState()
    class FailedException(val exception: Exception) : FormState()
    class FailedValidation(val errorsBySectionStringId: Map<Int, List<FieldError>>) : FormState()
    class FailedLoading(val message: String) : FormState()
  }

  private val _formState: MutableStateFlow<FormState> = MutableStateFlow(FormState.Loading)
  val formState: StateFlow<FormState> = _formState

  private val isFormDraftState = enabledState("isFormDraft")

  @Stable
  data class BpInfoFields(
    val district: DistrictState,
    val facility: HealthcareFacilityState,
    val dataCollectionDate: NoFutureDateState,
    val numBpReadingsTakenInFacilitySinceLastVisit: LimitedIntState,
    val numBpReadingsEndIn0Or5: LimitedIntState,
    val numBpReadingsWithAssociatedColorAndArrow: LimitedIntState,
    val localNotes: MutableState<String>,
    val isDraft: SavedStateMutableState<Boolean?>,
  ) {
    fun clearForm() {
      district.reset()
      facility.reset()
      numBpReadingsTakenInFacilitySinceLastVisit.reset()
      numBpReadingsEndIn0Or5.reset()
      numBpReadingsWithAssociatedColorAndArrow.reset()
      localNotes.value = ""
      isDraft.value = null
    }

    fun forceShowErrors() {
      district.enableShowErrors(force = true)
      facility.enableShowErrors(force = true)
      numBpReadingsTakenInFacilitySinceLastVisit.enableShowErrors(force = true)
      numBpReadingsEndIn0Or5.enableShowErrors(force = true)
      numBpReadingsWithAssociatedColorAndArrow.enableShowErrors(force = true)
    }
  }

  private val numBpReadings = limitedIntState(
    "facilityBpInfoNumTakenSinceLastVisit",
    isMandatory = false,
    range = FACILITY_BP_COUNT_RANGE,
  )

  val formFields = BpInfoFields(
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
    dataCollectionDate = dateState(
      "dataCollectionDate",
      isMandatory = true,
      areApproximateDatesAcceptable = false
    ),
    numBpReadingsTakenInFacilitySinceLastVisit = numBpReadings,
    numBpReadingsEndIn0Or5 = limitedIntState(
      "facilityBpInfoNumEndIn0Or5",
      isMandatory = false,
      range = FACILITY_BP_COUNT_RANGE,
      upperBoundState = numBpReadings,
      upperBoundErrorString = R.string.bp_info_error_cannot_exceed_readings_taken_today,
    ),
    numBpReadingsWithAssociatedColorAndArrow = limitedIntState(
      "facilityBpInfoNumWithAssociatedColorAndArrow",
      isMandatory = false,
      range = FACILITY_BP_COUNT_RANGE,
      upperBoundState = numBpReadings,
      upperBoundErrorString = R.string.bp_info_error_cannot_exceed_readings_taken_today,
    ),
    localNotes = handle.createMutableState("patientLocalNotes", ""),
    isDraft = isFormDraftState
  )

  private suspend fun populateFacilityField(facility: Facility) {
    formFields.facility.stateValue = FacilityAndPosition(
      facility,
      database.facilitiesDao().getFacilityIndexWhenOrderedByName(facility)
    )
  }

  init {
    Log.d(TAG, "initializing with pk = $existingFormPrimaryKey")
    viewModelScope.launch(appCoroutineDispatchers.main) {
      if (existingFormPrimaryKey != null) {

        val formFacilityDistrict: BpInfoFacilityDistrict? = database.bpInfoDao()
          .getFormFacilityDistrict(existingFormPrimaryKey)

        _formState.value = if (formFacilityDistrict == null) {
          Log.w(TAG, "Unable to find form with pk $existingFormPrimaryKey")
          FormState.FailedLoading(
            context.getString(R.string.bp_form_failed_to_load_with_pk_d, existingFormPrimaryKey)
          )
        } else if (formFacilityDistrict.bpInfo.serverInfo != null) {
          Log.w(TAG, "trying to edit a form with server info")
          FormState.FailedLoading(
            context.getString(R.string.form_cannot_edit_patient_already_on_server)
          )
        } else {
          val (
            form: FacilityBpInfo,
            formDistrict: District?,
            formFacility: Facility?,
          ) = formFacilityDistrict
          Log.d(TAG, "Setting up form for edit")

          with(formFields) {
            formDistrict?.let { district ->
              this.district.stateValue = DistrictAndPosition(
                district,
                dbWrapper.districtDao().getDistrictIndexWhenOrderedById(district)
              )
            }

            formFacility?.let { populateFacilityField(it) }
            dataCollectionDate.setStateFromFormDate(form.dataCollectionDate)

            numBpReadingsTakenInFacilitySinceLastVisit.backingState.value =
              form.numBpReadingsTakenInFacilitySinceLastVisit?.toString() ?: ""
            numBpReadingsEndIn0Or5.backingState.value =
              form.numBpReadingsEndIn0Or5?.toString() ?: ""
            numBpReadingsWithAssociatedColorAndArrow.backingState.value =
              form.numBpReadingsWithColorAndArrow?.toString() ?: ""

            localNotes.value = form.localNotes ?: ""
            isDraft.value = form.isDraft
          }

          FormState.Ready(formFacilityDistrict)
        }
      } else {
        valuesStore.districtIdFlow.firstOrNull()?.let { districtPk ->
          dbWrapper.districtDao().getDistrict(districtPk)?.let { district ->
            formFields.district.stateValue = DistrictAndPosition(
              district,
              dbWrapper.districtDao().getDistrictIndexWhenOrderedById(district)
            )
          }

          formFields.dataCollectionDate.setStateFromFormDate(FormDate.today())
        }

        initialFacilityPk?.let { pk ->
          dbWrapper.facilitiesDao().getFacility(pk)?.let { populateFacilityField(it) }
        }

        _formState.value = FormState.Ready(null)
      }
    }
  }

  fun save() {
    Log.d(TAG, "save()")
    saveRequestChannel.trySend(Unit)
  }

  fun isDraft() = formFields.isDraft.value == true

  private val saveRequestChannel = viewModelScope.actor<Unit>(
    context = appCoroutineDispatchers.default,
    capacity = Channel.RENDEZVOUS,
  ) {
    for (saveTick in channel) {
      Log.d(TAG, "Handling save request")
      _formState.value = FormState.Saving
      val isDraft = isDraft()

      formFields.forceShowErrors()

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
        val existingForm: BpInfoFacilityDistrict? = existingForm.firstOrNull()
        require(
          existingFormPrimaryKey == null ||
            existingForm?.bpInfo?.id == existingFormPrimaryKey
        ) {
          "Existing form doesn't match primary key"
        }

        val newForm = with(formFields) {
          if (!district.isValid) {
            fieldToErrorMap.addFieldError(
              categoryTitle = R.string.bp_info_form_title,
              fieldLabel = R.string.bp_info_district_label,
              errorMessage = district.errorFor(context, district.stateValue)
            )
          }
          if (!facility.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.bp_info_form_title,
              R.string.bp_info_facility_label,
              facility.errorFor(context, facility.stateValue)
            )
          }
          if (!dataCollectionDate.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.bp_info_form_title,
              R.string.bp_info_date_of_data_collection_label,
              dataCollectionDate.errorFor(context, dataCollectionDate.stateValue)
            )
          }
          if (!numBpReadingsTakenInFacilitySinceLastVisit.isValid) {
            fieldToErrorMap.addFieldError(
              categoryTitle = R.string.bp_info_form_title,
              fieldLabel = R.string.bp_info_bp_readings_in_facility_today_since_last_visited_label,
              errorMessage = numBpReadingsTakenInFacilitySinceLastVisit.errorFor(context, numBpReadingsTakenInFacilitySinceLastVisit.stateValue)
            )
          }
          if (!numBpReadingsEndIn0Or5.isValid) {
            fieldToErrorMap.addFieldError(
              categoryTitle = R.string.bp_info_form_title,
              fieldLabel = R.string.bp_info_bp_readings_end_in_a_0_or_a_5_label,
              errorMessage = numBpReadingsEndIn0Or5.errorFor(context, numBpReadingsEndIn0Or5.stateValue)
            )
          }
          if (!numBpReadingsWithAssociatedColorAndArrow.isValid) {
            fieldToErrorMap.addFieldError(
              categoryTitle = R.string.bp_info_form_title,
              fieldLabel = R.string.bp_info_bp_readings_have_color_or_arrow_label,
              errorMessage = numBpReadingsWithAssociatedColorAndArrow.errorFor(context, numBpReadingsWithAssociatedColorAndArrow.stateValue)
            )
          }

          runCatching {
            FacilityBpInfo(
              id = existingForm?.bpInfo?.id ?: 0L,
              serverInfo = existingForm?.bpInfo?.serverInfo,
              serverErrorMessage = null,
              recordLastUpdated = ZonedDateTime.now(),
              district = district.stateValue?.district?.id,
              facility = facility.stateValue?.facility?.id,
              dataCollectionDate = dataCollectionDate.dateFromStateOrNull(),
              numBpReadingsTakenInFacilitySinceLastVisit = numBpReadingsTakenInFacilitySinceLastVisit.stateValue.toIntOrNull(),
              numBpReadingsEndIn0Or5 = numBpReadingsEndIn0Or5.stateValue.toIntOrNull(),
              numBpReadingsWithColorAndArrow = numBpReadingsWithAssociatedColorAndArrow.stateValue.toIntOrNull(),
              localNotes = localNotes.value,
              isDraft = isDraft
            )
          }
        }

        with(formFields) {
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
          val created = newForm.getOrThrow()
          val primaryKey = database.bpInfoDao().upsert(created)
          if (existingFormPrimaryKey != null) {
            assert(existingFormPrimaryKey == primaryKey)
            FormState.SavedEditsToExistingForm(primaryKey, requireNotNull(created.facility))
          } else {
            FormState.SavedNewForm(primaryKey, requireNotNull(created.facility))
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
  ) = LimitedIntState(
    isMandatory = isMandatory,
    limit = range,
    handle.createMutableState(key, ""),
    isFormDraftState = isFormDraftState,
    upperBoundInfo = LimitedIntState.UpperBoundInfo(
      stateUpperBound = upperBoundState,
      upperBoundErrorString = upperBoundErrorString
    )
  )
}
