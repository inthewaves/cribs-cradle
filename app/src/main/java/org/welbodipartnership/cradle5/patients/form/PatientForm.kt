package org.welbodipartnership.cradle5.patients.form

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.ui.composables.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.composables.forms.DateOutlinedTextField
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
    val initials = remember { InitialsState() }
    val more = remember { InitialsState() }
    val presentationDate = remember { NoFutureDateState() }
    val dateOfBirth = remember { LimitedAgeDateState(DOB_RANGE) }

    Column(
      Modifier.padding(padding)
        .padding(horizontal = 20.dp)
        .supportWideScreen()
    ) {
      OutlinedTextFieldWithErrorHint(
        value = initials.text,
        onValueChange = {
          // TODO: Hard limit text
          initials.text = it
        },
        label = {
          Text(
            text = stringResource(id = R.string.patient_registration_initials_label),
            // style = MaterialTheme.typography.body2
          )
        },
        textFieldModifier = Modifier
          .fillMaxWidth()
          .then(initials.createFocusChangeModifier()),
        // textStyle = MaterialTheme.typography.body2,
        errorHint = initials.getError(),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
          onDone = {
            focusRequester.requestFocus()
          }
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      DateOutlinedTextField(
        date = presentationDate.text.toFormDateOrNull(),
        onDatePicked = {
          presentationDate.text = it.toString()
        },
        onPickerClose = { presentationDate.enableShowErrors(force = true) },
        label = {
          Text(
            text = stringResource(id = R.string.patient_registration_presentation_date_label),
          )
        },
        textFieldModifier = Modifier
          .fillMaxWidth()
          .then(more.createFocusChangeModifier()),
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
        date = dateOfBirth.text.toFormDateOrNull(),
        onDatePicked = {
          dateOfBirth.text = it.toString()
        },
        onPickerClose = { dateOfBirth.enableShowErrors(force = true) },
        label = {
          Text(
            text = stringResource(id = R.string.patient_registration_date_of_birth_label),
          )
        },
        textFieldModifier = Modifier
          .fillMaxWidth()
          .then(more.createFocusChangeModifier()),
        // textStyle = MaterialTheme.typography.body2,
        errorHint = dateOfBirth.getError(),
        keyboardOptions = KeyboardOptions.Default,
        keyboardActions = KeyboardActions(
          onDone = {
            // onImeAction()
          }
        )
      )
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
  errorFor = { ctx, _ -> ctx.getString(R.string.form_date_cannot_be_in_future_error) }
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
  errorFor = { ctx, _ -> ctx.getString(R.string.age_must_be_in_range_d_and_d, limit.first, limit.last) }
)
