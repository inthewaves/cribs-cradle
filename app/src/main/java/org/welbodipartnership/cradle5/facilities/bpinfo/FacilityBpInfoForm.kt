package org.welbodipartnership.cradle5.facilities.bpinfo

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.resultentities.BpInfoFacilityDistrict
import org.welbodipartnership.cradle5.patients.details.BaseDetailsCard
import org.welbodipartnership.cradle5.patients.form.OtherCard
import org.welbodipartnership.cradle5.patients.form.SaveButtonCard
import org.welbodipartnership.cradle5.ui.composables.CategoryHeader
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.forms.DateOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.DistrictAndFacilityFormPair
import org.welbodipartnership.cradle5.ui.composables.forms.IntegerField
import org.welbodipartnership.cradle5.ui.composables.forms.RequiredText
import org.welbodipartnership.cradle5.ui.composables.forms.formDateToTimestampMapper
import org.welbodipartnership.cradle5.ui.composables.forms.timestampToFormDateMapper
import org.welbodipartnership.cradle5.util.datetime.FormDate

@Composable
fun FacilityBpInfoForm(
  onNavigateBack: () -> Unit,
  onNavigateToFacilityAfterSaving: (facilityPk: Long) -> Unit,
  viewModel: FacilityBpInfoFormViewModel = hiltViewModel()
) {
  val formState = viewModel.formState.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  val context = LocalContext.current
  val resources = context.resources
  LaunchedEffect(formState.value) {
    when (val state = formState.value) {
      is FacilityBpInfoFormViewModel.FormState.FailedValidation -> {
        val totalErrors = state.errorsBySectionStringId.asSequence()
          .flatMap { it.value }
          .count()
        snackbarHostState.showSnackbar(
          resources.getQuantityString(
            R.plurals.form_snackbar_failed_to_save_there_are_d_errors,
            totalErrors,
            totalErrors,
          )
        )
      }
      is FacilityBpInfoFormViewModel.FormState.FailedException -> {
        snackbarHostState.showSnackbar(
          context.getString(R.string.form_snackbar_failed_to_save_exception)
        )
      }
      else -> {}
    }
  }

  var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }
  BackHandler(
    enabled = formState.value !is FacilityBpInfoFormViewModel.FormState.FailedLoading
  ) { showUnsavedChangesDialog = true }
  if (showUnsavedChangesDialog) {
    AlertDialog(
      onDismissRequest = { showUnsavedChangesDialog = false },
      title = {
        if (formState.value.isForFormEdit) {
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

  val existingForm: BpInfoFacilityDistrict? by viewModel.existingForm.collectAsState(initial = null)

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
          if (viewModel.isExistingEdit) {
            Column {
              existingForm.let { existingInfo ->
                Text(stringResource(R.string.bp_info_title_edit))
                existingInfo?.facility?.name?.let { facilityName ->
                  Text(facilityName, style = MaterialTheme.typography.subtitle2)
                }
              }
            }
          } else {
            Text(stringResource(R.string.bp_info_title_new))
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
    val textFieldToTextFieldHeight = 8.dp
    val categoryToCategorySpacerHeight = 16.dp

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LazyColumn(
      contentPadding = padding,
      modifier = Modifier.navigationBarsWithImePadding()
    ) {
      item {
        BaseDetailsCard(title = null, Modifier.padding(16.dp)) {
          existingForm?.bpInfo?.serverErrorMessage?.let { serverErrorMessage ->
            LabelAndValueOrNone(stringResource(R.string.errors_from_sync_label), serverErrorMessage)
            Spacer(Modifier.height(categoryToCategorySpacerHeight))
          }

          val fields = viewModel.formFields
          DistrictAndFacilityFormPair(
            districtState = fields.district,
            districtLabel = { RequiredText(stringResource(R.string.bp_info_district_label)) },
            facilityState = fields.facility,
            facilityCustomTextState = null,
            facilityLabel = { RequiredText(stringResource(R.string.bp_info_facility_label)) },
            viewModel.districtsPagerFlow,
            { district -> viewModel.getFacilitiesPagingDataForDistrict(district) },
            textFieldToTextFieldHeight,
            enabled = viewModel.canChangeFacility
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          DateOutlinedTextField(
            text = fields.dataCollectionDate.stateValue,
            onValueChange = { fields.dataCollectionDate.stateValue = it },
            timestampToDateStringMapper = timestampToFormDateMapper,
            dateStringToTimestampMapper = formDateToTimestampMapper,
            maxLength = FormDate.MAX_STRING_LEN_NO_SLASHES,
            onPickerClose = { fields.dataCollectionDate.enableShowErrors(force = true) },
            label = { Text(stringResource(id = R.string.bp_info_date_of_data_collection_label)) },
            modifier = Modifier.fillMaxWidth(),
            textFieldModifier = fields.dataCollectionDate.createFocusChangeModifier().fillMaxWidth(),
            errorHint = fields.dataCollectionDate.getError(),
            keyboardOptions = KeyboardOptions.Default,
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          val commonKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
          Spacer(Modifier.height(textFieldToTextFieldHeight))
          IntegerField(
            field = fields.numBpReadingsTakenInFacilitySinceLastVisit,
            label = stringResource(R.string.bp_info_bp_readings_in_facility_today_since_last_visited_label),
            keyboardOptions = commonKeyboardOptions
          )
          Spacer(Modifier.height(textFieldToTextFieldHeight))
          IntegerField(
            field = fields.numBpReadingsEndIn0Or5,
            label = stringResource(R.string.bp_info_bp_readings_end_in_a_0_or_a_5_label),
            keyboardOptions = commonKeyboardOptions
          )
          Spacer(Modifier.height(textFieldToTextFieldHeight))
          val focusManager = LocalFocusManager.current
          IntegerField(
            field = fields.numBpReadingsWithAssociatedColorAndArrow,
            label = stringResource(R.string.bp_info_bp_readings_have_color_or_arrow_label),
            keyboardOptions = commonKeyboardOptions.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
          )
        }
      }

      item {
        formState.value.let { currentFormState ->
          val detailsContent: @Composable (ColumnScope.() -> Unit)? = when (currentFormState) {
            is FacilityBpInfoFormViewModel.FormState.FailedValidation -> {
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
            is FacilityBpInfoFormViewModel.FormState.FailedException -> {
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
                if (existingForm?.bpInfo?.isUploadedToServer != true) {
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
        val (isDraft, setIsDraft) = viewModel.formFields.isDraft
        val (localNotes, setLocalNotes) = viewModel.formFields.localNotes
        OtherCard(
          hideDraft = existingForm?.bpInfo?.isUploadedToServer == true,
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
          isEnabled = formState.value !is FacilityBpInfoFormViewModel.FormState.Loading &&
            formState.value !is FacilityBpInfoFormViewModel.FormState.Saving,
          onSaveButtonClick = {
            focusManager.clearFocus()
            viewModel.save()
          },
          text = if (viewModel.isExistingEdit) {
            stringResource(R.string.bp_info_save_edits_button)
          } else {
            stringResource(R.string.bp_info_save_new_form_button)
          }
        )
      }
    }

    formState.value.let { currentFormState ->
      when (currentFormState) {
        is FacilityBpInfoFormViewModel.FormState.SavedEditsToExistingForm -> {
          LaunchedEffect(null) {
            // we need to go to the facility page; that's where it's displayed
            onNavigateToFacilityAfterSaving(currentFormState.primaryKeyOfFacility)
          }
        }
        is FacilityBpInfoFormViewModel.FormState.SavedNewForm -> {
          LaunchedEffect(null) {
            onNavigateToFacilityAfterSaving(currentFormState.primaryKeyOfFacility)
          }
        }
        else -> {}
      }
    }
  }
}
