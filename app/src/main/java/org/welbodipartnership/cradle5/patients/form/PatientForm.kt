package org.welbodipartnership.cradle5.patients.form

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
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.patients.details.BaseDetailsCard
import org.welbodipartnership.cradle5.patients.details.CategoryHeader
import org.welbodipartnership.cradle5.ui.composables.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.composables.forms.BooleanRadioButtonRow
import org.welbodipartnership.cradle5.ui.composables.forms.DateOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.EnumDropdownMenu
import org.welbodipartnership.cradle5.ui.composables.forms.EnumDropdownMenuWithOther
import org.welbodipartnership.cradle5.ui.composables.forms.FieldState
import org.welbodipartnership.cradle5.ui.composables.forms.TextFieldState
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.date.FormDate
import org.welbodipartnership.cradle5.util.date.toFormDateOrNull
import org.welbodipartnership.cradle5.util.date.toFormDateOrThrow

private val MAX_INITIALS_LENGTH = 5

private val DOB_RANGE = 10L..60L

private val VALID_LENGTH_OF_ITU_HDU_STAY = 1L..100L

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
fun PatientForm(
  serverEnumCollection: ServerEnumCollection,
  viewModel: PatientFormViewModel = hiltViewModel()
) {
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

    val textFieldToTextFieldHeight = 8.dp
    val categoryToCategorySpacerHeight = 16.dp

    Column(
      Modifier
        .padding(padding)
        .verticalScroll(scrollState)
    ) {
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

        Spacer(Modifier.height(textFieldToTextFieldHeight))

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

        Spacer(Modifier.height(textFieldToTextFieldHeight))

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

        Spacer(Modifier.height(textFieldToTextFieldHeight))

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

      var isEclampsiaEnabled: Boolean? by rememberSaveable { mutableStateOf(null) }
      val dateState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> NoFutureDateState().also { it.stateValue = savedText } }
        )
      ) { NoFutureDateState() }
      val placeOfFirstFitState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedValue ->
            EnumIdOnlyState(serverEnumCollection[DropdownType.Place]).also { it.stateValue = savedValue }
          }
        )
      ) {
        EnumIdOnlyState(serverEnumCollection[DropdownType.Place])
      }

      var isHysterectomyEnabled: Boolean? by rememberSaveable { mutableStateOf(null) }
      val hysterectomyDateState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> NoFutureDateState().also { it.stateValue = savedText } }
        )
      ) { NoFutureDateState() }
      val hysterectomyCauseState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedValue ->
            EnumWithOtherState(
              serverEnumCollection[DropdownType.CauseOfHysterectomy],
              isMandatory = false
            ).also { it.stateValue = savedValue }
          }
        )
      ) {
        EnumWithOtherState(
          serverEnumCollection[DropdownType.CauseOfHysterectomy],
          isMandatory = false
        )
      }
      var hysterectomyExtraInfo: String? by rememberSaveable { mutableStateOf("") }

      var isHduItuAdmissionEnabled: Boolean? by rememberSaveable { mutableStateOf(null) }
      val hduItuAdmissionDateState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> NoFutureDateState().also { it.stateValue = savedText } }
        )
      ) { NoFutureDateState() }
      val hduItuCauseState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedValue ->
            EnumWithOtherState(
              serverEnumCollection[DropdownType.CauseForHduOrItuAdmission],
              isMandatory = true
            ).also { it.stateValue = savedValue }
          }
        )
      ) {
        EnumWithOtherState(
          serverEnumCollection[DropdownType.CauseForHduOrItuAdmission],
          isMandatory = true
        )
      }
      val hduItuLengthDays = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedValue ->
            LimitedHduItuState(VALID_LENGTH_OF_ITU_HDU_STAY).also { it.stateValue = savedValue }
          }
        )
      ) {
        LimitedHduItuState(VALID_LENGTH_OF_ITU_HDU_STAY)
      }

      var isMaternalDeathEnabled: Boolean? by rememberSaveable { mutableStateOf(null) }
      val maternalDeathDateState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> NoFutureDateState().also { it.stateValue = savedText } }
        )
      ) { NoFutureDateState() }
      val maternalDeathUnderlyingCauseState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedValue ->
            EnumWithOtherState(
              serverEnumCollection[DropdownType.UnderlyingCauseOfDeath],
              isMandatory = false
            ).also { it.stateValue = savedValue }
          }
        )
      ) {
        EnumWithOtherState(
          serverEnumCollection[DropdownType.UnderlyingCauseOfDeath],
          isMandatory = false
        )
      }
      val maternalDeathPlaceState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedValue ->
            EnumIdOnlyState(serverEnumCollection[DropdownType.Place]).also { it.stateValue = savedValue }
          }
        )
      ) {
        EnumIdOnlyState(serverEnumCollection[DropdownType.Place])
      }

      var isSurgicalManagementEnabled: Boolean? by rememberSaveable { mutableStateOf(null) }
      val surgicalManagementDateState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> NoFutureDateState().also { it.stateValue = savedText } }
        )
      ) { NoFutureDateState() }
      val surgicalManagementTypeState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedValue ->
            EnumWithOtherState(
              serverEnumCollection[DropdownType.TypeOfSurgicalManagement],
              isMandatory = false
            ).also { it.stateValue = savedValue }
          }
        )
      ) {
        EnumWithOtherState(
          serverEnumCollection[DropdownType.TypeOfSurgicalManagement],
          isMandatory = false
        )
      }

      var isPerinatalFormEnabled: Boolean? by rememberSaveable { mutableStateOf(null) }
      val perinatalDeathDateState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedText -> NoFutureDateState().also { it.stateValue = savedText } }
        )
      ) { NoFutureDateState() }
      val perinatalDeathOutcomeState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedValue ->
            EnumIdOnlyState(serverEnumCollection[DropdownType.PerinatalOutcome]).also { it.stateValue = savedValue }
          }
        )
      ) {
        EnumIdOnlyState(serverEnumCollection[DropdownType.PerinatalOutcome])
      }
      val perinatalDeathMaternalFactorsState = rememberSaveable(
        saver = Saver(
          save = { it.stateValue },
          restore = { savedValue ->
            EnumWithOtherState(
              serverEnumCollection[DropdownType.MaternalFactorsRelatedToPerinatalLoss],
              isMandatory = false
            ).also { it.stateValue = savedValue }
          }
        )
      ) {
        EnumWithOtherState(
          serverEnumCollection[DropdownType.MaternalFactorsRelatedToPerinatalLoss],
          isMandatory = false
        )
      }

      BaseDetailsCard(
        stringResource(R.string.outcomes_card_title),
        Modifier.padding(16.dp)
      ) {
        CategoryHeader(stringResource(R.string.outcomes_eclampsia_label))

        EclampsiaForm(
          isFormEnabled = isEclampsiaEnabled,
          onFormEnabledStateChange = {
            isEclampsiaEnabled = it
            dateState.reset()
            placeOfFirstFitState.reset()
          },
          dateState = dateState,
          placeOfFirstFitState = placeOfFirstFitState,
          serverEnumCollection = serverEnumCollection
        )

        Spacer(Modifier.height(categoryToCategorySpacerHeight))

        CategoryHeader(stringResource(R.string.outcomes_hysterectomy_label))

        HysterectomyForm(
          isFormEnabled = isHysterectomyEnabled,
          onFormEnabledChange = {
            isHysterectomyEnabled = it
            hysterectomyDateState.reset()
            hysterectomyCauseState.reset()
            hysterectomyExtraInfo = null
          },
          dateState = hysterectomyDateState,
          causeState = hysterectomyCauseState,
          additionalInfo = hysterectomyExtraInfo ?: "",
          onAdditionInfoChanged = { hysterectomyExtraInfo = it },
          serverEnumCollection,
        )

        Spacer(Modifier.height(categoryToCategorySpacerHeight))

        CategoryHeader(stringResource(R.string.outcomes_admission_to_hdu_or_itu_label))

        AdmittedToHduItuForm(
          isFormEnabled = isHduItuAdmissionEnabled,
          onFormEnabledChange = {
            isHduItuAdmissionEnabled = it
            hduItuAdmissionDateState.reset()
            hduItuCauseState.reset()
            hduItuLengthDays.reset()
          },
          dateState = hduItuAdmissionDateState,
          causeState = hduItuCauseState,
          lengthOfStayInDaysState = hduItuLengthDays,
          serverEnumCollection,
        )

        Spacer(Modifier.height(categoryToCategorySpacerHeight))

        CategoryHeader(stringResource(R.string.outcomes_maternal_death_label))

        MaternalDeathForm(
          isFormEnabled = isMaternalDeathEnabled,
          onFormEnabledChange = {
            isMaternalDeathEnabled = it
            maternalDeathDateState.reset()
            maternalDeathUnderlyingCauseState.reset()
            maternalDeathPlaceState.reset()
          },
          dateState = maternalDeathDateState,
          underlyingCauseState = maternalDeathUnderlyingCauseState,
          placeOfDeathState = maternalDeathPlaceState,
          serverEnumCollection = serverEnumCollection
        )

        Spacer(Modifier.height(categoryToCategorySpacerHeight))

        CategoryHeader(stringResource(R.string.outcomes_surgical_management_label))

        SurgicalManagementForm(
          isFormEnabled = isSurgicalManagementEnabled,
          onFormEnabledChange = {
            isSurgicalManagementEnabled = it
            surgicalManagementDateState.reset()
            surgicalManagementTypeState.reset()
          },
          dateState = surgicalManagementDateState,
          surgicalManagementTypeState = surgicalManagementTypeState,
          serverEnumCollection = serverEnumCollection,
        )

        Spacer(Modifier.height(categoryToCategorySpacerHeight))

        CategoryHeader(stringResource(R.string.outcomes_perinatal_death_label))

        PerinatalDeathForm(
          isFormEnabled = isPerinatalFormEnabled,
          onFormEnabledChange = {
            isPerinatalFormEnabled = it
            perinatalDeathDateState.reset()
            perinatalDeathOutcomeState.reset()
            perinatalDeathMaternalFactorsState.reset()
          },
          dateState = perinatalDeathDateState,
          outcomeState = perinatalDeathOutcomeState,
          maternalFactorsState = perinatalDeathMaternalFactorsState,
          serverEnumCollection = serverEnumCollection
        )
      }

      Card(
        elevation = 4.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(16.dp)
      ) {
        Button(onClick = { /*TODO*/ }) {
        }
      }
    }
  }
}

