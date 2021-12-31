package org.welbodipartnership.cradle5.patients.form

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import kotlinx.parcelize.Parcelize
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.patients.details.BaseDetailsCard
import org.welbodipartnership.cradle5.ui.composables.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.composables.forms.DateOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.FieldState
import org.welbodipartnership.cradle5.ui.composables.forms.TextFieldState
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.date.FormDate
import org.welbodipartnership.cradle5.util.date.toFormDateOrNull
import org.welbodipartnership.cradle5.util.date.toFormDateOrThrow

private val MAX_INITIALS_LENGTH = 5

private val DOB_RANGE = 10L..60L

/**
 * Support wide screen by making the content width max 840dp, centered horizontally.
 */
fun Modifier.supportWideScreen() = this
  .fillMaxWidth()
  .wrapContentWidth(align = Alignment.CenterHorizontally)
  .widthIn(max = 840.dp)

@Composable
@ReadOnlyComposable
fun String.withRequiredStar() = buildAnnotatedString {
  append(this@withRequiredStar)
  withStyle(SpanStyle(color = MaterialTheme.colors.error)) {
    append('*')
  }
}

@Composable
fun PatientForm() {
  Scaffold(
    topBar = {
      TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        contentPadding = rememberInsetsPaddingValues(
          insets = LocalWindowInsets.current.systemBars,
          applyBottom = false,
        ),
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.new_patient_title)) },
      )
    },
  ) { padding ->
    val focusRequester = remember { FocusRequester() }

    val scrollState = rememberScrollState()

    Column(
      Modifier
        .padding(padding)
        .verticalScroll(scrollState)) {
      val initials = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> InitialsState().also { it.stateValue = savedText } }
        )
      ) { InitialsState() }
      val presentationDate = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> NoFutureDateState().also { it.stateValue = savedText } }
        )
      ) { NoFutureDateState() }
      val dateOfBirth = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> LimitedAgeDateState(DOB_RANGE).also { it.stateValue = savedText } }
        )
      ) { LimitedAgeDateState(DOB_RANGE) }
      val age = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> LimitedAgeIntState(DOB_RANGE).also { it.stateValue = savedText } }
        )
      ) { LimitedAgeIntState(DOB_RANGE) }

      BaseDetailsCard(
        stringResource(R.string.patient_registration_card_title),
        Modifier.padding(16.dp)
      ) {
        OutlinedTextFieldWithErrorHint(
          value = initials.stateValue.uppercase(),
          onValueChange = {
            // TODO: Hard limit text
            initials.stateValue = it.uppercase()
          },
          label = {
            Text(
              text = stringResource(id = R.string.patient_registration_initials_label).withRequiredStar(),
              // style = MaterialTheme.typography.body2
            )
          },
          textFieldModifier = Modifier
            .then(initials.createFocusChangeModifier())
            .fillMaxWidth(),
          // textStyle = MaterialTheme.typography.body2,
          errorHint = initials.getError(),
          keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Text
          ),
          singleLine = true,
          keyboardActions = KeyboardActions(
            onDone = {
              focusRequester.requestFocus()
            }
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        DateOutlinedTextField(
          date = presentationDate.stateValue.toFormDateOrNull(),
          onDatePicked = {
            presentationDate.stateValue = it.toString()
          },
          onPickerClose = { presentationDate.enableShowErrors(force = true) },
          label = {
            Text(
              text = stringResource(id = R.string.patient_registration_presentation_date_label)
                .withRequiredStar(),
            )
          },
          modifier = Modifier.fillMaxWidth(),
          textFieldModifier = presentationDate
            .createFocusChangeModifier()
            .fillMaxWidth(),
          // textStyle = MaterialTheme.typography.body2,
          errorHint = presentationDate.getError(),
          keyboardOptions = KeyboardOptions.Default,
          keyboardActions = KeyboardActions(
            onDone = {
              // onImeAction()
            }
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        DateOutlinedTextField(
          date = dateOfBirth.stateValue.toFormDateOrNull(),
          onDatePicked = {
            dateOfBirth.stateValue = it.toString()
            age.stateValue = it.getAgeInYearsFromNow().toString()
          },
          onPickerClose = {
            dateOfBirth.enableShowErrors(force = true)
            age.enableShowErrors(force = true)
          },
          label = {
            Text(
              text = stringResource(id = R.string.patient_registration_date_of_birth_label)
                .withRequiredStar(),
            )
          },
          modifier = Modifier.fillMaxWidth(),
          textFieldModifier = dateOfBirth
            .createFocusChangeModifier()
            .fillMaxWidth(),
          // textStyle = MaterialTheme.typography.body2,
          errorHint = null, // dateOfBirth.getError(),
          keyboardOptions = KeyboardOptions.Default,
          keyboardActions = KeyboardActions(
            onDone = {
              // onImeAction()
            }
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextFieldWithErrorHint(
          value = age.stateValue,
          onValueChange = { newAge ->
            age.stateValue = newAge
            newAge.toIntOrNull()?.let {
              dateOfBirth.stateValue = FormDate.fromAge(it).toString()
            }
          },
          label = {
            Text(
              stringResource(id = R.string.patient_registration_age_label).withRequiredStar()
            )
          },
          modifier = Modifier.fillMaxWidth(),
          textFieldModifier = age
            .createFocusChangeModifier()
            .then(dateOfBirth.createFocusChangeModifier())
            .fillMaxWidth(),
          // textStyle = MaterialTheme.typography.body2,
          errorHint = age.getError(),
          keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Number
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              focusRequester.requestFocus()
            }
          )
        )
      }



      BaseDetailsCard(
        stringResource(R.string.outcomes_card_title),
        Modifier.padding(16.dp)
      ) {
      }
    }
  }
}

