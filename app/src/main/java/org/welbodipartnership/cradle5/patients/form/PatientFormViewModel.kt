package org.welbodipartnership.cradle5.patients.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.welbodipartnership.cradle5.compose.mutableStateOf
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.settings.AppKeyValueStore
import javax.inject.Inject

private const val PATIENT_MAX_INITIALS_LENGTH = 5

private val AGE_RANGE = 10L..60L

private val VALID_LENGTH_OF_ITU_HDU_STAY = 1L..100L

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
      mutableState.value = value
      handle[key] = value
    }
  }
}

fun <T> SavedStateHandle.createMutableState(key: String, defaultValue: T) =
  SavedStateMutableState(this, key, defaultValue)

@HiltViewModel
class PatientFormViewModel @Inject constructor(
  private val handle: SavedStateHandle,
  private val keyValueStore: AppKeyValueStore,
  dbWrapper: CradleDatabaseWrapper
) : ViewModel() {
  val database = dbWrapper.database!!

  var x by handle.mutableStateOf("")

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
      )
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

  @Stable
  data class PatientFormFields(
    val patientFields: PatientFields,
    val eclampsia: OutcomeFields.Eclampsia,
    val hysterectomy: OutcomeFields.Hysterectomy,
    val hduItuAdmission: OutcomeFields.HduItuAdmission,
    val maternalDeath: OutcomeFields.MaternalDeath,
    val surgicalManagement: OutcomeFields.SurgicalManagement,
    val perinatalDeath: OutcomeFields.PerinatalDeath
  )

  @Stable
  data class PatientFields(
    val initials: InitialsState,
    val presentationDate: NoFutureDateState,
    val dateOfBirth: LimitedAgeDateState,
    val age: LimitedAgeIntState
  )

  @Stable
  sealed class OutcomeFields {
    abstract val isEnabled: MutableState<Boolean?>
    abstract val date: NoFutureDateState

    @Stable
    data class Eclampsia(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val placeOfFirstFit: EnumIdOnlyState
    ) : OutcomeFields()

    @Stable
    data class Hysterectomy(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val cause: EnumWithOtherState,
      val extraInfo: MutableState<String?>
    ) : OutcomeFields()

    @Stable
    data class HduItuAdmission(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val cause: EnumWithOtherState,
      val hduItuStayLengthInDays: LimitedHduItuState
    ) : OutcomeFields()

    @Stable
    data class MaternalDeath(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val underlyingCause: EnumWithOtherState,
      val placeOfDeath: EnumIdOnlyState
    ) : OutcomeFields()

    @Stable
    data class SurgicalManagement(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val type: EnumWithOtherState
    ) : OutcomeFields()

    @Stable
    data class PerinatalDeath(
      override val isEnabled: MutableState<Boolean?>,
      override val date: NoFutureDateState,
      val outcome: EnumIdOnlyState,
      val relatedMaternalFactors: EnumWithOtherState,
    ) : OutcomeFields()
  }
}
