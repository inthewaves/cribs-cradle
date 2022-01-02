package org.welbodipartnership.cradle5.patients.form

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.LeafScreen
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.EclampsiaFit
import org.welbodipartnership.cradle5.data.database.entities.HduOrItuAdmission
import org.welbodipartnership.cradle5.data.database.entities.Hysterectomy
import org.welbodipartnership.cradle5.data.database.entities.MaternalDeath
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.PerinatalDeath
import org.welbodipartnership.cradle5.data.database.entities.SurgicalManagementOfHaemorrhage
import org.welbodipartnership.cradle5.data.database.resultentities.PatientAndOutcomes
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.settings.AppKeyValueStore
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import org.welbodipartnership.cradle5.util.date.toFormDateOrThrow
import javax.inject.Inject

private const val PATIENT_MAX_INITIALS_LENGTH = 5

private val AGE_RANGE = 10L..60L

private val VALID_LENGTH_OF_ITU_HDU_STAY = 1L..100L

data class FieldError(@StringRes val fieldTitle: Int, val errorMessage: String)

@HiltViewModel
class PatientFormViewModel @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val handle: SavedStateHandle,
  private val keyValueStore: AppKeyValueStore,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  dbWrapper: CradleDatabaseWrapper
) : ViewModel() {
  val database = dbWrapper.database!!

  private val existingPatientPrimaryKey: Long? =
    handle[LeafScreen.PatientEdit.ARG_PATIENT_PRIMARY_KEY]

  val isExistingPatientEdit = existingPatientPrimaryKey != null

  val existingParentAndOutcomes: Flow<PatientAndOutcomes?> = existingPatientPrimaryKey?.let { pk ->
    database.patientDao().getPatientAndOutcomesFlow(pk)
  } ?: flowOf(null)

  sealed class FormState {
    val isForPatientEdit get() = (this as? Ready)?.existingInfo != null

    object Loading : FormState()
    class Ready(val existingInfo: PatientAndOutcomes?) : FormState()
    object Saving : FormState()
    class SavedNewPatient(val primaryKeyOfPatient: Long) : FormState()
    class SavedEditsToExistingPatient(val primaryKeyOfPatient: Long) : FormState()
    class FailedException(val exception: Exception) : FormState()
    class FailedValidation(val errorsBySectionStringId: Map<Int, List<FieldError>>) : FormState()
    class FailedLoading(val message: String) : FormState()
  }

  private val _formState: MutableStateFlow<FormState> = MutableStateFlow(FormState.Loading)
  val formState: StateFlow<FormState> = _formState

  val formFields = PatientFormFields(
    patientFields = PatientFields(
      initials = InitialsState(
        backingState = handle.createMutableState("patientInitials", "")
      ),
      presentationDate = NoFutureDateState(
        handle.createMutableState("patientPresentationDate", "")
      ),
      dateOfBirth = LimitedAgeDateState(
        AGE_RANGE,
        handle.createMutableState("patientDateOfBirth", "")
      ),
      age = LimitedAgeIntState(
        AGE_RANGE,
        handle.createMutableState("patientAge", "")
      ),
      localNotes = handle.createMutableState("patientLocalNotes", "")
    ),
    eclampsia = OutcomeFields.Eclampsia(
      isEnabled = enabledState("eclampsiaEnabled"),
      date = dateState("eclampsiaDate"),
      placeOfFirstFit = enumIdOnlyState(
        "eclampsiaPlace",
        DropdownType.Place,
        isMandatory = false
      ),
    ),
    hysterectomy = OutcomeFields.Hysterectomy(
      isEnabled = enabledState("hysterectomyEnabled"),
      date = dateState("hysterectomyDate"),
      cause = enumWithOtherState(
        "hysterectomyCause",
        DropdownType.CauseOfHysterectomy,
        isMandatory = false
      ),
      additionalInfo = handle.createMutableState("hysterectomyExtraInfo", null)
    ),
    hduItuAdmission = OutcomeFields.HduItuAdmission(
      isEnabled = enabledState("hduItuAdmissionEnabled"),
      date = dateState("hduItuAdmissionDate"),
      cause = enumWithOtherState(
        "hduItuAdmissionCause",
        DropdownType.CauseForHduOrItuAdmission,
        isMandatory = true
      ),
      hduItuStayLengthInDays = LimitedHduItuState(
        VALID_LENGTH_OF_ITU_HDU_STAY,
        handle.createMutableState("hduItuStayLengthDays", "")
      )
    ),
    maternalDeath = OutcomeFields.MaternalDeath(
      isEnabled = enabledState("maternalDeathEnabled"),
      date = dateState("maternalDeathDate"),
      underlyingCause = enumWithOtherState(
        "maternalDeathUnderlyingCause",
        DropdownType.UnderlyingCauseOfDeath,
        isMandatory = false
      ),
      placeOfDeath = enumIdOnlyState(
        "maternalDeathPlace",
        DropdownType.Place,
        isMandatory = false
      )
    ),
    surgicalManagement = OutcomeFields.SurgicalManagement(
      isEnabled = enabledState("surgicalManagementEnabled"),
      date = dateState("surgicalManagementDate"),
      type = enumWithOtherState(
        "surgicalManagementType",
        DropdownType.TypeOfSurgicalManagement,
        isMandatory = false
      ),
    ),
    perinatalDeath = OutcomeFields.PerinatalDeath(
      isEnabled = enabledState("perinatalDeathEnabled"),
      date = dateState("perinatalDeathDate"),
      outcome = enumIdOnlyState(
        "perinatalDeathOutcome",
        DropdownType.PerinatalOutcome,
        isMandatory = false
      ),
      relatedMaternalFactors = enumWithOtherState(
        "perinatalDeathRelatedMaternalFactors",
        DropdownType.MaternalFactorsRelatedToPerinatalLoss,
        isMandatory = false
      ),
    )
  )

  init {
    Log.d(TAG, "initializing with pk = $existingPatientPrimaryKey")
    if (existingPatientPrimaryKey != null) {
      viewModelScope.launch(appCoroutineDispatchers.main) {
        val patientAndOutcomes = database.patientDao()
          .getPatientAndOutcomes(existingPatientPrimaryKey)
        _formState.value = if (patientAndOutcomes == null) {
          Log.w(TAG, "Unable to find patient with pk $existingPatientPrimaryKey")
          FormState.FailedLoading(
            context.getString(R.string.patient_form_failed_to_load_patient_with_pk_d)
          )
        } else {
          val (patient, outcomes) = patientAndOutcomes
          Log.d(
            TAG,
            "Setting up form for edit (outcomes == $outcomes)"
          )

          with(formFields.patientFields) {
            initials.backingState.value = patient.initials
            presentationDate.backingState.value = patient.presentationDate.toString()
            age.backingState.value = patient
              .dateOfBirth
              .getAgeInYearsFromNow()
              .toString()
            dateOfBirth.backingState.value = patient.dateOfBirth.toString()
          }

          with(formFields.eclampsia) {
            outcomes?.eclampsiaFit?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              placeOfFirstFit.backingState.value = it.place
            } ?: reset()
          }

          with(formFields.hysterectomy) {
            outcomes?.hysterectomy?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              cause.backingState.value = it.cause
              additionalInfo.value = it.additionalInfo
            } ?: reset()
          }

          with(formFields.hduItuAdmission) {
            outcomes?.hduOrItuAdmission?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              cause.backingState.value = it.cause
              hduItuStayLengthInDays.backingState.value = it.stayInDays?.toString() ?: ""
            } ?: reset()
          }

          with(formFields.maternalDeath) {
            outcomes?.maternalDeath?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              underlyingCause.backingState.value = it.underlyingCause
              placeOfDeath.backingState.value = it.place
            } ?: reset()
          }

          with(formFields.surgicalManagement) {
            outcomes?.surgicalManagement?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              type.backingState.value = it.typeOfSurgicalManagement
            } ?: reset()
          }

          with(formFields.perinatalDeath) {
            outcomes?.perinatalDeath?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              outcome.backingState.value = it.outcome
              relatedMaternalFactors.backingState.value = it.relatedMaternalFactors
            } ?: reset()
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

  private val saveRequestChannel = viewModelScope.actor<Unit>(
    context = appCoroutineDispatchers.default,
    capacity = Channel.RENDEZVOUS,
  ) {
    for (saveTick in channel) {
      Log.d(TAG, "Handling save request")
      _formState.value = FormState.Saving

      formFields.forceAllErrors()

      val fieldToErrorMap = linkedMapOf<Int, List<FieldError>>()
      fun OutcomeFields.getCategoryStringRes() = when (this) {
        is OutcomeFields.Eclampsia -> R.string.outcomes_eclampsia_label
        is OutcomeFields.HduItuAdmission -> R.string.outcomes_admission_to_hdu_or_itu_label
        is OutcomeFields.Hysterectomy -> R.string.outcomes_hysterectomy_label
        is OutcomeFields.MaternalDeath -> R.string.outcomes_maternal_death_label
        is OutcomeFields.PerinatalDeath -> R.string.outcomes_perinatal_death_label
        is OutcomeFields.SurgicalManagement -> R.string.outcomes_surgical_management_label
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
        val patientAndOutcomes = existingParentAndOutcomes.first()
        require(
          existingPatientPrimaryKey == null ||
            patientAndOutcomes?.patient?.id == existingPatientPrimaryKey
        ) {
          "Existing patient doesn't match primary key"
        }

        val patient = with(formFields.patientFields) {
          if (!initials.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.patient_registration_card_title,
              R.string.patient_registration_initials_label,
              initials.errorFor(context, initials.stateValue)
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
            fieldToErrorMap.addFieldError(
              R.string.patient_registration_card_title,
              R.string.patient_registration_age_label,
              age.errorFor(context, age.stateValue)
            )
          }
          if (!dateOfBirth.isValid) {
            fieldToErrorMap.addFieldError(
              R.string.patient_registration_card_title,
              R.string.patient_registration_date_of_birth_label,
              dateOfBirth.errorFor(context, dateOfBirth.stateValue)
            )
          }

          runCatching {
            Patient(
              id = patientAndOutcomes?.patient?.id ?: 0L,
              initials = initials.stateValue,
              presentationDate = presentationDate.stateValue.toFormDateOrThrow(),
              dateOfBirth = dateOfBirth.stateValue.toFormDateOrThrow(),
              localNotes = localNotes.value
            )
          }
        }

        val eclampsia = with(formFields.eclampsia) {
          when (isEnabled.value) {
            null -> {
              fieldToErrorMap.addFieldError(
                getCategoryStringRes(),
                R.string.outcomes_eclampsia_label,
                context.getString(R.string.outcomes_eclampsia_not_selected_error)
              )
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
              if (!placeOfFirstFit.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.place_of_first_eclamptic_fit_label,
                  placeOfFirstFit.errorFor(context, placeOfFirstFit.stateValue)
                )
              }

              runCatching {
                EclampsiaFit(
                  date = date.stateValue.toFormDateOrThrow(),
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
              fieldToErrorMap.addFieldError(
                getCategoryStringRes(),
                R.string.outcomes_hysterectomy_label,
                context.getString(R.string.outcomes_hysterectomy_not_selected_error)
              )
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
                  date = date.stateValue.toFormDateOrThrow(),
                  cause = cause.stateValue,
                  additionalInfo = additionalInfo.value
                )
              }
            }
            else -> null
          }
        }

        val hduOrItuAdmission = with(formFields.hduItuAdmission) {
          when (isEnabled.value) {
            null -> {
              fieldToErrorMap.addFieldError(
                getCategoryStringRes(),
                R.string.outcomes_admission_to_hdu_or_itu_label,
                context.getString(R.string.outcomes_hdu_itu_admission_not_selected_error)
              )
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
              if (!cause.isValid || cause.stateValue == null) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.hdu_or_idu_admission_cause_label,
                  cause.errorFor(context, cause.stateValue)
                )
              }
              if (!hduItuStayLengthInDays.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.hdu_or_idu_admission_length_stay_days_if_known_label,
                  hduItuStayLengthInDays.errorFor(context, hduItuStayLengthInDays.stateValue)
                )
              }

              runCatching {
                HduOrItuAdmission(
                  date = date.stateValue.toFormDateOrThrow(),
                  cause = cause.stateValue!!,
                  stayInDays = hduItuStayLengthInDays.stateValue.toIntOrNull()
                )
              }
            }
            else -> null
          }
        }

        val maternalDeath = with(formFields.maternalDeath) {
          when (isEnabled.value) {
            null -> {
              fieldToErrorMap.addFieldError(
                getCategoryStringRes(),
                R.string.outcomes_maternal_death_label,
                context.getString(R.string.outcomes_maternal_death_not_selected_error)
              )
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
                  date = date.stateValue.toFormDateOrThrow(),
                  underlyingCause = underlyingCause.stateValue,
                  place = placeOfDeath.stateValue
                )
              }
            }
            else -> null
          }
        }

        val surgicalManagement = with(formFields.surgicalManagement) {
          when (isEnabled.value) {
            null -> {
              fieldToErrorMap.addFieldError(
                getCategoryStringRes(),
                R.string.outcomes_surgical_management_label,
                context.getString(R.string.outcomes_surgical_management_not_selected_error)
              )
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
                  date = date.stateValue.toFormDateOrThrow(),
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
              fieldToErrorMap.addFieldError(
                getCategoryStringRes(),
                R.string.outcomes_perinatal_death_label,
                context.getString(R.string.outcomes_perinatal_death_not_selected_error)
              )
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
              if (!relatedMaternalFactors.isValid) {
                fieldToErrorMap.addFieldError(
                  getCategoryStringRes(),
                  R.string.perinatal_death_related_maternal_factors_label,
                  relatedMaternalFactors.errorFor(context, relatedMaternalFactors.stateValue)
                )
              }
              runCatching {
                PerinatalDeath(
                  date = date.stateValue.toFormDateOrThrow(),
                  outcome = outcome.stateValue,
                  relatedMaternalFactors = relatedMaternalFactors.stateValue
                )
              }
            }
            else -> null
          }
        }

        if (fieldToErrorMap.isNotEmpty()) {
          Log.d(TAG, "Errors: $fieldToErrorMap")
          FormState.FailedValidation(fieldToErrorMap)
        } else {
          Log.d(TAG, "Attempting to save new patient")
          val patientPrimaryKey = database.withTransaction {
            val pk = database.patientDao().upsert(patient.getOrThrow())
            // we have a foreign key constraint here
            database.outcomesDao().upsert(
              Outcomes(
                id = patientAndOutcomes?.outcomes?.id ?: 0,
                patientId = pk,
                eclampsiaFit = eclampsia?.getOrThrow(),
                hysterectomy = hysterectomy?.getOrThrow(),
                hduOrItuAdmission = hduOrItuAdmission?.getOrThrow(),
                maternalDeath = maternalDeath?.getOrThrow(),
                surgicalManagement = surgicalManagement?.getOrThrow(),
                perinatalDeath = perinatalDeath?.getOrThrow()
              )
            )
            pk
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

  private fun enabledState(key: String): SavedStateMutableState<Boolean?> =
    handle.createMutableState(key, null)

  private fun dateState(key: String) = NoFutureDateState(handle.createMutableState(key, ""))

  private fun enumIdOnlyState(
    key: String,
    dropdownType: DropdownType,
    isMandatory: Boolean,
  ) = EnumIdOnlyState(
    keyValueStore.getServerEnumCollection()[dropdownType]!!,
    isMandatory = isMandatory,
    handle.createMutableState(key, null),
  )

  private fun enumWithOtherState(
    key: String,
    dropdownType: DropdownType,
    isMandatory: Boolean,
  ) = EnumWithOtherState(
    enum = keyValueStore.getServerEnumCollection()[dropdownType]!!,
    isMandatory = isMandatory,
    backingState = handle.createMutableState(key, null)
  )

  @Stable
  data class PatientFormFields(
    val patientFields: PatientFields,
    val eclampsia: OutcomeFields.Eclampsia,
    val hysterectomy: OutcomeFields.Hysterectomy,
    val hduItuAdmission: OutcomeFields.HduItuAdmission,
    val maternalDeath: OutcomeFields.MaternalDeath,
    val surgicalManagement: OutcomeFields.SurgicalManagement,
    val perinatalDeath: OutcomeFields.PerinatalDeath
  ) {
    fun forceAllErrors() {
      patientFields.forceShowErrors()
      eclampsia.forceShowErrors()
      hysterectomy.forceShowErrors()
      hduItuAdmission.forceShowErrors()
      maternalDeath.forceShowErrors()
      surgicalManagement.forceShowErrors()
      perinatalDeath.forceShowErrors()
    }
  }

  @Stable
  data class PatientFields(
    val initials: InitialsState,
    val presentationDate: NoFutureDateState,
    val dateOfBirth: LimitedAgeDateState,
    val age: LimitedAgeIntState,
    val localNotes: MutableState<String>
  ) {
    fun forceShowErrors() {
      initials.enableShowErrors(force = true)
      presentationDate.enableShowErrors(force = true)
      dateOfBirth.enableShowErrors(force = true)
      age.enableShowErrors(force = true)
    }
  }

  @Stable
  sealed class OutcomeFields {
    abstract val isEnabled: MutableState<Boolean?>
    abstract val date: NoFutureDateState

    abstract fun reset()

    abstract fun forceShowErrors()

    @Stable
    data class Eclampsia(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val placeOfFirstFit: EnumIdOnlyState
    ) : OutcomeFields() {
      override fun reset() {
        isEnabled.value = false
        date.reset()
        placeOfFirstFit.reset()
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          date.enableShowErrors(force = true)
          placeOfFirstFit.enableShowErrors(force = true)
        }
      }
    }

    @Stable
    data class Hysterectomy(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val cause: EnumWithOtherState,
      val additionalInfo: MutableState<String?>
    ) : OutcomeFields() {
      override fun reset() {
        isEnabled.value = false
        date.reset()
        cause.reset()
        additionalInfo.value = null
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          date.enableShowErrors(force = true)
          cause.enableShowErrors(force = true)
        }
      }
    }

    @Stable
    data class HduItuAdmission(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val cause: EnumWithOtherState,
      val hduItuStayLengthInDays: LimitedHduItuState
    ) : OutcomeFields() {
      override fun reset() {
        isEnabled.value = false
        date.reset()
        cause.reset()
        hduItuStayLengthInDays.reset()
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          date.enableShowErrors(force = true)
          cause.enableShowErrors(force = true)
          hduItuStayLengthInDays.enableShowErrors(force = true)
        }
      }
    }

    @Stable
    data class MaternalDeath(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val underlyingCause: EnumWithOtherState,
      val placeOfDeath: EnumIdOnlyState
    ) : OutcomeFields() {
      override fun reset() {
        isEnabled.value = false
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
      override val date: NoFutureDateState,
      val type: EnumWithOtherState
    ) : OutcomeFields() {
      override fun reset() {
        isEnabled.value = false
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
      override val date: NoFutureDateState,
      val outcome: EnumIdOnlyState,
      val relatedMaternalFactors: EnumWithOtherState,
    ) : OutcomeFields() {
      override fun reset() {
        isEnabled.value = false
        date.reset()
        outcome.reset()
        relatedMaternalFactors.reset()
      }

      override fun forceShowErrors() {
        if (isEnabled.value == true) {
          date.enableShowErrors(force = true)
          outcome.enableShowErrors(force = true)
          relatedMaternalFactors.enableShowErrors(force = true)
        }
      }
    }
  }

  companion object {
    private const val TAG = "PatientFormViewModel"
  }
}

class SavedStateMutableState<T>(
  private val handle: SavedStateHandle,
  private val key: String,
  defaultValue: T,
) : MutableState<T> {

  private val mutableState: MutableState<T>

  init {
    val savedValue = handle.get<T>(key)
    mutableState = mutableStateOf(savedValue ?: defaultValue)
  }

  override var value: T
    get() = mutableState.value
    set(value) {
      set(value)
    }

  private fun set(new: T) {
    mutableState.value = new
    handle[key] = new
  }

  override fun component1(): T = value

  override fun component2(): (T) -> Unit = ::set
}

fun <T> SavedStateHandle.createMutableState(key: String, defaultValue: T) =
  SavedStateMutableState(this, key, defaultValue)
