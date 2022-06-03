/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.welbodipartnership.cradle5.compose.forms.state

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.util.DistrictAndPosition
import org.welbodipartnership.cradle5.util.FacilityAndPosition
import org.welbodipartnership.cradle5.util.datetime.FormDate
import org.welbodipartnership.cradle5.util.datetime.toFormDateFromNoSlashesOrNull
import org.welbodipartnership.cradle5.util.datetime.toFormDateFromNoSlashesOrThrow

open class TextFieldState(
  validator: (String) -> Boolean,
  errorFor: (Context, String) -> String,
  initialValue: String = "",
  backingState: MutableState<String> = mutableStateOf(initialValue),
  isFormDraftState: State<Boolean?>,
  isMandatory: Boolean,
) : FieldState<String>(
  validator,
  errorFor,
  initialValue,
  backingState,
  isFormDraftState,
  isMandatory
) {
  override val showErrorOnInput: Boolean = false
  override fun isMissing(): Boolean = stateValue.isBlank()
}

/**
 * Derived from official Compose samples (Jetsurvey). There's likely a better way to handle this.
 */
abstract class FieldState<T>(
  val validator: (T) -> Boolean = { true },
  val errorFor: (Context, T) -> String = { _, _ -> "" },
  val initialValue: T,
  val backingState: MutableState<T> = mutableStateOf(initialValue),
  val isFormDraftState: State<Boolean?>,
  val isMandatory: Boolean
) {
  abstract val showErrorOnInput: Boolean

  var stateValue: T
    get() = backingState.value
    set(value) {
      if (showErrorOnInput) {
        isFocusedDirty = true
      }

      backingState.value = value
      onNewStateValue(value)
    }

  open fun onNewStateValue(newValue: T) {}

  abstract fun isMissing(): Boolean

  // was the TextField ever focused
  var isFocusedDirty by mutableStateOf(false)
  var isFocused: Boolean by mutableStateOf(false)
  private var displayErrors: Boolean by mutableStateOf(false)

  fun reset() {
    isFocusedDirty = showErrorOnInput
    isFocused = false
    displayErrors = false
    stateValue = initialValue
  }

  val isValid: Boolean by derivedStateOf { validator(stateValue) }

  fun createFocusChangeModifier() = Modifier
    .onFocusChanged { focusState ->
      onFocusChange(focusState.isFocused)
      if (!focusState.isFocused) {
        enableShowErrors()
      }
    }

  private fun onFocusChange(focused: Boolean) {
    isFocused = focused
    if (focused) isFocusedDirty = true
  }

  fun enableShowErrors(force: Boolean = false) {
    // only show errors if the text was at least once focused
    if (isFocusedDirty || force) {
      displayErrors = true
    }
  }

  fun showErrors() = !isValid && displayErrors

  @Composable
  open fun getError(): String? {
    return if (showErrors()) {
      errorFor(LocalContext.current, stateValue)
    } else {
      null
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FieldState<*>

    if (showErrorOnInput != other.showErrorOnInput) return false
    if (stateValue != other.stateValue) return false
    if (isFocusedDirty != other.isFocusedDirty) return false
    if (isFocused != other.isFocused) return false
    if (isMandatory != other.isMandatory) return false

    return true
  }

  override fun hashCode(): Int {
    var result = stateValue?.hashCode() ?: 0
    result = 31 * result + isFocusedDirty.hashCode()
    result = 31 * result + isFocused.hashCode()
    result = 31 * result + isMandatory.hashCode()
    return result
  }
}

class NonEmptyTextState(
  isMandatory: Boolean,
  backingState: MutableState<String?>,
  isFormDraftState: State<Boolean?>
) : FieldState<String?>(
  validator = { !it.isNullOrBlank() },
  errorFor = { ctx, _ -> ctx.getString(R.string.missing_text_error) },
  initialValue = null,
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory,
) {
  override val showErrorOnInput: Boolean = true
  override fun isMissing(): Boolean {
    return stateValue == null
  }
}

class HealthcareFacilityState(
  isMandatory: Boolean,
  backingState: MutableState<FacilityAndPosition?>,
  isFormDraftState: State<Boolean?>
) : FieldState<FacilityAndPosition?>(
  validator = { facility ->
    if (isFormDraftState.value == true && facility == null) {
      true
    } else if (isMandatory) {
      facility?.facility != null
    } else {
      true
    }
  },
  errorFor = { ctx, _ -> ctx.getString(R.string.missing_healthcare_facility_error) },
  initialValue = null,
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory,
) {
  override val showErrorOnInput: Boolean = true
  override fun isMissing(): Boolean {
    return stateValue == null
  }
}

class DistrictState(
  isMandatory: Boolean,
  backingState: MutableState<DistrictAndPosition?>,
  isFormDraftState: State<Boolean?>
) : FieldState<DistrictAndPosition?>(
  validator = { district ->
    if (isFormDraftState.value == true && district == null) {
      true
    } else if (isMandatory) {
      district != null
    } else {
      true
    }
  },
  errorFor = { ctx, _ -> ctx.getString(R.string.missing_district_error) },
  initialValue = null,
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory,
) {
  override val showErrorOnInput: Boolean = true
  override fun isMissing(): Boolean {
    return stateValue == null
  }
}

class InitialsState(
  isMandatory: Boolean,
  backingState: MutableState<String> = mutableStateOf(""),
  isFormDraftState: State<Boolean?>,
  maxInitialsLength: Int,
) : TextFieldState(
  validator = { it.length in 1..maxInitialsLength },
  errorFor = { ctx, _, -> ctx.getString(R.string.patient_registration_initials_error) },
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory,
) {
  override val showErrorOnInput: Boolean = false
}

class NoFutureDateAndAheadOfMaternalDeathState(
  isMandatory: Boolean,
  areApproximateDatesAcceptable: Boolean,
  backingState: MutableState<String> = mutableStateOf(""),
  isFormDraftState: State<Boolean?>,
  maternalDeathDateState: NoFutureDateState
) : NoFutureDateState(
  validator = { possibleDate ->
    run {
      if (isFormDraftState.value == true && possibleDate.isEmpty()) {
        return@run true
      }

      if (!isMandatory && possibleDate.isEmpty()) {
        return@run true
      }

      val formDate = try {
        possibleDate.toFormDateFromNoSlashesOrThrow()
      } catch (e: NumberFormatException) {
        return@run false
      }

      if (!formDate.isValid(areApproximateDatesAcceptable)) {
        return@run false
      }
      if (formDate > FormDate.today()) {
        return@run false
      }
      val maternalDeathDate = maternalDeathDateState.dateFromStateOrNull() ?: return@run true
      formDate <= maternalDeathDate
    }
  },
  errorFor = { ctx, date ->
    val formDate = date.toFormDateFromNoSlashesOrNull()
    if (formDate != null) {
      when {
        formDate.isValid(areApproximateDatesAcceptable) -> {
          if (formDate > FormDate.today()) {
            ctx.getString(R.string.form_date_cannot_be_in_future_error)
          } else {
            ctx.getString(R.string.form_date_cannot_be_after_maternal_death_error)
          }
        }
        formDate.isValidIfItWereMmDdYyyyFormat(areApproximateDatesAcceptable) -> {
          ctx.getString(R.string.form_date_expected_day_month_year_format_error)
        }
        else -> {
          ctx.getString(R.string.form_date_invalid_error)
        }
      }
    } else {
      if (isMandatory && date.isBlank()) {
        ctx.getString(R.string.form_date_required_error)
      } else {
        ctx.getString(R.string.form_date_invalid_error)
      }
    }
  },
  areApproximateDatesAcceptable = areApproximateDatesAcceptable,
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory,
)

open class NoFutureDateState(
  isMandatory: Boolean,
  val areApproximateDatesAcceptable: Boolean,
  backingState: MutableState<String> = mutableStateOf(""),
  isFormDraftState: State<Boolean?>,
  validator: (String) -> Boolean = { possibleDate ->
    run {
      if (isFormDraftState.value == true && possibleDate.isEmpty()) {
        return@run true
      }

      if (!isMandatory && possibleDate.isEmpty()) {
        return@run true
      }

      val formDate = try {
        possibleDate.toFormDateFromNoSlashesOrThrow()
      } catch (e: NumberFormatException) {
        return@run false
      }

      if (!formDate.isValid(areApproximateDatesAcceptable)) {
        return@run false
      }

      formDate <= FormDate.today()
    }
  },
  errorFor: (Context, String) -> String = { ctx, date ->
    val formDate = date.toFormDateFromNoSlashesOrNull()
    if (formDate != null) {
      when {
        formDate.isValid(areApproximateDatesAcceptable) -> ctx.getString(R.string.form_date_cannot_be_in_future_error)
        formDate.isValidIfItWereMmDdYyyyFormat(areApproximateDatesAcceptable) -> {
          ctx.getString(R.string.form_date_expected_day_month_year_format_error)
        }
        else -> {
          ctx.getString(R.string.form_date_invalid_error)
        }
      }
    } else {
      if (isMandatory && date.isBlank()) {
        ctx.getString(R.string.form_date_required_error)
      } else {
        ctx.getString(R.string.form_date_invalid_error)
      }
    }
  },
) : TextFieldState(
  validator = validator,
  errorFor = errorFor,
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory,
) {

  fun dateFromStateOrNull() = stateValue.toFormDateFromNoSlashesOrNull()
  fun dateFromStateOrThrow() = stateValue.toFormDateFromNoSlashesOrThrow()
  fun setStateFromFormDate(formDate: FormDate?) {
    stateValue = formDate?.toString(withSlashes = false) ?: ""
  }
}

class LimitedIntState(
  isMandatory: Boolean,
  val limit: IntRange,
  backingState: MutableState<String> = mutableStateOf(""),
  isFormDraftState: State<Boolean?>,
  private val upperBoundInfo: UpperBoundInfo? = null,
) : TextFieldState(
  validator = { possibleInt ->
    run {
      if ((isFormDraftState.value == true || !isMandatory) && possibleInt.isBlank()) return@run true
      val value = possibleInt.toIntOrNull() ?: return@run false
      if (value !in limit) return@run false
      if (upperBoundInfo != null) {
        val upper = upperBoundInfo.stateUpperBound.stateValue.toIntOrNull()
        if (upper == null) true else value <= upper
      } else {
        true
      }
    }
  },
  errorFor = { ctx, possibleInt ->
    run {
      val age = possibleInt.toIntOrNull() ?: return@run ctx.getString(R.string.input_must_be_in_range_d_and_d, limit.first, limit.last)
      if (age !in limit) return@run ctx.getString(R.string.input_must_be_in_range_d_and_d, limit.first, limit.last)
      if (upperBoundInfo != null) {
        val upper = upperBoundInfo.stateUpperBound.stateValue.toIntOrNull()
        if (upper == null) {
          ctx.getString(R.string.input_must_be_in_range_d_and_d, limit.first, limit.last)
        } else {
          ctx.getString(upperBoundInfo.upperBoundErrorString)
        }
      } else {
        ctx.getString(R.string.input_must_be_in_range_d_and_d, limit.first, limit.last)
      }
    }
  },
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory
) {
  @Stable
  data class UpperBoundInfo(
    val stateUpperBound: TextFieldState,
    @StringRes val upperBoundErrorString: Int,
  )
}

class LimitedAgeDateState(
  isMandatory: Boolean,
  val limit: LongRange,
  val areApproximateDatesAcceptable: Boolean,
  backingState: MutableState<String> = mutableStateOf(""),
  isFormDraftState: State<Boolean?>,
) : TextFieldState(
  validator = { possibleDate ->
    run {
      if (isFormDraftState.value == true && possibleDate.isBlank()) {
        return@run true
      }

      val formDate = try {
        possibleDate.toFormDateFromNoSlashesOrThrow()
      } catch (e: NumberFormatException) {
        return@run false
      }

      if (!formDate.isValid(areNonExactDatesValid = areApproximateDatesAcceptable)) {
        return@run false
      }

      formDate.getAgeInYearsFromNow() in limit
    }
  },
  errorFor = { ctx, date ->
    val formDate = date.toFormDateFromNoSlashesOrNull()
    when {
      formDate != null -> {
        when {
          formDate.isValid(areApproximateDatesAcceptable) -> {
            ctx.getString(R.string.age_must_be_in_range_d_and_d, limit.first, limit.last)
          }
          formDate.isValidIfItWereMmDdYyyyFormat(areApproximateDatesAcceptable) -> {
            ctx.getString(R.string.form_date_expected_day_month_year_format_error)
          }
          else -> ctx.getString(R.string.form_date_invalid_error)
        }
      }
      date.isBlank() -> {
        ctx.getString(R.string.form_date_required_error)
      }
      else -> {
        ctx.getString(R.string.form_date_invalid_error)
      }
    }
  },
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory,
) {
  fun dateFromStateOrNull() = stateValue.toFormDateFromNoSlashesOrNull()
  fun dateFromStateOrThrow() = stateValue.toFormDateFromNoSlashesOrThrow()
  fun setStateFromFormDate(formDate: FormDate?) {
    stateValue = formDate?.toString(withSlashes = false) ?: ""
  }
}

class LimitedAgeIntState(
  isMandatory: Boolean,
  val limit: LongRange,
  backingState: MutableState<String> = mutableStateOf(""),
  isFormDraftState: State<Boolean?>,
) : TextFieldState(
  validator = { possibleAge ->
    run {
      if (isFormDraftState.value == true && possibleAge.isBlank()) {
        return@run true
      }

      val age = possibleAge.toIntOrNull() ?: return@run false
      age in limit
    }
  },
  errorFor = { ctx, _ -> ctx.getString(R.string.age_must_be_in_range_d_and_d, limit.first, limit.last) },
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory
)

class NullableToggleState(
  backingState: MutableState<Boolean?> = mutableStateOf(null),
  isFormDraftState: State<Boolean?>,
  isMandatory: Boolean,
) : FieldState<Boolean?>(
  validator = { it != null },
  errorFor = { ctx, _ -> ctx.getString(R.string.selection_required_error) },
  backingState = backingState,
  initialValue = null,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory
) {
  override val showErrorOnInput: Boolean = false
  override fun isMissing(): Boolean = stateValue == null
}

class EnumIdOnlyState(
  val enum: ServerEnum?,
  isMandatory: Boolean,
  backingState: MutableState<EnumSelection.IdOnly?> = mutableStateOf(null),
  isFormDraftState: State<Boolean?>,
) : FieldState<EnumSelection.IdOnly?>(
  validator = { selection ->
    when {
      selection == null -> !isMandatory || (isMandatory && isFormDraftState.value == true)
      enum == null -> true
      else -> enum.get(selection.selectionId) != null
    }
  },
  errorFor = { ctx, selection, ->
    val entry = selection?.let { enum?.get(it.selectionId) }
    if (entry == null && isMandatory) {
      ctx.getString(R.string.selection_required_error)
    } else {
      ctx.getString(R.string.server_enum_unknown_selection_error)
    }
  },
  initialValue = null,
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory,
) {
  override val showErrorOnInput: Boolean = true
  override fun isMissing(): Boolean = stateValue == null
}

class EnumWithOtherState(
  val enum: ServerEnum?,
  isMandatory: Boolean,
  private val otherSelection: ServerEnum.Entry? = enum?.validSortedValues?.find { it.name == "Other" },
  backingState: MutableState<EnumSelection.WithOther?> = mutableStateOf(null),
  isFormDraftState: State<Boolean?>
) : FieldState<EnumSelection.WithOther?>(
  validator = { selection ->
    when {
      selection == null -> !isMandatory || (isMandatory && isFormDraftState.value == true)
      enum == null -> true
      else -> {
        val entry = selection.let { enum.get(it.selectionId) }
        if (entry == null) {
          false
        } else if (entry == otherSelection && selection.otherString.isNullOrBlank()) {
          isFormDraftState.value == true
        } else {
          true
        }
      }
    }
  },
  errorFor = { ctx, selection, ->
    val entry = selection?.let { enum?.get(it.selectionId) }
    if (entry == otherSelection && selection?.otherString.isNullOrBlank()) {
      ctx.getString(R.string.server_enum_other_selection_missing_error)
    } else if (entry == null && isMandatory) {
      ctx.getString(R.string.selection_required_error)
    } else {
      ctx.getString(R.string.server_enum_unknown_selection_error)
    }
  },
  initialValue = null,
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory,
) {
  override val showErrorOnInput: Boolean = true
  override fun isMissing() = stateValue == null
  override fun onNewStateValue(newValue: EnumSelection.WithOther?) {
    if (isMandatory && newValue != null) {
      enableShowErrors(force = true)
    }
  }
}
