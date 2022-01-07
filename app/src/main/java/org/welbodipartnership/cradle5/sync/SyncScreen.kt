package org.welbodipartnership.cradle5.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkInfo
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.domain.sync.SyncWorker
import org.welbodipartnership.cradle5.home.AccountInfoButton
import org.welbodipartnership.cradle5.patients.details.BaseDetailsCard
import org.welbodipartnership.cradle5.ui.composables.AnimatedVisibilityFadingWrapper
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp

@Composable
fun SyncScreen() {
  SyncScreen(viewModel = hiltViewModel())
}

@Composable
private fun SyncScreen(viewModel: SyncScreenViewModel) {
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
        title = { Text(text = stringResource(R.string.sync_title)) },
        actions = { AccountInfoButton() }
      )
    }
  ) { padding ->
    val syncStatus by viewModel.currentSyncJobFlow.collectAsState()
    val scrollState = rememberScrollState()
    Column(
      Modifier
        .padding(padding)
        .fillMaxSize()
        .padding(24.dp)
        .verticalScroll(scrollState),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      syncStatus.let { status ->
        when (status) {
          is SyncRepository.SyncStatus.Active -> {
            ActiveSyncCard(status)
          }
          is SyncRepository.SyncStatus.Inactive, null -> {
            val patientsToUpload by viewModel.patientsToUploadCountFlow.collectAsState()
            val lastTimeSyncCompleted by viewModel.lastSyncCompletedTimestamp.collectAsState()
            InactiveOrNoSyncCard(
              onSyncButtonClicked = { viewModel.enqueueSync() },
              onCancelButtonClicked = { viewModel.cancelSync() },
              syncStatus = status as SyncRepository.SyncStatus.Inactive?,
              lastTimeSyncCompleted = lastTimeSyncCompleted,
              numPatientsToUpload = patientsToUpload
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ActiveSyncCard(
  syncStatus: SyncRepository.SyncStatus.Active,
  modifier: Modifier = Modifier,
) {
  BaseDetailsCard(
    title = null,
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    when (syncStatus.progress?.stage) {
      SyncWorker.Stage.STARTING -> {
        Text("Starting")
      }
      SyncWorker.Stage.UPLOADING_PATIENTS -> {
        Text("Uploading patients")
      }
      SyncWorker.Stage.DOWNLOADING_FACILITIES -> {
        Text("Downloading facilities")
      }
      SyncWorker.Stage.DOWNLOADING_DROPDOWN_VALUES -> {
        Text("Downloading dropdown values")
      }
      null -> {
        Text("Performing sync")
      }
    }

    Spacer(Modifier.height(24.dp))

    when (val progress = syncStatus.progress) {
      is SyncWorker.Progress.WithFiniteProgress -> {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          LinearProgressIndicator(progress.progressPercent)
          Spacer(Modifier.height(8.dp))
          Text("${progress.doneSoFar} out of ${progress.totalToDo}")
        }
      }
      else -> LinearProgressIndicator()
    }
  }
}

@Composable
fun InactiveOrNoSyncCard(
  onSyncButtonClicked: () -> Unit,
  onCancelButtonClicked: () -> Unit,
  syncStatus: SyncRepository.SyncStatus.Inactive?,
  numPatientsToUpload: Int?,
  lastTimeSyncCompleted: UnixTimestamp?,
  modifier: Modifier = Modifier,
) {
  BaseDetailsCard(
    title = null,
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    when (syncStatus?.workState) {
      WorkInfo.State.ENQUEUED -> {
        Text("Waiting to start sync (internet not available)")
      }
      WorkInfo.State.RUNNING -> Text("Running sync")
      WorkInfo.State.SUCCEEDED -> Text("Last sync successful")
      WorkInfo.State.FAILED -> Text("Last sync failed")
      WorkInfo.State.BLOCKED -> Text("Last sync blocked")
      WorkInfo.State.CANCELLED -> Text("Last sync cancelled")
      null -> CircularProgressIndicator()
    }

    AnimatedVisibilityFadingWrapper(visible = lastTimeSyncCompleted != null) {
      Spacer(Modifier.height(12.dp))
      val localDateString = remember(lastTimeSyncCompleted) {
        lastTimeSyncCompleted?.formatAsConciseDate() ?: ""
      }
      Text("Last synced at $localDateString")
    }
    Spacer(Modifier.height(24.dp))

    if (numPatientsToUpload != null) {
      val resources = LocalContext.current.resources
      Text(
        resources.getQuantityString(
          R.plurals.sync_screen_there_are_currently_d_patients_to_upload,
          numPatientsToUpload,
          numPatientsToUpload
        ),
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(24.dp))
    }

    val showSyncButton = syncStatus != null && syncStatus.workState != WorkInfo.State.ENQUEUED
    val showCancelButton = syncStatus?.workState == WorkInfo.State.ENQUEUED
    if (showSyncButton) {
      Button(onClick = onSyncButtonClicked) {
        Text(stringResource(R.string.sync_screen_start_sync_button))
      }
    } else if (showCancelButton) {
      Button(
        onClick = onCancelButtonClicked,
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
      ) {
        Text(stringResource(R.string.sync_screen_cancel_sync_button))
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
fun SyncPagePreview() {
  CradleTrialAppTheme {
    Surface {
      Column {
        ActiveSyncCard(
          syncStatus = SyncRepository.SyncStatus.Active(
            progress = SyncWorker.Progress.WithFiniteProgress(
              stage = SyncWorker.Stage.UPLOADING_PATIENTS,
              doneSoFar = 5,
              totalToDo = 10,
            )
          )
        )
        Spacer(modifier = Modifier.height(24.dp))
        InactiveOrNoSyncCard(
          onSyncButtonClicked = {},
          onCancelButtonClicked = {},
          syncStatus = SyncRepository.SyncStatus.Inactive(WorkInfo.State.SUCCEEDED),
          lastTimeSyncCompleted = UnixTimestamp.now(),
          numPatientsToUpload = 5
        )
      }
    }
  }
}
