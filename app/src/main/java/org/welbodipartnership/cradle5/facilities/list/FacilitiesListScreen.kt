package org.welbodipartnership.cradle5.facilities.list

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Note
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import org.welbodipartnership.cradle5.home.AccountInfoButton
import org.welbodipartnership.cradle5.ui.composables.AnimatedVisibilityFadingWrapper
import org.welbodipartnership.cradle5.ui.composables.carousel.Carousel
import org.welbodipartnership.cradle5.ui.composables.screenlists.ScreenListItem
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@Composable
fun FacilitiesListScreen(onOpenFacilityDetails: (facilityPk: Long) -> Unit,) {
  FacilitiesListScreen(
    viewModel = hiltViewModel(),
    onOpenFacilityDetails
  )
}

@Composable
private fun FacilitiesListScreen(
  viewModel: FacilitiesListViewModel,
  onOpenFacilityDetails: (facilityPk: Long) -> Unit,
) {

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
      dismissButton = {
        TextButton(
          onClick = {
            showFilterDialog = false
            viewModel.filterOption.value = FacilitiesListViewModel.FilterOption.NONE
          }
        ) { Text(stringResource(R.string.filter_button_clear_filter)) }
      },
      title = { Text("Select a filtering option") },
      text = {
        // https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#RadioButton(kotlin.Boolean,kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.material.RadioButtonColors)
        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
        Column(Modifier.selectableGroup()) {
          FacilitiesListViewModel.FilterOption.values().forEach { currentOpt ->
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
        title = { Text(text = stringResource(R.string.facilities_title)) },
        actions = { AccountInfoButton() }
      )
    }
  ) { padding ->
    Column(Modifier.padding(padding)) {
      val lazyPagingItems = rememberFlowWithLifecycle(
        viewModel.facilitiesPagerFlow, minActiveState = Lifecycle.State.RESUMED
      ).collectAsLazyPagingItems()
      val facilitiesCount by rememberFlowWithLifecycle(
        viewModel.facilitiesCountFlow, minActiveState = Lifecycle.State.RESUMED
      ).collectAsState(initial = null)

      val lazyListState = rememberLazyListState()

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
      FacilityListHeader()
      Box(Modifier.fillMaxSize()) {
        AnimatedVisibilityFadingWrapper(
          visible = lazyPagingItems.loadState.refresh is LoadState.Loading
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator()
          }
        }

        AnimatedVisibilityFadingWrapper(
          modifier = Modifier.align(Alignment.Center),
          visible = lazyPagingItems.loadState.refresh !is LoadState.Loading &&
            facilitiesCount == 0
        ) {
          Text(
            stringResource(R.string.facility_list_no_facilities_available),
            textAlign = TextAlign.Center
          )
        }

        AnimatedVisibilityFadingWrapper(
          visible = lazyPagingItems.loadState.refresh !is LoadState.Loading
        ) {
          LazyColumn(state = lazyListState) {
            items(lazyPagingItems) { facility ->
              if (facility != null) {
                FacilityListItem(
                  facility,
                  onClick = { onOpenFacilityDetails(facility.id) }
                )
              } else {
                FacilityListItemPlaceholder()
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
fun FacilityListHeader(modifier: Modifier = Modifier) {
  Card(elevation = 1.dp) {
    BaseFacilityListItem(
      first = stringResource(R.string.facility_list_header_id),
      second = stringResource(R.string.facility_list_header_name),
      third = stringResource(R.string.facility_list_header_visited),
      localNotes = null,
      minHeight = 24.dp,
      textStyle = MaterialTheme.typography.subtitle2,
      onClick = null,
      modifier = modifier
    )
  }
}

@Composable
fun FacilityListItem(
  facility: Facility,
  onClick: (facility: Facility) -> Unit,
  modifier: Modifier = Modifier
) {
  BaseFacilityListItem(
    first = facility.id.toString(),
    second = facility.name ?: stringResource(R.string.unknown),
    third = stringResource(if (facility.hasVisited) R.string.yes else R.string.no),
    localNotes = facility.localNotes?.ifBlank { null },
    minHeight = 48.dp,
    textStyle = MaterialTheme.typography.body2,
    onClick = { onClick(facility) },
    modifier = modifier
  )
}

@Composable
fun FacilityListItemPlaceholder(modifier: Modifier = Modifier) {
  ScreenListItem(
    minHeight = 48.dp,
    onClick = null,
    modifier = modifier,
    horizontalArrangement = Arrangement.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun BaseFacilityListItem(
  first: String,
  second: String,
  third: String,
  localNotes: String?,
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
      first,
      modifier = Modifier
        .weight(0.12f)
        .align(Alignment.CenterVertically),
      style = textStyle
    )
    Text(
      second,
      modifier = Modifier
        .weight(0.70f)
        .align(Alignment.CenterVertically),
      style = textStyle
    )
    Spacer(Modifier.width(5.dp))
    Text(
      third,
      modifier = Modifier
        .weight(0.2f)
        .align(Alignment.CenterVertically),
      style = textStyle
    )
    Spacer(Modifier.width(2.dp))
    val hasLocalNotesIconAlpha = if (localNotes != null) 1f else 0f
    Icon(
      imageVector = Icons.Filled.Note, contentDescription = "Notes status",
      modifier = Modifier.alpha(hasLocalNotesIconAlpha)
    )
  }
}

@Preview(widthDp = 800)
@Composable
fun FacilityListItemPreview() {
  CradleTrialAppTheme {
    Surface {
      Column {
        FacilityListHeader()
        FacilityListItem(
          facility = Facility(
            id = 50,
            name = "CHC Test Facility",
            listOrder = 0,
            districtId = 2,
            hasVisited = false
          ),
          onClick = {}
        )
        FacilityListItemPlaceholder()
      }
    }
  }
}
