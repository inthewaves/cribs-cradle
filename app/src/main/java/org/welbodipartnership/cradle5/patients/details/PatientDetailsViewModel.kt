package org.welbodipartnership.cradle5.patients.details

import android.util.Log
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
import org.welbodipartnership.cradle5.data.database.resultentities.PatientFacilityDistrictOutcomes
import org.welbodipartnership.cradle5.domain.patients.PatientsManager
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import javax.inject.Inject

@HiltViewModel
class PatientDetailsViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val dbWrapper: CradleDatabaseWrapper,
  private val patientsManager: PatientsManager,
) : ViewModel() {
  sealed class State {
    object Loading : State()
    class Ready(val patientFacilityOutcomes: PatientFacilityDistrictOutcomes) : State()
    object Failed : State()
  }

  private val patientPrimaryKey: Long? =
    savedStateHandle[LeafScreen.PatientDetails.ARG_PATIENT_PRIMARY_KEY]

  val patientOutcomesStateFlow: StateFlow<State> =
    (
      patientPrimaryKey
        ?.let { pk ->
          dbWrapper.database!!.patientDao().getPatientAndOutcomesFlow(pk)
            .map { patientWithFacilityAndOutcomes ->
              if (patientWithFacilityAndOutcomes != null) {
                State.Ready(patientWithFacilityAndOutcomes)
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

  val editStateFlow: StateFlow<SyncRepository.FormEditState?> = patientsManager
    .editPatientsOutcomesState
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(2000L),
      initialValue = null
    )

  override fun onCleared() {
    super.onCleared()
    Log.d("PatientDetailsViewModel", "onCleared()")
  }
}
