package org.welbodipartnership.cradle5.patients.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.compose.rememberFlowWithLifecycle
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.database.resultentities.ListPatient
import org.welbodipartnership.cradle5.data.database.resultentities.ListPatientAndOutcomeError
import org.welbodipartnership.cradle5.home.AccountInfoButton
import org.welbodipartnership.cradle5.ui.composables.AnimatedVisibilityFadingWrapper
import org.welbodipartnership.cradle5.ui.composables.carousel.Carousel
import org.welbodipartnership.cradle5.ui.composables.forms.DatabasePagingListDropdown
import org.welbodipartnership.cradle5.ui.composables.forms.FixLongPressExposedDropdownMenuBox
import org.welbodipartnership.cradle5.ui.composables.forms.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.composables.forms.darkerDisabledOutlinedTextFieldColors
import org.welbodipartnership.cradle5.ui.composables.screenlists.ScreenListItem
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate

@Composable
fun PatientsListScreen(
  onOpenPatientDetails: (patientPrimaryKey: Long) -> Unit,
  onOpenNewPatientCreation: () -> Unit,
) {
  PatientsListScreen(
    viewModel = hiltViewModel(),
    onOpenPatientDetails = onOpenPatientDetails,
    onOpenNewPatientCreation = onOpenNewPatientCreation
  )
}

/**
 * Returns whether the lazy list is currently scrolling up.
 * From Google's Animation code lab.
 */
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
  var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
  var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
  return remember(this) {
    derivedStateOf {
      if (previousIndex != firstVisibleItemIndex) {
        previousIndex > firstVisibleItemIndex
      } else {
        previousScrollOffset >= firstVisibleItemScrollOffset
      }.also {
        previousIndex = firstVisibleItemIndex
        previousScrollOffset = firstVisibleItemScrollOffset
      }
    }
  }.value
}