@Preview
@Composable
fun PatientFormPreview() {
  CradleTrialAppTheme {
    Scaffold {
      PatientForm()
    }
  }
}

class InitialsState : TextFieldState(
  validator = { it.length in 1..MAX_INITIALS_LENGTH },
  errorFor = { ctx, _, -> ctx.getString(R.string.patient_registration_initials_error) }
)

class NoFutureDateState : TextFieldState(
  validator = { possibleDate ->
    run {
      val formDate = try {
        possibleDate.toFormDateOrThrow()
      } catch (e: NumberFormatException) {
        return@run false
      }

      formDate <= FormDate.today()
    }
  },
  errorFor = { ctx, date ->
    if (date.toFormDateOrNull() != null) {
      ctx.getString(R.string.form_date_cannot_be_in_future_error)
    } else {
      ctx.getString(R.string.form_date_required_error)
    }
  }
)

class LimitedAgeDateState(val limit: LongRange) : TextFieldState(
  validator = { possibleDate ->
    run {
      val formDate = try {
        possibleDate.toFormDateOrThrow()
      } catch (e: NumberFormatException) {
        return@run false
      }
      Log.d("MainActivity", "patient form DOB validation: age = ${formDate.getAgeInYearsFromNow()}")

      formDate.getAgeInYearsFromNow() in limit
    }
  },
  errorFor = { ctx, date ->
    if (date.toFormDateOrNull() != null) {
      ctx.getString(R.string.age_must_be_in_range_d_and_d, limit.first, limit.last)
    } else {
      ctx.getString(R.string.form_date_required_error)
    }
  }
)

class LimitedAgeIntState(val limit: LongRange) : TextFieldState(
  validator = { possibleAge ->
    run {
      val age = possibleAge.toIntOrNull() ?: return@run false
      age in limit
    }
  },
  errorFor = { ctx, _ -> ctx.getString(R.string.age_must_be_in_range_d_and_d, limit.first, limit.last) }
)

@Parcelize
@Immutable
data class EclampsiaBuilder(
  val date: FormDate? = null,
  val place: EnumSelection.IdOnly? = null,
) : Parcelable

class EnumIdOnlyState(
  private val enum: ServerEnum
) : FieldState<EnumSelection.IdOnly?>(
  validator = { selection -> selection?.let { enum.getValueFromId(it.selectionId) } != null  },
  errorFor = { ctx, _, -> ctx.getString(R.string.server_enum_unknown_selection_error) },
) {
  override var stateValue: EnumSelection.IdOnly? by mutableStateOf(null)
}

class EnumWithOtherState(
  private val enum: ServerEnum,
  val otherSelection: ServerEnum.Entry? = enum.sortedValues.find { it.name == "Other" }
) : FieldState<EnumSelection.WithOther?>(
  validator = { selection ->
    val entry = selection?.let { enum.getValueFromId(it.selectionId) }
    if (entry == null) {
      false
    } else {
      !(entry == otherSelection && selection.otherString.isNullOrBlank())
    }
  },
  errorFor = { ctx, selection, ->
    val entry = selection?.let { enum.getValueFromId(it.selectionId) }
    if (entry == otherSelection && selection?.otherString.isNullOrBlank()) {
      ctx.getString(R.string.server_enum_unknown_selection_error)
    } else {
      ctx.getString(R.string.server_enum_unknown_selection_error)
    }
  },
) {


  override var stateValue: EnumSelection.WithOther? by mutableStateOf(null)
}


