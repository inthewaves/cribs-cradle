package org.welbodipartnership.cradle5.patients.form

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
import androidx.room.withTransaction
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
import org.welbodipartnership.cradle5.data.database.entities.AgeAtDelivery
import org.welbodipartnership.cradle5.data.database.entities.BirthWeight
import org.welbodipartnership.cradle5.data.database.entities.CausesOfNeonatalDeath
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.EclampsiaFit
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Hysterectomy
import org.welbodipartnership.cradle5.data.database.entities.MaternalDeath
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.PatientReferralInfo
import org.welbodipartnership.cradle5.data.database.entities.PerinatalDeath
import org.welbodipartnership.cradle5.data.database.entities.SurgicalManagementOfHaemorrhage
import org.welbodipartnership.cradle5.data.database.entities.TouchedState
import org.welbodipartnership.cradle5.data.database.resultentities.PatientFacilityDistrictOutcomes
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import org.welbodipartnership.cradle5.util.datetime.FormDate
import javax.inject.Inject

private const val PATIENT_MAX_INITIALS_LENGTH = 5

private val AGE_RANGE = 0L..60L

private val VALID_LENGTH_OF_ITU_HDU_STAY = 1L..100L

data class FieldError(@StringRes val fieldTitle: Int, val errorMessage: String)

