package org.welbodipartnership.cradle5.facilities.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.compose.rememberFlowWithLifecycle
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.FacilityBpInfo
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.patients.list.isScrollingUp
import org.welbodipartnership.cradle5.ui.composables.AnimatedVisibilityFadingWrapper
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate
import java.time.ZonedDateTime

@Composable
fun FacilityDetailsScreen(
  onBackPressed: () -> Unit,
  onFacilityOtherInfoEditPress: (facilityPrimaryKey: Long) -> Unit,
  onNewBpInfoPressed: (facilityPk: Long) -> Unit,
  onEditBpInfoPressed: (pk: Long) -> Unit,
  viewModel: FacilityDetailsViewModel = hiltViewModel()
) {
  var bpInfoToDelete: FacilityBpInfo? by viewModel.bpInfoToDelete
  bpInfoToDelete?.let { bpToDelete ->
    AlertDialog(
      onDismissRequest = { bpInfoToDelete = null },
      title = { Text(stringResource(id = R.string.delete_info_dialog_title)) },
      text = {
        Column {
          Text(stringResource(R.string.delete_bp_info_dialog_this_will_delete_the_following_entry))
          Spacer(Modifier.height(12.dp))
          BpInfoColumnTexts(bpToDelete, useSubtitleStyle = false)
        }
      },
      confirmButton = {
        TextButton(
          onClick = { viewModel.deleteBpInfo(bpToDelete) },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error)
        ) {
          Text(stringResource(id = R.string.delete_bp_info_dialog_confirm_button))
        }
      },
      dismissButton = {
        TextButton(onClick = { bpInfoToDelete = null }) {
          Text(stringResource(id = R.string.cancel))
        }
      }
    )
  }

  val lazyListState = rememberLazyListState()
  val state by viewModel.facilityStateFlow.collectAsState()
  Scaffold(
    topBar = {
      TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        contentPadding = WindowInsets.statusBars
          .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
          .asPaddingValues(),
        navigationIcon = {
          IconButton(onClick = onBackPressed) {
            Icon(
              imageVector = Icons.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back_button)
            )
          }
        },
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.facility_details_title)) },
      )
    },
    floatingActionButton = {
      (state as? FacilityDetailsViewModel.State.Ready)?.facility?.let { facility ->
        FloatingActionButton(
          onClick = { onNewBpInfoPressed(facility.id) }
        ) {
          Row(Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Add, stringResource(R.string.patients_list_add_new_button))

            AnimatedVisibility(visible = lazyListState.isScrollingUp()) {
              Text(
                text = stringResource(R.string.bp_info_add_new_button),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
              )
            }
          }
        }
      }
    }
  ) { padding ->
    state.let { facilityState ->
      when (facilityState) {
        is FacilityDetailsViewModel.State.Ready -> {
          val bpInfoCount by viewModel.bpInfoCount.collectAsState(initial = null)
          val editFormState by viewModel.canEditBpInfoState.collectAsState()
          FacilityDetailsScreen(
            facility = facilityState.facility,
            areEditButtonsEnabled = editFormState?.canEdit == true,
            onFacilityOtherInfoEditPress = onFacilityOtherInfoEditPress,
            onEditBpInfoPressed = onEditBpInfoPressed,
            onDeleteBpInfoPressed = { viewModel.bpInfoToDelete.value = it },
            bpInfoCount = bpInfoCount,
            bpInfoItems = viewModel.bpInfoFlow?.let {
              rememberFlowWithLifecycle(it, minActiveState = Lifecycle.State.RESUMED)
                .collectAsLazyPagingItems()
            },
            lazyListState = lazyListState,
            contentPadding = padding
          )
        }
        FacilityDetailsViewModel.State.Failed -> {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("Failed to load facility")
          }
        }
        FacilityDetailsViewModel.State.Loading -> {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator()
          }
        }
      }
    }
  }
}

@Composable
private fun BpInfoColumnTexts(bpInfo: FacilityBpInfo, useSubtitleStyle: Boolean = true) {
  Text(
    bpInfo.dataCollectionDate?.toString() ?: stringResource(R.string.unknown_date),
    style = if (useSubtitleStyle) MaterialTheme.typography.subtitle1 else LocalTextStyle.current,
  )
  bpInfo.numBpReadingsTakenInFacilitySinceLastVisit?.let {
    Text(stringResource(R.string.bp_info_d_readings_collected, it))
  }
  bpInfo.numBpReadingsEndIn0Or5?.let {
    Text(stringResource(R.string.bp_info_d_readings_end_in_0_or_5, it))
  }
  bpInfo.numBpReadingsWithColorAndArrow?.let {
    Text(stringResource(R.string.bp_info_d_readings_have_color_or_arrow, it))
  }
}

