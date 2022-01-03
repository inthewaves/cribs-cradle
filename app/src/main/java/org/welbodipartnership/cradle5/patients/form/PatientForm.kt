package org.welbodipartnership.cradle5.patients.form

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
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
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.patients.details.BaseDetailsCard
import org.welbodipartnership.cradle5.patients.details.CategoryHeader
import org.welbodipartnership.cradle5.ui.composables.forms.BooleanRadioButtonRow
import org.welbodipartnership.cradle5.ui.composables.forms.DateOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.EnumDropdownMenuIdOnly
import org.welbodipartnership.cradle5.ui.composables.forms.EnumDropdownMenuWithOther
import org.welbodipartnership.cradle5.ui.composables.forms.FieldState
import org.welbodipartnership.cradle5.ui.composables.forms.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.composables.forms.TextFieldState
import org.welbodipartnership.cradle5.ui.composables.forms.darkerDisabledOutlinedTextFieldColors
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate
import org.welbodipartnership.cradle5.util.datetime.toFormDateOrNull
import org.welbodipartnership.cradle5.util.datetime.toFormDateOrThrow

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
fun RequiredText(text: String, required: Boolean = true) {
  if (required) {
    Text(text.withRequiredStar())
  } else {
    Text(text)
  }
}

@Composable
fun PatientForm(
  serverEnumCollection: ServerEnumCollection,
  onNavigateBack: () -> Unit,
  onNavigateToPatient: (patientPrimaryKey: Long) -> Unit,
  viewModel: PatientFormViewModel = hiltViewModel()
) {
  val formState = viewModel.formState.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  val resources = LocalContext.current.resources
  LaunchedEffect(formState.value) {
    when (val state = formState.value) {
      is PatientFormViewModel.FormState.FailedValidation -> {
        val totalErrors = state.errorsBySectionStringId.asSequence()
          .flatMap { it.value }
          .count()
        snackbarHostState.showSnackbar(
          resources.getQuantityString(
            R.plurals.patient_form_snackbar_failed_to_save_there_are_d_errors,
            totalErrors,
            totalErrors,
          )
        )
      }
      else -> {}
    }
  }

  var showUnsavedChangesDialog by remember { mutableStateOf(false) }
  BackHandler { showUnsavedChangesDialog = true }
  if (showUnsavedChangesDialog) {
    AlertDialog(
      onDismissRequest = { showUnsavedChangesDialog = false },
      title = {
        if (formState.value.isForPatientEdit) {
          Text(stringResource(id = R.string.discard_unsaved_changes_dialog_title))
        } else {
          Text(stringResource(id = R.string.discard_unsaved_changes_dialog_title_new_entry))
        }
      },
      confirmButton = {
        TextButton(onClick = {
          showUnsavedChangesDialog = false
          onNavigateBack()
        }) { Text(stringResource(id = R.string.discard)) }
      },
      dismissButton = {
        TextButton(onClick = { showUnsavedChangesDialog = false }) {
          Text(stringResource(id = R.string.cancel))
        }
      }
    )
  }

  Scaffold(
    scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
    topBar = {
      TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        contentPadding = rememberInsetsPaddingValues(
          insets = LocalWindowInsets.current.systemBars,
          applyBottom = false,
        ),
        modifier = Modifier.fillMaxWidth(),
        title = {
          if (viewModel.isExistingPatientEdit) {
            Column {
              Text(stringResource(R.string.edit_patient_title))
              formState.value.let { state ->
                if (
                  state is PatientFormViewModel.FormState.Ready &&
                  state.existingInfo != null
                ) {
                  Text(
                    state.existingInfo.patient.initials,
                    style = MaterialTheme.typography.subtitle2
                  )
                }
              }
            }
          } else {
            Text(stringResource(R.string.new_patient_title))
          }
        },
        navigationIcon = {
          val backPressedDispatcher = requireNotNull(
            LocalOnBackPressedDispatcherOwner.current!!, { "failed to get a back pressed dispatcher" }
          )
            .onBackPressedDispatcher
          IconButton(onClick = backPressedDispatcher::onBackPressed) {
            Icon(
              imageVector = Icons.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back_button)
            )
          }
        },
      )
    },

  ) { padding ->
    val focusRequester = remember { FocusRequester() }

    val textFieldToTextFieldHeight = 8.dp
    val categoryToCategorySpacerHeight = 16.dp

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LazyColumn(
      contentPadding = padding,
      modifier = Modifier.navigationBarsWithImePadding()
    ) {
      item {
        BaseDetailsCard(
          stringResource(R.string.patient_registration_card_title),
          Modifier.padding(16.dp)
        ) {
          val patientFields = viewModel.formFields.patientFields
          OutlinedTextFieldWithErrorHint(
            value = patientFields.initials.stateValue.uppercase(),
            onValueChange = {
              // TODO: Hard limit text
              patientFields.initials.stateValue = it.uppercase()
            },
            label = { RequiredText(stringResource(R.string.patient_registration_initials_label)) },
            textFieldModifier = Modifier
              .then(patientFields.initials.createFocusChangeModifier())
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            // textStyle = MaterialTheme.typography.body2,
            errorHint = patientFields.initials.getError(),
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
            date = patientFields.presentationDate.stateValue.toFormDateOrNull(),
            onDatePicked = {
              patientFields.presentationDate.stateValue = it.toString()
            },
            onPickerClose = { patientFields.presentationDate.enableShowErrors(force = true) },
            label = {
              RequiredText(stringResource(id = R.string.patient_registration_presentation_date_label))
            },
            modifier = Modifier.fillMaxWidth(),
            textFieldModifier = patientFields.presentationDate
              .createFocusChangeModifier()
              .fillMaxWidth(),
            // textStyle = MaterialTheme.typography.body2,
            errorHint = patientFields.presentationDate.getError(),
            keyboardOptions = KeyboardOptions.Default,
            keyboardActions = KeyboardActions(
              onDone = {
                // onImeAction()
              }
            )
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          DateOutlinedTextField(
            date = patientFields.dateOfBirth.stateValue.toFormDateOrNull(),
            onDatePicked = {
              patientFields.dateOfBirth.stateValue = it.toString()
              patientFields.age.stateValue = it.getAgeInYearsFromNow().toString()
            },
            onPickerClose = {
              patientFields.dateOfBirth.enableShowErrors(force = true)
              patientFields.age.enableShowErrors(force = true)
            },
            label = {
              RequiredText(stringResource(R.string.patient_registration_date_of_birth_label))
            },
            modifier = Modifier.fillMaxWidth(),
            textFieldModifier = patientFields.dateOfBirth
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
            value = patientFields.age.stateValue,
            onValueChange = { newAge ->
              patientFields.age.stateValue = newAge
              newAge.toIntOrNull()?.let {
                patientFields.dateOfBirth.stateValue = FormDate.fromAge(it).toString()
              }
            },
            label = {
              RequiredText(stringResource(id = R.string.patient_registration_age_label))
            },
            modifier = Modifier.fillMaxWidth(),
            textFieldModifier = patientFields.age
              .createFocusChangeModifier()
              .then(patientFields.dateOfBirth.createFocusChangeModifier())
              .fillMaxWidth(),
            // textStyle = MaterialTheme.typography.body2,
            errorHint = patientFields.age.getError(),
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
      }

      item {
        BaseDetailsCard(
          stringResource(R.string.outcomes_card_title),
          Modifier.padding(16.dp)
        ) {
          CategoryHeader(stringResource(R.string.outcomes_eclampsia_label))

          val eclampsia = viewModel.formFields.eclampsia
          EclampsiaForm(
            isFormEnabled = eclampsia.isEnabled.value,
            onFormEnabledStateChange = {
              eclampsia.isEnabled.value = it
              if (!it) eclampsia.reset()
            },
            dateState = eclampsia.date,
            placeOfFirstFitState = eclampsia.placeOfFirstFit,
            serverEnumCollection = serverEnumCollection,
            textFieldModifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
          )

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_hysterectomy_label))

          val hysterectomy = viewModel.formFields.hysterectomy
          val scope = rememberCoroutineScope()
          HysterectomyForm(
            isFormEnabled = hysterectomy.isEnabled.value,
            onFormEnabledChange = {
              hysterectomy.isEnabled.value = it
              if (!it) hysterectomy.reset()
            },
            dateState = hysterectomy.date,
            causeState = hysterectomy.cause,
            additionalInfo = hysterectomy.additionalInfo.value ?: "",
            onAdditionInfoChanged = { hysterectomy.additionalInfo.value = it },
            textFieldModifier = Modifier
              .bringIntoViewRequester(bringIntoViewRequester)
              /*
              .onFocusEvent {
                if (it.isFocused) {
                  scope.launch {
                    delay(150L)
                    bringIntoViewRequester.bringIntoView()
                  }
                }
              }

               */
          )

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_admission_to_hdu_or_itu_label))

          val hduItuAdmission = viewModel.formFields.hduItuAdmission
          AdmittedToHduItuForm(
            isFormEnabled = hduItuAdmission.isEnabled.value,
            onFormEnabledChange = {
              hduItuAdmission.isEnabled.value = it
              if (!it) hduItuAdmission.reset()
            },
            dateState = hduItuAdmission.date,
            causeState = hduItuAdmission.cause,
            lengthOfStayInDaysState = hduItuAdmission.hduItuStayLengthInDays,
          )

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_maternal_death_label))

          val maternalDeath = viewModel.formFields.maternalDeath
          MaternalDeathForm(
            isFormEnabled = maternalDeath.isEnabled.value,
            onFormEnabledChange = {
              maternalDeath.isEnabled.value = it
              if (!it) maternalDeath.reset()
            },
            dateState = maternalDeath.date,
            underlyingCauseState = maternalDeath.underlyingCause,
            placeOfDeathState = maternalDeath.placeOfDeath,
          )

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_surgical_management_label))

          val surgicalManagement = viewModel.formFields.surgicalManagement
          SurgicalManagementForm(
            isFormEnabled = surgicalManagement.isEnabled.value,
            onFormEnabledChange = {
              surgicalManagement.isEnabled.value = it
              if (!it) surgicalManagement.reset()
            },
            dateState = surgicalManagement.date,
            surgicalManagementTypeState = surgicalManagement.type,
            serverEnumCollection = serverEnumCollection,
          )

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_perinatal_death_label))

          val perinatalDeath = viewModel.formFields.perinatalDeath
          PerinatalDeathForm(
            isFormEnabled = perinatalDeath.isEnabled.value,
            onFormEnabledChange = {
              perinatalDeath.isEnabled.value = it
              if (!it) perinatalDeath.reset()
            },
            dateState = perinatalDeath.date,
            outcomeState = perinatalDeath.outcome,
            maternalFactorsState = perinatalDeath.relatedMaternalFactors,
            serverEnumCollection = serverEnumCollection
          )
        }
      }

      item {
        formState.value.let { currentFormState ->
          if (currentFormState is PatientFormViewModel.FormState.FailedValidation) {
            BaseDetailsCard(
              title = stringResource(R.string.errors_card_title),
              modifier = Modifier.padding(16.dp),
              backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.3f)
            ) {
              for ((section, errors) in currentFormState.errorsBySectionStringId) {
                CategoryHeader(stringResource(section))
                for (error in errors) {
                  Text(error.errorMessage)
                }
              }
            }
          }
        }
      }

      item {
        SaveButtonCard(
          isEnabled = formState.value !is PatientFormViewModel.FormState.Loading &&
            formState.value !is PatientFormViewModel.FormState.Saving,
          onSaveButtonClick = { viewModel.save() },
          isExistingPatientEdit = viewModel.isExistingPatientEdit
        )
      }
    }

    formState.value.let { currentFormState ->
      when (currentFormState) {
        is PatientFormViewModel.FormState.SavedEditsToExistingPatient -> {
          LaunchedEffect(null) {
            onNavigateToPatient(currentFormState.primaryKeyOfPatient)
          }
        }
        is PatientFormViewModel.FormState.SavedNewPatient -> {
          LaunchedEffect(null) {
            onNavigateToPatient(currentFormState.primaryKeyOfPatient)
          }
        }
      }
    }
  }
}

