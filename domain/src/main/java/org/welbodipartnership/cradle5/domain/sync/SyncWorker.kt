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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.acra.ktx.sendSilentlyWithAcra
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.domain.DefaultNetworkResult
import org.welbodipartnership.cradle5.domain.RestApi
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.cradletraining.CradleTrainingFormManager
import org.welbodipartnership.cradle5.domain.toApiBody
import org.welbodipartnership.cradle5.util.ApplicationCoroutineScope
import java.security.SecureRandom
import javax.annotation.concurrent.Immutable
import kotlin.random.asKotlinRandom
import kotlin.random.nextLong
import kotlin.time.Duration.Companion.seconds

@HiltWorker
class SyncWorker @AssistedInject constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val appValuesStore: AppValuesStore,
  private val cradleFormsManager: CradleTrainingFormManager,
  private val dbWrapper: CradleDatabaseWrapper,
  private val restApi: RestApi,
  private val authRepository: AuthRepository,
  @ApplicationCoroutineScope private val appCoroutineScope: CoroutineScope
) : CoroutineWorker(appContext, workerParams) {

  private val secureRandom = SecureRandom()
  private val kotlinSecureRadnom = secureRandom.asKotlinRandom()

  private suspend fun delayWithRetryJitter() {
    val delayMillis = kotlinSecureRadnom.nextLong(1000L..4000L)
    Log.d(TAG, "delaying for $delayMillis ms")
    delay(delayMillis)
  }

  data class FailureInfo(
    val formName: String,
    val id: Long,
    val failedResultString: String
  ) {
    companion object {
      fun fromCradleFormResult(id: Long, result: CradleTrainingFormManager.UploadResult): FailureInfo {
        val errorMessage = buildString {
          when (result) {
            is CradleTrainingFormManager.UploadResult.Failure -> {
              append("FormId $id failed to upload with error message[")
              append(result.serverErrorMessage)
              append("] and error")
              append(result.error)
            }
            CradleTrainingFormManager.UploadResult.NoMetaInfoFailure -> {
              append("Failed to retrieve meta info for form $id")
            }
            CradleTrainingFormManager.UploadResult.Success -> { /* nothing */
            }
          }
        }
        return FailureInfo("CradleTrainingForm", id, errorMessage)
      }

      fun locationFormResult(id: Long, result: DefaultNetworkResult<*>): FailureInfo {
        return FailureInfo("LocationCheckIn", id, result.toString())
      }
    }
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
        .filter { it.id !in newPatientsUploadResult.failedPatientIds }
    )

    Log.d(TAG, "uploading check ins")
    val checkInFailures = runUploadForCheckIns(dbWrapper.locationCheckInDao().getCheckInsForUpload())

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

    if (newPatientsUploadResult.nonSuccessResults.isNotEmpty() || checkInFailures.isNotEmpty()) {
      val failedResults = newPatientsUploadResult.nonSuccessResults.asSequence() +
        checkInFailures.asSequence()
      Log.w(TAG, "encountered errors; launching delayed coroutine to report them")
      // FIXME: Figure out why ACRA is cancelling our workers.
      appCoroutineScope.launch {
        delay(1.seconds)
        Log.w(TAG, "reporting errors to ACRA them")
        val errorMessage = buildString {
          failedResults.forEach { (formName, id, message) ->
            append("Failure for form $formName and id $id with message")
            appendLine(message)
          }
        }.trimEnd('\n')
        SyncException(errorMessage).sendSilentlyWithAcra()
      }
    }

    return Result.success()
  }

  private data class CradleFormUploadResult(
    val successfulPatientIds: Set<Long>,
    val failedPatientIds: Set<Long>,
    val nonSuccessResults: List<FailureInfo>
  )

  private suspend fun runUploadForForms(
    stage: Stage,
    forms: List<CradleTrainingForm>
  ): CradleFormUploadResult = coroutineScope {
    val successfulPatientIds = linkedSetOf<Long>()
    val failedPatientIds = linkedSetOf<Long>()
    val partialUploadIds = linkedSetOf<Long>()
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

    val failedResults = mutableListOf<FailureInfo>()
    forms.forEachIndexed { index, form ->
      val result = cradleFormsManager.uploadCradleForm(form)
      Log.d(TAG, "form result: $result")
      if (result is CradleTrainingFormManager.UploadResult.Success) {
        successfulPatientIds.add(form.id)
      } else {
        failedResults.add(FailureInfo.fromCradleFormResult(form.id, result))
        if (result is CradleTrainingFormManager.UploadResult.NoMetaInfoFailure) {
          partialUploadIds.add(form.id)
        } else {
          failedPatientIds.add(form.id)
        }
        Log.d(TAG, "got a failed result; applying retry jitter")
        delayWithRetryJitter()
      }
      updateChannel.trySend(index + 1)
    }
    updateChannel.close()

    CradleFormUploadResult(successfulPatientIds, failedPatientIds, failedResults)
  }

  class SyncException(override val message: String) : Exception(message)

  private suspend fun runUploadForCheckIns(
    checkIns: List<LocationCheckIn>
  ): List<FailureInfo> = coroutineScope {
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
    val failureInfo = mutableListOf<FailureInfo>()
    try {
      val userId = appValuesStore.userIdFlow.firstOrNull() ?: return@coroutineScope emptyList()
      checkIns.forEachIndexed { index, checkIn ->
        if (checkIn.isUploaded) {
          Log.w(TAG, "refusing to upload checkIn ${checkIn.id} because already uploaded")
        } else {
          val result = restApi.postGpsForm(checkIn.toApiBody(userId))
          Log.d(TAG, "result: $result")
          if (result.isSuccess) {
            dbWrapper.locationCheckInDao().markAsUploaded(checkInId = checkIn.id)
          } else {
            failureInfo.add(FailureInfo.locationFormResult(checkIn.id, result))
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
    return@coroutineScope failureInfo
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
