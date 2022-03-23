package org.welbodipartnership.cradle5.patients.form

import android.content.Context
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.patients.details.BaseDetailsCard
import org.welbodipartnership.cradle5.patients.details.CategoryHeader
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.forms.BooleanRadioButtonRow
import org.welbodipartnership.cradle5.ui.composables.forms.BringIntoViewOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.DateOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.EnumDropdownMenuIdOnly
import org.welbodipartnership.cradle5.ui.composables.forms.EnumDropdownMenuWithOther
import org.welbodipartnership.cradle5.ui.composables.forms.FieldState
import org.welbodipartnership.cradle5.ui.composables.forms.DatabasePagingListDropdown
import org.welbodipartnership.cradle5.ui.composables.forms.MoreInfoIconButton
import org.welbodipartnership.cradle5.ui.composables.forms.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.composables.forms.TextFieldState
import org.welbodipartnership.cradle5.ui.composables.forms.darkerDisabledOutlinedTextFieldColors
import org.welbodipartnership.cradle5.ui.composables.forms.formDateToTimestampMapper
import org.welbodipartnership.cradle5.ui.composables.forms.timestampToFormDateMapper
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate
import org.welbodipartnership.cradle5.util.datetime.toFormDateFromNoSlashesOrNull
import org.welbodipartnership.cradle5.util.datetime.toFormDateFromNoSlashesOrThrow

private val MAX_INITIALS_LENGTH = 5

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

@Immutable
@Parcelize
data class FacilityAndPosition(
  val facility: Facility,
  val position: Int?
) : Parcelable

@Immutable
@Parcelize
data class DistrictAndPosition(
  val district: District,
  val position: Int?
) : Parcelable

