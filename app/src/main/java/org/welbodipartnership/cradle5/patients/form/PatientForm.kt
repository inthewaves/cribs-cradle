package org.welbodipartnership.cradle5.patients.form

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.compose.forms.state.DistrictState
import org.welbodipartnership.cradle5.compose.forms.state.EnumIdOnlyState
import org.welbodipartnership.cradle5.compose.forms.state.EnumWithOtherState
import org.welbodipartnership.cradle5.compose.forms.state.HealthcareFacilityState
import org.welbodipartnership.cradle5.compose.forms.state.NoFutureDateState
import org.welbodipartnership.cradle5.compose.forms.state.NonEmptyTextState
import org.welbodipartnership.cradle5.compose.forms.state.NullableToggleState
import org.welbodipartnership.cradle5.data.database.entities.CausesOfNeonatalDeath
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.patients.details.BaseDetailsCard
import org.welbodipartnership.cradle5.patients.details.CategoryHeader
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.forms.AnimatedErrorHint
import org.welbodipartnership.cradle5.ui.composables.forms.BooleanRadioButtonRow
import org.welbodipartnership.cradle5.ui.composables.forms.BringIntoViewOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.DatabasePagingListDropdown
import org.welbodipartnership.cradle5.ui.composables.forms.DateOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.EnumDropdownMenuIdOnly
import org.welbodipartnership.cradle5.ui.composables.forms.EnumDropdownMenuWithOther
import org.welbodipartnership.cradle5.ui.composables.forms.IntegerField
import org.welbodipartnership.cradle5.ui.composables.forms.MoreInfoIconButton
import org.welbodipartnership.cradle5.ui.composables.forms.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.composables.forms.RequiredText
import org.welbodipartnership.cradle5.ui.composables.forms.darkerDisabledOutlinedTextFieldColors
import org.welbodipartnership.cradle5.ui.composables.forms.formDateToTimestampMapper
import org.welbodipartnership.cradle5.ui.composables.forms.timestampToFormDateMapper
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.DistrictAndPosition
import org.welbodipartnership.cradle5.util.FacilityAndPosition
import org.welbodipartnership.cradle5.util.datetime.FormDate

private val MAX_INITIALS_LENGTH = 5

/**
 * Support wide screen by making the content width max 840dp, centered horizontally.
 */
