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
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.database.resultentities.PatientFacilityDistrictOutcomes
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.patients.PatientPreviewClasses
import org.welbodipartnership.cradle5.ui.composables.AnimatedVisibilityFadingWrapper
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

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
            patientState.patientFacilityOutcomes,
            editState = editState,
            onPatientEditPress = onPatientEdit,
            onPatientOtherInfoEditPress = onPatientOtherInfoEditPress,
            onPatientDeletePress = {
              viewModel.deletePatient(
                patientState.patientFacilityOutcomes.patient,
                patientState.patientFacilityOutcomes.outcomes
              )
              onBackPressed()
            },
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
  patientAndRelatedInfo: PatientFacilityDistrictOutcomes,
  editState: SyncRepository.FormEditState?,
  onPatientEditPress: (patientPrimaryKey: Long) -> Unit,
  onPatientOtherInfoEditPress: (patientPrimaryKey: Long) -> Unit,
  onPatientDeletePress: (patientPrimaryKey: Long) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues()
) {
  val patient: Patient = patientAndRelatedInfo.patient
  val outcomes: Outcomes? = patientAndRelatedInfo.outcomes

  var isDeleteConfirmDialogShowing by rememberSaveable { mutableStateOf(false) }
  if (isDeleteConfirmDialogShowing) {
    AlertDialog(
      onDismissRequest = { isDeleteConfirmDialogShowing = false },
      title = { Text(stringResource(id = R.string.delete_info_dialog_title)) },
      text = { Text(stringResource(id = R.string.delete_info_dialog_body)) },
      confirmButton = {
        TextButton(
          onClick = { onPatientDeletePress(patient.id) },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error)
        ) {
          Text(stringResource(id = R.string.delete_info_dialog_confirm_button))
        }
      },
      dismissButton = {
        TextButton(onClick = { isDeleteConfirmDialogShowing = false }) {
          Text(stringResource(id = R.string.cancel))
        }
      }
    )
  }

  LazyColumn(modifier = modifier, contentPadding = contentPadding) {
    item {
      BaseDetailsCard(title = null, modifier = modifier) {
        // Don't allow editing patients already uploaded.

        val isRegistrationUploadedButNotOutcomes = patient.isUploadedToServer &&
          outcomes?.isUploadedToServer != true
        OutlinedButton(
          enabled = (!patient.isUploadedToServer || isRegistrationUploadedButNotOutcomes) &&
            editState?.canEdit == true,
          onClick = { onPatientEditPress(patient.id) }
        ) {
          Text(
            stringResource(
              if (isRegistrationUploadedButNotOutcomes) {
                R.string.patient_details_screen_edit_outcomes_button
              } else {
                R.string.patient_details_screen_edit_button
              }
            )
          )
        }

        OutlinedButton(
          enabled = !patient.isUploadedToServer && editState?.canEdit == true,
          onClick = { isDeleteConfirmDialogShowing = true },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error),
        ) {
          Text(stringResource(R.string.delete_info_button))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          val icon: ImageVector
          val contentDescription: String
          val text: String
          val isError: Boolean
          when {
            isRegistrationUploadedButNotOutcomes -> {
              icon = Icons.Default.Error
              contentDescription = "Patient registration locked, but outcomes have errors"
              text = "Patient registration has been uploaded to MedSciNet, but there were errors with the outcomes during sync"
              isError = true
            }
            patient.isUploadedToServer -> {
              icon = Icons.Default.Lock
              contentDescription = "Patient locked"
              text = "Patient has been uploaded to MedSciNet and is locked for editing on the app"
              isError = false
            }
            patient.isDraft -> {
              icon = Icons.Outlined.Edit
              contentDescription = "Patient marked as draft"
              text = "Patient is marked as draft and won't be included in the next sync"
              isError = false
            }
            else -> {
              icon = Icons.Default.LockOpen
              contentDescription = "Patient ready for upload"
              text = "Patient is ready for upload"
              isError = false
            }
          }

          val color = if (isError) MaterialTheme.colors.error else LocalContentColor.current
          CompositionLocalProvider(LocalContentColor provides color) {
            Icon(imageVector = icon, contentDescription = contentDescription)
            Spacer(Modifier.width(4.dp))
            Text(text)
          }
        }

        AnimatedVisibilityFadingWrapper(
          visible = editState?.canEdit == false && (!patient.isUploadedToServer || outcomes?.isUploadedToServer == false)
        ) {
          Spacer(Modifier.width(4.dp))
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
      PatientCard(patientAndRelatedInfo, modifier = Modifier.padding(16.dp))
    }

    item { Spacer(Modifier.height(8.dp)) }

    item {
      OutcomesCard(
        outcomes = outcomes,
        enumCollection = LocalServerEnumCollection.current,
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
        PatientFacilityDistrictOutcomes(
          patient = PatientPreviewClasses.createTestPatient(),
          facility = Facility(5, "Test facility", 0, 2, false, "My notes"),
          referralFromDistrict = null,
          referralFromFacility = null,
          referralToDistrict = null,
          referralToFacility = null,
          outcomes = PatientPreviewClasses.createTestOutcomes(),
        ),
        editState = SyncRepository.FormEditState.CAN_EDIT,
        onPatientEditPress = {},
        onPatientOtherInfoEditPress = {},
        onPatientDeletePress = {}
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
        PatientFacilityDistrictOutcomes(
          patient = PatientPreviewClasses.createTestPatient(serverInfo = ServerInfo(nodeId = 5L, objectId = null)),
          facility = Facility(5, "Test facility", 0, 2, false, "My notes"),
          outcomes = PatientPreviewClasses.createTestOutcomes(),
          referralFromDistrict = District(PatientPreviewClasses.FROM_DISTRICT_ID, "Test 'from' district"),
          referralFromFacility = Facility(
            PatientPreviewClasses.FROM_FACILITY_ID, "Test 'from' facility", districtId = 2, hasVisited = false, listOrder = 1,
          ),
          referralToDistrict = District(PatientPreviewClasses.TO_DISTRICT_ID, "Test 'to' district"),
          referralToFacility = Facility(
            PatientPreviewClasses.TO_FACILITY_ID, "Test 'to' facility",
            districtId = 2, hasVisited = false, listOrder = 1,
          ),
        ),
        editState = SyncRepository.FormEditState.CAN_EDIT,
        onPatientEditPress = {},
        onPatientOtherInfoEditPress = {},
        onPatientDeletePress = {}
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
        PatientFacilityDistrictOutcomes(
          patient = PatientPreviewClasses.createTestPatient(isDraft = false),
          facility = Facility(5, "Test facility", 0, 2, false, "My notes"),
          outcomes = PatientPreviewClasses.createTestOutcomes(),
          referralFromDistrict = District(PatientPreviewClasses.FROM_DISTRICT_ID, "Test 'from' district"),
          referralFromFacility = Facility(
            PatientPreviewClasses.FROM_FACILITY_ID, "Test 'from' facility",
            hasVisited = false, districtId = 2, listOrder = 1,
          ),
          referralToDistrict = District(PatientPreviewClasses.TO_DISTRICT_ID, "Test 'to' district"),
          referralToFacility = Facility(
            PatientPreviewClasses.TO_FACILITY_ID, "Test 'to' facility",
            hasVisited = false, districtId = 2, listOrder = 1,
          ),
        ),
        editState = SyncRepository.FormEditState.CANT_EDIT_SYNC_IN_PROGRESS,
        onPatientEditPress = {},
        onPatientOtherInfoEditPress = {},
        onPatientDeletePress = {},
      )
    }
  }
}
