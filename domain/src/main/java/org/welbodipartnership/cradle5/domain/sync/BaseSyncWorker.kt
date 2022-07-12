package org.welbodipartnership.cradle5.domain.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import javax.annotation.concurrent.Immutable

abstract class BaseSyncWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  protected suspend fun reportProgress(
    stage: Stage,
    doneSoFar: Int = 0,
    totalToDo: Int = 0,
    numFailed: Int = 0
  ) {
    if (doneSoFar == 0 && totalToDo == 0 && numFailed == 0) {
      setProgress(Data.Builder().putString(PROGRESS_DATA_STAGE_KEY, stage.name).build())
    } else {
      setProgress(
        Data.Builder()
          .putString(PROGRESS_DATA_STAGE_KEY, stage.name)
          .putInt(PROGRESS_DATA_PROGRESS_KEY, doneSoFar)
          .putInt(PROGRESS_DATA_TOTAL_KEY, totalToDo)
          .putInt(PROGRESS_DATA_FAILED_KEY, numFailed)
          .build()
      )
    }
  }

  protected suspend fun reportInfoProgress(
    infoSyncStage: AuthRepository.InfoSyncStage,
    infoSyncText: String
  ) {
    setProgress(
      Data.Builder()
        .putString(PROGRESS_DATA_STAGE_KEY, Stage.PERFORMING_INFO_SYNC.name)
        .putString(PROGRESS_DATA_SYNC_INFO_STAGE_KEY, infoSyncStage.name)
        .putString(PROGRESS_DATA_SYNC_INFO_TEXT_KEY, infoSyncText)
        .build()
    )
  }

  companion object {
    private const val PROGRESS_DATA_STAGE_KEY = "stage"
    private const val PROGRESS_DATA_PROGRESS_KEY = "numCompleted"
    private const val PROGRESS_DATA_TOTAL_KEY = "numTotal"
    private const val PROGRESS_DATA_FAILED_KEY = "numFailed"

    private const val PROGRESS_DATA_SYNC_INFO_STAGE_KEY = "infoSyncStage"
    private const val PROGRESS_DATA_SYNC_INFO_TEXT_KEY = "infoSyncText"

    const val WORK_TAG = "BaseSyncWorker-tag"

    /**
     * Unique work queue shared between all BaseSyncWorkers
     */
    const val UNIQUE_WORK_NAME = "SyncWorker-unique-work"

    private fun Data.getStageOrNull(): Stage? {
      val stageString = getString(PROGRESS_DATA_STAGE_KEY) ?: return null
      return try {
        Stage.valueOf(stageString)
      } catch (e: IllegalArgumentException) {
        null
      }
    }

    fun getProgressFromWorkInfo(workInfo: WorkInfo): Progress? {
      val progress = workInfo.progress
      val stage = progress.getStageOrNull() ?: return null

      when (stage) {
        Stage.PERFORMING_INFO_SYNC -> {
          val syncStage = progress.getString(PROGRESS_DATA_SYNC_INFO_STAGE_KEY)?.let {
            try {
              AuthRepository.InfoSyncStage.valueOf(it)
            } catch (e: IllegalArgumentException) {
              null
            }
          }
          val syncText = progress.getString(PROGRESS_DATA_SYNC_INFO_TEXT_KEY)
          return Progress.InfoSync(
            stage = Stage.PERFORMING_INFO_SYNC,
            infoSyncStage = syncStage,
            infoSyncText = syncText
          )
        }
        Stage.STARTING,
        Stage.UPLOADING_NEW_CRADLE_FORMS,
        Stage.UPLOADING_INCOMPLETE_PATIENTS,
        Stage.UPLOADING_LOCATION_CHECK_INS -> {
          val doneSoFar = progress.getInt(PROGRESS_DATA_PROGRESS_KEY, Int.MIN_VALUE)
          if (doneSoFar == Int.MIN_VALUE) {
            return Progress.WithIndeterminateProgress(stage)
          }
          val totalToDo = progress.getInt(PROGRESS_DATA_TOTAL_KEY, Int.MIN_VALUE)
          if (doneSoFar == Int.MIN_VALUE) {
            return Progress.WithIndeterminateProgress(stage)
          }
          val numFailed = progress.getInt(PROGRESS_DATA_FAILED_KEY, Int.MIN_VALUE)
            .takeUnless { it == Int.MIN_VALUE }
            ?: 0

          return Progress.WithFiniteProgress(stage, doneSoFar, totalToDo, numFailed)
        }
      }
    }
  }

  @Immutable
  sealed class Progress {
    abstract val stage: Stage
    @Immutable
    data class WithFiniteProgress(
      override val stage: Stage,
      val doneSoFar: Int,
      val totalToDo: Int,
      val numFailed: Int,
    ) : Progress() {
      val progressPercent = if (totalToDo == 0) 0f else doneSoFar.toFloat() / totalToDo.toFloat()
    }
    @Immutable
    class WithIndeterminateProgress(override val stage: Stage) : Progress()
    @Immutable
    class InfoSync(
      override val stage: Stage,
      val infoSyncStage: AuthRepository.InfoSyncStage?,
      val infoSyncText: String?,
    ) : Progress()
  }

  @Immutable
  enum class Stage {
    STARTING,
    UPLOADING_NEW_CRADLE_FORMS,
    /**
     * The stage when we are uploading patients for which we have stored a NodeId but not an
     * ObjectId. An ObjectId is strictly required for posting an outcome for a patient.
     */
    UPLOADING_INCOMPLETE_PATIENTS,
    UPLOADING_LOCATION_CHECK_INS,
    PERFORMING_INFO_SYNC
  }
}