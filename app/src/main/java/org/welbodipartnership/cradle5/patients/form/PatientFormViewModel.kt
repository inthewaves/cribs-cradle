package org.welbodipartnership.cradle5.patients.form

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.constraintlayout.compose.parseKeyAttribute
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.settings.AppKeyValueStore
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import org.welbodipartnership.cradle5.util.date.toFormDateOrThrow
import java.lang.Exception
import java.lang.NullPointerException
import javax.inject.Inject

private const val PATIENT_MAX_INITIALS_LENGTH = 5

private val AGE_RANGE = 10L..60L

private val VALID_LENGTH_OF_ITU_HDU_STAY = 1L..100L


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

  sealed class FormState {
    object Loading : FormState()
    object Ready : FormState()
    object Saving : FormState()
    class SavedNewPatient(val primaryKeyOfPatient: Long) : FormState()
    class SavedEditsToExistingPatient(val primaryKeyOfPatient: Long) : FormState()
    object Failed : FormState()
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
      placeOfFirstFit = enumIdOnlyState("eclampsiaPlace", DropdownType.Place),
    ),
    hysterectomy = OutcomeFields.Hysterectomy(
      isEnabled = enabledState("hysterectomyEnabled"),
      date = dateState("hysterectomyDate"),
      cause = enumWithOtherState(
        "hysterectomyCause",
        DropdownType.CauseOfHysterectomy,
        isMandatory = false
      ),
      extraInfo = handle.createMutableState("hysterectomyExtraInfo", null)
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
      ),
      relatedMaternalFactors = enumWithOtherState(
        "perinatalDeathRelatedMaternalFactors",
        DropdownType.MaternalFactorsRelatedToPerinatalLoss,
        isMandatory = false
      ),
    )
  )

  init {
    Log.d(TAG, "I am initializing.")
    if (existingPatientPrimaryKey != null) {
      viewModelScope.launch(appCoroutineDispatchers.default) {
        val patientAndOutcomes = database.patientDao().getPatientAndOutcomes(existingPatientPrimaryKey)
        _formState.value = if (patientAndOutcomes == null) {
          Log.w(TAG, "Unable to find patient with pk $existingPatientPrimaryKey")
          FormState.Failed
        } else {
          val (patient, outcomes) = patientAndOutcomes

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
            }
          }

          with(formFields.hysterectomy) {
            outcomes?.hysterectomy?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              cause.backingState.value = it.cause
              extraInfo.value = it.additionalInfo
            }
          }

          with(formFields.hduItuAdmission) {
            outcomes?.hduOrItuAdmission?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              cause.backingState.value = it.cause
              hduItuStayLengthInDays.backingState.value = it.stayInDays?.toString() ?: ""
            }
          }

          with(formFields.maternalDeath) {
            outcomes?.maternalDeath?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              underlyingCause.backingState.value = it.underlyingCause
              placeOfDeath.backingState.value = it.place
            }
          }

          with(formFields.surgicalManagement) {
            outcomes?.surgicalManagement?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              type.backingState.value = it.typeOfSurgicalManagement
            }
          }

          with(formFields.perinatalDeath) {
            outcomes?.perinatalDeath?.let {
              isEnabled.value = true
              date.backingState.value = it.date.toString()
              outcome.backingState.value = it.outcome
              relatedMaternalFactors.backingState.value = it.relatedMaternalFactors
            }
          }

          FormState.Ready
        }
      }
    } else {
      _formState.value = FormState.Ready
    }
  }

  fun save() {
    saveRequestChannel.trySend(Unit)
  }

  private val saveRequestChannel = viewModelScope.actor<Unit>(
    context = appCoroutineDispatchers.default,
    capacity = Channel.RENDEZVOUS,
  ) {
    for (saveTick in channel) {
      _formState.value = FormState.Saving

      formFields.forceAllErrors()

      class InvalidFieldException(override val message: String) : Exception()

      _formState.value = try {
        val patient: Patient = with(formFields.patientFields) {
          if (!initials.isValid) {
            throw InvalidFieldException(initials.errorFor(context, initials.stateValue))
          }
          if (!presentationDate.isValid) {
            throw InvalidFieldException(presentationDate.errorFor(context, initials.stateValue))
          }
          if (!age.isValid) {
            throw InvalidFieldException(age.errorFor(context, initials.stateValue))
          }
          if (!dateOfBirth.isValid) {
            throw InvalidFieldException(dateOfBirth.errorFor(context, initials.stateValue))
          }

          Patient(
            initials = initials.stateValue,
            presentationDate = presentationDate.stateValue.toFormDateOrThrow(),
            dateOfBirth = dateOfBirth.stateValue.toFormDateOrThrow(),
            localNotes = localNotes.value
          )
        }

        val eclampsia: EclampsiaFit? = with(formFields.eclampsia) {
          when (isEnabled.value) {
            null -> throw InvalidFieldException(
              context.getString(R.string.outcomes_eclampsia_not_selected_error)
            )
            true -> {
              if (!date.isValid) {
                throw InvalidFieldException(date.errorFor(context, date.stateValue))
              }
              if (!placeOfFirstFit.isValid) {
                throw InvalidFieldException(placeOfFirstFit.errorFor(context, placeOfFirstFit.stateValue))
              }
              EclampsiaFit(
                date = date.stateValue.toFormDateOrThrow(),
                place = placeOfFirstFit.stateValue
              )
            }
            else -> null
          }
        }

        val hysterectomy: Hysterectomy? = with(formFields.hysterectomy) {
          when (isEnabled.value) {
            null -> throw InvalidFieldException(
              context.getString(R.string.outcomes_hysterectomy_not_selected_error)
            )
            true -> {
              if (!date.isValid) {
                throw InvalidFieldException(date.errorFor(context, date.stateValue))
              }
              if (!cause.isValid) {
                throw InvalidFieldException(cause.errorFor(context, cause.stateValue))
              }
              Hysterectomy(
                date = date.stateValue.toFormDateOrThrow(),
                cause = cause.stateValue
              )
            }
            else -> {
              null
            }
          }
        }

        val hduOrItuAdmission = with(formFields.hduItuAdmission) {
          when (isEnabled.value) {
            null -> throw InvalidFieldException(
              context.getString(R.string.outcomes_eclampsia_not_selected_error)
            )
            true -> {
              if (!date.isValid) {
                throw InvalidFieldException(date.errorFor(context, date.stateValue))
              }
              if (!cause.isValid || cause.stateValue == null) {
                throw InvalidFieldException(cause.errorFor(context, cause.stateValue))
              }
              if (!hduItuStayLengthInDays.isValid) {
                throw InvalidFieldException(
                  hduItuStayLengthInDays.errorFor(context, hduItuStayLengthInDays.stateValue)
                )
              }
              HduOrItuAdmission(
                date = date.stateValue.toFormDateOrThrow(),
                cause = cause.stateValue!!,
                stayInDays = hduItuStayLengthInDays.stateValue.toIntOrNull()
              )
            }
            else -> null
          }
        }

        val maternalDeath = with(formFields.maternalDeath) {
          when (isEnabled.value) {
            null -> throw InvalidFieldException(
              context.getString(R.string.outcomes_eclampsia_not_selected_error)
            )
            true -> {
              if (!date.isValid) {
                throw InvalidFieldException(date.errorFor(context, date.stateValue))
              }
              if (!underlyingCause.isValid) {
                throw InvalidFieldException(underlyingCause.errorFor(context, underlyingCause.stateValue))
              }
              if (!placeOfDeath.isValid) {
                throw InvalidFieldException(placeOfDeath.errorFor(context, placeOfDeath.stateValue))
              }
              MaternalDeath(
                date = date.stateValue.toFormDateOrThrow(),
                underlyingCause = underlyingCause.stateValue,
                place = placeOfDeath.stateValue
              )
            }
            else -> null
          }
        }

        val surgicalManagement = with(formFields.surgicalManagement) {
          when (isEnabled.value) {
            null -> throw InvalidFieldException(
              context.getString(R.string.outcomes_eclampsia_not_selected_error)
            )
            true -> {
              if (!date.isValid) {
                throw InvalidFieldException(date.errorFor(context, date.stateValue))
              }
              if (!type.isValid) {
                throw InvalidFieldException(type.errorFor(context, type.stateValue))
              }
              SurgicalManagementOfHaemorrhage(
                date = date.stateValue.toFormDateOrThrow(),
                typeOfSurgicalManagement = type.stateValue
              )
            }
            else -> null
          }
        }

        val perinatalDeath = with(formFields.perinatalDeath) {
          when (isEnabled.value) {
            null -> throw InvalidFieldException(
              context.getString(R.string.outcomes_eclampsia_not_selected_error)
            )
            true -> {
              if (!date.isValid) {
                throw InvalidFieldException(date.errorFor(context, date.stateValue))
              }
              if (!outcome.isValid) {
                throw InvalidFieldException(outcome.errorFor(context, outcome.stateValue))
              }
              if (!relatedMaternalFactors.isValid) {
                throw InvalidFieldException(
                  relatedMaternalFactors.errorFor(context, relatedMaternalFactors.stateValue)
                )
              }
              PerinatalDeath(
                date = date.stateValue.toFormDateOrThrow(),
                outcome = outcome.stateValue,
                relatedMaternalFactors = relatedMaternalFactors.stateValue
              )
            }
            else -> null
          }
        }

        val patientPrimaryKey = database.withTransaction {
          val pk = database.patientDao().upsert(patient)
          // we have a foreign key constraint here
          database.outcomesDao().upsert(
            Outcomes(
              patientId = pk,
              eclampsiaFit = eclampsia,
              hysterectomy = hysterectomy,
              hduOrItuAdmission = hduOrItuAdmission,
              maternalDeath = maternalDeath,
              surgicalManagement = surgicalManagement,
              perinatalDeath = perinatalDeath
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
      } catch (e: InvalidFieldException) {
        Log.w(TAG, "Failed to save patient")
        FormState.Failed
      } catch (e: NullPointerException) {
        Log.w(TAG, "Failed to save patient", e)
        FormState.Failed
      }
    }
  }

  private fun enabledState(key: String): SavedStateMutableState<Boolean?> =
    handle.createMutableState(key, null)

  private fun dateState(key: String) = NoFutureDateState(handle.createMutableState(key, ""))

  private fun enumIdOnlyState(
    key: String,
    dropdownType: DropdownType,
  ) = EnumIdOnlyState(
    keyValueStore.getServerEnumCollection()[dropdownType]!!,
    handle.createMutableState(key, null)
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
      val extraInfo: MutableState<String?>
    ) : OutcomeFields() {
      override fun reset() {
        isEnabled.value = false
        date.reset()
        cause.reset()
        extraInfo.value = null
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
    mutableState = mutableStateOf(
      savedValue ?: defaultValue
    )
  }

  override var value: T
    get() = mutableState.value
    set(value) {
      mutableState.value = value
      handle[key] = value
    }

  override fun component1(): T = value

  override fun component2(): (T) -> Unit {
    return {
      mutableState.value = it
      handle[key] = it
    }
  }
}

fun <T> SavedStateHandle.createMutableState(key: String, defaultValue: T) =
  SavedStateMutableState(this, key, defaultValue)
