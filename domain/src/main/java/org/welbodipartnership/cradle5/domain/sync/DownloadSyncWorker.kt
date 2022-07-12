package org.welbodipartnership.cradle5.domain.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.domain.auth.AuthState
import org.welbodipartnership.cradle5.util.ApplicationCoroutineScope
import java.time.Duration

@HiltWorker
class DownloadSyncWorker @AssistedInject constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val authRepository: AuthRepository,
  @ApplicationCoroutineScope private val appCoroutineScope: CoroutineScope
) : BaseSyncWorker(appContext, workerParams) {
  companion object {
    private const val TAG = "InfoSyncWorker"
    const val PERIODIC_JOB_NAME = "DownloadSyncWorker-periodic"

    fun enqueue(workManager: WorkManager) {
      val request = OneTimeWorkRequestBuilder<DownloadSyncWorker>()
        .setConstraints(
          Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        )
        .addTag(WORK_TAG)
        .build()
      workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    fun enqueuePerioidic(workManager: WorkManager, forceReplacePendingWork: Boolean) {
      val request = PeriodicWorkRequestBuilder<DownloadSyncWorker>(
        repeatInterval = Duration.ofDays(3),
        flexTimeInterval = Duration.ofHours(12)
      ).setConstraints(
        Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()
      ).addTag(WORK_TAG)
        .build()
      workManager.enqueueUniquePeriodicWork(
        PERIODIC_JOB_NAME,
        if (forceReplacePendingWork) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
        request
      )
    }
  }

  override suspend fun doWork(): Result {
    val authState = authRepository.authStateFlow.firstOrNull() ?: return Result.failure()
    if (!authState.hasValidToken) {
      Log.w(TAG, "Cancelling work due to invalid auth token")
      return Result.failure()
    }

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

    return Result.success()
  }
}