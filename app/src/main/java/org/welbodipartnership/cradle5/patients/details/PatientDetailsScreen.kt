package org.welbodipartnership.cradle5.patients.details

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.LocalServerEnumCollection
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.ui.composables.AnimatedVisibilityFadingWrapper
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate

@Composable
fun PatientDetailsScreen(
  onBackPressed: () -> Unit,
  onPatientEdit: (patientPrimaryKey: Long) -> Unit,
  onPatientOtherInfoEditPress: (patientPrimaryKey: Long) -> Unit,
  viewModel: PatientDetailsViewModel = hiltViewModel()
) {
  Scaffold(
    topBar = {
      TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        contentPadding = rememberInsetsPaddingValues(
          insets = LocalWindowInsets.current.systemBars,
          applyBottom = false,
        ),
        navigationIcon = {
          IconButton(onClick = onBackPressed) {
            Icon(
              imageVector = Icons.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back_button)
            )
          }
        },
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.patient_details_title)) },
      )
    }
  ) { padding ->
    val state by viewModel.patientOutcomesStateFlow.collectAsState()
    val editState by viewModel.editStateFlow.collectAsState()

    LaunchedEffect(state) {
      Log.d("PatientDetailsViewModel", "new state $state")
    }
    state.let { patientState ->
      when (patientState) {
        is PatientDetailsViewModel.State.Ready -> {
          PatientDetailsScreen(
            patientState.patientFacilityOutcomes.patient,
            patientState.patientFacilityOutcomes.facility,
            patientState.patientFacilityOutcomes.outcomes,
            editState = editState,
            onPatientEditPress = onPatientEdit,
            onPatientOtherInfoEditPress = onPatientOtherInfoEditPress,
            contentPadding = padding
          )
        }
        PatientDetailsViewModel.State.Failed -> {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("Failed to load patient")
          }
        }
        PatientDetailsViewModel.State.Loading -> {
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
private fun PatientDetailsScreen(
  patient: Patient,
  facility: Facility?,
  outcomes: Outcomes?,
  editState: SyncRepository.FormEditState?,
  onPatientEditPress: (patientPrimaryKey: Long) -> Unit,
  onPatientOtherInfoEditPress: (patientPrimaryKey: Long) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues()
) {
  LazyColumn(modifier = modifier, contentPadding = contentPadding) {
    item {
      BaseDetailsCard(title = null, modifier = modifier) {
        // Don't allow editing patients already uploaded.
        OutlinedButton(
          enabled = !patient.isUploadedToServer && editState?.canEdit == true,
          onClick = { onPatientEditPress(patient.id) }
        ) {
          Text(stringResource(R.string.patient_details_screen_edit_button))
        }

        Column {
          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              val icon: ImageVector
              val contentDescription: String
              val text: String
              when {
                patient.isUploadedToServer -> {
                  icon = Icons.Default.Lock
                  contentDescription = "Patient locked"
                  text = "Patient has been uploaded to MedSciNet and is locked for editing on the app"
                }
                patient.isDraft -> {
                  icon = Icons.Outlined.Edit
                  contentDescription = "Patient marked as draft"
                  text = "Patient is marked as draft and won't be included in the next sync"
                }
                else -> {
                  icon = Icons.Default.LockOpen
                  contentDescription = "Patient ready for upload"
                  text = "Patient is ready for upload"
                }
              }

              Icon(imageVector = icon, contentDescription = contentDescription)
              Spacer(Modifier.width(4.dp))
              Text(text)
            }
          }

          AnimatedVisibilityFadingWrapper(
            visible = editState?.canEdit == false && !patient.isUploadedToServer
          ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
              val text = when (editState) {
                SyncRepository.FormEditState.CANT_EDIT_SYNC_ENQUEUED -> {
                  "Unable to make edits when sync is enqueued"
                }
                SyncRepository.FormEditState.CANT_EDIT_SYNC_IN_PROGRESS -> {
                  "Unable to make edits while sync is running"
                }
                SyncRepository.FormEditState.CAN_EDIT, null -> ""
              }
              Text(text = text)
            }
          }
        }
      }
    }

    item { Spacer(Modifier.height(8.dp)) }

    item {
      OtherInfoCard(
        isPatientUploadedToServer = patient.isUploadedToServer,
        isDraft = patient.isDraft,
        localNotes = patient.localNotes,
        onEditOtherInfoButtonClick = { onPatientOtherInfoEditPress(patient.id) },
        modifier = Modifier.padding(16.dp)
      )
    }

    item { Spacer(Modifier.height(8.dp)) }

    item {
      PatientCard(patient = patient, facility = facility, modifier = Modifier.padding(16.dp))
    }

    item { Spacer(Modifier.height(8.dp)) }

    item {
      OutcomesCard(
        outcomes = outcomes,
        LocalServerEnumCollection.current,
        modifier = Modifier.padding(16.dp)
      )
    }
  }
}

@Preview
@Composable
fun PatientDetailsScreenNotUploadedPreview() {
  CradleTrialAppTheme {
    Surface {
      PatientDetailsScreen(
        patient = Patient(
          initials = "AA",
          serverInfo = null,
          serverErrorMessage = null,
          presentationDate = FormDate(day = 10, month = 2, year = 1995),
          dateOfBirth = FormDate(day = 19, month = 8, year = 1989),
          healthcareFacilityId = 50L,
          lastUpdatedTimestamp = 162224953,
          isDraft = true,
        ),
        facility = Facility(5L, "Test facility", 0, false, "My notes"),
        outcomes = testOutcomes,
        editState = SyncRepository.FormEditState.CAN_EDIT,
        onPatientEditPress = {},
        onPatientOtherInfoEditPress = {}
      )
    }
  }
}

@Preview
@Composable
fun PatientDetailsScreenUploadedPreview() {
  CradleTrialAppTheme {
    Surface {
      PatientDetailsScreen(
        patient = Patient(
          initials = "AA",
          serverInfo = ServerInfo(nodeId = 5L, objectId = null),
          serverErrorMessage = null,
          presentationDate = FormDate(day = 10, month = 2, year = 1995),
          dateOfBirth = FormDate(day = 19, month = 8, year = 1989),
          healthcareFacilityId = 50L,
          lastUpdatedTimestamp = 162224953,
          isDraft = true,
        ),
        facility = Facility(5L, "Test facility", 0, false, "My notes"),
        outcomes = testOutcomes,
        editState = SyncRepository.FormEditState.CAN_EDIT,
        onPatientEditPress = {},
        onPatientOtherInfoEditPress = {}
      )
    }
  }
}

@Preview
@Composable
fun PatientDetailsScreenSyncingPreview() {
  CradleTrialAppTheme {
    Surface {
      PatientDetailsScreen(
        patient = Patient(
          initials = "AA",
          serverInfo = null,
          serverErrorMessage = null,
          presentationDate = FormDate(day = 10, month = 2, year = 1995),
          dateOfBirth = FormDate(day = 19, month = 8, year = 1989),
          healthcareFacilityId = 50L,
          lastUpdatedTimestamp = 162224953,
          isDraft = true,
        ),
        facility = Facility(5L, "Test facility", 0, false, "My notes"),
        outcomes = testOutcomes,
        editState = SyncRepository.FormEditState.CAN_EDIT,
        onPatientEditPress = {},
        onPatientOtherInfoEditPress = {}
      )
    }
  }
}