@Composable
private fun PatientsListScreen(
  viewModel: PatientsListViewModel,
  onOpenPatientDetails: (patientPrimaryKey: Long) -> Unit,
  onOpenNewPatientCreation: () -> Unit,
) {
  val lazyListState = rememberLazyListState()

  var showFilterDialog by rememberSaveable { mutableStateOf(false) }
  if (showFilterDialog) {
    val (selectedOption, onOptionSelected) = remember {
      mutableStateOf(viewModel.filterOption.value)
    }
    AlertDialog(
      onDismissRequest = { showFilterDialog = false },
      confirmButton = {
        TextButton(
          onClick = {
            showFilterDialog = false
            viewModel.filterOption.value = selectedOption
          }
        ) { Text(stringResource(R.string.filter_button_ok)) }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showFilterDialog = false
            viewModel.filterOption.value = PatientsListViewModel.FilterOption.None
          }
        ) { Text(stringResource(R.string.filter_button_clear_filter)) }
      },
      title = { Text("Select a filtering option") },
      text = {
        // https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#RadioButton(kotlin.Boolean,kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.material.RadioButtonColors)
        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
        Column(Modifier.selectableGroup()) {
          PatientsListViewModel.FilterOption.defaultButtonsList.forEach { currentOpt ->
            Row(
              Modifier
                .fillMaxWidth()
                .height(56.dp)
                .selectable(
                  selected = currentOpt == selectedOption,
                  onClick = { onOptionSelected(currentOpt) },
                  role = Role.RadioButton
                )
                .padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = currentOpt == selectedOption,
                onClick = null // null recommended for accessibility with screen readers
              )
              Text(
                text = stringResource(currentOpt.selectionStringResId),
                style = MaterialTheme.typography.body1.merge(),
                modifier = Modifier.padding(start = 16.dp)
              )
              currentOpt.icon?.let {
                Icon(
                  imageVector = it,
                  contentDescription = stringResource(currentOpt.selectionStringResId),
                  modifier = Modifier.padding(start = 8.dp)
                )
              }
            }
          }

          DatabasePagingListDropdown(
            selectedItem = (selectedOption as? PatientsListViewModel.FilterOption.ByFacility)
              ?.facility,
            positionInList = (selectedOption as? PatientsListViewModel.FilterOption.ByFacility)
              ?.position,
            onItemSelected = { idx, facility ->
              onOptionSelected(PatientsListViewModel.FilterOption.ByFacility(facility, idx))
            },
            pagingItemFlow = viewModel.selfFacilityPagingFlow,
            formatTextForListItem = Facility::name,
            title = { Text(stringResource(R.string.patient_registration_district_dialog_title)) },
            label = { Text(stringResource(R.string.patients_list_filter_option_facility)) },
            errorHint = null
          )

          MonthDropdownMenu(
            currentMonthNumber = (selectedOption as? PatientsListViewModel.FilterOption.Month)
              ?.monthOneBased,
            onSelect = { selectedMonthNumber ->
              if (selectedMonthNumber != null) {
                onOptionSelected(PatientsListViewModel.FilterOption.Month(selectedMonthNumber))
              }
            },
            label = { Text(stringResource(R.string.patients_list_filter_option_month)) },
            errorHint = null
          )
        }
      }
    )
  }

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
        title = { Text(text = stringResource(R.string.patients_title)) },
        actions = { AccountInfoButton() }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = onOpenNewPatientCreation) {
        Row(Modifier.padding(16.dp)) {
          Icon(Icons.Filled.Add, stringResource(R.string.patients_list_add_new_button))

          AnimatedVisibility(visible = lazyListState.isScrollingUp()) {
            Text(
              text = stringResource(R.string.patients_list_add_new_button),
              modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
          }
        }
      }
    }
  ) { padding ->
    Column(Modifier.padding(padding)) {
      val lazyPagingItems = rememberFlowWithLifecycle(
        viewModel.patientsPagerFlow, minActiveState = Lifecycle.State.RESUMED
      ).collectAsLazyPagingItems()
      val patientsCount by rememberFlowWithLifecycle(
        viewModel.patientsCountFlow, minActiveState = Lifecycle.State.RESUMED
      ).collectAsState(initial = null)

      Row {
        Button(onClick = { showFilterDialog = true }) {
          val currentFilterOption by viewModel.filterOption.collectAsState()
          currentFilterOption.let { nowOption -> // smart casting
            Text(
              stringResource(
                R.string.facilities_list_filter_by_s_button,
                when (nowOption) {
                  is PatientsListViewModel.FilterOption.Month -> stringResource(
                    R.string.patients_list_filter_by_month_s,
                    getMonthNameFromNumber(nowOption.monthOneBased)
                  )
                  is PatientsListViewModel.FilterOption.ByFacility -> stringResource(
                    R.string.patients_list_filter_by_facility_s,
                    nowOption.facility.name ?: "ID ${nowOption.facility.id}"
                  )
                  else -> stringResource(currentFilterOption.selectionStringResId)
                }
              )
            )
          }
        }
      }
      PatientListHeader()
      Box(Modifier.fillMaxSize()) {
        AnimatedVisibilityFadingWrapper(
          visible = lazyPagingItems.loadState.refresh is LoadState.Loading
        ) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }

        AnimatedVisibilityFadingWrapper(
          modifier = Modifier.align(Alignment.Center),
          visible = lazyPagingItems.loadState.refresh !is LoadState.Loading &&
            patientsCount == 0
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              stringResource(R.string.patient_list_no_patients_available),
              textAlign = TextAlign.Center
            )
            Text(
              stringResource(R.string.patient_list_no_patients_app_doesnt_download_from_medscinet),
              textAlign = TextAlign.Center
            )
          }
        }

        AnimatedVisibilityFadingWrapper(
          visible = lazyPagingItems.loadState.refresh !is LoadState.Loading
        ) {
          LazyColumn(state = lazyListState) {
            items(lazyPagingItems) { listPatient ->
              if (listPatient != null) {
                PatientListItem(listPatient, onClick = { onOpenPatientDetails(it.id) })
              } else {
                PatientListItemPlaceholder()
              }
            }
          }
        }
        Column(
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Carousel(
            state = lazyListState,
            modifier = Modifier
              .size(4.dp, 120.dp)
              .weight(1f)
          )
        }
      }
    }
  }
}

