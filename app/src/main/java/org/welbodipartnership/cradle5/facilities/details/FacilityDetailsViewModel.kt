package org.welbodipartnership.cradle5.facilities.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.welbodipartnership.cradle5.LeafScreen
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Facility
import javax.inject.Inject

@HiltViewModel
class FacilityDetailsViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val dbWrapper: CradleDatabaseWrapper,
) : ViewModel() {
  sealed class State {
    object Loading : State()
    class Ready(val facility: Facility) : State()
    object Failed : State()
  }

  private val facilityPrimaryKey: Int? =
    savedStateHandle[LeafScreen.FacilityDetails.ARG_FACILITY_PRIMARY_KEY]

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
