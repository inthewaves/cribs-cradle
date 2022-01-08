package org.welbodipartnership.cradle5.domain.sync

import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import org.welbodipartnership.cradle5.util.coroutines.asFlow
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
  private val workManager: WorkManager,
  private val appValuesStore: AppValuesStore,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {

  @Immutable
  sealed class SyncStatus {
    @Immutable
    data class Active(val progress: SyncWorker.Progress?) : SyncStatus()
    @Immutable
    data class Inactive(val workState: WorkInfo.State?) : SyncStatus()
  }

  val lastTimeSyncCompletedFlow = appValuesStore.lastSyncCompletedTimestamp

  val currentSyncStatusFlow: Flow<SyncStatus> = workManager
    .getWorkInfosForUniqueWorkLiveData(SyncWorker.UNIQUE_WORK_NAME)
    .asFlow(appCoroutineDispatchers)
    .map { workInfoList ->
      workInfoList.find { it.state == WorkInfo.State.RUNNING } ?: workInfoList.firstOrNull()
    }
    .map { workInfo ->
      when (workInfo?.state) {
        WorkInfo.State.RUNNING -> {
          SyncStatus.Active(SyncWorker.getProgressFromWorkInfo(workInfo))
        }
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.SUCCEEDED,
        WorkInfo.State.FAILED,
        WorkInfo.State.BLOCKED,
        WorkInfo.State.CANCELLED -> {
          SyncStatus.Inactive(workInfo.state)
        }
        null -> SyncStatus.Inactive(null)
      }
    }
    .flowOn(appCoroutineDispatchers.default)

  /**
   * Represents whether the main fields of any forms (registration and outcomes) can be
   * edited in the app.
   */
  enum class FormEditState(val canEdit: Boolean) {
    CAN_EDIT(true),
    CANT_EDIT_SYNC_ENQUEUED(false),
    CANT_EDIT_SYNC_IN_PROGRESS(false)
  }

  val editFormState: Flow<FormEditState> = currentSyncStatusFlow
    .map { status ->
      when (status) {
        is SyncStatus.Active -> FormEditState.CANT_EDIT_SYNC_IN_PROGRESS
        is SyncStatus.Inactive -> {
          when (status.workState) {
            WorkInfo.State.ENQUEUED -> FormEditState.CANT_EDIT_SYNC_ENQUEUED
            WorkInfo.State.RUNNING -> FormEditState.CANT_EDIT_SYNC_IN_PROGRESS
            WorkInfo.State.SUCCEEDED,
            WorkInfo.State.FAILED,
            WorkInfo.State.BLOCKED,
            WorkInfo.State.CANCELLED,
            null -> FormEditState.CAN_EDIT
          }
        }
      }
    }

  fun enqueueSyncJob() {
    SyncWorker.enqueue(workManager)
  }

  suspend fun cancelAllSyncWork() {
    withContext(appCoroutineDispatchers.io) {
      workManager.cancelUniqueWork(SyncWorker.UNIQUE_WORK_NAME).await()
    }
  }

  suspend fun pruneAllWork() {
    withContext(appCoroutineDispatchers.io) {
      workManager.pruneWork().await()
    }
  }

  companion object {
    private const val TAG = "SyncRepository"
  }
}
