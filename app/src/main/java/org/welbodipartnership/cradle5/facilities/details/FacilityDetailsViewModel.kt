package org.welbodipartnership.cradle5.facilities.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.welbodipartnership.cradle5.LeafScreen
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.FacilityBpInfo
import javax.inject.Inject

@HiltViewModel
class FacilityDetailsViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val dbWrapper: CradleDatabaseWrapper,
) : ViewModel() {
  companion object {
    private const val TAG = "FacilityDetailsViewModel"
  }

  sealed class State {
    object Loading : State()
    class Ready(val facility: Facility) : State()
    object Failed : State()
  }

  override fun onCleared() {
    super.onCleared()
    Log.d(TAG, "onCleared")
  }

  private val facilityPrimaryKey: Long? =
    savedStateHandle[LeafScreen.FacilityDetails.ARG_FACILITY_PRIMARY_KEY]

  val bpInfoCount: Flow<Int?> = facilityPrimaryKey?.let {
    dbWrapper.bpInfoDao().countTotalByFacilityId(it)
  } ?: flowOf(null)

  val bpInfoFlow: Flow<PagingData<FacilityBpInfo>>? = facilityPrimaryKey?.let {
    Pager(PagingConfig(pageSize = 60, enablePlaceholders = true, maxSize = 200)) {
      dbWrapper.bpInfoDao().bpInfoPagingSourceByFacilityId(it)
    }.flow
  }

  val facilityStateFlow: StateFlow<State> =
    (
      facilityPrimaryKey
        ?.let { pk ->
          dbWrapper.facilitiesDao().getFacilityFlow(pk)
            .map { facility ->
              if (facility != null) {
                State.Ready(facility)
              } else {
                State.Failed
              }
            }
        } ?: flowOf(State.Failed)
      ).stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(500L),
      State.Loading
    )
}
