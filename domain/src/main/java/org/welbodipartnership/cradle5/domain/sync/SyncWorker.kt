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
) : BaseSyncWorker(appContext, workerParams) {

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
            append("Failure for form $formName and id $id: ")
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
    stage: BaseSyncWorker.Stage,
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
          BaseSyncWorker.Stage.UPLOADING_LOCATION_CHECK_INS,
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



  companion object {
    private const val TAG = "SyncWorker"

    /**
     * Enqueues a [SyncWorker], which performs 2-way syncing
     */
    fun enqueue(workManager: WorkManager) {
      val request = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(
          Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        )
        .addTag(WORK_TAG)
        .build()
      workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
  }
}