fun Modifier.supportWideScreen() = this
  .fillMaxWidth()
  .wrapContentWidth(align = Alignment.CenterHorizontally)
  .widthIn(max = 840.dp)

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

          FacilityBpInfoForm(
            fields = patientFields.bpInfo,
            textFieldToTextFieldHeight
          )

          Spacer(modifier = Modifier.height(textFieldToTextFieldHeight * 2))

          Box(
            Modifier
              .fillMaxWidth()
              .height(2.dp)
              .background(MaterialTheme.colors.primary.copy(alpha = 0.5f))
          )

          Spacer(modifier = Modifier.height(textFieldToTextFieldHeight * 2))

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
            pagingItemFlow = viewModel.facilitiesForSelfDistrictPagerFlow,
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
            facilityPagingFlowGetter = { viewModel.getFacilitiesPagingDataForDistrict(it) },
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
            causeOfStillbirth = perinatalDeath.causeOfStillBirth,
            causesOfNeonatalDeathState = perinatalDeath.causeOfNeonatalDeath,
            additionalInfo = perinatalDeath.additionalInfo.value ?: "",
            onAdditionalInfoChanged = { perinatalDeath.additionalInfo.value = it }
          )
          Spacer(Modifier.height(categoryToCategorySpacerHeight))
          CategoryHeader(stringResource(R.string.outcomes_birthweight_label))
          BirthWeightForm(
            birthWeightState = viewModel.formFields.birthWeight.birthWeight,
            isNotReported = viewModel.formFields.birthWeight.isNotReported,
          )
          Spacer(Modifier.height(categoryToCategorySpacerHeight))
          CategoryHeader(stringResource(R.string.outcomes_age_at_delivery_label))
          AgeAtDeliveryForm(
            ageAtDeliveryState = viewModel.formFields.ageAtDelivery.ageAtDelivery,
            isNotReported = viewModel.formFields.ageAtDelivery.isNotReported,
          )
          Spacer(Modifier.height(categoryToCategorySpacerHeight))

          CategoryHeader(stringResource(R.string.outcomes_eclampsia_label))
          val eclampsia = viewModel.formFields.eclampsia
          EclampsiaForm(
            isFormEnabled = eclampsia.isEnabled.value,
            onFormEnabledStateChange = {
              eclampsia.isEnabled.value = it
              if (!it) eclampsia.clearFormsAndSetCheckbox(newEnabledState = false)
            },
            didTheWomanFitState = eclampsia.didTheWomanFit,
            whenWasFirstFitState = eclampsia.whenWasFirstFit,
            placeOfFirstFitState = eclampsia.placeOfFirstFit,
            serverEnumCollection = serverEnumCollection,
            textFieldModifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
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
            mdsrFindingsState = maternalDeath.summaryOfMdsrFindings,
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
fun FacilityBpInfoForm(
  fields: PatientFormViewModel.PatientFields.BpInfoFields,
  textFieldToTextFieldHeight: Dp,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  val (
    isFormEnabledState,
    numBpReadingsTakenInFacilitySinceLastVisit,
    numBpReadingsEndIn0Or5,
    numBpReadingsWithAssociatedColorAndArrow
  ) = fields
  var isFormEnabled by isFormEnabledState
  Column(modifier) {
    RequiredText(text = stringResource(R.string.facility_bp_info_checkbox_label))
    // using negation, because No means have to fill it out, but we want to preserve semantics of
    // "isFormEnabled"
    BooleanRadioButtonRow(
      isTrue = isFormEnabled?.not(),
      onBooleanChange = { newValue ->
        val actualNewValue = !newValue
        val oldValue = isFormEnabled?.not()
        isFormEnabled = actualNewValue
        if (oldValue != actualNewValue) fields.clearFormsAndSetCheckbox(newEnabledState = actualNewValue)
      }
    )

    val commonKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    if (isFormEnabled == true) {
      Spacer(Modifier.height(textFieldToTextFieldHeight))
      IntegerField(
        field = numBpReadingsTakenInFacilitySinceLastVisit,
        label = stringResource(R.string.facility_bp_info_bp_readings_in_facility_today_since_last_visited_label),
        keyboardOptions = commonKeyboardOptions
      )
      Spacer(Modifier.height(textFieldToTextFieldHeight))
      IntegerField(
        field = numBpReadingsEndIn0Or5,
        label = stringResource(R.string.facility_bp_info_bp_readings_end_in_a_0_or_a_5_label),
        keyboardOptions = commonKeyboardOptions
      )
      Spacer(Modifier.height(textFieldToTextFieldHeight))
      IntegerField(
        field = numBpReadingsWithAssociatedColorAndArrow,
        label = stringResource(R.string.facility_bp_info_bp_readings_have_color_or_arrow_label),
        keyboardOptions = commonKeyboardOptions
      )
    }
  }
}

@Composable
fun PatientReferralInfoForm(
  referralInfoFields: PatientFormViewModel.PatientFields.ReferralInfoFields,
  districtPagingFlow: Flow<PagingData<District>>,
  facilityPagingFlowGetter: (District?) -> Flow<PagingData<Facility>>,
  textFieldToTextFieldHeight: Dp,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
) {
  val (
    isFormEnabledState,
    fromDistrictState: DistrictState,
    fromFacilityState: HealthcareFacilityState,
    fromFacilityTextState: NonEmptyTextState,
    toDistrictState: DistrictState,
    toFacilityState: HealthcareFacilityState,
    toFacilityTextState: NonEmptyTextState,
  ) = referralInfoFields
  var isFormEnabled by isFormEnabledState
  Column(modifier) {
    RequiredText(text = stringResource(R.string.patient_referral_checkbox_label))
    BooleanRadioButtonRow(isTrue = isFormEnabled, onBooleanChange = { newValue ->
      val oldValue = isFormEnabled
      isFormEnabled = newValue
      if (oldValue != newValue) {
        referralInfoFields.clearFormsAndSetCheckbox(newEnabledState = newValue)
      }
    })

    if (isFormEnabled == true) {
      DistrictAndFacilityFormPair(
        districtState = fromDistrictState,
        districtLabel = { RequiredText(stringResource(R.string.patient_referral_info_from_district_label)) },
        facilityState = fromFacilityState,
        facilityCustomTextState = fromFacilityTextState,
        facilityLabel = { RequiredText(stringResource(R.string.patient_referral_info_from_facility_label)) },
        districtPagingFlow,
        facilityPagingFlowGetter,
        textFieldToTextFieldHeight
      )

      Spacer(Modifier.height(textFieldToTextFieldHeight))

      DistrictAndFacilityFormPair(
        districtState = toDistrictState,
        districtLabel = { RequiredText(stringResource(R.string.patient_referral_info_to_district_label)) },
        facilityState = toFacilityState,
        facilityCustomTextState = toFacilityTextState,
        facilityLabel = { RequiredText(stringResource(R.string.patient_referral_info_to_facility_label)) },
        districtPagingFlow,
        facilityPagingFlowGetter,
        textFieldToTextFieldHeight
      )
    }
  }
}

@Composable
fun DistrictAndFacilityFormPair(
  districtState: DistrictState,
  districtLabel: @Composable() (() -> Unit)?,
  facilityState: HealthcareFacilityState,
  facilityCustomTextState: NonEmptyTextState,
  facilityLabel: @Composable() (() -> Unit)?,
  districtPagingFlow: Flow<PagingData<District>>,
  facilityPagingFlowGetter: (District?) -> Flow<PagingData<Facility>>,
  textFieldToTextFieldHeight: Dp,
) {
  DistrictListDropdown(
    state = districtState,
    pagingItemFlow = districtPagingFlow,
    label = districtLabel,
    modifier = Modifier.fillMaxWidth(),
    textFieldModifier = Modifier
      .fillMaxWidth()
      .then(districtState.createFocusChangeModifier()),
    extraOnItemSelected = { old, new ->
      if (old != new) {
        facilityState.reset()
        facilityCustomTextState.reset()
      }
    },
  )

  Spacer(Modifier.height(textFieldToTextFieldHeight))

  if (districtState.stateValue?.district?.isOther == true) {
    OutlinedTextFieldWithErrorHint(
      value = facilityCustomTextState.stateValue ?: "",
      onValueChange = { facilityCustomTextState.stateValue = it },
      modifier = Modifier.fillMaxWidth(),
      label = facilityLabel,
      colors = darkerDisabledOutlinedTextFieldColors(),
      errorHint = facilityCustomTextState.getError()
    )
  } else {
    val fromFacilityFlow = remember(districtState.stateValue) {
      facilityPagingFlowGetter(districtState.stateValue?.district)
    }

    FacilityListDropdown(
      state = facilityState,
      pagingItemFlow = fromFacilityFlow,
      label = facilityLabel,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier
        .fillMaxWidth()
        .then(facilityState.createFocusChangeModifier())
    )
  }
}

@Composable
fun EclampsiaForm(
  isFormEnabled: Boolean?,
  onFormEnabledStateChange: (newState: Boolean) -> Unit,
  didTheWomanFitState: NullableToggleState,
  whenWasFirstFitState: EnumIdOnlyState,
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

    RequiredText(
      text = stringResource(R.string.outcomes_eclampsia_did_woman_fit_label),
      enabled = isFormEnabled == true
    )
    BooleanRadioButtonRow(
      isTrue = didTheWomanFitState.stateValue,
      onBooleanChange = {
        didTheWomanFitState.stateValue = it
        if (!it) placeOfFirstFitState.stateValue = null
      },
      enabled = isFormEnabled == true
    )
    AnimatedErrorHint(errorHint = didTheWomanFitState.getError(), enabled = isFormEnabled == true)

    val whenWasFirstFitEnum = requireNotNull(serverEnumCollection[DropdownType.EclampticFitTime]) {
      "missing EclampticFitTime lookup values from the server"
    }
    EnumDropdownMenuIdOnly(
      currentSelection = whenWasFirstFitState.stateValue,
      onSelect = { whenWasFirstFitState.stateValue = it },
      serverEnum = whenWasFirstFitEnum,
      label = { Text(stringResource(R.string.when_was_first_eclamptic_fit_label)) },
      enabled = isFormEnabled == true && didTheWomanFitState.stateValue == true,
      errorHint = whenWasFirstFitState.getError(),
      textModifier = textFieldModifier.fillMaxWidth()
    )

    val placeEnum = requireNotNull(serverEnumCollection[DropdownType.Place]) {
      "missing Place lookup values from the server"
    }
    EnumDropdownMenuIdOnly(
      currentSelection = placeOfFirstFitState.stateValue,
      onSelect = { placeOfFirstFitState.stateValue = it },
      serverEnum = placeEnum,
      label = { Text(stringResource(R.string.place_of_first_eclamptic_fit_label)) },
      enabled = isFormEnabled == true && didTheWomanFitState.stateValue == true,
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
        RequiredText(text = stringResource(R.string.form_date_label), required = isFormEnabled == true)
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
fun MaternalDeathForm(
  isFormEnabled: Boolean?,
  onFormEnabledChange: (newState: Boolean) -> Unit,
  dateState: NoFutureDateState,
  underlyingCauseState: EnumWithOtherState,
  placeOfDeathState: EnumIdOnlyState,
  mdsrFindingsState: MutableState<String?>,
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
        RequiredText(text = stringResource(R.string.form_date_label), required = isFormEnabled == true)
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

    BringIntoViewOutlinedTextField(
      label = { Text(stringResource(R.string.maternal_death_summary_of_mdsr_findings_label)) },
      value = mdsrFindingsState.value ?: "",
      onValueChange = { mdsrFindingsState.value = it },
      modifier = Modifier.fillMaxWidth(),
      colors = darkerDisabledOutlinedTextFieldColors()
    )
  }
}

@Composable
fun PerinatalDeathForm(
  isFormEnabled: Boolean?,
  onFormEnabledChange: (newState: Boolean) -> Unit,
  dateState: NoFutureDateState,
  outcomeState: EnumIdOnlyState,
  causeOfStillbirth: EnumIdOnlyState,
  causesOfNeonatalDeathState: MutableState<CausesOfNeonatalDeath?>,
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
        RequiredText(text = stringResource(R.string.form_date_label), required = isFormEnabled == true)
      },
      enabled = isFormEnabled == true,
      modifier = Modifier.fillMaxWidth(),
      textFieldModifier = Modifier
        .fillMaxWidth()
        .then(dateState.createFocusChangeModifier()),
      errorHint = dateState.getError(),
      keyboardOptions = KeyboardOptions.Default,
    )

    // don't really know what the server will do in the future, so just do string matching
    fun EnumSelection.IdOnly?.isStillbirth(serverEnum: ServerEnum) =
      this?.let(serverEnum::get)?.name?.contains("stillbirth", ignoreCase = true) == true

    fun EnumSelection.IdOnly?.isNeonatalDeath(serverEnum: ServerEnum) =
      this?.let(serverEnum::get)?.name?.contains("neonatal", ignoreCase = true) == true

    EnumDropdownMenuIdOnly(
      currentSelection = outcomeState.stateValue,
      onSelect = { newState ->
        val oldState = outcomeState.stateValue
        outcomeState.stateValue = newState
        if (oldState != newState) {
          if (newState.isStillbirth(outcomeState.enum!!)) {
            if (!oldState.isStillbirth(outcomeState.enum)) {
              causeOfStillbirth.stateValue = null
              causesOfNeonatalDeathState.value = null
            }
          } else if (newState.isNeonatalDeath(outcomeState.enum)) {
            if (!oldState.isNeonatalDeath(outcomeState.enum)) {
              causeOfStillbirth.stateValue = null
              causesOfNeonatalDeathState.value = null
            }
          } else if (newState == null) { // empty option selected
            causeOfStillbirth.stateValue = null
            causesOfNeonatalDeathState.value = null
          }
        }
      },
      serverEnum = outcomeState.enum!!,
      label = { Text(stringResource(R.string.perinatal_death_outcome_label)) },
      enabled = isFormEnabled == true,
      errorHint = outcomeState.getError(),
      textModifier = Modifier.fillMaxWidth()
    )

    var causesOfNeonatalDeath by causesOfNeonatalDeathState
    if (
      causeOfStillbirth.stateValue != null ||
      outcomeState.stateValue.isStillbirth(outcomeState.enum)
    ) {
      EnumDropdownMenuIdOnly(
        currentSelection = causeOfStillbirth.stateValue,
        onSelect = { causeOfStillbirth.stateValue = it },
        serverEnum = requireNotNull(causeOfStillbirth.enum) { "missing stillbirth cause enum" },
        label = { Text(stringResource(R.string.perinatal_death_cause_of_stillbirth_label)) },
        enabled = isFormEnabled == true,
        errorHint = causeOfStillbirth.getError(),
        textModifier = Modifier.fillMaxWidth()
      )
    } else if (
      (causesOfNeonatalDeath?.areAllFieldsFalse == false) ||
      outcomeState.stateValue.isNeonatalDeath(outcomeState.enum)
    ) {
      PerinatalNeonatalDeathList(
        causesOfNeonatalDeath,
        onCausesChanged = {
          causesOfNeonatalDeath = if (!it.areAllFieldsFalse && it.notReported) {
            it.copy(notReported = false)
          } else {
            it
          }
        }
      )
    }

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
fun PerinatalNeonatalDeathList(
  causesOfNeonatalDeath: CausesOfNeonatalDeath?,
  onCausesChanged: (newCauses: CausesOfNeonatalDeath) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  Column(modifier) {
    @Composable
    fun CheckboxTextRow(
      label: String,
      checked: Boolean,
      onCheckedChange: (Boolean) -> Unit,
      modifier: Modifier = Modifier,
      enabled: Boolean = true,
    ) = Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
      val interactionSource = remember { MutableInteractionSource() }
      Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        interactionSource = interactionSource,
        enabled = enabled,
      )
      Text(
        label,
        Modifier
          .clickable(
            indication = null,
            interactionSource = interactionSource,
            enabled = enabled
          ) { onCheckedChange(!checked) }
          .fillMaxWidth()
      )
    }

    // refactoring this is a bit pointless without reflection
    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.respiratoryDistressSyndrome == true,
      onCheckedChange = {
        onCausesChanged(
          causesOfNeonatalDeath?.copy(respiratoryDistressSyndrome = it) ?: CausesOfNeonatalDeath(respiratoryDistressSyndrome = it)
        )
      },
      label = "Respiratory distress syndrome",
      enabled = enabled,
    )
    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.birthAsphyxia == true,
      onCheckedChange = {
        onCausesChanged(
          causesOfNeonatalDeath?.copy(birthAsphyxia = it) ?: CausesOfNeonatalDeath(birthAsphyxia = it)
        )
      },
      label = "Birth asphyxia",
      enabled = enabled,
    )

    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.sepsis == true,
      onCheckedChange = {
        onCausesChanged(
          causesOfNeonatalDeath?.copy(sepsis = it) ?: CausesOfNeonatalDeath(sepsis = it)
        )
      },
      label = "Sepsis",
      enabled = enabled,
    )

    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.pneumonia == true,
      onCheckedChange = {
        onCausesChanged(causesOfNeonatalDeath?.copy(pneumonia = it) ?: CausesOfNeonatalDeath(pneumonia = it))
      },
      label = "Pneumonia",
      enabled = enabled,
    )

    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.meningitis == true,
      onCheckedChange = {
        onCausesChanged(causesOfNeonatalDeath?.copy(meningitis = it) ?: CausesOfNeonatalDeath(meningitis = it))
      },
      label = "Meningitis",
      enabled = enabled,
    )

    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.malaria == true,
      onCheckedChange = {
        onCausesChanged(
          causesOfNeonatalDeath?.copy(malaria = it) ?: CausesOfNeonatalDeath(malaria = it)
        )
      },
      label = "Malaria",
      enabled = enabled,
    )

    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.majorCongenitialMalformation == true,
      onCheckedChange = {
        onCausesChanged(
          causesOfNeonatalDeath?.copy(majorCongenitialMalformation = it) ?: CausesOfNeonatalDeath(majorCongenitialMalformation = it)
        )
      },
      label = "Major congenital malformation",
      enabled = enabled,
    )
    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.prematurity == true,
      onCheckedChange = {
        onCausesChanged(
          causesOfNeonatalDeath?.copy(prematurity = it) ?: CausesOfNeonatalDeath(prematurity = it)
        )
      },
      label = "Prematurity",
      enabled = enabled,
    )
    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.causeNotEstablished == true,
      onCheckedChange = {
        onCausesChanged(
          causesOfNeonatalDeath?.copy(causeNotEstablished = it)
            ?: CausesOfNeonatalDeath(causeNotEstablished = it)
        )
      },
      label = "Cause not established",
      enabled = enabled,
    )

    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.other == true,
      onCheckedChange = {
        onCausesChanged(
          causesOfNeonatalDeath?.copy(other = it) ?: CausesOfNeonatalDeath(other = it)
        )
      },
      label = "Other",
      enabled = enabled,
    )

    CheckboxTextRow(
      checked = causesOfNeonatalDeath?.notReported == true,
      onCheckedChange = {
        // don't copy to clear out everything else
        onCausesChanged(CausesOfNeonatalDeath(notReported = it))
      },
      label = "Not reported",
      enabled = enabled,
    )
  }
}

