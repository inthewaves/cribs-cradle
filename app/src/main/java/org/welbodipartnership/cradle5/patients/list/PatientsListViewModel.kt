package org.welbodipartnership.cradle5.patients.list

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.resultentities.ListPatientAndOutcomeError
import javax.inject.Inject

@HiltViewModel
class PatientsListViewModel @Inject constructor(
  private val dbWrapper: CradleDatabaseWrapper,
) : ViewModel() {
  enum class FilterOption(
    @StringRes val selectionStringResId: Int,
    val icon: ImageVector?
  ) {
    NONE(R.string.none, null),
    DRAFT(R.string.patients_list_filter_option_draft, Icons.Outlined.Edit),
    READY_FOR_UPLOAD(R.string.patients_list_filter_option_ready_for_upload, Icons.Default.LockOpen),
    UPLOADED(R.string.patients_list_filter_option_uploaded, Icons.Default.Lock),
  }

  val filterOption = MutableStateFlow(FilterOption.NONE)

  private val pagingConfig = PagingConfig(
    pageSize = 60,
    enablePlaceholders = true,
    maxSize = 200
  )

  val patientsPagerFlow: Flow<PagingData<ListPatientAndOutcomeError>> = filterOption
    .flatMapLatest { filterOpt ->
      Pager(pagingConfig) {
        when (filterOpt) {
          FilterOption.NONE -> dbWrapper.patientsDao().patientsPagingSource()
          FilterOption.DRAFT -> dbWrapper.patientsDao().patientsPagingSourceFilterByDraft()
          FilterOption.READY_FOR_UPLOAD -> dbWrapper.patientsDao()
            .patientsPagingSourceFilterByNotUploadedAndNotDraft()
          FilterOption.UPLOADED -> dbWrapper.patientsDao()
            .patientsPagingSourceFilterByUploaded()
        }
      }.flow
    }

  val patientsCountFlow = dbWrapper.patientsDao().countTotalPatients()

  suspend fun addPatient(patient: Patient) {
    val dao = dbWrapper.database!!.patientDao()

    dao.upsert(patient)
  }
}
