package org.welbodipartnership.cradle5.cradleform.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.LeafScreen
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.TAG
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.resultentities.CradleTrainingFormFacilityDistrict
import org.welbodipartnership.cradle5.domain.cradletraining.CradleTrainingFormManager
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.util.ApplicationCoroutineScope
import javax.inject.Inject

@HiltViewModel
class CradleFormDetailsViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val dbWrapper: CradleDatabaseWrapper,
  cradleFormManager: CradleTrainingFormManager,
  @ApplicationCoroutineScope private val appCoroutineScope: CoroutineScope
) : ViewModel() {
  companion object {
    private const val TAG = "CradleFormDetailsViewModel"
  }

  sealed class State {
    object Loading : State()
    class Ready(val formFacilityDistrict: CradleTrainingFormFacilityDistrict) : State()
    object Failed : State()
  }

  private val patientPrimaryKey: Long? =
    savedStateHandle[LeafScreen.PatientDetails.ARG_PATIENT_PRIMARY_KEY]

  val formDetailsStateFlow: StateFlow<State> =
    (
      patientPrimaryKey
        ?.let { pk ->
          dbWrapper.cradleTrainingFormDao().getFormFlow(pk)
            .map { formWithDistrictAndFacility ->
              if (formWithDistrictAndFacility != null) {
                State.Ready(formWithDistrictAndFacility)
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

  val editStateFlow: StateFlow<SyncRepository.FormEditState?> = cradleFormManager
    .editFormOutcomesState
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(2000L),
      initialValue = null
    )

  override fun onCleared() {
    super.onCleared()
    Log.d(TAG, "onCleared()")
  }

  fun deleteForm(form: CradleTrainingForm) {
    Log.d(TAG, "Deleting CRADLE form ${form.id}")
    appCoroutineScope.launch {
      // foreign keys are broken with SQLCipher + Room
      dbWrapper.withTransaction { db ->
        db.cradleTrainingFormDao().delete(form)
      }
    }
  }
}
