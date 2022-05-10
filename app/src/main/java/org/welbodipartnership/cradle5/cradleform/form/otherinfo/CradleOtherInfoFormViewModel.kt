package org.welbodipartnership.cradle5.cradleform.form.otherinfo

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
import org.welbodipartnership.cradle5.data.database.resultentities.CradleTrainingFormFacilityDistrict
import javax.inject.Inject

@HiltViewModel
class CradleOtherInfoFormViewModel @Inject constructor(
  private val dbWrapper: CradleDatabaseWrapper,
  private val handle: SavedStateHandle,
) : ViewModel() {
  sealed class FormState {
    object Loading : FormState()
    object Loaded : FormState()
    object Saving : FormState()
    object SaveSuccess : FormState()
    class Error(val errorMessage: String) : FormState()
  }

  private val existingPrimaryKey: Long = requireNotNull(
    handle[LeafScreen.PatientOtherInfoEdit.ARG_PATIENT_PRIMARY_KEY]
  ) { "missing existingPrimaryKey" }

  private val _formState: MutableStateFlow<FormState> = MutableStateFlow(FormState.Loading)
  val formState: StateFlow<FormState> = _formState

  val existingInfo: StateFlow<CradleTrainingFormFacilityDistrict?> =
    dbWrapper.cradleTrainingFormDao()
      .getFormFlow(existingPrimaryKey)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2000L), null)

  val localNotesState: SavedStateMutableState<String> =
    handle.createMutableState("otherInfoLocalNotes", "")

  init {
    viewModelScope.launch {
      val otherInfo = dbWrapper.cradleTrainingFormDao().getOtherInfo(existingPrimaryKey)

      _formState.value = if (otherInfo == null) {
        Log.e(TAG, "no form with primary key $existingPrimaryKey")
        FormState.Error("no form with primary key $existingPrimaryKey")
      } else {
        localNotesState.value = otherInfo.localNotes ?: ""
        FormState.Loaded
      }
    }
  }

  private val submissionActor = viewModelScope.actor<Unit>(capacity = Channel.RENDEZVOUS) {
    consumeEach {
      _formState.value = FormState.Saving
      _formState.value = try {
        val updateSuccess = dbWrapper.cradleTrainingFormDao().updateLocalNotesInfo(
          existingPrimaryKey,
          localNotes = localNotesState.value.ifBlank { null }
        )

        if (updateSuccess) {
          FormState.SaveSuccess
        } else {
          FormState.Error("Database failed to update other info")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error while saving other info", e)
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
    private const val TAG = "PatientOtherInfoForm"
  }
}