fun getMonthNameFromNumber(monthNumber: Int?) = when (monthNumber) {
  1 -> "January"
  2 -> "February"
  3 -> "March"
  4 -> "April"
  5 -> "May"
  6 -> "June"
  7 -> "July"
  8 -> "August"
  9 -> "September"
  10 -> "October"
  11 -> "November"
  12 -> "December"
  else -> ""
}

@Composable
private fun MonthDropdownMenu(
  currentMonthNumber: Int?,
  onSelect: (Int?) -> Unit,
  modifier: Modifier = Modifier,
  dropdownTextModifier: Modifier = Modifier,
  label: @Composable () -> Unit,
  errorHint: String?,
  enabled: Boolean = true,
  dropdownColors: TextFieldColors = darkerDisabledOutlinedTextFieldColors(),
) {
  var expanded by remember { mutableStateOf(false) }

  val onClick = {
    expanded = if (enabled) {
      // ensure that tapping on the menu again closes it
      !expanded
    } else {
      expanded
    }
  }
  FixLongPressExposedDropdownMenuBox(
    expanded = enabled && expanded,
    onExpandedChange = {
      // ensure that tapping on the menu again closes it
      expanded = if (expanded) false else it
    },
    enabled = enabled,
    modifier = modifier,
  ) {
    OutlinedTextFieldWithErrorHint(
      readOnly = true,
      value = getMonthNameFromNumber(currentMonthNumber),
      onValueChange = {},
      label = label,
      maxLines = 2,
      enabled = enabled,
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(
          expanded = enabled && expanded,
          onIconClick = onClick
        )
      },
      errorHint = errorHint,
      colors = dropdownColors,
      textFieldModifier = dropdownTextModifier,
      interactionSource = remember { MutableInteractionSource() }
        .also { interactionSource ->
          if (enabled) {
            LaunchedEffect(interactionSource) {
              interactionSource.interactions.collect {
                if (it is PressInteraction.Release) {
                  onClick()
                }
              }
            }
          }
        }
    )
    ExposedDropdownMenu(
      expanded = enabled && expanded,
      onDismissRequest = { expanded = false },
      modifier = dropdownTextModifier,
    ) {
      generateSequence(1) { it + 1 }
        .takeWhile { it in 1..12 }
        .forEach { monthNumber ->
          DropdownMenuItem(
            onClick = {
              expanded = false
              onSelect(monthNumber)
            },
          ) {
            Text(text = getMonthNameFromNumber(monthNumber))
          }
        }
    }
  }
}

@Composable
fun PatientListHeader(modifier: Modifier = Modifier) {
  Card(elevation = 1.dp) {
    BasePatientListItem(
      id = stringResource(R.string.patient_list_header_id),
      initials = stringResource(R.string.patient_list_header_initials),
      age = stringResource(R.string.patient_list_header_age),
      listIconType = ListIconType.DontShow,
      minHeight = 24.dp,
      textStyle = MaterialTheme.typography.subtitle2,
      onClick = null,
      modifier = modifier
    )
  }
}

@Composable
fun PatientListItem(
  listPatientAndOutcomeError: ListPatientAndOutcomeError,
  onClick: (listPatient: ListPatient) -> Unit,
  modifier: Modifier = Modifier
) {
  val listPatient = listPatientAndOutcomeError.listPatient

  val isPatientUploadedButOutcomeIsNot = listPatient.serverInfo?.objectId != null &&
    listPatientAndOutcomeError.outcomeError?.serverInfo?.nodeId == null

  BasePatientListItem(
    id = listPatient.serverInfo?.objectId?.toString() ?: stringResource(R.string.not_available_n_slash_a),
    initials = listPatient.initials,
    age = listPatient.dateOfBirth?.getAgeInYearsFromNow()?.toString() ?: stringResource(R.string.not_available_n_slash_a),
    listIconType = ListIconType.ShowIcons(
      isPatientUploaded = listPatient.serverInfo != null,
      hasLocalNotes = !listPatient.localNotes.isNullOrBlank(),
      isPatientDraft = listPatient.isDraft,
      hasErrors = !listPatient.serverErrorMessage.isNullOrBlank() ||
        !listPatientAndOutcomeError.outcomeError?.serverErrorMessage.isNullOrBlank() ||
        isPatientUploadedButOutcomeIsNot
    ),
    minHeight = 48.dp,
    textStyle = MaterialTheme.typography.body2,
    onClick = { onClick(listPatient) },
    modifier = modifier
  )
}

