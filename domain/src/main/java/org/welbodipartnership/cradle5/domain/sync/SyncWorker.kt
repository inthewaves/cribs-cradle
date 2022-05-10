package org.welbodipartnership.cradle5.domain.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.domain.RestApi
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.cradletraining.CradleTrainingFormManager
import org.welbodipartnership.cradle5.domain.toApiBody
import java.security.SecureRandom
import javax.annotation.concurrent.Immutable
import kotlin.random.asKotlinRandom
import kotlin.random.nextLong

@HiltWorker
class SyncWorker @AssistedInject constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val appValuesStore: AppValuesStore,
  private val cradleFormsManager: CradleTrainingFormManager,
  private val dbWrapper: CradleDatabaseWrapper,
  private val restApi: RestApi,
  private val authRepository: AuthRepository,
) : CoroutineWorker(appContext, workerParams) {

  private val secureRandom = SecureRandom()
  private val kotlinSecureRadnom = secureRandom.asKotlinRandom()

  private suspend fun delayWithRetryJitter() {
    val delayMillis = kotlinSecureRadnom.nextLong(1000L..4000L)
    Log.d(TAG, "delaying for $delayMillis ms")
    delay(delayMillis)
  }

  override suspend fun doWork(): Result {
    Log.d(TAG, "starting SyncWorker")
    reportProgress(Stage.STARTING)

    Log.d(TAG, "uploading forms")
    val newPatientsUploadResult = runUploadForForms(
      Stage.UPLOADING_NEW_CRADLE_FORMS,
      dbWrapper
        .cradleTrainingFormDao()
        .getNewFormsToUploadOrderedById()
    )

    Log.d(TAG, "uploading any failed forms")
    val failedPatientsUploadResult = runUploadForForms(
      Stage.UPLOADING_INCOMPLETE_PATIENTS,
      dbWrapper
        .cradleTrainingFormDao()
        .getFormsWithPartialServerInfoOrderedById()
    )

    Log.d(TAG, "uploading check ins")
    runUploadForCheckIns(dbWrapper.locationCheckInDao().getCheckInsForUpload())

    coroutineScope {
      val progressChannel = actor<AuthRepository.InfoSyncProgress>(capacity = Channel.CONFLATED) {
        consumeEach {
          Log.d(TAG, "${it.stage}: ${it.text}")
          reportInfoProgress(it.stage, it.text)
        }
      }
      try {
        authRepository.doLoginInfoSync(
          AuthRepository.InfoSyncProgressReceiver.StageAndStringMessages(progressChannel)
        )
      } finally {
        progressChannel.close()
      }
    }

    appValuesStore.setLastTimeSyncCompletedToNow()
    return Result.success()
  }

  private data class CradleFormUploadResult(
    val successfulPatientIds: Set<Long>,
    val failedPatientIds: Set<Long>
  )

  private suspend fun runUploadForForms(
    stage: Stage,
    forms: List<CradleTrainingForm>
  ): CradleFormUploadResult = coroutineScope {
    val successfulPatientIds = linkedSetOf<Long>()
    val failedPatientIds = linkedSetOf<Long>()
    reportProgress(stage, doneSoFar = 0, totalToDo = forms.size)
    val updateChannel = actor<Int>(capacity = Channel.CONFLATED) {
      consumeEach { progress ->
        reportProgress(
          stage,
          doneSoFar = progress.coerceAtMost(forms.size),
          totalToDo = forms.size,
          // don't really care about thread-safety here
          numFailed = failedPatientIds.size
        )
        // prevent contention with WorkManager's database
        delay(75L)
      }
    }

    forms.forEachIndexed { index, form ->
      val result = cradleFormsManager.uploadCradleForm(form)
      Log.d(TAG, "form result: $result")
      if (result is CradleTrainingFormManager.UploadResult.Success) {
        successfulPatientIds.add(form.id)
      } else {
        failedPatientIds.add(form.id)
        Log.d(TAG, "got a failed result; applying retry jitter")
        delayWithRetryJitter()
      }
      updateChannel.trySend(index + 1)
    }
    updateChannel.close()

    CradleFormUploadResult(successfulPatientIds, failedPatientIds)
  }

  private suspend fun runUploadForCheckIns(checkIns: List<LocationCheckIn>): Unit = coroutineScope {
    reportProgress(
      Stage.UPLOADING_LOCATION_CHECK_INS,
      doneSoFar = 0,
      totalToDo = checkIns.size
    )
    val failedCheckInIds = mutableListOf<Long>()
    val updateChannel = actor<Int>(capacity = Channel.CONFLATED) {
      consumeEach { progress ->
        reportProgress(
          Stage.UPLOADING_LOCATION_CHECK_INS,
          doneSoFar = progress.coerceAtMost(checkIns.size),
          totalToDo = checkIns.size,
          // don't really care about thread-safety here
          numFailed = failedCheckInIds.size
        )
        // prevent contention with WorkManager's database
        delay(75L)
      }
    }

    Log.d(TAG, "getting userId")
    try {
      val userId = appValuesStore.userIdFlow.firstOrNull() ?: return@coroutineScope
      checkIns.forEachIndexed { index, checkIn ->
        if (checkIn.isUploaded) {
          Log.w(TAG, "refusing to upload checkIn ${checkIn.id} because already uploaded")
        } else {
          val result = restApi.postGpsForm(checkIn.toApiBody(userId))
          Log.d(TAG, "result: $result")
          if (result.isSuccess) {
            dbWrapper.locationCheckInDao().markAsUploaded(checkInId = checkIn.id)
          } else {
            failedCheckInIds.add(checkIn.id)
            Log.d(TAG, "got a failed result; applying retry jitter")
            delayWithRetryJitter()
          }
        }
        updateChannel.trySend(index + 1)
      }
    } finally {
      updateChannel.close()
    }
  }

  private suspend fun reportProgress(
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

  private suspend fun reportInfoProgress(
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

  companion object {
    private const val PROGRESS_DATA_STAGE_KEY = "stage"
    private const val PROGRESS_DATA_PROGRESS_KEY = "numCompleted"
    private const val PROGRESS_DATA_TOTAL_KEY = "numTotal"
    private const val PROGRESS_DATA_FAILED_KEY = "numFailed"

    private const val PROGRESS_DATA_SYNC_INFO_STAGE_KEY = "infoSyncStage"
    private const val PROGRESS_DATA_SYNC_INFO_TEXT_KEY = "infoSyncText"

    private const val TAG = "SyncWorker"
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

    /**
     * Enqueues a [SyncWorker]
     */
    fun enqueue(workManager: WorkManager) {
      val request = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(
          Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        )
        .build()
      workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
  }
}
