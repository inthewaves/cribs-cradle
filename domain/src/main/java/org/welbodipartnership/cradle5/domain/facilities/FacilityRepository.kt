package org.welbodipartnership.cradle5.domain.facilities

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.SendChannel
import org.welbodipartnership.api.cradle5.HealthcareFacilityLookupEntry
import org.welbodipartnership.api.cradle5.Registration
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.domain.ControlId
import org.welbodipartnership.cradle5.domain.FormId
import org.welbodipartnership.cradle5.domain.NetworkResult
import org.welbodipartnership.cradle5.domain.ObjectId
import org.welbodipartnership.cradle5.domain.RestApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacilityRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val restApi: RestApi,
  private val dbWrapper: CradleDatabaseWrapper
) {
  sealed class DownloadResult {
    object Success : DownloadResult()

    data class Invalid(val errorMessage: String?, val errorCode: Int) : DownloadResult()

    data class Exception(val cause: java.lang.Exception, val errorMessage: String?) : DownloadResult()
  }

  suspend fun downloadAndSaveFacilities(
    eventMessagesChannel: SendChannel<String>?
  ): DownloadResult {
    when (
      val result = restApi
        .getDynamicLookupData<HealthcareFacilityLookupEntry>(
          ControlId("Control2092"),
          FormId.fromAnnotationOrThrow<Registration>(),
          ObjectId.QUERIES
        )
    ) {
      is NetworkResult.Success -> {
        Log.d(
          TAG,
          "facilities count: ${result.value.totalNumberOfRecords}," +
            " pages: ${result.value.totalNumberOfPages}"
        )
        dbWrapper.withTransaction { db ->
          eventMessagesChannel?.trySend(
            "Saving ${result.value.totalNumberOfRecords} facilities"
          )
          val dao = db.facilitiesDao()
          result.value.results.forEach { dao.upsert(Facility(it.id, it.name)) }
        }
      }
      is NetworkResult.Failure -> {
        val message = result.errorValue.decodeToString()
        return DownloadResult.Invalid(
          "Unable to get facilities: HTTP ${result.statusCode} error (message: $message)",
          result.statusCode
        )
      }
      is NetworkResult.NetworkException -> {
        return DownloadResult.Exception(result.cause, result.formatErrorMessage(context))
      }
    }

    return DownloadResult.Success
  }

  companion object {
    private const val TAG = "FacilityRepository"
  }
}