@Composable
fun PatientForm(
  serverEnumCollection: ServerEnumCollection,
  onNavigateBack: () -> Unit,
  onNavigateToPatient: (patientPrimaryKey: Long) -> Unit,
  viewModel: PatientFormViewModel = hiltViewModel()
) {
  val formState = viewModel.formState.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  val context = LocalContext.current
  val resources = context.resources
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
      is PatientFormViewModel.FormState.FailedException -> {
        snackbarHostState.showSnackbar(
          context.getString(R.string.patient_form_snackbar_failed_to_save_exception)
        )
      }
      else -> {}
    }
  }

  var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }
  BackHandler(
    enabled = formState.value !is PatientFormViewModel.FormState.FailedLoading
  ) { showUnsavedChangesDialog = true }
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

  val existingPatientInfo by viewModel.existingParentFacilityOutcomes.collectAsState(initial = null)

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
              existingPatientInfo.let { existingInfo ->
                if (existingInfo != null) {
                  if (existingInfo.patient.isUploadedToServer) {
                    Text(stringResource(R.string.edit_outcomes_title))
                  } else {
                    Text(stringResource(R.string.edit_patient_title))
                  }
                  Text(
                    existingInfo.patient.initials,
                    style = MaterialTheme.typography.subtitle2
                  )
                } else {
                  Text(stringResource(R.string.edit_patient_title))
                }
              }
            }
          } else {
            Text(stringResource(R.string.new_patient_title))
          }
        },
        navigationIcon = {
          val backPressedDispatcher = requireNotNull(
            LocalOnBackPressedDispatcherOwner.current!!,
            { "failed to get a back pressed dispatcher" }
          ).onBackPressedDispatcher
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
          if (existingPatientInfo?.patient?.isUploadedToServer == true) {
            Text("Patient registration info has already been uploaded; only the outcomes can be edited")
            return@BaseDetailsCard
          }

          existingPatientInfo?.patient?.serverErrorMessage?.let { serverErrorMessage ->
            LabelAndValueOrNone(stringResource(R.string.errors_from_sync_label), serverErrorMessage)
            Spacer(Modifier.height(categoryToCategorySpacerHeight))
          }

          val patientFields = viewModel.formFields.patientFields
          OutlinedTextFieldWithErrorHint(
            value = patientFields.initials.stateValue,
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
            text = patientFields.presentationDate.stateValue,
            onValueChange = { patientFields.presentationDate.stateValue = it },
            timestampToDateStringMapper = timestampToFormDateMapper,
            dateStringToTimestampMapper = formDateToTimestampMapper,
            maxLength = FormDate.MAX_STRING_LEN_NO_SLASHES,
            onPickerClose = { patientFields.presentationDate.enableShowErrors(force = true) },
            label = {
              Text(stringResource(id = R.string.patient_registration_presentation_date_label))
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

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            var isAgeUnknown by patientFields.isAgeUnknown
            OutlinedTextFieldWithErrorHint(
              value = patientFields.age.stateValue,
              onValueChange = { newAge ->
                if (newAge.length <= 2) {
                  patientFields.age.stateValue = newAge
                  patientFields.isAgeUnknown.value = false
                }
              },
              enabled = !isAgeUnknown,
              label = {
                if (isAgeUnknown) {
                  Text(stringResource(id = R.string.patient_registration_age_label))
                } else {
                  RequiredText(stringResource(id = R.string.patient_registration_age_label))
                }
              },
              modifier = Modifier.weight(1f),
              textFieldModifier = patientFields.age
                .createFocusChangeModifier()
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

            val focusManager = LocalFocusManager.current
            Checkbox(
              checked = isAgeUnknown,
              onCheckedChange = { newState ->
                isAgeUnknown = newState
                focusManager.clearFocus()
                if (newState) {
                  patientFields.age.stateValue = ""
                }
              },
            )

            Text(stringResource(R.string.unknown))
          }

          val (address, onAddressChange) = patientFields.address
          BringIntoViewOutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.patient_address_label)) },
            colors = darkerDisabledOutlinedTextFieldColors()
          )

          FacilityListDropdown(
            state = patientFields.healthcareFacility,
            pagingItemFlow = viewModel.facilitiesPagerFlow,
            label = {
              RequiredText(stringResource(R.string.patient_registration_healthcare_facility_label))
            },
            modifier = Modifier.fillMaxWidth(),
            textFieldModifier = Modifier
              .fillMaxWidth()
              .then(patientFields.healthcareFacility.createFocusChangeModifier())
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          PatientReferralInfoForm(
            referralInfoFields = patientFields.referralInfo,
            districtPagingFlow = viewModel.districtsPagerFlow,
            facilityPagingFlow = viewModel.facilitiesPagerFlow,
            textFieldToTextFieldHeight = textFieldToTextFieldHeight,
          )
        }
      }

      item {
        BaseDetailsCard(
          stringResource(R.string.outcomes_card_title),
          Modifier.padding(16.dp)
        ) {
          existingPatientInfo?.outcomes?.serverErrorMessage?.let { serverErrorMessage ->
            LabelAndValueOrNone(stringResource(R.string.errors_from_sync_label), serverErrorMessage)
            Spacer(Modifier.height(categoryToCategorySpacerHeight))
          }

          CategoryHeader(stringResource(R.string.outcomes_eclampsia_label))

          val eclampsia = viewModel.formFields.eclampsia
          EclampsiaForm(
            isFormEnabled = eclampsia.isEnabled.value,
            onFormEnabledStateChange = {
              eclampsia.isEnabled.value = it
              if (!it) eclampsia.clearFormsAndSetCheckbox(newEnabledState = false)
            },
            dateState = eclampsia.date,
            placeOfFirstFitState = eclampsia.placeOfFirstFit,
            serverEnumCollection = serverEnumCollection,
            textFieldModifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
          )

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_hysterectomy_label))

          val hysterectomy = viewModel.formFields.hysterectomy
          HysterectomyForm(
            isFormEnabled = hysterectomy.isEnabled.value,
            onFormEnabledChange = {
              hysterectomy.isEnabled.value = it
              if (!it) hysterectomy.clearFormsAndSetCheckbox(newEnabledState = false)
            },
            dateState = hysterectomy.date,
            causeState = hysterectomy.cause,
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
              if (!it) hduItuAdmission.clearFormsAndSetCheckbox(newEnabledState = false)
            },
            dateState = hduItuAdmission.date,
            causeState = hduItuAdmission.cause,
            lengthOfStayInDaysState = hduItuAdmission.hduItuStayLengthInDays,
            additionalInfo = hduItuAdmission.additionalInfo.value ?: "",
            onAdditionalInfoChanged = { hduItuAdmission.additionalInfo.value = it }
          )

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_maternal_death_label))

          val maternalDeath = viewModel.formFields.maternalDeath
          MaternalDeathForm(
            isFormEnabled = maternalDeath.isEnabled.value,
            onFormEnabledChange = {
              maternalDeath.isEnabled.value = it
              if (!it) maternalDeath.clearFormsAndSetCheckbox(newEnabledState = false)
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
              if (!it) surgicalManagement.clearFormsAndSetCheckbox(newEnabledState = false)
            },
            dateState = surgicalManagement.date,
            surgicalManagementTypeState = surgicalManagement.type,
          )

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_perinatal_death_label))

          val perinatalDeath = viewModel.formFields.perinatalDeath
          PerinatalDeathForm(
            isFormEnabled = perinatalDeath.isEnabled.value,
            onFormEnabledChange = {
              perinatalDeath.isEnabled.value = it
              if (!it) perinatalDeath.clearFormsAndSetCheckbox(newEnabledState = false)
            },
            dateState = perinatalDeath.date,
            outcomeState = perinatalDeath.outcome,
            maternalFactorsState = perinatalDeath.relatedMaternalFactors,
            additionalInfo = perinatalDeath.additionalInfo.value ?: "",
            onAdditionalInfoChanged = { perinatalDeath.additionalInfo.value = it }
          )

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_birthweight_label))

          BirthWeightForm(birthWeightState = viewModel.formFields.birthWeight.birthWeight)

          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_age_at_delivery_label))

          AgeAtDeliveryForm(ageAtDeliveryState = viewModel.formFields.ageAtDelivery.ageAtDelivery)
        }
      }

      item {
        formState.value.let { currentFormState ->
          val detailsContent: @Composable (ColumnScope.() -> Unit)? = when (currentFormState) {
            is PatientFormViewModel.FormState.FailedValidation -> {
              {
                for ((section, errors) in currentFormState.errorsBySectionStringId) {
                  CategoryHeader(stringResource(section))
                  for (error in errors) {
                    val fieldTitle = stringResource(error.fieldTitle)
                    Text("$fieldTitle: ${error.errorMessage}")
                  }
                }
              }
            }
            is PatientFormViewModel.FormState.FailedException -> {
              {
                val horizontalScrollState = rememberScrollState()
                SelectionContainer {
                  Column {
                    Text(
                      currentFormState.exception.stackTraceToString(),
                      modifier = Modifier.horizontalScroll(horizontalScrollState)
                    )
                  }
                }
              }
            }
            else -> {
              null
            }
          }

          detailsContent?.let { columnContent ->
            BaseDetailsCard(
              title = stringResource(R.string.errors_card_title),
              modifier = Modifier.padding(16.dp),
              backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.3f),
              columnContent = {
                columnContent()
                if (existingPatientInfo?.patient?.isUploadedToServer != true) {
                  Spacer(Modifier.height(16.dp))
                  Divider(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    thickness = 4.dp
                  )
                  Spacer(Modifier.height(16.dp))
                  Text(stringResource(R.string.other_card_draft_more_info))
                }
              },
            )
          }
        }
      }

      item {
        val (isDraft, setIsDraft) = viewModel.formFields.patientFields.isDraft
        val (localNotes, setLocalNotes) = viewModel.formFields.patientFields.localNotes
        OtherCard(
          hideDraft = existingPatientInfo?.patient?.isUploadedToServer == true,
          isDraft = isDraft,
          onIsDraftChange = setIsDraft,
          localNotes = localNotes,
          onLocalNotesChange = setLocalNotes,
          modifier = Modifier.padding(16.dp)
        )
      }

      item {
        val focusManager = LocalFocusManager.current
        SaveButtonCard(
          isEnabled = formState.value !is PatientFormViewModel.FormState.Loading &&
            formState.value !is PatientFormViewModel.FormState.Saving,
          onSaveButtonClick = {
            focusManager.clearFocus()
            viewModel.save()
          },
          text = if (viewModel.isExistingPatientEdit) {
            stringResource(id = R.string.patient_form_save_edits)
          } else {
            stringResource(R.string.patient_form_save_new_patient_button)
          }
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
        else -> {}
      }
    }
  }
}

