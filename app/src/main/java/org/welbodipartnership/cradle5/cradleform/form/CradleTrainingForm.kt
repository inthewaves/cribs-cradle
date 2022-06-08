package org.welbodipartnership.cradle5.cradleform.form

import android.content.Context
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
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
import org.welbodipartnership.cradle5.cradleform.details.BaseDetailsCard
import org.welbodipartnership.cradle5.cradleform.details.CategoryHeader
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.PowerSupply
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.database.resultentities.CradleTrainingFormFacilityDistrict
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.forms.DatabasePagingListDropdown
import org.welbodipartnership.cradle5.ui.composables.forms.DateOutlinedTextField
import org.welbodipartnership.cradle5.ui.composables.forms.FieldState
import org.welbodipartnership.cradle5.ui.composables.forms.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.composables.forms.TextFieldState
import org.welbodipartnership.cradle5.ui.composables.forms.darkerDisabledOutlinedTextFieldColors
import org.welbodipartnership.cradle5.ui.composables.forms.formDateToTimestampMapper
import org.welbodipartnership.cradle5.ui.composables.forms.timestampToFormDateMapper
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate
import org.welbodipartnership.cradle5.util.datetime.toFormDateFromNoSlashesOrNull
import org.welbodipartnership.cradle5.util.datetime.toFormDateFromNoSlashesOrThrow

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
fun RequiredText(
  text: String,
  modifier: Modifier = Modifier,
  required: Boolean = true,
  enabled: Boolean = true
) {
  val previous = LocalContentAlpha.current
  CompositionLocalProvider(LocalContentAlpha provides if (enabled) previous else ContentAlpha.disabled) {
    if (required && enabled) {
      Text(text.withRequiredStar(), modifier = modifier)
    } else {
      Text(text, modifier = modifier)
    }
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
fun IntegerField(
  field: TextFieldState,
  label: String,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  textStyle: TextStyle = MaterialTheme.typography.body2,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = true,
  maxLines: Int = Int.MAX_VALUE,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  shape: Shape = MaterialTheme.shapes.small,
  colors: TextFieldColors = darkerDisabledOutlinedTextFieldColors()
) {
  OutlinedTextFieldWithErrorHint(
    value = field.stateValue,
    onValueChange = { newValue -> field.stateValue = newValue },
    modifier = modifier,
    textFieldModifier = textFieldModifier,
    enabled = enabled,
    readOnly = readOnly,
    textStyle = textStyle,
    label = { RequiredText(label, required = field.isMandatory) },
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    errorHint = field.getError(),
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    maxLines,
    interactionSource,
    shape,
    colors
  )
}

@Composable
fun CradleTrainingForm(
  serverEnumCollection: ServerEnumCollection,
  onNavigateBack: () -> Unit,
  onNavigateToCompleteForm: (primaryKey: Long) -> Unit,
  viewModel: CradleTrainingFormViewModel = hiltViewModel()
) {
  val formState = viewModel.formState.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  val context = LocalContext.current
  val resources = context.resources
  LaunchedEffect(formState.value) {
    when (val state = formState.value) {
      is CradleTrainingFormViewModel.FormState.FailedValidation -> {
        val totalErrors = state.errorsBySectionStringId.asSequence()
          .flatMap { it.value }
          .count()
        snackbarHostState.showSnackbar(
          resources.getQuantityString(
            R.plurals.cradle_form_snackbar_failed_to_save_there_are_d_errors,
            totalErrors,
            totalErrors,
          )
        )
      }
      is CradleTrainingFormViewModel.FormState.FailedException -> {
        snackbarHostState.showSnackbar(
          context.getString(R.string.cradle_form_snackbar_failed_to_save_exception)
        )
      }
      else -> {}
    }
  }

  var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }
  BackHandler(
    enabled = formState.value !is CradleTrainingFormViewModel.FormState.FailedLoading
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

  val existingCradleForm: CradleTrainingFormFacilityDistrict? by viewModel.existingCradleTrainingForm.collectAsState(initial = null)

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
              existingCradleForm.let { existingInfo ->
                if (existingInfo != null) {
                  Text(stringResource(R.string.edit_cradle_form_title))
                  existingInfo.facility?.name?.let { facilityName ->
                    Text(facilityName, style = MaterialTheme.typography.subtitle2)
                  }
                } else {
                  Text(stringResource(R.string.edit_cradle_form_title))
                }
              }
            }
          } else {
            Text(stringResource(R.string.new_cradle_form_title))
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
        BaseDetailsCard(
          stringResource(R.string.cradle_form_title),
          Modifier.padding(16.dp)
        ) {
          existingCradleForm?.form?.serverErrorMessage?.let { serverErrorMessage ->
            LabelAndValueOrNone(stringResource(R.string.errors_from_sync_label), serverErrorMessage)
            Spacer(Modifier.height(categoryToCategorySpacerHeight))
          }

          val fields = viewModel.formFieldsNew

          DistrictAndFacilityFormPair(
            districtState = fields.district,
            districtLabel = { RequiredText(stringResource(R.string.cradle_form_district_label)) },
            facilityState = fields.facility,
            facilityCustomTextState = null,
            facilityLabel = { RequiredText(stringResource(R.string.cradle_form_healthcare_facility_label)) },
            viewModel.districtsPagerFlow,
            { district -> viewModel.getFacilitiesPagingDataForDistrict(district) },
            textFieldToTextFieldHeight
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          DateOutlinedTextField(
            text = fields.dateOfTraining.stateValue,
            onValueChange = { fields.dateOfTraining.stateValue = it },
            timestampToDateStringMapper = timestampToFormDateMapper,
            dateStringToTimestampMapper = formDateToTimestampMapper,
            maxLength = FormDate.MAX_STRING_LEN_NO_SLASHES,
            onPickerClose = { fields.dateOfTraining.enableShowErrors(force = true) },
            label = { Text(stringResource(id = R.string.cradle_form_date_of_training_label)) },
            modifier = Modifier.fillMaxWidth(),
            textFieldModifier = fields.dateOfTraining
              .createFocusChangeModifier()
              .fillMaxWidth(),
            errorHint = fields.dateOfTraining.getError(),
            keyboardOptions = KeyboardOptions.Default,
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight * 2))

          CategoryHeader(stringResource(R.string.cradle_form_today_during_the_cradle_training_subtitle))

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          val commonIntFieldKeyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Number
          )
          val commonKeyboardActions = KeyboardActions.Default

          IntegerField(
            field = fields.numOfBpDevicesFunction,
            label = stringResource(R.string.cradle_form_number_of_functioning_bp_devices_label),
            textFieldModifier = fields.numOfBpDevicesFunction
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.numOfCradleDevicesFunctioning,
            label = stringResource(R.string.cradle_form_number_of_functioning_cradle_devices_label),
            textFieldModifier = fields.numOfCradleDevicesFunctioning
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.numOfCradleDevicesBroken,
            label = stringResource(R.string.cradle_form_number_of_broken_cradle_devices_label),

            textFieldModifier = fields.numOfCradleDevicesBroken
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          Text(stringResource(R.string.cradle_form_power_supply_label),)

          PowerSupplyList(
            powerSupply = fields.powerSupply.value,
            onPowerSupplyChanged = { fields.powerSupply.value = it },
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffWorking,
            label = stringResource(R.string.cradle_form_total_staff_working_at_facility_label),
            textFieldModifier = fields.totalStaffWorking
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )
          IntegerField(
            field = fields.totalStaffProvidingMaternityServices,
            label = stringResource(R.string.cradle_form_total_staff_providing_maternity_services_at_facility_label),
            textFieldModifier = fields.totalStaffProvidingMaternityServices
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          CategoryHeader(stringResource(R.string.cradle_form_staff_trained_title))

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedToday,
            label = stringResource(R.string.cradle_form_total_staff_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedToday
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))
          IntegerField(
            field = fields.totalStaffTrainedTodayDoctors,
            label = stringResource(R.string.cradle_form_total_doctors_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodayDoctors
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedTodayMidwives,
            label = stringResource(R.string.cradle_form_total_midwives_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodayMidwives
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedTodaySACHOS,
            label = stringResource(R.string.cradle_form_total_SACHOS_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodaySACHOS
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedTodaySECHNMidwives,
            label = stringResource(R.string.cradle_form_total_SECHN_midwives_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodaySECHNMidwives
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedTodaySRNs,
            label = stringResource(R.string.cradle_form_total_SRNs_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodaySRNs
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedTodayCHOs,
            label = stringResource(R.string.cradle_form_total_CHOs_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodayCHOs
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedTodayCHAs,
            label = stringResource(R.string.cradle_form_total_CHAs_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodayCHAs
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedTodayCSECHNs,
            label = stringResource(R.string.cradle_form_total_CSECHNs_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodayCSECHNs
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedTodayMCHAides,
            label = stringResource(R.string.cradle_form_total_MCH_aides_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodayMCHAides
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedTodayTBA,
            label = stringResource(R.string.cradle_form_total_TBA_trained_today_label),
            textFieldModifier = fields.totalStaffTrainedTodayTBA
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          IntegerField(
            field = fields.totalStaffTrainedBefore,
            label = stringResource(R.string.cradle_form_total_trained_before_label),
            textFieldModifier = fields.totalStaffTrainedBefore
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions,
            keyboardActions = commonKeyboardActions
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))

          val focusManager = LocalFocusManager.current
          IntegerField(
            field = fields.totalStaffTrainedScoredMoreThan8,
            label = stringResource(R.string.cradle_form_total_trained_score_more_than_8_label),
            textFieldModifier = fields.totalStaffTrainedScoredMoreThan8
              .createFocusChangeModifier()
              .bringIntoViewRequester(bringIntoViewRequester)
              .fillMaxWidth(),
            keyboardOptions = commonIntFieldKeyboardOptions.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
          )

          Spacer(Modifier.height(textFieldToTextFieldHeight))
        }
      }

      item {
        formState.value.let { currentFormState ->
          val detailsContent: @Composable (ColumnScope.() -> Unit)? = when (currentFormState) {
            is CradleTrainingFormViewModel.FormState.FailedValidation -> {
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
            is CradleTrainingFormViewModel.FormState.FailedException -> {
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
                if (existingCradleForm?.form?.isUploadedToServer != true) {
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
        val (isDraft, setIsDraft) = viewModel.formFieldsNew.isDraft
        val (localNotes, setLocalNotes) = viewModel.formFieldsNew.localNotes
        OtherCard(
          hideDraft = existingCradleForm?.form?.isUploadedToServer == true,
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
          isEnabled = formState.value !is CradleTrainingFormViewModel.FormState.Loading &&
            formState.value !is CradleTrainingFormViewModel.FormState.Saving,
          onSaveButtonClick = {
            focusManager.clearFocus()
            viewModel.save()
          },
          text = if (viewModel.isExistingEdit) {
            stringResource(id = R.string.cradle_form_save_edits)
          } else {
            stringResource(R.string.cradle_form_save_new_form_button)
          }
        )
      }
    }

    formState.value.let { currentFormState ->
      when (currentFormState) {
        is CradleTrainingFormViewModel.FormState.SavedEditsToExistingPatient -> {
          LaunchedEffect(null) {
            onNavigateToCompleteForm(currentFormState.primaryKeyOfForm)
          }
        }
        is CradleTrainingFormViewModel.FormState.SavedNewPatient -> {
          LaunchedEffect(null) {
            onNavigateToCompleteForm(currentFormState.primaryKeyOfForm)
          }
        }
        else -> {}
      }
    }
  }
}

@Composable
fun PowerSupplyList(
  powerSupply: PowerSupply?,
  onPowerSupplyChanged: (new: PowerSupply) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  Column(modifier) {
    // refactoring this is a bit pointless without reflection
    CheckboxTextRow(
      checked = powerSupply?.generator == true,
      onCheckedChange = {
        onPowerSupplyChanged(powerSupply?.copy(generator = it, none = false) ?: PowerSupply(generator = it))
      },
      label = stringResource(R.string.cradle_form_power_supply_generator),
      enabled = enabled,
    )
    CheckboxTextRow(
      checked = powerSupply?.solar == true,
      onCheckedChange = {
        onPowerSupplyChanged(powerSupply?.copy(solar = it, none = false) ?: PowerSupply(solar = it))
      },
      label = stringResource(R.string.cradle_form_power_supply_solar),
      enabled = enabled,
    )
    CheckboxTextRow(
      checked = powerSupply?.grid == true,
      onCheckedChange = {
        onPowerSupplyChanged(powerSupply?.copy(grid = it, none = false) ?: PowerSupply(grid = it))
      },
      label = stringResource(R.string.cradle_form_power_supply_grid),
      enabled = enabled,
    )
    CheckboxTextRow(
      checked = powerSupply?.none == true,
      onCheckedChange = {
        // don't copy to clear out everything else
        onPowerSupplyChanged(PowerSupply(none = it))
      },
      label = stringResource(R.string.cradle_form_power_supply_none),
      enabled = enabled,
    )
  }
}

/**
 * If [facilityCustomTextState] is null, the OTHER option won't be available.
 */
@Composable
fun DistrictAndFacilityFormPair(
  districtState: DistrictState,
  districtLabel: @Composable (() -> Unit)?,
  facilityState: HealthcareFacilityState,
  facilityCustomTextState: NonEmptyTextState?,
  facilityLabel: @Composable (() -> Unit)?,
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
        facilityCustomTextState?.reset()
      }
    },
  )

  Spacer(Modifier.height(textFieldToTextFieldHeight))

  if (facilityCustomTextState != null && districtState.stateValue?.district?.isOther == true) {
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

@Preview
@Composable
fun PatientFormPreview() {
  CradleTrialAppTheme {
    Scaffold {
      CradleTrainingForm(ServerEnumCollection.defaultInstance, onNavigateBack = {}, onNavigateToCompleteForm = {})
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
      stringResource(R.string.district_dialog_title),
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
      stringResource(R.string.health_facility_dialog_title),
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

class LimitedIntStateWithStateUpperBound(
  isMandatory: Boolean,
  val limit: IntRange,
  backingState: MutableState<String> = mutableStateOf(""),
  isFormDraftState: State<Boolean?>,
  stateUpperBound: TextFieldState,
  @StringRes upperBoundErrorString: Int,
) : TextFieldState(
  validator = { possibleInt ->
    run {
      if ((isFormDraftState.value == true || !isMandatory) && possibleInt.isBlank()) return@run true
      val age = possibleInt.toIntOrNull() ?: return@run false
      if (age !in limit) return@run false
      val upper = stateUpperBound.stateValue.toIntOrNull()
      if (upper == null) true else age <= upper
    }
  },
  errorFor = { ctx, possibleInt ->
    run {
      val age = possibleInt.toIntOrNull() ?: return@run ctx.getString(R.string.input_must_be_in_range_d_and_d, limit.first, limit.last)
      if (age !in limit) return@run ctx.getString(R.string.input_must_be_in_range_d_and_d, limit.first, limit.last)
      val upper = stateUpperBound.stateValue.toIntOrNull()
      if (upper == null) {
        ctx.getString(R.string.input_must_be_in_range_d_and_d, limit.first, limit.last)
      } else {
        ctx.getString(upperBoundErrorString)
      }
    }
  },
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory
)

class LimitedIntState(
  isMandatory: Boolean,
  val limit: IntRange,
  backingState: MutableState<String> = mutableStateOf(""),
  isFormDraftState: State<Boolean?>,
) : TextFieldState(
  validator = { possibleInt ->
    run {
      if ((isFormDraftState.value == true || !isMandatory) && possibleInt.isBlank()) return@run true
      val age = possibleInt.toIntOrNull() ?: return@run false
      age in limit
    }
  },
  errorFor = { ctx, _ -> ctx.getString(R.string.input_must_be_in_range_d_and_d, limit.first, limit.last) },
  backingState = backingState,
  isFormDraftState = isFormDraftState,
  isMandatory = isMandatory
)

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
