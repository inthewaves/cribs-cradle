package org.welbodipartnership.cradle5.patients.list

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.settings.AuthToken
import org.welbodipartnership.cradle5.domain.RestApi
import javax.inject.Inject

@HiltViewModel
class PatientsListViewModel @Inject constructor(
  private val dbWrapper: CradleDatabaseWrapper,
  private val restApi: RestApi,
) : ViewModel() {

  val pager = Pager(
    PagingConfig(
      pageSize = 60,
      enablePlaceholders = true,
      maxSize = 200
    )
  ) { dbWrapper.database!!.patientDao().patientsPagingSource() }

  suspend fun addPatient(patient: Patient) {
    val dao = dbWrapper.database!!.patientDao()

    dao.upsert(patient)
  }

  suspend fun login(username: String = "test", password: String = "test1"): AuthToken? {
    return restApi.login(username, password).valueOrNull()
  }
}
