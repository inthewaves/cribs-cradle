package org.welbodipartnership.cradle5.patients.list

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.database.resultentities.ListPatient
import org.welbodipartnership.cradle5.home.AccountInfoButton
import org.welbodipartnership.cradle5.ui.composables.AnimatedVisibilityFadingWrapper
import org.welbodipartnership.cradle5.ui.composables.carousel.Carousel
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
        ) {
          Text(stringResource(R.string.filter_button_ok))
        }
      },
      title = { Text("Select a filtering option") },
      text = {
        // https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#RadioButton(kotlin.Boolean,kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.material.RadioButtonColors)
        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
        Column(Modifier.selectableGroup()) {
          PatientsListViewModel.FilterOption.values().forEach { currentOpt ->
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
          Text(
            stringResource(
              R.string.facilities_list_filter_by_s_button,
              stringResource(currentFilterOption.selectionStringResId)
            )
          )
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

@Composable
fun PatientListHeader(modifier: Modifier = Modifier) {
  Card(elevation = 1.dp) {
    BasePatientListItem(
      id = stringResource(R.string.patient_list_header_id),
      initials = stringResource(R.string.patient_list_header_initials),
      dateOfBirth = stringResource(R.string.patient_list_header_date_of_birth),
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
  listPatient: ListPatient,
  onClick: (listPatient: ListPatient) -> Unit,
  modifier: Modifier = Modifier
) {
  BasePatientListItem(
    id = listPatient.serverInfo?.objectId?.toString() ?: stringResource(R.string.not_available_n_slash_a),
    initials = listPatient.initials,
    dateOfBirth = listPatient.dateOfBirth?.toString() ?: stringResource(R.string.not_available_n_slash_a),
    listIconType = ListIconType.ShowIcons(
      isPatientUploaded = listPatient.serverInfo != null,
      hasLocalNotes = !listPatient.localNotes.isNullOrBlank(),
      isPatientDraft = listPatient.isDraft
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
    val isPatientUploaded: Boolean
  ) : ListIconType()
}

@Composable
private fun BasePatientListItem(
  id: String,
  initials: String,
  dateOfBirth: String,
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
      dateOfBirth,
      modifier = Modifier
        .weight(0.3f)
        .align(Alignment.CenterVertically),
      style = textStyle
    )
    Row {
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
          listPatient = ListPatient(
            0L,
            serverInfo = null,
            "AA",
            FormDate.today(),
            localNotes = "My notes",
            isDraft = true
          ),
          onClick = {}
        )
        PatientListItem(
          listPatient = ListPatient(
            id = 0L,
            serverInfo = ServerInfo(nodeId = 50, objectId = 50),
            initials = "AA",
            dateOfBirth = FormDate.today(),
            localNotes = null,
            isDraft = false
          ),
          onClick = {}
        )
        PatientListItemPlaceholder()
      }
    }
  }
}
