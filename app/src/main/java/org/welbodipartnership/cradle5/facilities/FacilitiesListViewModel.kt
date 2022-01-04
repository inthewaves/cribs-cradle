package org.welbodipartnership.cradle5.facilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Facility
import javax.inject.Inject

@HiltViewModel
class FacilitiesListViewModel @Inject constructor(
  private val dbWrapper: CradleDatabaseWrapper
) : ViewModel() {
  val facilitiesPagerFlow: Flow<PagingData<Facility>> = Pager(
    PagingConfig(
      pageSize = 60,
      enablePlaceholders = true,
      maxSize = 200
    )
  ) { dbWrapper.facilitiesDao().patientsPagingSource() }
    .flow
    .cachedIn(viewModelScope)
}