@Composable
fun PatientListItemPlaceholder(modifier: Modifier = Modifier) {
  ScreenListItem(
    minHeight = 48.dp,
    onClick = null,
    modifier = modifier,
    horizontalArrangement = Arrangement.Center
  ) {
    CircularProgressIndicator()
  }
}

@Immutable
sealed class ListIconType {
  @Immutable
  object DontShow : ListIconType()
  @Immutable
  data class ShowIcons(
    val isPatientDraft: Boolean,
    val hasLocalNotes: Boolean,
    val isPatientUploaded: Boolean,
    val hasErrors: Boolean,
  ) : ListIconType()
}

@Composable
private fun BasePatientListItem(
  id: String,
  initials: String,
  age: String,
  listIconType: ListIconType,
  minHeight: Dp,
  textStyle: TextStyle,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  ScreenListItem(
    minHeight = minHeight,
    onClick = onClick,
    modifier = modifier
  ) {
    Text(
      id,
      modifier = Modifier
        .weight(0.15f)
        .align(Alignment.CenterVertically),
      style = textStyle
    )
    Text(
      initials,
      modifier = Modifier
        .weight(0.2f)
        .align(Alignment.CenterVertically),
      style = textStyle
    )
    Text(
      age,
      modifier = Modifier
        .weight(0.2f)
        .align(Alignment.CenterVertically),
      style = textStyle
    )
    Row {
      Spacer(Modifier.width(5.dp))
      val hasErrorsIconAlpha = if (
        listIconType is ListIconType.ShowIcons && listIconType.hasErrors
      ) {
        1f
      } else {
        0f
      }
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.error) {
        Icon(
          imageVector = Icons.Filled.Error, contentDescription = "Error status",
          modifier = Modifier.alpha(hasErrorsIconAlpha)
        )
      }

      Spacer(Modifier.width(5.dp))
      val hasLocalNotesIconAlpha = if (
        listIconType is ListIconType.ShowIcons && listIconType.hasLocalNotes
      ) {
        1f
      } else {
        0f
      }
      Icon(
        imageVector = Icons.Filled.Note, contentDescription = "Draft status",
        modifier = Modifier.alpha(hasLocalNotesIconAlpha)
      )

      Spacer(Modifier.width(5.dp))

      val uploadedIconAlpha = if (listIconType is ListIconType.DontShow) 0f else 1f
      val contentDescription: String
      val icon: ImageVector
      when (listIconType) {
        ListIconType.DontShow -> {
          icon = Icons.Default.LockOpen
          contentDescription = ""
        }
        is ListIconType.ShowIcons -> {
          icon = when {
            listIconType.isPatientDraft -> Icons.Outlined.Edit
            listIconType.isPatientUploaded -> Icons.Default.Lock
            else -> Icons.Default.LockOpen
          }
          contentDescription = stringResource(R.string.patient_list_icon_locked_cd)
        }
      }

      Icon(
        imageVector = icon, contentDescription = contentDescription,
        modifier = Modifier.alpha(uploadedIconAlpha)
      )
    }
  }
}

@Preview
@Composable
fun PatientListItemPreview() {
  CradleTrialAppTheme {
    Surface {
      Column {
        PatientListHeader()
        PatientListItem(
          listPatientAndOutcomeError = ListPatientAndOutcomeError(
            listPatient = ListPatient(
              0L,
              serverInfo = null,
              serverErrorMessage = "Errors",
              "AA",
              FormDate.today(),
              localNotes = "My notes",
              isDraft = true
            ),
            outcomeError = null
          ),
          onClick = {}
        )
        PatientListItem(
          listPatientAndOutcomeError = ListPatientAndOutcomeError(
            listPatient = ListPatient(
              id = 0L,
              serverInfo = ServerInfo(nodeId = 50, objectId = 50),
              serverErrorMessage = null,
              initials = "AA",
              dateOfBirth = FormDate.today(),
              localNotes = null,
              isDraft = false
            ),
            outcomeError = null,
          ),
          onClick = {}
        )
        PatientListItemPlaceholder()
      }
    }
  }
}
