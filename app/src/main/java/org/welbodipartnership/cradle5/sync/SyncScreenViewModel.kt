package org.welbodipartnership.cradle5.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp
import javax.inject.Inject

@HiltViewModel
class SyncScreenViewModel @Inject constructor(
  private val syncRepository: SyncRepository,
  private val dbWrapper: CradleDatabaseWrapper,
) : ViewModel() {

  val currentSyncStatusFlow: StateFlow<SyncRepository.SyncStatus?> =
    syncRepository.currentSyncStatusFlow
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
        null
      )

  val formsToUploadNoErrorsFlow: StateFlow<Int?> = dbWrapper.cradleTrainingFormDao()
    .countFormsToUploadWithoutErrors()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val formsToUploadWithErrorsFlow: StateFlow<Int?> = dbWrapper.cradleTrainingFormDao()
    .countFormsToUploadWithErrors()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val incompleteFormsToUploadCountFlow: StateFlow<Int?> = dbWrapper
    .cradleTrainingFormDao()
    .countPartialFormsToUpload()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val lastSyncCompletedTimestamp: StateFlow<UnixTimestamp?> = syncRepository
    .lastTimeSyncCompletedFlow
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val locationCheckInsToUploadCountFlow: StateFlow<Int?> = dbWrapper.locationCheckInDao()
    .countCheckInsForUpload()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  fun enqueueSync() {
    viewModelScope.launch {
      syncRepository.enqueueSyncJob()
    }
  }

  fun cancelSync() {
    viewModelScope.launch {
      syncRepository.cancelTwoWaySyncWork()
    }
  }
}
