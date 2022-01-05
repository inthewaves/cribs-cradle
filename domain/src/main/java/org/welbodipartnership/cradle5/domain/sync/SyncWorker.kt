package org.welbodipartnership.cradle5.domain.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.domain.patients.PatientsManager
import java.util.UUID
import javax.annotation.concurrent.Immutable

@HiltWorker
class SyncWorker @AssistedInject constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val appValuesStore: AppValuesStore,
  private val patientsManager: PatientsManager,
  private val dbWrapper: CradleDatabaseWrapper,
) : CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result {
    Log.d(TAG, "I am supposed to do some work but I'm not going to do something")
    reportProgress(Stage.STARTING)

    val patientsToUpload = dbWrapper.patientsDao().getPatientsToUpload()
    reportProgress(Stage.UPLOADING_PATIENTS, doneSoFar = 0, totalToDo = patientsToUpload.size)
    coroutineScope {
      val updateChannel = actor<Int>(capacity = Channel.CONFLATED) {
        consumeEach { progress ->
          reportProgress(
            Stage.UPLOADING_PATIENTS,
            doneSoFar = progress.coerceAtMost(patientsToUpload.size),
            totalToDo = patientsToUpload.size
          )
          delay(50L)
        }
      }

      patientsToUpload.forEachIndexed { index, (patient, outcomes) ->
        // TODO: Better progress, better error handling, prevent edits while in sync,
        //  and run jobs to clean up loose ends like cases where we have a nodeId but no objectId
        if (outcomes == null) {
          Log.w(TAG, "refusing to upload patient ${patient.id} because they have no outcomes")
        } else {
          val result = patientsManager.uploadPatientAndOutcomes(patient, outcomes)
          Log.d(TAG, "result: $result")
        }

        updateChannel.trySend(index + 1)
      }

      updateChannel.close()
    }
    appValuesStore.setLastTimeSyncCompletedToNow()
    return Result.success()
  }

  private suspend fun reportProgress(stage: Stage, doneSoFar: Int, totalToDo: Int) {
    setProgress(
      workDataOf(
        PROGRESS_DATA_STAGE_KEY to stage.name,
        PROGRESS_DATA_PROGRESS_KEY to doneSoFar,
        PROGRESS_DATA_TOTAL_KEY to totalToDo
      )
    )
  }

  private suspend fun reportProgress(stage: Stage) {
    setProgress(workDataOf(PROGRESS_DATA_STAGE_KEY to stage.name,))
  }

  @Immutable
  sealed class Progress {
    abstract val stage: Stage
    @Immutable
    data class WithFiniteProgress(
      override val stage: Stage,
      val doneSoFar: Int,
      val totalToDo: Int
    ) : Progress() {
      val progressPercent = if (totalToDo == 0) 0f else doneSoFar.toFloat() / totalToDo.toFloat()
    }
    @Immutable
    class WithIndeterminateProgress(override val stage: Stage) : Progress()
  }

  @Immutable
  enum class Stage {
    STARTING,
    UPLOADING_PATIENTS,
    DOWNLOADING_FACILITIES,
    DOWNLOADING_DROPDOWN_VALUES,
  }

  companion object {
    private const val PROGRESS_DATA_STAGE_KEY = "stage"
    private const val PROGRESS_DATA_PROGRESS_KEY = "numCompleted"
    private const val PROGRESS_DATA_TOTAL_KEY = "numTotal"

    private const val TAG = "SyncWorker"
    const val UNIQUE_WORK_NAME = "SyncWorker-unique-work"

    fun getProgressFromWorkInfo(workInfo: WorkInfo): Progress? {
      val progress = workInfo.progress
      val stageString = progress.getString(PROGRESS_DATA_STAGE_KEY) ?: return null
      val stage = try {
        Stage.valueOf(stageString)
      } catch (e: IllegalArgumentException) {
        return null
      }

      val doneSoFar = progress.getInt(PROGRESS_DATA_PROGRESS_KEY, Int.MIN_VALUE)
      if (doneSoFar == Int.MIN_VALUE) {
        return Progress.WithIndeterminateProgress(stage)
      }
      val totalToDo = progress.getInt(PROGRESS_DATA_TOTAL_KEY, Int.MIN_VALUE)
      if (doneSoFar == Int.MIN_VALUE) {
        return Progress.WithIndeterminateProgress(stage)
      }

      return Progress.WithFiniteProgress(stage, doneSoFar, totalToDo)
    }

    /**
     * Enqueues a [SyncWorker] and returns the [UUID] of the work
     */
    fun enqueue(workManager: WorkManager): UUID {
      val request = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(
          Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        )
        .build()
      workManager.enqueueUniqueWork(
        UNIQUE_WORK_NAME,
        ExistingWorkPolicy.APPEND_OR_REPLACE,
        request
      )
      return request.id
    }
  }
}
