package org.welbodipartnership.cradle5.patients.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Patient
import javax.inject.Inject

@HiltViewModel
class PatientsListViewModel @Inject constructor(
  private val dbWrapper: CradleDatabaseWrapper,
) : ViewModel() {

  val patientsPagerFlow = Pager(
    PagingConfig(
      pageSize = 60,
      enablePlaceholders = true,
      maxSize = 200
    )
  ) { dbWrapper.patientsDao().patientsPagingSource() }
    .flow
    .cachedIn(viewModelScope)

  val patientsCountFlow = dbWrapper.patientsDao().countTotalPatients()

  suspend fun addPatient(patient: Patient) {
    val dao = dbWrapper.database!!.patientDao()

    dao.upsert(patient)
  }
}
