package org.welbodipartnership.cradle5.patients.list

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.compose.rememberFlowWithLifecycle
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.util.date.FormDate

@Composable
fun PatientsList(
  onOpenPatientDetails: @Composable (patientPrimaryKey: Long) -> Unit,
  onOpenNewPatientCreation: @Composable () -> Unit,
) {
  PatientsList(
    viewModel = hiltViewModel(),
    onOpenPatientDetails = onOpenPatientDetails,
    onOpenNewPatientCreation = onOpenNewPatientCreation
  )
}

@Composable
private fun PatientsList(
  viewModel: PatientsListViewModel,
  onOpenPatientDetails: @Composable (patientPrimaryKey: Long) -> Unit,
  onOpenNewPatientCreation: @Composable () -> Unit,
) {
  val lazyPagingItems = rememberFlowWithLifecycle(viewModel.pager.flow).collectAsLazyPagingItems()
  Column {
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

    LazyColumn {
      items(lazyPagingItems) { listPatient ->
        if (listPatient != null) {
          Row {
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