@Composable
fun SaveButtonCard(
  onSaveButtonClick: () -> Unit,
  isExistingPatientEdit: Boolean,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
) {
  Card(
    elevation = 4.dp,
    shape = MaterialTheme.shapes.small,
    modifier = modifier.padding(36.dp)
  ) {
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp),
      onClick = onSaveButtonClick,
      enabled = isEnabled,
    ) {
      Text(
        if (isExistingPatientEdit) {
          stringResource(id = R.string.patient_form_save_edits)
        } else {
          stringResource(R.string.patient_form_save_new_patient_button)
        }
      )
    }
  }
}

@Preview
@Composable
fun SaveButtonCardPreview() {
  CradleTrialAppTheme {
    Scaffold {
      SaveButtonCard(onSaveButtonClick = { /*TODO*/ }, isExistingPatientEdit = false)
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
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  Column(modifier) {
    BooleanRadioButtonRow(
      isTrue = isFormEnabled,
      onBooleanChange = onFormEnabledStateChange,
    )

    val serverEnum = requireNotNull(serverEnumCollection[DropdownType.Place]) {
      "missing Place lookup values from the server"
    }
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { dateState.enableShowErrors(force = true) },
      label = {
        RequiredText(text = stringResource(R.string.form_date_label), isFormEnabled == true)
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = textFieldModifier.fillMaxWidth(),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenuIdOnly(
      currentSelection = placeOfFirstFitState.stateValue,
      onSelect = {
        placeOfFirstFitState.stateValue = it
      },
      serverEnum = serverEnum,
      label = { Text(stringResource(R.string.place_of_first_eclamptic_fit_label)) },
      enabled = isFormEnabled == true,
      errorHint = placeOfFirstFitState.getError(),
      textModifier = textFieldModifier.fillMaxWidth()
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
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  Column(modifier) {
    BooleanRadioButtonRow(
      isTrue = isFormEnabled,
      onBooleanChange = onFormEnabledChange,
    )

    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { dateState.enableShowErrors(force = true) },
      label = {
        RequiredText(text = stringResource(R.string.form_date_label), isFormEnabled == true)
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = textFieldModifier
        .fillMaxWidth()
        .then(dateState.createFocusChangeModifier()),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenuWithOther(
      currentSelection = causeState.stateValue,
      onSelect = { causeState.stateValue = it },
      serverEnum = causeState.enum!!,
      label = { Text(stringResource(R.string.hysterectomy_cause_label)) },
      enabled = isFormEnabled == true,
      dropdownTextModifier = Modifier.fillMaxWidth(),
      otherTextModifier = textFieldModifier
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
      colors = darkerDisabledOutlinedTextFieldColors()
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
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  Column(modifier) {
    BooleanRadioButtonRow(
      isTrue = isFormEnabled,
      onBooleanChange = onFormEnabledChange,
    )

    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { dateState.enableShowErrors(force = true) },
      label = {
        RequiredText(stringResource(id = R.string.form_date_label), isFormEnabled == true)
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier
        .fillMaxWidth()
        .then(dateState.createFocusChangeModifier()),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    // mandatory
    EnumDropdownMenuWithOther(
      currentSelection = causeState.stateValue,
      onSelect = { causeState.stateValue = it },
      serverEnum = causeState.enum!!,
      label = {
        RequiredText(
          text = stringResource(R.string.hdu_or_idu_admission_cause_label),
          required = isFormEnabled == true && causeState.isMandatory
        )
      },
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
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  BooleanRadioButtonRow(
    isTrue = isFormEnabled,
    onBooleanChange = onFormEnabledChange,
  )

  Column(modifier) {
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { dateState.enableShowErrors(force = true) },
      label = {
        RequiredText(text = stringResource(R.string.form_date_label), isFormEnabled == true)
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier
        .fillMaxWidth()
        .then(dateState.createFocusChangeModifier()),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenuWithOther(
      currentSelection = underlyingCauseState.stateValue,
      onSelect = { underlyingCauseState.stateValue = it },
      serverEnum = underlyingCauseState.enum!!,
      label = { Text(stringResource(R.string.maternal_death_underlying_cause_label)) },
      enabled = isFormEnabled == true,
      dropdownTextModifier = Modifier.fillMaxWidth(),
      showErrorHintOnOtherField = underlyingCauseState.stateValue != null,
      otherTextModifier = Modifier
        .fillMaxWidth()
        .then(underlyingCauseState.createFocusChangeModifier()),
      errorHint = underlyingCauseState.getError()
    )

    EnumDropdownMenuIdOnly(
      currentSelection = placeOfDeathState.stateValue,
      onSelect = { placeOfDeathState.stateValue = it },
      serverEnum = placeOfDeathState.enum!!,
      label = { Text(stringResource(R.string.maternal_death_place_label)) },
      enabled = isFormEnabled == true,
      errorHint = placeOfDeathState.getError(),
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

  Column(modifier) {
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { dateState.enableShowErrors(force = true) },
      label = {
        RequiredText(text = stringResource(R.string.form_date_label), isFormEnabled == true)
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier
        .fillMaxWidth()
        .then(dateState.createFocusChangeModifier()),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenuWithOther(
      currentSelection = surgicalManagementTypeState.stateValue,
      onSelect = { surgicalManagementTypeState.stateValue = it },
      serverEnum = surgicalManagementTypeState.enum!!,
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

  Column(modifier) {
    DateOutlinedTextField(
      date = dateState.stateValue.toFormDateOrNull(),
      onDatePicked = { dateState.stateValue = it.toString() },
      onPickerClose = { dateState.enableShowErrors(force = true) },
      label = {
        RequiredText(text = stringResource(R.string.form_date_label), isFormEnabled == true)
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier
        .fillMaxWidth()
        .then(dateState.createFocusChangeModifier()),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    EnumDropdownMenuIdOnly(
      currentSelection = outcomeState.stateValue,
      onSelect = { outcomeState.stateValue = it },
      serverEnum = outcomeState.enum!!,
      label = { Text(stringResource(R.string.perinatal_death_outcome_label)) },
      enabled = isFormEnabled == true,
      errorHint = outcomeState.getError(),
      textModifier = Modifier.fillMaxWidth()
    )

    EnumDropdownMenuWithOther(
      currentSelection = maternalFactorsState.stateValue,
      onSelect = { maternalFactorsState.stateValue = it },
      serverEnum = maternalFactorsState.enum!!,
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
          defaultEnums[DropdownType.Place],
          isMandatory = false
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
      PatientForm(ServerEnumCollection.defaultInstance, onNavigateBack = {}, onNavigateToPatient = {})
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
  val enum: ServerEnum?,
  private val isMandatory: Boolean,
  backingState: MutableState<EnumSelection.IdOnly?> = mutableStateOf(null)
) : FieldState<EnumSelection.IdOnly?>(
  validator = { selection ->
    when {
      selection == null -> !isMandatory
      enum == null -> true
      else -> enum.getValueFromId(selection.selectionId) != null
    }
  },
  errorFor = { ctx, _, -> ctx.getString(R.string.server_enum_unknown_selection_error) },
  initialValue = null,
  backingState = backingState
) {
  override val showErrorOnInput: Boolean = true
}

class EnumWithOtherState(
  val enum: ServerEnum?,
  val isMandatory: Boolean,
  private val otherSelection: ServerEnum.Entry? = enum?.validSortedValues?.find { it.name == "Other" },
  backingState: MutableState<EnumSelection.WithOther?> = mutableStateOf(null)
) : FieldState<EnumSelection.WithOther?>(
  validator = { selection ->
    when {
      selection == null -> !isMandatory
      enum == null -> true
      else -> {
        val entry = selection.let { enum.getValueFromId(it.selectionId) }
        if (entry == null) {
          false
        } else {
          !(entry == otherSelection && selection.otherString.isNullOrBlank())
        }
      }
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
  backingState,
) {
  override val showErrorOnInput: Boolean = true
  override fun onNewStateValue(newValue: EnumSelection.WithOther?) {
    if (isMandatory && newValue != null) {
      enableShowErrors(force = true)
    }
  }
}

class MutableStateWithCallback<T>(
  private val delegate: MutableState<T>,
  val newValueCallback: (newValue: T) -> Unit
) : MutableState<T> {
  override var value: T
    get() = delegate.value
    set(value) {
      delegate.value = value
      newValueCallback(value)
    }

  override fun component1(): T = delegate.component1()

  override fun component2(): (T) -> Unit {
    return { newValue ->
      delegate.component2()(newValue)
      newValueCallback(newValue)
    }
  }
}
