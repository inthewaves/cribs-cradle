package org.welbodipartnership.cradle5.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.auth.AuthState
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp
import javax.inject.Inject

@HiltViewModel
class SyncScreenViewModel @Inject constructor(
  private val syncRepository: SyncRepository,
  private val authRepository: AuthRepository,
  private val dbWrapper: CradleDatabaseWrapper,
) : ViewModel() {

  val isAuthTokenExpiredFlow: StateFlow<Boolean> = authRepository.authStateFlow
    .map { state ->
      if (state is AuthState.LoggedInUnlocked) {
        state.isTokenExpired
      } else {
        false
      }
    }
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      false
    )

  val currentSyncStatusFlow: StateFlow<SyncRepository.SyncStatus?> =
    syncRepository.currentSyncStatusFlow
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
        null
      )

  val patientsToUploadCountFlow: StateFlow<Int?> = dbWrapper.patientsDao().countPatientsToUpload()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val incompletePatientsToUploadCountFlow: StateFlow<Int?> = dbWrapper.patientsDao()
    .countPartialPatientsToUpload()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val patientsWithOutcomesNotFullyUploadedWithErrorsCountFlow: StateFlow<Int?> = dbWrapper.outcomesDao()
    .countOutcomesNotFullyUploadedWithErrors()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val patientsWithOutcomesNotFullyUploadedWithoutErrorsCountFlow: StateFlow<Int?> = dbWrapper.outcomesDao()
    .countOutcomesNotFullyUploadedWithoutErrors()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val bpInfoFormsToUploadCountFlow = dbWrapper.bpInfoDao()
    .countFormsToUploadWithoutErrors()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val bpInfoFormsWithErrorsCountFlow = dbWrapper.bpInfoDao()
    .countFormsToUploadWithErrors()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
      null
    )

  val bpInfoFormsToReuploadCountFlow = dbWrapper.bpInfoDao()
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

  fun onReloginClicked() {
    viewModelScope.launch { authRepository.forceLockscreenAndServerLogin() }
  }

  fun enqueueSync() {
    viewModelScope.launch {
      syncRepository.enqueueSyncJob()
    }
  }

  fun cancelSync() {
    viewModelScope.launch {
      syncRepository.cancelAllSyncWork()
    }
  }
}