@HiltViewModel
class PatientFormViewModel @Inject constructor(
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

  private val existingPatientPrimaryKey: Long? =
    handle[LeafScreen.PatientEdit.ARG_PATIENT_PRIMARY_KEY]

  val isExistingPatientEdit = existingPatientPrimaryKey != null

  val existingParentFacilityOutcomes: Flow<PatientFacilityDistrictOutcomes?> = existingPatientPrimaryKey?.let { pk ->
    database.patientDao().getPatientAndOutcomesFlow(pk)
  } ?: flowOf(null)

  val districtsPagerFlow: Flow<PagingData<District>> = Pager(
    PagingConfig(pageSize = 60, enablePlaceholders = true, maxSize = 200)
  ) { dbWrapper.districtDao().districtsPagingSource() }
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
    val isForPatientEdit get() = (this as? Ready)?.existingInfo != null

    object Loading : FormState()
    class Ready(val existingInfo: PatientFacilityDistrictOutcomes?) : FormState()
    object Saving : FormState()
    class SavedNewPatient(val primaryKeyOfPatient: Long) : FormState()
    class SavedEditsToExistingPatient(val primaryKeyOfPatient: Long) : FormState()
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
  private val maternalDeathState = OutcomeFieldsWithCheckbox.MaternalDeath(
    isEnabled = enabledState("maternalDeathEnabled"),
    date = dateState(
      "maternalDeathDate",
      isMandatory = true,
      areApproximateDatesAcceptable = false,
    ),
    underlyingCause = enumWithOtherState(
      "maternalDeathUnderlyingCause",
      DropdownType.UnderlyingCauseOfMaternalDeath,
      isMandatory = true
    ),
    placeOfDeath = enumIdOnlyState(
      "maternalDeathPlace",
      DropdownType.Place,
      isMandatory = false
    )
  )

  val formFields = PatientFormFields(
    patientFields = PatientFields(
      initials = InitialsState(
        backingState = handle.createMutableState("patientInitials", ""),
        isFormDraftState = isFormDraftState,
        isMandatory = true,
      ),
      presentationDate = dateState(
        "patientPresentationDate",
        isMandatory = false,
        areApproximateDatesAcceptable = false,
      ),
      age = LimitedAgeIntState(
        isMandatory = true,
        AGE_RANGE,
        handle.createMutableState("patientAge", ""),
        isFormDraftState = isFormDraftState,
      ),
      isAgeUnknown = nonNullBooleanState("isAgeUnknown", false),
      address = handle.createMutableState("patientAddress", ""),
      healthcareFacility = HealthcareFacilityState(
        isMandatory = true,
        handle.createMutableState("patientHealthFacility", null),
        isFormDraftState = isFormDraftState
      ),
      referralInfo = PatientFields.ReferralInfoFields(
        isEnabled = enabledState("referralPresent"),
        fromDistrict = DistrictState(
          isMandatory = true,
          handle.createMutableState("patientFromDistrict", null),
          isFormDraftState = isFormDraftState
        ),
        fromFacility = HealthcareFacilityState(
          isMandatory = true,
          handle.createMutableState("patientFromFacility", null),
          isFormDraftState = isFormDraftState
        ),
        toDistrict = DistrictState(
          isMandatory = true,
          handle.createMutableState("patientToDistrict", null),
          isFormDraftState = isFormDraftState
        ),
        toFacility = HealthcareFacilityState(
          isMandatory = true,
          handle.createMutableState("patientToFacility", null),
          isFormDraftState = isFormDraftState
        ),
      ),
      localNotes = handle.createMutableState("patientLocalNotes", ""),
      isDraft = isFormDraftState
    ),
    eclampsia = OutcomeFieldsWithCheckbox.Eclampsia(
      isEnabled = enabledState("eclampsiaEnabled"),
      didTheWomanFit = toggleState("eclampsiaDidWomanFit", isMandatory = true),
      placeOfFirstFit = enumIdOnlyState(
        "eclampsiaPlace",
        DropdownType.Place,
        isMandatory = false
      ),
    ),
    hysterectomy = OutcomeFieldsWithCheckbox.Hysterectomy(
      isEnabled = enabledState("hysterectomyEnabled"),
      date = maternalDeathBoundedDateState(
        "hysterectomyDate",
        isMandatory = true,
        areApproximateDatesAcceptable = false,
      ),
      cause = enumWithOtherState(
        "hysterectomyCause",
        DropdownType.CauseOfHysterectomy,
        isMandatory = false
      ),
    ),
    maternalDeath = maternalDeathState,
    surgicalManagement = OutcomeFieldsWithCheckbox.SurgicalManagement(
      isEnabled = enabledState("surgicalManagementEnabled"),
      date = maternalDeathBoundedDateState(
        "surgicalManagementDate",
        isMandatory = true,
        areApproximateDatesAcceptable = false,
      ),
      type = enumWithOtherState(
        "surgicalManagementType",
        DropdownType.TypeOfSurgicalManagement,
        isMandatory = false
      ),
    ),
    perinatalDeath = OutcomeFieldsWithCheckbox.PerinatalDeath(
      isEnabled = enabledState("perinatalDeathEnabled"),
      date = maternalDeathBoundedDateState(
        "perinatalDeathDate",
        isMandatory = true,
        areApproximateDatesAcceptable = false,
      ),
      outcome = enumIdOnlyState(
        "perinatalDeathOutcome",
        DropdownType.PerinatalOutcome,
        isMandatory = false
      ),
      causeOfStillBirth = enumIdOnlyState(
        "perinatalDeathStillBirthCause",
        DropdownType.CauseOfStillbirth,
        isMandatory = false
      ),
      causeOfNeonatalDeath = parcelableState("perinatalDeath_neonatalDeathCause"),
      additionalInfo = handle.createMutableState("perinatalDeathAdditionalInfo", null)
    ),
    birthWeight = OutcomeFieldsWithoutCheckbox.BirthWeight(
      birthWeight = enumIdOnlyState(
        "birthWeight",
        DropdownType.Birthweight,
        isMandatory = false,
      )
    ),
    ageAtDelivery = OutcomeFieldsWithoutCheckbox.AgeAtDelivery(
      ageAtDelivery = enumIdOnlyState(
        "ageAtDelivery",
        DropdownType.AgeAtDelivery,
        isMandatory = false,
      )
    )
  )

  init {
    Log.d(TAG, "initializing with pk = $existingPatientPrimaryKey")
    if (existingPatientPrimaryKey != null) {
      viewModelScope.launch(appCoroutineDispatchers.main) {
        val patientAndOutcomes = database.patientDao()
          .getPatientFacilityAndOutcomes(existingPatientPrimaryKey)
        _formState.value = if (patientAndOutcomes == null) {
          Log.w(TAG, "Unable to find patient with pk $existingPatientPrimaryKey")
          FormState.FailedLoading(
            context.getString(R.string.patient_form_failed_to_load_patient_with_pk_d)
          )
        } else if (
          patientAndOutcomes.patient.serverInfo != null &&
          patientAndOutcomes.outcomes?.serverInfo != null
        ) {
          Log.w(TAG, "trying to edit a patient with server info")
          FormState.FailedLoading(
            context.getString(R.string.patient_form_cannot_edit_patient_already_on_server)
          )
        } else {
          val (
            patient: Patient,
            facility: Facility?,
            referralFromDistrict: District?,
            referralFromFacility: Facility?,
            referralToDistrict: District?,
            referralToFacility: Facility?,
            outcomes: Outcomes?
          ) = patientAndOutcomes
          Log.d(TAG, "Setting up form for edit")

          if (patient.isUploadedToServer) {
            Log.d(TAG, "This is an outcomes-only session")
          }

          with(formFields.patientFields) {
            initials.backingState.value = patient.initials
            presentationDate.setStateFromFormDate(patient.presentationDate)
            val parsedAge = patient.dateOfBirth?.getAgeInYearsFromNow()?.toString()
            age.backingState.value = parsedAge ?: ""
            isAgeUnknown.value = parsedAge == null && patient.isAgeUnknown
            address.value = patient.address ?: ""

            val facilityPosition = facility?.id
              ?.let { facilityId ->
                database.facilitiesDao().getFacilityIndexWhenOrderedByName(
                  facilityId = facilityId,
                  districtId = valuesStore.districtIdFlow.firstOrNull() ?: Facility.DEFAULT_DISTRICT_ID
                )
              }
              ?.toInt()
              ?.coerceAtLeast(0)
            healthcareFacility.stateValue = facility?.let {
              FacilityAndPosition(
                it,
                facilityPosition
              )
            }

            with(referralInfo) { // field
              patient.referralInfo?.let { // previous info
                isEnabled.value = true
                referralFromDistrict?.let { district ->
                  fromDistrict.stateValue = DistrictAndPosition(
                    district,
                    district.id
                      .let { id -> dbWrapper.districtDao().getDistrictIndexWhenOrderedById(id) }
                      ?.toInt()
                      ?.coerceAtLeast(0)
                  )
                }
                referralFromFacility?.let { facility ->
                  fromFacility.stateValue = FacilityAndPosition(
                    facility,
                    facility.id
                      .let { id -> dbWrapper.facilitiesDao().getFacilityIndexWhenOrderedByName(id) }
                      ?.toInt()
                      ?.coerceAtLeast(0)
                  )
                }
                referralToDistrict?.let { district ->
                  toDistrict.stateValue = DistrictAndPosition(
                    district,
                    district.id
                      .let { id -> dbWrapper.districtDao().getDistrictIndexWhenOrderedById(id) }
                      ?.toInt()
                      ?.coerceAtLeast(0)
                  )
                }
                referralToFacility?.let { facility ->
                  toFacility.stateValue = FacilityAndPosition(
                    facility,
                    facility.id
                      .let { id -> dbWrapper.facilitiesDao().getFacilityIndexWhenOrderedByName(id) }
                      ?.toInt()
                      ?.coerceAtLeast(0)
                  )
                }
              } ?: clearFormsAndSetCheckbox(
                newEnabledState = patient.referralInfoTouched.nullEnabledState
              )
            }

            localNotes.value = patient.localNotes ?: ""
            isDraft.value = patient.isDraft
          }

          with(formFields.eclampsia) {
            outcomes?.eclampsiaFit?.let {
              isEnabled.value = true
              didTheWomanFit.stateValue = it.didTheWomanFit
              placeOfFirstFit.backingState.value = it.place
            } ?: clearFormsAndSetCheckbox(
              newEnabledState = outcomes?.eclampsiaFitTouched?.nullEnabledState
            )
          }

          with(formFields.hysterectomy) {
            outcomes?.hysterectomy?.let {
              isEnabled.value = true
              date.setStateFromFormDate(it.date)
              cause.backingState.value = it.cause
            } ?: clearFormsAndSetCheckbox(outcomes?.hysterectomyTouched?.nullEnabledState)
          }

          with(formFields.maternalDeath) {
            outcomes?.maternalDeath?.let {
              isEnabled.value = true
              date.setStateFromFormDate(it.date)
              underlyingCause.backingState.value = it.underlyingCause
              placeOfDeath.backingState.value = it.place
            } ?: clearFormsAndSetCheckbox(
              newEnabledState = outcomes?.maternalDeathTouched?.nullEnabledState
            )
          }

          with(formFields.surgicalManagement) {
            outcomes?.surgicalManagement?.let {
              isEnabled.value = true
              date.setStateFromFormDate(it.date)
              type.backingState.value = it.typeOfSurgicalManagement
            } ?: clearFormsAndSetCheckbox(
              newEnabledState = outcomes?.surgicalManagementTouched?.nullEnabledState
            )
          }

          with(formFields.perinatalDeath) {
            outcomes?.perinatalDeath?.let {
              isEnabled.value = true
              date.setStateFromFormDate(it.date)
              outcome.backingState.value = it.outcome
              causeOfStillBirth.backingState.value = it.causeOfStillbirth
              causeOfNeonatalDeath.value = it.causesOfNeonatalDeath
              additionalInfo.value = it.additionalInfo
            } ?: clearFormsAndSetCheckbox(
              newEnabledState = outcomes?.perinatalDeathTouched?.nullEnabledState
            )
          }

          with(formFields.birthWeight) {
            outcomes?.birthWeight?.let {
              birthWeight.backingState.value = it.birthWeight
            } ?: clearForms()
          }

          with(formFields.ageAtDelivery) {
            outcomes?.ageAtDelivery?.let {
              ageAtDelivery.backingState.value = it.ageAtDelivery
            } ?: clearForms()
          }

          FormState.Ready(patientAndOutcomes)
        }
      }
    } else {
      _formState.value = FormState.Ready(null)
    }
  }

  fun save() {
    Log.d(TAG, "save()")
    saveRequestChannel.trySend(Unit)
  }

  fun isDraft() = formFields.patientFields.isDraft.value == true

  private val saveRequestChannel = viewModelScope.actor<Unit>(
    context = appCoroutineDispatchers.default,
    capacity = Channel.RENDEZVOUS,
  ) {
    for (saveTick in channel) {
      Log.d(TAG, "Handling save request")
      _formState.value = FormState.Saving
      val isDraft = isDraft()

      formFields.forceAllErrors()

      val fieldToErrorMap = linkedMapOf<Int, List<FieldError>>()
      fun FieldsWithCheckbox.getCategoryStringRes() = when (this) {
        is OutcomeFieldsWithCheckbox.Eclampsia -> R.string.outcomes_eclampsia_label
        is OutcomeFieldsWithCheckbox.Hysterectomy -> R.string.outcomes_hysterectomy_label
        is OutcomeFieldsWithCheckbox.MaternalDeath -> R.string.outcomes_maternal_death_label
        is OutcomeFieldsWithCheckbox.PerinatalDeath -> R.string.outcomes_perinatal_death_label
        is OutcomeFieldsWithCheckbox.SurgicalManagement -> R.string.outcomes_surgical_management_label
        is PatientFields.ReferralInfoFields -> R.string.patient_referral_info_labels
        else -> R.string.unknown
      }
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
        val patientAndOutcomes = existingParentFacilityOutcomes.first()
        require(
          existingPatientPrimaryKey == null ||
            patientAndOutcomes?.patient?.id == existingPatientPrimaryKey
        ) {
          "Existing patient doesn't match primary key"
        }

        val patient = if (patientAndOutcomes?.patient?.isUploadedToServer != true) {
          with(formFields.patientFields) {
            if (!initials.isValid) {
              fieldToErrorMap.addFieldError(
                categoryTitle = R.string.patient_registration_card_title,
                fieldLabel = R.string.patient_registration_initials_label,
                errorMessage = initials.errorFor(context, initials.stateValue)
              )
            }
            if (!presentationDate.isValid) {
              fieldToErrorMap.addFieldError(
                R.string.patient_registration_card_title,
                R.string.patient_registration_presentation_date_label,
                presentationDate.errorFor(context, presentationDate.stateValue)
              )
            }
            if (!age.isValid) {
              if (age.stateValue.isBlank() && isAgeUnknown.value) {
                // do nothing
              } else {
                fieldToErrorMap.addFieldError(
                  R.string.patient_registration_card_title,
                  R.string.patient_registration_age_label,
                  age.errorFor(context, age.stateValue)
                )
              }
            }
            if (!healthcareFacility.isValid) {
              fieldToErrorMap.addFieldError(
                R.string.patient_registration_card_title,
                R.string.patient_registration_healthcare_facility_label,
                healthcareFacility.errorFor(context, healthcareFacility.stateValue)
              )
            }
            val referralInfoResult: Result<PatientReferralInfo>? = with(referralInfo) {
              when (isEnabled.value) {
                null -> {
                  if (!isDraft) {
                    fieldToErrorMap.addFieldError(
                      getCategoryStringRes(),
                      R.string.patient_referral_checkbox_label,
                      context.getString(R.string.patient_referral_checkbox_missing_error)
                    )
                  }
                  null
                }
                true -> {
                  if (!fromDistrict.isValid) {
                    fieldToErrorMap.addFieldError(
                      getCategoryStringRes(),
                      R.string.patient_referral_info_from_district_label,
                      fromDistrict.errorFor(context, fromDistrict.stateValue)
                    )
                  }
                  if (!fromFacility.isValid) {
                    fieldToErrorMap.addFieldError(
                      getCategoryStringRes(),
                      R.string.patient_referral_info_from_facility_label,
                      fromFacility.errorFor(context, fromFacility.stateValue)
                    )
                  }
                  if (!toDistrict.isValid) {
                    fieldToErrorMap.addFieldError(
                      getCategoryStringRes(),
                      R.string.patient_referral_info_to_district_label,
                      toDistrict.errorFor(context, toDistrict.stateValue)
                    )
                  }
                  if (!toFacility.isValid) {
                    fieldToErrorMap.addFieldError(
                      getCategoryStringRes(),
                      R.string.patient_referral_info_to_facility_label,
                      toFacility.errorFor(context, toFacility.stateValue)
                    )
                  }
                  runCatching {
                    PatientReferralInfo(
                      fromDistrict = fromDistrict.stateValue?.district?.id,
                      fromFacility = fromFacility.stateValue?.facility?.id,
                      toDistrict = toDistrict.stateValue?.district?.id,
                      toFacility = toFacility.stateValue?.facility?.id,
                    )
                  }
                }
                false -> null
              }
            }

            runCatching {
              val parsedDob = if (isDraft || isAgeUnknown.value) {
                age.stateValue.toIntOrNull()?.let(FormDate::fromAgeFromNow)
              } else {
                age.stateValue.toInt().let(FormDate::fromAgeFromNow)
              }
              Patient(
                id = patientAndOutcomes?.patient?.id ?: 0L,
                serverInfo = patientAndOutcomes?.patient?.serverInfo,
                serverErrorMessage = null,
                initials = initials.stateValue,
                presentationDate = presentationDate.dateFromStateOrNull(),
                dateOfBirth = parsedDob,
                isAgeUnknown = parsedDob == null && isAgeUnknown.value,
                address = address.value,
                healthcareFacilityId = if (isDraft) {
                  healthcareFacility.stateValue?.facility?.id
                } else {
                  requireNotNull(healthcareFacility.stateValue?.facility?.id) {
                    "Missing healthcareFacilityId"
                  }
                },
                referralInfoTouched = when (referralInfo.isEnabled.value) {
                  true -> TouchedState.TOUCHED_ENABLED
                  false -> TouchedState.TOUCHED
                  null -> TouchedState.NOT_TOUCHED
                },
                referralInfo = referralInfoResult?.getOrThrow(),
                localNotes = localNotes.value,
                isDraft = isDraft
              )
            }
          }
        } else {
          Log.d(TAG, "saving an outcomes-only session")
          null
        }

        val eclampsia: Result<EclampsiaFit>? = with(formFields.eclampsia) {
          when (isEnabled.value) {
            null -> {
              if (!isDraft) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.outcomes_eclampsia_label,
                  context.getString(R.string.outcomes_eclampsia_not_selected_error)
                )
              }
              null
            }
            true -> {
              if (!didTheWomanFit.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.outcomes_eclampsia_did_woman_fit_label,
                  didTheWomanFit.errorFor(context, didTheWomanFit.stateValue)
                )
              }
              if (!placeOfFirstFit.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.place_of_first_eclamptic_fit_label,
                  placeOfFirstFit.errorFor(context, placeOfFirstFit.stateValue)
                )
              }

              runCatching {
                EclampsiaFit(
                  didTheWomanFit = didTheWomanFit.stateValue,
                  place = placeOfFirstFit.stateValue
                )
              }
            }
            else -> null
          }
        }

        val hysterectomy = with(formFields.hysterectomy) {
          when (isEnabled.value) {
            null -> {
              if (!isDraft) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.outcomes_hysterectomy_label,
                  context.getString(R.string.outcomes_hysterectomy_not_selected_error)
                )
              }
              null
            }
            true -> {
              if (!date.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.form_date_label,
                  date.errorFor(context, date.stateValue)
                )
              }
              if (!cause.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.hysterectomy_cause_label,
                  cause.errorFor(context, cause.stateValue)
                )
              }
              // additional info has no checking

              runCatching {
                Hysterectomy(
                  date = if (isDraft) date.dateFromStateOrNull() else date.dateFromStateOrThrow(),
                  cause = cause.stateValue,
                )
              }
            }
            else -> null
          }
        }

        val maternalDeath = with(formFields.maternalDeath) {
          when (isEnabled.value) {
            null -> {
              if (!isDraft) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.outcomes_maternal_death_label,
                  context.getString(R.string.outcomes_maternal_death_not_selected_error)
                )
              }
              null
            }
            true -> {
              if (!date.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.form_date_label,
                  date.errorFor(context, date.stateValue)
                )
              }
              if (!underlyingCause.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.maternal_death_underlying_cause_label,
                  underlyingCause.errorFor(context, underlyingCause.stateValue)
                )
              }
              if (!placeOfDeath.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.maternal_death_place_label,
                  placeOfDeath.errorFor(context, placeOfDeath.stateValue)
                )
              }
              runCatching {
                MaternalDeath(
                  date = if (isDraft) date.dateFromStateOrNull() else date.dateFromStateOrThrow(),
                  underlyingCause = underlyingCause.stateValue,
                  place = placeOfDeath.stateValue
                )
              }
            }
            else -> null
          }
        }

        val surgicalManagement: Result<SurgicalManagementOfHaemorrhage>? = with(formFields.surgicalManagement) {
          when (isEnabled.value) {
            null -> {
              if (!isDraft) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.outcomes_surgical_management_label,
                  context.getString(R.string.outcomes_surgical_management_not_selected_error)
                )
              }
              null
            }
            true -> {
              if (!date.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.form_date_label,
                  date.errorFor(context, date.stateValue)
                )
              }
              if (!type.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.surgical_management_type_label,
                  type.errorFor(context, type.stateValue)
                )
              }
              runCatching {
                SurgicalManagementOfHaemorrhage(
                  date = if (isDraft) date.dateFromStateOrNull() else date.dateFromStateOrThrow(),
                  typeOfSurgicalManagement = type.stateValue
                )
              }
            }
            else -> null
          }
        }

        val perinatalDeath = with(formFields.perinatalDeath) {
          when (isEnabled.value) {
            null -> {
              if (!isDraft) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.outcomes_perinatal_death_label,
                  context.getString(R.string.outcomes_perinatal_death_not_selected_error)
                )
              }
              null
            }
            true -> {
              if (!date.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.form_date_label,
                  date.errorFor(context, date.stateValue)
                )
              }
              if (!outcome.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.perinatal_death_outcome_label,
                  outcome.errorFor(context, outcome.stateValue)
                )
              }
              if (!causeOfStillBirth.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.perinatal_death_cause_of_stillbirth_label,
                  causeOfStillBirth.errorFor(context, causeOfStillBirth.stateValue)
                )
              }
              runCatching {
                PerinatalDeath(
                  date = if (isDraft) date.dateFromStateOrNull() else date.dateFromStateOrThrow(),
                  outcome = outcome.stateValue,
                  causeOfStillbirth = causeOfStillBirth.stateValue,
                  causesOfNeonatalDeath = causeOfNeonatalDeath.value,
                  additionalInfo = additionalInfo.value
                )
              }
            }
            else -> null
          }
        }

        val birthWeight: BirthWeight? = with(formFields.birthWeight) {
          birthWeight.stateValue?.let { BirthWeight(it) }
        }

        val ageAtDelivery: AgeAtDelivery? = with(formFields.ageAtDelivery) {
          ageAtDelivery.stateValue?.let { AgeAtDelivery(it) }
        }

        with(formFields.patientFields) {
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
          Log.d(TAG, "Attempting to save new patient")
          val patientPrimaryKey = database.withTransaction {
            val patientPk = patient?.let { database.patientDao().upsert(patient.getOrThrow()) }
              ?: requireNotNull(patientAndOutcomes?.patient?.id) {
                "expected patient ID for existing patient"
              }
            // we DON'T have a foreign key constraint here
            database.outcomesDao().upsert(
              Outcomes(
                id = patientAndOutcomes?.outcomes?.id ?: 0,
                patientId = patientPk,
                serverInfo = patientAndOutcomes?.outcomes?.serverInfo,
                serverErrorMessage = null,
                eclampsiaFitTouched = when (formFields.eclampsia.isEnabled.value) {
                  true -> TouchedState.TOUCHED_ENABLED
                  false -> TouchedState.TOUCHED
                  null -> TouchedState.NOT_TOUCHED
                },
                eclampsiaFit = eclampsia?.getOrThrow(),
                hysterectomyTouched = when (formFields.hysterectomy.isEnabled.value) {
                  true -> TouchedState.TOUCHED_ENABLED
                  false -> TouchedState.TOUCHED
                  null -> TouchedState.NOT_TOUCHED
                },
                hysterectomy = hysterectomy?.getOrThrow(),
                maternalDeathTouched = when (formFields.maternalDeath.isEnabled.value) {
                  true -> TouchedState.TOUCHED_ENABLED
                  false -> TouchedState.TOUCHED
                  null -> TouchedState.NOT_TOUCHED
                },
                maternalDeath = maternalDeath?.getOrThrow(),
                surgicalManagementTouched = when (formFields.surgicalManagement.isEnabled.value) {
                  true -> TouchedState.TOUCHED_ENABLED
                  false -> TouchedState.TOUCHED
                  null -> TouchedState.NOT_TOUCHED
                },
                surgicalManagement = surgicalManagement?.getOrThrow(),
                perinatalDeathTouched = when (formFields.perinatalDeath.isEnabled.value) {
                  true -> TouchedState.TOUCHED_ENABLED
                  false -> TouchedState.TOUCHED
                  null -> TouchedState.NOT_TOUCHED
                },
                perinatalDeath = perinatalDeath?.getOrThrow(),
                birthWeight = birthWeight,
                ageAtDelivery = ageAtDelivery,
              )
            )
            patientPk
          }

          if (existingPatientPrimaryKey != null) {
            assert(existingPatientPrimaryKey == patientPrimaryKey)
            FormState.SavedEditsToExistingPatient(patientPrimaryKey)
          } else {
            FormState.SavedNewPatient(patientPrimaryKey)
          }.also {
            channel.close()
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to save patient", e)
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

  private fun maternalDeathBoundedDateState(
    key: String,
    isMandatory: Boolean,
    areApproximateDatesAcceptable: Boolean
  ) = NoFutureDateAndAheadOfMaternalDeathState(
    isMandatory,
    areApproximateDatesAcceptable = areApproximateDatesAcceptable,
    backingState = handle.createMutableState(key, ""),
    isFormDraftState = isFormDraftState,
    maternalDeathDateState = maternalDeathState.date
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
  data class PatientFormFields(
    val patientFields: PatientFields,
    val eclampsia: OutcomeFieldsWithCheckbox.Eclampsia,
    val hysterectomy: OutcomeFieldsWithCheckbox.Hysterectomy,
    val maternalDeath: OutcomeFieldsWithCheckbox.MaternalDeath,
    val surgicalManagement: OutcomeFieldsWithCheckbox.SurgicalManagement,
    val perinatalDeath: OutcomeFieldsWithCheckbox.PerinatalDeath,
    val birthWeight: OutcomeFieldsWithoutCheckbox.BirthWeight,
    val ageAtDelivery: OutcomeFieldsWithoutCheckbox.AgeAtDelivery,
  ) {
    fun forceAllErrors() {
      patientFields.forceShowErrors()
      eclampsia.forceShowErrors()
      hysterectomy.forceShowErrors()
      maternalDeath.forceShowErrors()
      surgicalManagement.forceShowErrors()
      perinatalDeath.forceShowErrors()
    }
  }

  @Stable
  data class PatientFields(
    val initials: InitialsState,
    val presentationDate: NoFutureDateState,
    val age: LimitedAgeIntState,
    val isAgeUnknown: MutableState<Boolean>,
    val address: MutableState<String>,
    val referralInfo: ReferralInfoFields,
    val healthcareFacility: HealthcareFacilityState,
    val localNotes: MutableState<String>,
    val isDraft: SavedStateMutableState<Boolean?>,
  ) {
    @Stable
    data class ReferralInfoFields(
      override val isEnabled: MutableState<Boolean?>,
      val fromDistrict: DistrictState,
      val fromFacility: HealthcareFacilityState,
      val toDistrict: DistrictState,
      val toFacility: HealthcareFacilityState,
    ) : FieldsWithCheckbox() {
      override fun clearFormsAndSetCheckbox(newEnabledState: Boolean?) {
        isEnabled.value = newEnabledState
        fromDistrict.reset()
        fromFacility.reset()
        toDistrict.reset()
        toFacility.reset()
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          fromDistrict.enableShowErrors(force = true)
          fromFacility.enableShowErrors(force = true)
          toDistrict.enableShowErrors(force = true)
          toFacility.enableShowErrors(force = true)
        }
      }
    }

    fun forceShowErrors() {
      initials.enableShowErrors(force = true)
      presentationDate.enableShowErrors(force = true)
      age.enableShowErrors(force = true)
      healthcareFacility.enableShowErrors(force = true)
      referralInfo.forceShowErrors()
    }
  }

  @Stable
  sealed class OutcomeFieldsWithoutCheckbox : BaseFields() {
    abstract fun clearForms()

    @Stable
    data class BirthWeight(
      val birthWeight: EnumIdOnlyState,
    ) : OutcomeFieldsWithoutCheckbox() {
      override fun forceShowErrors() {
        birthWeight.enableShowErrors(force = true)
      }

      override fun clearForms() {
        birthWeight.stateValue = null
      }
    }

    @Stable
    data class AgeAtDelivery(
      val ageAtDelivery: EnumIdOnlyState,
    ) : OutcomeFieldsWithoutCheckbox() {
      override fun forceShowErrors() {
        ageAtDelivery.enableShowErrors(force = true)
      }

      override fun clearForms() {
        ageAtDelivery.stateValue = null
      }
    }
  }

  @Stable
  sealed class OutcomeFieldsWithCheckbox : BaseFields() {
    @Stable
    data class Eclampsia(
      override val isEnabled: MutableState<Boolean?>,
      val didTheWomanFit: NullableToggleState,
      val placeOfFirstFit: EnumIdOnlyState
    ) : FieldsWithCheckbox() {
      override fun clearFormsAndSetCheckbox(newEnabledState: Boolean?) {
        isEnabled.value = newEnabledState
        didTheWomanFit.reset()
        placeOfFirstFit.reset()
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          didTheWomanFit.enableShowErrors(force = true)
          placeOfFirstFit.enableShowErrors(force = true)
        }
      }
    }

    @Stable
    data class Hysterectomy(
      override val isEnabled: MutableState<Boolean?>,
      val date: NoFutureDateState,
      val cause: EnumWithOtherState,
    ) : FieldsWithCheckbox() {
      override fun clearFormsAndSetCheckbox(newEnabledState: Boolean?) {
        isEnabled.value = newEnabledState
        date.reset()
        cause.reset()
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          date.enableShowErrors(force = true)
          cause.enableShowErrors(force = true)
        }
      }
    }

    @Stable
    data class MaternalDeath(
      override val isEnabled: MutableState<Boolean?>,
      val date: NoFutureDateState,
      val underlyingCause: EnumWithOtherState,
      val placeOfDeath: EnumIdOnlyState
    ) : FieldsWithCheckbox() {
      override fun clearFormsAndSetCheckbox(newEnabledState: Boolean?) {
        isEnabled.value = newEnabledState
        date.reset()
        underlyingCause.reset()
        placeOfDeath.reset()
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          date.enableShowErrors(force = true)
          underlyingCause.enableShowErrors(force = true)
          placeOfDeath.enableShowErrors(force = true)
        }
      }
    }

    @Stable
    data class SurgicalManagement(
      override val isEnabled: MutableState<Boolean?>,
      val date: NoFutureDateState,
      val type: EnumWithOtherState
    ) : FieldsWithCheckbox() {
      override fun clearFormsAndSetCheckbox(newEnabledState: Boolean?) {
        isEnabled.value = newEnabledState
        date.reset()
        type.reset()
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          date.enableShowErrors(force = true)
          type.enableShowErrors(force = true)
        }
      }
    }

    @Stable
    data class PerinatalDeath(
      override val isEnabled: MutableState<Boolean?>,
      val date: NoFutureDateState,
      val outcome: EnumIdOnlyState,
      /** This is only used if the [outcome] is relevant */
      val causeOfStillBirth: EnumIdOnlyState,
      /** This is only used if the [outcome] is relevant */
      val causeOfNeonatalDeath: MutableState<CausesOfNeonatalDeath?>,
      val additionalInfo: MutableState<String?>,
    ) : FieldsWithCheckbox() {
      override fun clearFormsAndSetCheckbox(newEnabledState: Boolean?) {
        isEnabled.value = newEnabledState
        date.reset()
        outcome.reset()
        causeOfStillBirth.reset()
        causeOfNeonatalDeath.value = null
        additionalInfo.value = null
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          date.enableShowErrors(force = true)
          outcome.enableShowErrors(force = true)
          causeOfStillBirth.enableShowErrors(force = true)
        }
      }
    }
  }
}