@Composable
fun EclampsiaForm(
  isFormEnabled: Boolean?,
  onFormEnabledStateChange: (newState: Boolean) -> Unit,
  dateState: NoFutureDateState,
  placeOfFirstFitState: EnumIdOnlyState,
  serverEnumCollection: ServerEnumCollection,
  modifier: Modifier = Modifier
) {
  BooleanRadioButtonRow(
    isTrue = isFormEnabled,
    onBooleanChange = onFormEnabledStateChange,
  )

  val serverEnum = requireNotNull(serverEnumCollection[DropdownType.Place]) {
    "missing Place lookup values from the server"
  }
  val enum: ServerEnum.Entry? = placeOfFirstFitState.stateValue?.let {
    serverEnum.getValueFromId(it.selectionId)
  }

  Column(modifier) {
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { },
      label = {
        Text(
          text = stringResource(id = R.string.form_date_label).withRequiredStar(),
        )
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier.fillMaxWidth(),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenu(
      currentSelection = enum,
      onSelect = {
        placeOfFirstFitState.stateValue = it?.let { EnumSelection.IdOnly(it.id) }
      },
      serverEnum = serverEnum,
      label = { Text(stringResource(R.string.place_of_first_eclamptic_fit_label)) },
      enabled = isFormEnabled == true,
      textModifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun HysterectomyForm(
  isFormEnabled: Boolean?,
  onFormEnabledChange: (newState: Boolean) -> Unit,
  dateState: NoFutureDateState,
  causeState: EnumWithOtherState,
  additionalInfo: String,
  onAdditionInfoChanged: (String) -> Unit,
  serverEnumCollection: ServerEnumCollection,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  BooleanRadioButtonRow(
    isTrue = isFormEnabled,
    onBooleanChange = onFormEnabledChange,
  )

  val serverEnum = requireNotNull(serverEnumCollection[DropdownType.CauseOfHysterectomy]) {
    "missing Hysterectomy Cause lookup values from the server"
  }

  Column(modifier) {
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { },
      label = {
        Text(
          text = stringResource(id = R.string.form_date_label).withRequiredStar(),
        )
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier.fillMaxWidth(),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenuWithOther(
      currentSelection = causeState.stateValue,
      onSelect = { causeState.stateValue = it },
      serverEnum = serverEnum,
      label = { Text(stringResource(R.string.hysterectomy_cause_label)) },
      enabled = isFormEnabled == true,
      dropdownTextModifier = Modifier.fillMaxWidth(),
      otherTextModifier = Modifier
        .fillMaxWidth()
        .then(causeState.createFocusChangeModifier()),
      errorHint = causeState.getError()
    )

    OutlinedTextField(
      value = additionalInfo,
      onValueChange = onAdditionInfoChanged,
      modifier = textFieldModifier
        .fillMaxWidth(),
      label = { Text(stringResource(R.string.hysterectomy_additional_info_label)) },
      enabled = isFormEnabled == true,
    )
  }
}

@Composable
fun AdmittedToHduItuForm(
  isFormEnabled: Boolean?,
  onFormEnabledChange: (newState: Boolean) -> Unit,
  dateState: NoFutureDateState,
  causeState: EnumWithOtherState,
  lengthOfStayInDaysState: LimitedHduItuState,
  serverEnumCollection: ServerEnumCollection,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  BooleanRadioButtonRow(
    isTrue = isFormEnabled,
    onBooleanChange = onFormEnabledChange,
  )

  val serverEnum = requireNotNull(serverEnumCollection[DropdownType.CauseForHduOrItuAdmission]) {
    "missing Hysterectomy Cause lookup values from the server"
  }

  Column(modifier) {
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { },
      label = {
        Text(
          text = stringResource(id = R.string.form_date_label).withRequiredStar(),
        )
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier.fillMaxWidth(),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenuWithOther(
      currentSelection = causeState.stateValue,
      onSelect = { causeState.stateValue = it },
      serverEnum = serverEnum,
      label = { Text(stringResource(R.string.hdu_or_idu_admission_cause_label).withRequiredStar()) },
      enabled = isFormEnabled == true,
      dropdownTextModifier = Modifier.fillMaxWidth(),
      showErrorHintOnOtherField = causeState.stateValue != null,
      otherTextModifier = Modifier
        .fillMaxWidth()
        .then(causeState.createFocusChangeModifier()),
      errorHint = causeState.getError()
    )

    OutlinedTextFieldWithErrorHint(
      value = lengthOfStayInDaysState.stateValue,
      onValueChange = { lengthOfStayInDaysState.stateValue = it },
      label = {
        Text(stringResource(id = R.string.hdu_or_idu_admission_length_stay_days_if_known_label))
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = lengthOfStayInDaysState
        .createFocusChangeModifier()
        .fillMaxWidth(),
      errorHint = lengthOfStayInDaysState.getError(),
      keyboardOptions = KeyboardOptions.Default.copy(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Number
      ),
    )
  }
}

@Composable
fun MaternalDeathForm(
  isFormEnabled: Boolean?,
  onFormEnabledChange: (newState: Boolean) -> Unit,
  dateState: NoFutureDateState,
  underlyingCauseState: EnumWithOtherState,
  placeOfDeathState: EnumIdOnlyState,
  serverEnumCollection: ServerEnumCollection,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  BooleanRadioButtonRow(
    isTrue = isFormEnabled,
    onBooleanChange = onFormEnabledChange,
  )

  val placeOfDeathEnum = requireNotNull(serverEnumCollection[DropdownType.Place]) {
    "missing Maternal Death Underlying Cause lookup values from the server"
  }
  val placeOfDeathEntry: ServerEnum.Entry? = placeOfDeathState.stateValue?.let {
    placeOfDeathEnum.getValueFromId(it.selectionId)
  }

  val underlyingCauseEnum = requireNotNull(serverEnumCollection[DropdownType.UnderlyingCauseOfDeath]) {
    "missing Maternal Death Place of Death lookup values from the server"
  }

  Column(modifier) {
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { },
      label = { Text(stringResource(R.string.form_date_label).withRequiredStar()) },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier.fillMaxWidth(),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenuWithOther(
      currentSelection = underlyingCauseState.stateValue,
      onSelect = { underlyingCauseState.stateValue = it },
      serverEnum = underlyingCauseEnum,
      label = { Text(stringResource(R.string.maternal_death_underlying_cause_label)) },
      enabled = isFormEnabled == true,
      dropdownTextModifier = Modifier.fillMaxWidth(),
      showErrorHintOnOtherField = underlyingCauseState.stateValue != null,
      otherTextModifier = Modifier
        .fillMaxWidth()
        .then(underlyingCauseState.createFocusChangeModifier()),
      errorHint = underlyingCauseState.getError()
    )

    EnumDropdownMenu(
      currentSelection = placeOfDeathEntry,
      onSelect = {
        placeOfDeathState.stateValue = it?.let { EnumSelection.IdOnly(it.id) }
      },
      serverEnum = placeOfDeathEnum,
      label = { Text(stringResource(R.string.maternal_death_place_label)) },
      enabled = isFormEnabled == true,
      textModifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun SurgicalManagementForm(
  isFormEnabled: Boolean?,
  onFormEnabledChange: (newState: Boolean) -> Unit,
  dateState: NoFutureDateState,
  surgicalManagementTypeState: EnumWithOtherState,
  serverEnumCollection: ServerEnumCollection,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  BooleanRadioButtonRow(
    isTrue = isFormEnabled,
    onBooleanChange = onFormEnabledChange,
  )

  val serverEnum = requireNotNull(serverEnumCollection[DropdownType.TypeOfSurgicalManagement]) {
    "missing Type of Surgical Management Of Postpartum ... lookup values from the server"
  }

  Column(modifier) {
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { },
      label = { Text(stringResource(R.string.form_date_label).withRequiredStar()) },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier.fillMaxWidth(),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenuWithOther(
      currentSelection = surgicalManagementTypeState.stateValue,
      onSelect = { surgicalManagementTypeState.stateValue = it },
      serverEnum = serverEnum,
      label = { Text(stringResource(R.string.surgical_management_type_label)) },
      enabled = isFormEnabled == true,
      dropdownTextModifier = Modifier.fillMaxWidth(),
      showErrorHintOnOtherField = surgicalManagementTypeState.stateValue != null,
      otherTextModifier = Modifier
        .fillMaxWidth()
        .then(surgicalManagementTypeState.createFocusChangeModifier()),
      errorHint = surgicalManagementTypeState.getError()
    )
  }
}

@Composable
fun PerinatalDeathForm(
  isFormEnabled: Boolean?,
  onFormEnabledChange: (newState: Boolean) -> Unit,
  dateState: NoFutureDateState,
  outcomeState: EnumIdOnlyState,
  maternalFactorsState: EnumWithOtherState,
  serverEnumCollection: ServerEnumCollection,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  BooleanRadioButtonRow(
    isTrue = isFormEnabled,
    onBooleanChange = onFormEnabledChange,
  )

  val perinatalOutcomeEnum = requireNotNull(serverEnumCollection[DropdownType.PerinatalOutcome]) {
    "missing Perinatal Death Outcome lookup values from the server"
  }
  val perinatalOutcomeEntry: ServerEnum.Entry? = outcomeState.stateValue?.let {
    perinatalOutcomeEnum.getValueFromId(it.selectionId)
  }
  val perinatalRelatedMaternalFactorsEnum = requireNotNull(serverEnumCollection[DropdownType.MaternalFactorsRelatedToPerinatalLoss]) {
    "missing Perinatal Related Maternal factors lookup values from the server"
  }

  Column(modifier) {
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { },
      label = { Text(stringResource(R.string.form_date_label).withRequiredStar()) },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier.fillMaxWidth(),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenu(
      currentSelection = perinatalOutcomeEntry,
      onSelect = {
        outcomeState.stateValue = it?.let { EnumSelection.IdOnly(it.id) }
      },
      serverEnum = perinatalOutcomeEnum,
      label = { Text(stringResource(R.string.perinatal_death_outcome_label)) },
      enabled = isFormEnabled == true,
      textModifier = Modifier.fillMaxWidth()
    )

    EnumDropdownMenuWithOther(
      currentSelection = maternalFactorsState.stateValue,
      onSelect = { maternalFactorsState.stateValue = it },
      serverEnum = perinatalRelatedMaternalFactorsEnum,
      label = { Text(stringResource(R.string.perinatal_death_related_maternal_factors_label)) },
      enabled = isFormEnabled == true,
      dropdownTextModifier = Modifier.fillMaxWidth(),
      showErrorHintOnOtherField = maternalFactorsState.stateValue != null,
      otherTextModifier = Modifier
        .fillMaxWidth()
        .then(maternalFactorsState.createFocusChangeModifier()),
      errorHint = maternalFactorsState.getError()
    )
  }
}

@Preview
@Composable
fun EclampsiaFormPreview() {
  CradleTrialAppTheme {
    Scaffold {
      val defaultEnums = ServerEnumCollection.defaultInstance
      EclampsiaForm(
        isFormEnabled = false,
        onFormEnabledStateChange = {},
        dateState = NoFutureDateState(),
        placeOfFirstFitState = EnumIdOnlyState(
          defaultEnums[DropdownType.Place]
        ),
        serverEnumCollection = defaultEnums,
      )
    }
  }
}

@Preview
@Composable
fun PatientFormPreview() {
  CradleTrialAppTheme {
    Scaffold {
      PatientForm(ServerEnumCollection.defaultInstance)
    }
  }
}

class InitialsState(backingState: MutableState<String> = mutableStateOf("")) : TextFieldState(
  validator = { it.length in 1..MAX_INITIALS_LENGTH },
  errorFor = { ctx, _, -> ctx.getString(R.string.patient_registration_initials_error) },
  backingState = backingState
) {
  override val showErrorOnInput: Boolean = false
}

class NoFutureDateState(
  backingState: MutableState<String> = mutableStateOf("")
) : TextFieldState(
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
  },
  backingState = backingState
)

class LimitedAgeDateState(
  val limit: LongRange,
  backingState: MutableState<String> = mutableStateOf("")
) : TextFieldState(
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
  },
  backingState = backingState
)

class LimitedAgeIntState(
  val limit: LongRange,
  backingState: MutableState<String> = mutableStateOf("")
) : TextFieldState(
  validator = { possibleAge ->
    run {
      val age = possibleAge.toIntOrNull() ?: return@run false
      age in limit
    }
  },
  errorFor = { ctx, _ -> ctx.getString(R.string.age_must_be_in_range_d_and_d, limit.first, limit.last) },
  backingState = backingState
)

class LimitedHduItuState(
  val limit: LongRange,
  backingState: MutableState<String> = mutableStateOf("")
) : TextFieldState(
  validator = { stay ->
    run {
      if (stay.isBlank()) return@run true
      val stayAsInt = stay.toIntOrNull() ?: return@run false
      stayAsInt in limit
    }
  },
  errorFor = { ctx, _ ->
    ctx.getString(
      R.string.length_of_stay_in_itu_hdu_must_be_in_range_d_and_d_days,
      limit.first,
      limit.last
    )
  },
  backingState = backingState
)

class EnumIdOnlyState(
  private val enum: ServerEnum?,
  backingState: MutableState<EnumSelection.IdOnly?> = mutableStateOf(null)
) : FieldState<EnumSelection.IdOnly?>(
  validator = { selection -> selection?.let { enum?.getValueFromId(it.selectionId) } != null },
  errorFor = { ctx, _, -> ctx.getString(R.string.server_enum_unknown_selection_error) },
  initialValue = null,
  backingState = backingState
) {
  override val showErrorOnInput: Boolean = true
  override var stateValue: EnumSelection.IdOnly? by mutableStateOf(null)
}

class EnumWithOtherState(
  private val enum: ServerEnum?,
  private val isMandatory: Boolean,
  val otherSelection: ServerEnum.Entry? = enum?.validSortedValues?.find { it.name == "Other" },
  backingState: MutableState<EnumSelection.WithOther?> = mutableStateOf(null)
) : FieldState<EnumSelection.WithOther?>(
  validator = { selection ->
    val entry = selection?.let { enum?.getValueFromId(it.selectionId) }
    if (entry == null && isMandatory) {
      false
    } else {
      !(entry == otherSelection && selection?.otherString.isNullOrBlank())
    }
  },
  errorFor = { ctx, selection, ->
    val entry = selection?.let { enum?.getValueFromId(it.selectionId) }
    if (entry == otherSelection && selection?.otherString.isNullOrBlank()) {
      ctx.getString(R.string.server_enum_other_selection_missing_error)
    } else if (entry == null && isMandatory) {
      ctx.getString(R.string.server_enum_selection_required_error)
    } else {
      ctx.getString(R.string.server_enum_unknown_selection_error)
    }
  },
  initialValue = null,
  backingState
) {
  override val showErrorOnInput: Boolean = true
  override var stateValue: EnumSelection.WithOther?
    get() = backingState.value
    set(value) {
      backingState.value = value
      if (isMandatory && value != null) {
        enableShowErrors(force = true)
      }
    }
}