@Composable
fun PatientReferralInfoForm(
  referralInfoFields: PatientFormViewModel.PatientFields.ReferralInfoFields,
  districtPagingFlow: Flow<PagingData<District>>,
  facilityPagingFlow: Flow<PagingData<Facility>>,
  textFieldToTextFieldHeight: Dp,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  val (
    isFormEnabledState,
    fromDistrictState: DistrictState,
    fromFacilityState: HealthcareFacilityState,
    toDistrictState: DistrictState,
    toFacilityState: HealthcareFacilityState
  ) = referralInfoFields
  var isFormEnabled by isFormEnabledState
  Column(modifier) {
    RequiredText(text = stringResource(R.string.patient_referral_checkbox_label))
    BooleanRadioButtonRow(isTrue = isFormEnabled, onBooleanChange = {
      isFormEnabled = it
      if (!it) referralInfoFields.clearFormsAndSetCheckbox(newEnabledState = false)
    })

    if (isFormEnabled == true) {
      DistrictListDropdown(
        state = fromDistrictState,
        pagingItemFlow = districtPagingFlow,
        label = {
          RequiredText(stringResource(R.string.patient_referral_info_from_district_label))
        },
        modifier = Modifier.fillMaxWidth(),
        textFieldModifier = Modifier
          .fillMaxWidth()
          .then(fromDistrictState.createFocusChangeModifier())
      )

      Spacer(Modifier.height(textFieldToTextFieldHeight))

      FacilityListDropdown(
        state = fromFacilityState,
        pagingItemFlow = facilityPagingFlow,
        label = { RequiredText(stringResource(R.string.patient_referral_info_from_facility_label)) },
        modifier = Modifier.fillMaxWidth(),
        textFieldModifier = Modifier
          .fillMaxWidth()
          .then(fromFacilityState.createFocusChangeModifier())
      )

      Spacer(Modifier.height(textFieldToTextFieldHeight))

      DistrictListDropdown(
        state = toDistrictState,
        pagingItemFlow = districtPagingFlow,
        label = { RequiredText(stringResource(R.string.patient_referral_info_to_district_label)) },
        modifier = Modifier.fillMaxWidth(),
        textFieldModifier = Modifier
          .fillMaxWidth()
          .then(toDistrictState.createFocusChangeModifier())
      )

      Spacer(Modifier.height(textFieldToTextFieldHeight))

      FacilityListDropdown(
        state = toFacilityState,
        pagingItemFlow = facilityPagingFlow,
        label = {
          RequiredText(stringResource(R.string.patient_referral_info_to_facility_label))
        },
        modifier = Modifier.fillMaxWidth(),
        textFieldModifier = Modifier
          .fillMaxWidth()
          .then(toFacilityState.createFocusChangeModifier())
      )
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
    Row {
      BooleanRadioButtonRow(
        isTrue = isFormEnabled,
        onBooleanChange = onFormEnabledStateChange,
      )
      MoreInfoIconButton(stringResource(R.string.outcomes_eclampsia_more_info))
    }

    val serverEnum = requireNotNull(serverEnumCollection[DropdownType.Place]) {
      "missing Place lookup values from the server"
    }
    DateOutlinedTextField(
      text = dateState.stateValue,
      onValueChange = { dateState.stateValue = it },
      dateStringToTimestampMapper = formDateToTimestampMapper,
      timestampToDateStringMapper = timestampToFormDateMapper,
      maxLength = FormDate.MAX_STRING_LEN_NO_SLASHES,
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
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  Column(modifier) {
    Row {
      BooleanRadioButtonRow(
        isTrue = isFormEnabled,
        onBooleanChange = onFormEnabledChange,
      )
      MoreInfoIconButton(stringResource(R.string.outcomes_hysterectomy_more_info))
    }

    DateOutlinedTextField(
      text = dateState.stateValue,
      onValueChange = { dateState.stateValue = it },
      dateStringToTimestampMapper = formDateToTimestampMapper,
      timestampToDateStringMapper = timestampToFormDateMapper,
      maxLength = FormDate.MAX_STRING_LEN_NO_SLASHES,
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
  }
}

@Composable
fun AdmittedToHduItuForm(
  isFormEnabled: Boolean?,
  onFormEnabledChange: (newState: Boolean) -> Unit,
  dateState: NoFutureDateState,
  causeState: EnumWithOtherState,
  lengthOfStayInDaysState: LimitedHduItuState,
  additionalInfo: String,
  onAdditionalInfoChanged: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Row {
      BooleanRadioButtonRow(
        isTrue = isFormEnabled,
        onBooleanChange = onFormEnabledChange,
      )
      MoreInfoIconButton(stringResource(R.string.outcomes_admission_to_hdu_or_itu_more_info))
    }

    DateOutlinedTextField(
      text = dateState.stateValue,
      onValueChange = { dateState.stateValue = it },
      dateStringToTimestampMapper = formDateToTimestampMapper,
      timestampToDateStringMapper = timestampToFormDateMapper,
      maxLength = FormDate.MAX_STRING_LEN_NO_SLASHES,
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
    BringIntoViewOutlinedTextField(
      value = additionalInfo,
      onValueChange = onAdditionalInfoChanged,
      modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(R.string.hdu_or_idu_admission_additional_info)) },
      enabled = isFormEnabled == true,
      colors = darkerDisabledOutlinedTextFieldColors()
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
) {
  Row {
    BooleanRadioButtonRow(
      isTrue = isFormEnabled,
      onBooleanChange = onFormEnabledChange,
    )
    MoreInfoIconButton(stringResource(R.string.outcomes_maternal_death_more_info))
  }

  Column(modifier) {
    DateOutlinedTextField(
      text = dateState.stateValue,
      onValueChange = { dateState.stateValue = it },
      dateStringToTimestampMapper = formDateToTimestampMapper,
      timestampToDateStringMapper = timestampToFormDateMapper,
      maxLength = FormDate.MAX_STRING_LEN_NO_SLASHES,
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
      label = {
        RequiredText(
          stringResource(R.string.maternal_death_underlying_cause_label),
          required = underlyingCauseState.isMandatory && isFormEnabled == true
        )
      },
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
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Row {
      BooleanRadioButtonRow(
        isTrue = isFormEnabled,
        onBooleanChange = onFormEnabledChange,
      )
      MoreInfoIconButton(stringResource(R.string.outcomes_surgical_management_more_info))
    }

    DateOutlinedTextField(
      text = dateState.stateValue,
      onValueChange = { dateState.stateValue = it },
      dateStringToTimestampMapper = formDateToTimestampMapper,
      timestampToDateStringMapper = timestampToFormDateMapper,
      maxLength = FormDate.MAX_STRING_LEN_NO_SLASHES,
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
  additionalInfo: String,
  onAdditionalInfoChanged: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row {
    BooleanRadioButtonRow(
      isTrue = isFormEnabled,
      onBooleanChange = onFormEnabledChange,
    )
    MoreInfoIconButton(stringResource(R.string.outcomes_perinatal_death_more_info))
  }

  Column(modifier) {
    DateOutlinedTextField(
      text = dateState.stateValue,
      onValueChange = { dateState.stateValue = it },
      dateStringToTimestampMapper = formDateToTimestampMapper,
      timestampToDateStringMapper = timestampToFormDateMapper,
      maxLength = FormDate.MAX_STRING_LEN_NO_SLASHES,
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
      serverEnum = requireNotNull(maternalFactorsState.enum) { "missing maternal factors enum" },
      label = { Text(stringResource(R.string.perinatal_death_related_maternal_factors_label)) },
      enabled = isFormEnabled == true,
      dropdownTextModifier = Modifier.fillMaxWidth(),
      showErrorHintOnOtherField = maternalFactorsState.stateValue != null,
      otherTextModifier = Modifier
        .fillMaxWidth()
        .then(maternalFactorsState.createFocusChangeModifier()),
      errorHint = maternalFactorsState.getError()
    )

    BringIntoViewOutlinedTextField(
      value = additionalInfo,
      onValueChange = onAdditionalInfoChanged,
      modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(R.string.perinatal_death_additional_info_label)) },
      enabled = isFormEnabled == true,
      colors = darkerDisabledOutlinedTextFieldColors()
    )
  }
}

@Composable
fun BirthWeightForm(
  birthWeightState: EnumIdOnlyState,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    EnumDropdownMenuIdOnly(
      currentSelection = birthWeightState.stateValue,
      onSelect = { birthWeightState.stateValue = it },
      serverEnum = birthWeightState.enum!!,
      label = { Text(stringResource(R.string.birthweight_selection_label)) },
      enabled = true,
      errorHint = birthWeightState.getError(),
      textModifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun AgeAtDeliveryForm(
  ageAtDeliveryState: EnumIdOnlyState,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    EnumDropdownMenuIdOnly(
      currentSelection = ageAtDeliveryState.stateValue,
      onSelect = { ageAtDeliveryState.stateValue = it },
      serverEnum = ageAtDeliveryState.enum!!,
      label = { Text(stringResource(R.string.age_at_delivery_selection_label)) },
      enabled = true,
      errorHint = ageAtDeliveryState.getError(),
      textModifier = Modifier.fillMaxWidth()
    )
  }
}

@Preview
@Composable
fun EclampsiaFormPreview() {
  CradleTrialAppTheme {
    Scaffold {
      val defaultEnums = ServerEnumCollection.defaultInstance
      val draft = remember { mutableStateOf(false) }
      EclampsiaForm(
        isFormEnabled = false,
        onFormEnabledStateChange = {},
        dateState = NoFutureDateState(
          isMandatory = true,
          areApproximateDatesAcceptable = true,
          isFormDraftState = draft,
        ),
        placeOfFirstFitState = EnumIdOnlyState(
          defaultEnums[DropdownType.Place],
          isMandatory = false,
          isFormDraftState = draft
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

@Composable
fun DistrictListDropdown(
  state: DistrictState,
  pagingItemFlow: Flow<PagingData<District>>,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  label: @Composable (() -> Unit)? = null,
) = DatabasePagingListDropdown(
  selectedItem = state.stateValue?.district,
  positionInList = state.stateValue?.position,
  onItemSelected = { idx, district -> state.stateValue = DistrictAndPosition(district, idx) },
  formatTextForListItem = District::name,
  title = {
    Text(
      stringResource(R.string.patient_registration_district_dialog_title),
      style = MaterialTheme.typography.subtitle1
    )
  },
  pagingItemFlow = pagingItemFlow,
  errorHint = state.getError(),
  enabled = enabled,
  label = label,
  modifier = modifier,
  textFieldModifier = textFieldModifier
)

@Composable
fun FacilityListDropdown(
  state: HealthcareFacilityState,
  pagingItemFlow: Flow<PagingData<Facility>>,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  label: @Composable (() -> Unit)? = null,
) = DatabasePagingListDropdown(
  selectedItem = state.stateValue?.facility,
  positionInList = state.stateValue?.position,
  onItemSelected = { idx, district -> state.stateValue = FacilityAndPosition(district, idx) },
  formatTextForListItem = Facility::name,
  title = {
    Text(
      stringResource(R.string.patient_registration_health_facility_dialog_title),
      style = MaterialTheme.typography.subtitle1
    )
  },
  pagingItemFlow = pagingItemFlow,
  errorHint = state.getError(),
  enabled = enabled,
  label = label,
  modifier = modifier,
  textFieldModifier = textFieldModifier
)

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
  isFormDraftState: State<Boolean?>
) : TextFieldState(
  validator = { it.length in 1..MAX_INITIALS_LENGTH },
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

class LimitedHduItuState(
  isMandatory: Boolean,
  val limit: LongRange,
  backingState: MutableState<String> = mutableStateOf(""),
  isFormDraftState: State<Boolean?>
) : TextFieldState(
  validator = { stay ->
    run {
      // optional
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
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory
)

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
  errorFor = { ctx, _, -> ctx.getString(R.string.server_enum_unknown_selection_error) },
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
      ctx.getString(R.string.server_enum_selection_required_error)
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
