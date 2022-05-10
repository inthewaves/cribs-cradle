package org.welbodipartnership.cradle5.cradleform.list

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.resultentities.ListCradleTrainingForm
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import javax.inject.Inject

@HiltViewModel
class CradleFormListViewModel @Inject constructor(
  private val dbWrapper: CradleDatabaseWrapper,
  private val valuesStore: AppValuesStore,
) : ViewModel() {
  sealed class FilterOption(
    @StringRes val selectionStringResId: Int,
    val icon: ImageVector?
  ) {
    object None : FilterOption(R.string.none, null)
    object Draft : FilterOption(R.string.patients_list_filter_option_draft, Icons.Outlined.Edit)
    object ReadyForUpload : FilterOption(R.string.patients_list_filter_option_ready_for_upload, Icons.Default.LockOpen)
    object Uploaded : FilterOption(R.string.patients_list_filter_option_uploaded, Icons.Default.Lock)
    class ByFacility(val facility: Facility, val position: Int) : FilterOption(R.string.patients_list_filter_option_facility, Icons.Outlined.LocationCity)
    class Month(val monthOneBased: Int) : FilterOption(R.string.patients_list_filter_option_month, Icons.Outlined.CalendarViewMonth)

    companion object {
      val defaultButtonsList by lazy { listOf(None, Draft, ReadyForUpload, Uploaded) }
    }
  }

  val selfFacilityPagingFlow: Flow<PagingData<Facility>> = valuesStore.districtIdFlow
    .flatMapLatest { districtId ->
      Pager(PagingConfig(pageSize = 60, enablePlaceholders = true, maxSize = 200)) {
        if (districtId != null) {
          dbWrapper.facilitiesDao().facilitiesPagingSource(districtId)
        } else {
          dbWrapper.facilitiesDao().facilitiesPagingSource()
        }
      }.flow
    }

  val filterOption = MutableStateFlow<FilterOption>(FilterOption.None)

  private val pagingConfig = PagingConfig(
    pageSize = 60,
    enablePlaceholders = true,
    maxSize = 200
  )

  val cradleFormPagerFlow: Flow<PagingData<ListCradleTrainingForm>> = filterOption
    .flatMapLatest { filterOpt ->
      Pager(pagingConfig) {
        val dao = dbWrapper.cradleTrainingFormDao()
        when (filterOpt) {
          FilterOption.None -> dao.cradleFormPagingSource()
          FilterOption.Draft -> dao.cradleFormPagingSourceFilterByDraft()
          FilterOption.ReadyForUpload ->
            dao.cradleFormPagingSourceFilterByNotUploadedAndNotDraft()
          FilterOption.Uploaded ->
            dao.cradleFormPagingSourceFilterByUploaded()
          is FilterOption.ByFacility ->
            dao.cradleFormPagingSourceFilterByFacility(filterOpt.facility.id)
          is FilterOption.Month ->
            dao.cradleFormPagingSourceFilterByTrainingMonth(filterOpt.monthOneBased)
        }
      }.flow
    }.cachedIn(viewModelScope)

  val patientsCountFlow = dbWrapper.cradleTrainingFormDao().countTotal()
}
