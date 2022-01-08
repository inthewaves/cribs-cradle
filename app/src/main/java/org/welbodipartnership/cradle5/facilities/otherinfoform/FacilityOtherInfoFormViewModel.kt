package org.welbodipartnership.cradle5.facilities.otherinfoform

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.LeafScreen
import org.welbodipartnership.cradle5.compose.SavedStateMutableState
import org.welbodipartnership.cradle5.compose.createMutableState
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Facility
import javax.inject.Inject

@HiltViewModel
class FacilityOtherInfoFormViewModel @Inject constructor(
  private val dbWrapper: CradleDatabaseWrapper,
  handle: SavedStateHandle,
) : ViewModel() {
  sealed class FormState {
    object Loading : FormState()
    object Loaded : FormState()
    object Saving : FormState()
    object SaveSuccess : FormState()
    class Error(val errorMessage: String) : FormState()
  }

  private val existingFacilityPrimaryKey: Long = requireNotNull(
    handle[LeafScreen.FacilityDetails.ARG_FACILITY_PRIMARY_KEY]
  ) { "missing existingFacilityPrimaryKey" }

  private val _formState: MutableStateFlow<FormState> = MutableStateFlow(FormState.Loading)
  val formState: StateFlow<FormState> = _formState

  val facilityFlow: StateFlow<Facility?> = dbWrapper.facilitiesDao()
    .getFacilityFlow(existingFacilityPrimaryKey)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2000L), null)

  val hasVisitedState: SavedStateMutableState<Boolean?> =
    handle.createMutableState("facilityOtherInfoHasVisited", null)
  val localNotesState: SavedStateMutableState<String> =
    handle.createMutableState("facilityOtherInfoLocalNotes", "")

  init {
    viewModelScope.launch {
      val facility = dbWrapper.facilitiesDao().getFacility(existingFacilityPrimaryKey)

      _formState.value = if (facility == null) {
        Log.e(TAG, "no facility with primary key $existingFacilityPrimaryKey")
        FormState.Error("No patient with primary key $existingFacilityPrimaryKey")
      } else {
        hasVisitedState.value = facility.hasVisited
        localNotesState.value = facility.localNotes ?: ""

        FormState.Loaded
      }
    }
  }

  private val submissionActor = viewModelScope.actor<Unit>(capacity = Channel.RENDEZVOUS) {
    consumeEach {
      _formState.value = FormState.Saving
      _formState.value = try {
        val updateSuccess = dbWrapper.facilitiesDao().updateFacilityOtherInfo(
          existingFacilityPrimaryKey,
          hasVisited = hasVisitedState.value!!,
          localNotes = localNotesState.value.ifBlank { null }
        )

        if (updateSuccess) {
          FormState.SaveSuccess
        } else {
          FormState.Error("Database failed to update other info")
        }
      } catch (e: Exception) {
        Log.e(TAG, "rror while saving other info", e)
        FormState.Error(
          "${e::class.java.simpleName}: ${e.localizedMessage}: ${e.stackTraceToString()}"
        )
      }
    }
  }

  fun submit() {
    submissionActor.trySend(Unit)
  }

  companion object {
    private const val TAG = "FacilityOtherInfoForm"
  }
}
