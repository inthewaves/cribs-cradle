package org.welbodipartnership.cradle5.facilities.list

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import javax.inject.Inject

@HiltViewModel
class FacilitiesListViewModel @Inject constructor(
  private val dbWrapper: CradleDatabaseWrapper,
  private val appValuesStore: AppValuesStore,
) : ViewModel() {
  private data class FilterConfig(
    val filterOption: FilterOption,
    val districtId: Int,
  )

  enum class FilterOption(@StringRes val selectionStringResId: Int) {
    NONE(R.string.none),
    VISITED(R.string.facility_list_filter_option_visited),
    NOT_VISITED(R.string.facility_list_filter_option_not_visited)
  }

  private val pagingConfig = PagingConfig(
    pageSize = 60,
    enablePlaceholders = true,
    maxSize = 200
  )

  val filterOption = MutableStateFlow(FilterOption.NONE)

  val facilitiesPagerFlow: Flow<PagingData<Facility>> = combine(
    filterOption,
    appValuesStore.districtIdFlow,
  ) { opt, districtId ->
    districtId?.let { FilterConfig(opt, it) }
  }.filterNotNull()
    .distinctUntilChanged()
    .flatMapLatest { (filterOption, districtId) ->
      Pager(pagingConfig) {
        when (filterOption) {
          FilterOption.NONE -> dbWrapper.facilitiesDao()
            .facilitiesPagingSource(districtId = districtId)
          FilterOption.VISITED -> dbWrapper.facilitiesDao()
            .facilitiesPagingSourceFilterByVisited(visited = true, districtId = districtId)
          FilterOption.NOT_VISITED -> dbWrapper.facilitiesDao()
            .facilitiesPagingSourceFilterByVisited(visited = false, districtId = districtId)
        }
      }.flow
    }

  val facilitiesCountFlow = dbWrapper.facilitiesDao().countTotalFacilities()
}