@Composable
private fun FacilityDetailsScreen(
  facility: Facility,
  areEditButtonsEnabled: Boolean,
  onFacilityOtherInfoEditPress: (facilityPrimaryKey: Long) -> Unit,
  onEditBpInfoPressed: (pk: Long) -> Unit,
  onDeleteBpInfoPressed: (bpInfo: FacilityBpInfo) -> Unit,
  bpInfoCount: Int?,
  bpInfoItems: LazyPagingItems<FacilityBpInfo>?,
  lazyListState: LazyListState,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues()
) {
  LazyColumn(modifier = modifier, contentPadding = contentPadding, state = lazyListState) {
    item("otherinfo") {
      OtherFacilityInfoCard(
        hasVisited = facility.hasVisited,
        localNotes = facility.localNotes,
        onEditOtherInfoButtonClick = { onFacilityOtherInfoEditPress(facility.id) },
        modifier = Modifier.padding(16.dp)
      )
    }

    item("spacer") { Spacer(Modifier.height(8.dp)) }

    item("main_info") {
      FacilityCard(facility = facility, modifier = Modifier.padding(16.dp))
    }

    item("bp_info_title") {
      Column {
        Spacer(Modifier.height(8.dp))
        Text(
          "Blood pressure data",
          modifier = Modifier.padding(16.dp),
          style = MaterialTheme.typography.h4
        )
        Box {
          AnimatedVisibilityFadingWrapper(visible = bpInfoCount == null) {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              CircularProgressIndicator()
            }
          }
          AnimatedVisibilityFadingWrapper(visible = bpInfoCount == 0) {
            Text(
              stringResource(R.string.none),
              modifier = Modifier.padding(horizontal = 16.dp),
              style = MaterialTheme.typography.h6
            )
          }
        }
      }
    }

    if (bpInfoItems != null && bpInfoItems.itemCount != 0) {
      items(bpInfoItems, key = { info -> info.id }) { bpInfo: FacilityBpInfo? ->
        BpInfoCard(
          bpInfo = bpInfo,
          areEditButtonsEnabled = areEditButtonsEnabled,
          onEditPressed = onEditBpInfoPressed,
          onDeletePressed = onDeleteBpInfoPressed,
          modifier = Modifier.padding(16.dp)
        )
      }
      item {
        Spacer(Modifier.height(100.dp))
      }
    }
  }
}

@Preview
@Composable
fun BpInfoCardPreview() {
  CradleTrialAppTheme {
    Surface {
      BpInfoCard(
        bpInfo = FacilityBpInfo(
          id = 5,
          serverInfo = ServerInfo(null, null, null, null),
          dataCollectionDate = FormDate.today(),
          serverErrorMessage = null,
          district = 5,
          facility = 6,
          numBpReadingsTakenInFacilitySinceLastVisit = 6,
          numBpReadingsEndIn0Or5 = 4,
          numBpReadingsWithColorAndArrow = 2,
          recordLastUpdated = ZonedDateTime.now(),
          localNotes = null,
          isDraft = true,
        ),
        areEditButtonsEnabled = true,
        onEditPressed = {},
        onDeletePressed = {}
      )
    }
  }
}

@Composable
fun BpInfoCard(
  bpInfo: FacilityBpInfo?,
  areEditButtonsEnabled: Boolean,
  onEditPressed: (pk: Long) -> Unit,
  onDeletePressed: (bpInfo: FacilityBpInfo) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    elevation = 4.dp,
    backgroundColor = MaterialTheme.colors.surface,
    shape = MaterialTheme.shapes.small,
    modifier = modifier
  ) {
    Column(
      Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      if (bpInfo != null) {
        BpInfoColumnTexts(bpInfo)
        if (!bpInfo.localNotes.isNullOrBlank()) {
          Spacer(Modifier.height(8.dp))
          Text(bpInfo.localNotes ?: "")
        }
        if (bpInfo.isDraft) {
          Spacer(Modifier.height(8.dp))
          Row(
            Modifier.fillMaxWidth(),
          ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
              Text("Draft")
            }
          }
        }
        if (!bpInfo.isUploadedToServer) {
          Spacer(Modifier.height(8.dp))
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(
              enabled = areEditButtonsEnabled,
              onClick = { onDeletePressed(bpInfo) },
              colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error)
            ) {
              Text("Delete")
            }
            TextButton(
              enabled = areEditButtonsEnabled,
              onClick = { onEditPressed(bpInfo.id) },
            ) {
              Text("Edit")
            }
          }
        }
      } else {
        CircularProgressIndicator()
      }
    }
  }
}