@Composable
fun BirthWeightForm(
  birthWeightState: EnumIdOnlyState,
  isNotReported: MutableState<Boolean>,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    EnumDropdownMenuIdOnly(
      currentSelection = birthWeightState.stateValue,
      onSelect = { birthWeightState.stateValue = it },
      serverEnum = birthWeightState.enum!!,
      label = { RequiredText(stringResource(R.string.birthweight_selection_label), required = birthWeightState.isMandatory) },
      enabled = !isNotReported.value,
      errorHint = birthWeightState.getError(),
      textModifier = Modifier.fillMaxWidth()
    )

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Checkbox(
        checked = isNotReported.value,
        onCheckedChange = { newState ->
          isNotReported.value = newState
          if (newState) {
            birthWeightState.stateValue = null
          }
        },
      )
      Text(stringResource(R.string.outcomes_not_reported))
    }
  }
}

@Composable
fun AgeAtDeliveryForm(
  ageAtDeliveryState: EnumIdOnlyState,
  isNotReported: MutableState<Boolean>,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    EnumDropdownMenuIdOnly(
      currentSelection = ageAtDeliveryState.stateValue,
      onSelect = { ageAtDeliveryState.stateValue = it },
      serverEnum = ageAtDeliveryState.enum!!,
      label = {
        RequiredText(stringResource(R.string.age_at_delivery_selection_label), required = ageAtDeliveryState.isMandatory)
      },
      enabled = !isNotReported.value,
      errorHint = ageAtDeliveryState.getError(),
      textModifier = Modifier.fillMaxWidth()
    )

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Checkbox(
        checked = isNotReported.value,
        onCheckedChange = { newState ->
          isNotReported.value = newState
          if (newState) {
            ageAtDeliveryState.stateValue = null
          }
        },
      )
      Text(stringResource(R.string.outcomes_not_reported))
    }
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
        didTheWomanFitState = NullableToggleState(
          remember { mutableStateOf(true) },
          remember { mutableStateOf(null) },
          true
        ),
        whenWasFirstFitState = EnumIdOnlyState(
          defaultEnums[DropdownType.EclampticFitTime],
          isMandatory = false,
          isFormDraftState = draft
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
  extraOnItemSelected: (previous: District?, new: District) -> Unit = { _, _ -> },
) = DatabasePagingListDropdown(
  selectedItem = state.stateValue?.district,
  positionInList = state.stateValue?.position,
  onItemSelected = { idx, district ->
    val old = state.stateValue?.district
    val new = DistrictAndPosition(district, idx)
    state.stateValue = new
    extraOnItemSelected(old, district)
  },
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
  textFieldModifier = textFieldModifier,
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

