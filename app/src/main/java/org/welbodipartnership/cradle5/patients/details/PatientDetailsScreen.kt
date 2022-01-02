package org.welbodipartnership.cradle5.patients.details

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection

@Composable
fun PatientDetailsScreen(
  onBackPressed: () -> Unit,
  onPatientEdit: (patientPrimaryKey: Long) -> Unit,
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
        title = { Text(text = stringResource(R.string.patients_details_title)) },
      )
    }
  ) { padding ->
    val state by viewModel.patientOutcomesStateFlow
      .collectAsState(PatientDetailsViewModel.State.Loading)

    LaunchedEffect(state) {
      Log.d("PatientDetailsViewModel", "new state $state")
    }
    state.let { patientState ->
      when (patientState) {
        is PatientDetailsViewModel.State.Ready -> {
          PatientDetailsScreen(
            patientState.patient,
            patientState.outcomes,
            onPatientEditPress = onPatientEdit,
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
  outcomes: Outcomes?,
  onPatientEditPress: (patientPrimaryKey: Long) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues()
) {
  LazyColumn(modifier = modifier, contentPadding = contentPadding) {
    item {
      BaseDetailsCard(title = null, modifier = modifier) {
        // Don't allow editing patients already uploaded.
        OutlinedButton(enabled = patient.serverInfo == null, onClick = { onPatientEditPress(patient.id) }) {
          Text(stringResource(R.string.patient_edit_button))
        }
      }
    }

    item { Spacer(Modifier.height(8.dp)) }

    item {
      PatientCard(patient = patient, modifier = Modifier.padding(16.dp))
    }

    item { Spacer(Modifier.height(8.dp)) }

    item {
      OutcomesCard(
        outcomes = outcomes,
        // TODO: Get enums from server
        ServerEnumCollection.defaultInstance,
        modifier = Modifier.padding(16.dp)
      )
    }
  }
}
