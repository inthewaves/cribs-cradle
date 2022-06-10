package org.welbodipartnership.cradle5.sync

import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
        contentPadding = WindowInsets.statusBars
          .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
          .asPaddingValues(),
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.sync_title)) },
        actions = { AccountInfoButton() }
      )
    }
  ) { padding ->
    val syncStatus by viewModel.currentSyncStatusFlow.collectAsState()
    val scrollState = rememberScrollState()
    Column(
      Modifier
        .padding(padding)
        .fillMaxSize()
        .padding(24.dp)
        .verticalScroll(scrollState),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(textAlign = TextAlign.Center)
      ) {
        syncStatus.let { status ->
          when (status) {
            is SyncRepository.SyncStatus.Active -> {
              ActiveSyncCard(status)
            }
            is SyncRepository.SyncStatus.Inactive, null -> {
              val patientsToUpload by viewModel.patientsToUploadCountFlow.collectAsState()
              val partialPatientsToUpload by viewModel.incompletePatientsToUploadCountFlow
                .collectAsState()
              val patientsWithOutcomesNotFullyUploadedWithErrors by viewModel
                .patientsWithOutcomesNotFullyUploadedWithErrorsCountFlow
                .collectAsState()
              val patientsWithOutcomesNotFullyUploadedWithoutErrors by viewModel
                .patientsWithOutcomesNotFullyUploadedWithoutErrorsCountFlow
                .collectAsState()
              val bpFormsToUpload by viewModel.bpInfoFormsToUploadCountFlow.collectAsState()
              val bpFormsWithErrors by viewModel.bpInfoFormsWithErrorsCountFlow.collectAsState()
              val bpFormsToReupload by viewModel.bpInfoFormsToReuploadCountFlow.collectAsState()
              val locationCheckInsToUpload by viewModel.locationCheckInsToUploadCountFlow
                .collectAsState()
              val lastTimeSyncCompleted by viewModel.lastSyncCompletedTimestamp.collectAsState()
              InactiveOrNoSyncCard(
                onSyncButtonClicked = { viewModel.enqueueSync() },
                onCancelButtonClicked = { viewModel.cancelSync() },
                syncStatus = status as SyncRepository.SyncStatus.Inactive?,
                lastTimeSyncCompleted = lastTimeSyncCompleted,
                numPatientsToUpload = patientsToUpload,
                numIncompletePatientsToUpload = partialPatientsToUpload,
                numBpInfoToUpload = bpFormsToUpload,
                numBpInfoWithErrors = bpFormsWithErrors,
                numBpInfoToRetryUpload = bpFormsToReupload,
                numLocationCheckinsToUpload = locationCheckInsToUpload,
                numPatientsWithOutcomesNotFullyUploadedWithErrors = patientsWithOutcomesNotFullyUploadedWithErrors,
                numPatientsWithOutcomesNotFullyUploadedWithoutErrors = patientsWithOutcomesNotFullyUploadedWithoutErrors
              )
            }
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
    CompositionLocalProvider(
      LocalTextStyle provides LocalTextStyle.current.copy(textAlign = TextAlign.Center)
    ) {
      when (syncStatus.progress?.stage) {
        SyncWorker.Stage.STARTING -> {
          Text("Starting")
        }
        SyncWorker.Stage.UPLOADING_NEW_PATIENTS -> {
          Text("Uploading new patients")
        }
        SyncWorker.Stage.UPLOADING_INCOMPLETE_PATIENTS -> {
          Text("Uploading patients that failed to upload before")
        }
        SyncWorker.Stage.UPLOADING_INCOMPLETE_OUTCOMES -> {
          Text("Uploading patients with outcomes that failed to upload before")
        }
        SyncWorker.Stage.UPLOADING_LOCATION_CHECK_INS -> {
          Text("Uploading location check ins")
        }
        SyncWorker.Stage.UPLOADING_BP_INFO -> {
          Text("Uploading blood pressure data")
        }
        SyncWorker.Stage.UPLOADING_INCOMPLETE_BP_INFO -> {
          Text("Uploading blood pressure data that failed before")
        }
        SyncWorker.Stage.PERFORMING_INFO_SYNC -> {
          Text("Updating info")
        }
        null -> {
          Text("Performing sync")
        }
      }
    }

    Spacer(Modifier.height(24.dp))

    when (val progress = syncStatus.progress) {
      is SyncWorker.Progress.WithFiniteProgress -> {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          LinearProgressIndicator(progress.progressPercent)
          Spacer(Modifier.height(8.dp))
          Text("${progress.doneSoFar} out of ${progress.totalToDo}")
          Spacer(Modifier.height(8.dp))
          val resources = LocalContext.current.resources
          AnimatedVisibilityFadingWrapper(visible = progress.numFailed > 0) {
            Text(
              resources.getQuantityString(
                R.plurals.sync_screen_d_patients_failed_to_upload,
                progress.numFailed,
                progress.numFailed
              ),
              color = MaterialTheme.colors.error
            )
          }
        }
      }
      is SyncWorker.Progress.InfoSync -> {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          LinearProgressIndicator()
          Spacer(Modifier.height(8.dp))
          progress.infoSyncStage?.let {
            Text(it.logString)
          }
          progress.infoSyncText?.let { syncText ->
            if (syncText != progress.infoSyncStage?.logString) {
              Spacer(Modifier.height(8.dp))
              Text(syncText, textAlign = TextAlign.Center)
            }
          }
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
  numIncompletePatientsToUpload: Int?,
  numPatientsWithOutcomesNotFullyUploadedWithErrors: Int?,
  numPatientsWithOutcomesNotFullyUploadedWithoutErrors: Int?,
  numBpInfoToUpload: Int?,
  numBpInfoWithErrors: Int?,
  numBpInfoToRetryUpload: Int?,
  lastTimeSyncCompleted: UnixTimestamp?,
  modifier: Modifier = Modifier,
  numLocationCheckinsToUpload: Int?,
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
      null -> {}
    }

    AnimatedVisibilityFadingWrapper(visible = lastTimeSyncCompleted != null) {
      Spacer(Modifier.height(12.dp))
      val localDateString = remember(lastTimeSyncCompleted) {
        lastTimeSyncCompleted?.formatAsConciseDate() ?: ""
      }
      Text("Last synced at $localDateString")
      Spacer(Modifier.height(24.dp))
    }

    val resources = LocalContext.current.resources
    if (numPatientsToUpload != null) {
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

    if (numIncompletePatientsToUpload != null && numIncompletePatientsToUpload >= 1) {
      Text(
        resources.getQuantityString(
          R.plurals.sync_screen_there_are_currently_d_incomplete_patients_to_upload,
          numIncompletePatientsToUpload,
          numIncompletePatientsToUpload
        ),
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(24.dp))
    }

    if (
      numPatientsWithOutcomesNotFullyUploadedWithErrors != null &&
      numPatientsWithOutcomesNotFullyUploadedWithErrors >= 1
    ) {
      Text(
        resources.getQuantityString(
          R.plurals.sync_screen_there_are_currently_d_patients_with_not_uploaded_outcomes_errors_need_to_be_addressed,
          numPatientsWithOutcomesNotFullyUploadedWithErrors,
          numPatientsWithOutcomesNotFullyUploadedWithErrors
        ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.error
      )
      Spacer(Modifier.height(24.dp))
    }

    if (
      numPatientsWithOutcomesNotFullyUploadedWithoutErrors != null &&
      numPatientsWithOutcomesNotFullyUploadedWithoutErrors >= 1
    ) {
      Text(
        resources.getQuantityString(
          R.plurals.sync_screen_there_are_currently_d_patients_with_not_uploaded_outcomes,
          numPatientsWithOutcomesNotFullyUploadedWithoutErrors,
          numPatientsWithOutcomesNotFullyUploadedWithoutErrors
        ),
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(24.dp))
    }

    if (numBpInfoToUpload != null) {
      Text(
        resources.getQuantityString(
          R.plurals.sync_screen_there_are_d_bp_forms_to_upload,
          numBpInfoToUpload,
          numBpInfoToUpload
        ),
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(24.dp))
    }

    if (numBpInfoWithErrors != null && numBpInfoWithErrors >= 1) {
      Text(
        resources.getQuantityString(
          R.plurals.sync_screen_d_bp_forms_failed_to_upload_errors,
          numBpInfoWithErrors,
          numBpInfoWithErrors
        ),
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(24.dp))
    }

    if (numBpInfoToRetryUpload != null && numBpInfoToRetryUpload >= 1) {
      Text(
        resources.getQuantityString(
          R.plurals.sync_screen_there_d_bp_forms_to_retry_upload,
          numBpInfoToRetryUpload,
          numBpInfoToRetryUpload
        ),
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(24.dp))
    }

    if (numLocationCheckinsToUpload != null) {
      Text(
        if (numLocationCheckinsToUpload == 1) {
          "There is 1 location check in to upload"
        } else {
          "There are $numLocationCheckinsToUpload location check ins to upload"
        },
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
              stage = SyncWorker.Stage.UPLOADING_NEW_PATIENTS,
              doneSoFar = 5,
              totalToDo = 10,
              numFailed = 0,
            )
          )
        )
        Spacer(modifier = Modifier.height(24.dp))
        ActiveSyncCard(
          syncStatus = SyncRepository.SyncStatus.Active(
            progress = SyncWorker.Progress.WithFiniteProgress(
              stage = SyncWorker.Stage.UPLOADING_NEW_PATIENTS,
              doneSoFar = 5,
              totalToDo = 10,
              numFailed = 5,
            )
          )
        )
        Spacer(modifier = Modifier.height(24.dp))
        InactiveOrNoSyncCard(
          onSyncButtonClicked = {},
          onCancelButtonClicked = {},
          syncStatus = SyncRepository.SyncStatus.Inactive(WorkInfo.State.SUCCEEDED),
          numPatientsToUpload = 5,
          numIncompletePatientsToUpload = 1,
          numPatientsWithOutcomesNotFullyUploadedWithErrors = 1,
          numPatientsWithOutcomesNotFullyUploadedWithoutErrors = 1,
          numBpInfoToUpload = 4,
          numBpInfoWithErrors = 2,
          numBpInfoToRetryUpload = 1,
          lastTimeSyncCompleted = UnixTimestamp.now(),
          numLocationCheckinsToUpload = 1
        )
      }
    }
  }
}
