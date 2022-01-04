package org.welbodipartnership.cradle5.patients.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.welbodipartnership.cradle5.LeafScreen
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.resultentities.PatientFacilityOutcomes
import javax.inject.Inject

@HiltViewModel
class PatientDetailsViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val dbWrapper: CradleDatabaseWrapper
) : ViewModel() {
  sealed class State {
    object Loading : State()
    class Ready(val patientFacilityOutcomes: PatientFacilityOutcomes) : State()
    object Failed : State()
  }

  private val patientPrimaryKey: Long? =
    savedStateHandle[LeafScreen.PatientDetails.ARG_PATIENT_PRIMARY_KEY]

  val patientOutcomesStateFlow: Flow<State> = flowOf<State>(State.Loading)
    .onCompletion {
      emitAll(
        patientPrimaryKey?.let { pk ->
          dbWrapper.database!!.patientDao().getPatientAndOutcomesFlow(pk)
            .map { patientWithFacilityAndOutcomes ->
              if (patientWithFacilityAndOutcomes != null) {
                State.Ready(patientWithFacilityAndOutcomes)
              } else {
                State.Failed
              }
            }
        } ?: flowOf(State.Failed)
      )
    }

  override fun onCleared() {
    super.onCleared()
    Log.d("PatientDetailsViewModel", "onCleared()")
  }
}
