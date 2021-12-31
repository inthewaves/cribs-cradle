package org.welbodipartnership.cradle5.patients.list

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.compose.rememberFlowWithLifecycle
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.util.date.FormDate

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

@Composable
private fun PatientsListScreen(
  viewModel: PatientsListViewModel,
  onOpenPatientDetails: (patientPrimaryKey: Long) -> Unit,
  onOpenNewPatientCreation: () -> Unit,
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
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.patients_list_title)) },
      )
    }
  ) { padding ->
    Column(Modifier.padding(padding)) {
      Text("Patients")

      val scope = rememberCoroutineScope()
      OutlinedButton(
        onClick = {
          Log.d("PatientsList", "")
          scope.launch {
            viewModel.addPatient(
              Patient(
                initials = "AB",
                presentationDate = FormDate(day = 5, month = 4, year = 2010),
                dateOfBirth = FormDate(day = 5, month = 4, year = 2010),
                isExactDateOfBirth = false,
                lastUpdatedTimestamp = System.currentTimeMillis() / 1000
              )
            )
          }
        }
      ) {
        Text("Add patient")
      }

      val lazyPagingItems = rememberFlowWithLifecycle(viewModel.pager.flow).collectAsLazyPagingItems()
      LazyColumn {
        items(lazyPagingItems) { listPatient ->
          if (listPatient != null) {
            Row(
              Modifier.clickable {
                onOpenPatientDetails(listPatient.id)
              }
            ) {
              Text("${listPatient.id}")
              Text(listPatient.initials)
              Text(listPatient.dateOfBirth.toString())
            }
          } else {
            Text("No patients")
          }
        }
      }
    }
  }
}
