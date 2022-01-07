package org.welbodipartnership.cradle5.domain.facilities

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.SendChannel
import org.welbodipartnership.api.cradle5.HealthcareFacilityDynamicLookupEntry
import org.welbodipartnership.api.cradle5.Registration
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.daos.FacilityDao
import org.welbodipartnership.cradle5.domain.ControlId
import org.welbodipartnership.cradle5.domain.DefaultNetworkResult
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

  /**
   * TODO: Might be worth streaming this response from the server
   */
  suspend fun downloadAndSaveFacilities(
    eventMessagesChannel: SendChannel<String>?
  ): DownloadResult {
    when (
      val result: DefaultNetworkResult<List<HealthcareFacilityDynamicLookupEntry>> = restApi
        .getDynamicLookupData(
          HealthcareFacilityDynamicLookupEntry::class.java,
          ControlId("Control2092"),
          FormId.fromAnnotationOrThrow<Registration>(),
          ObjectId.QUERIES
        )
    ) {
      is NetworkResult.Success -> {
        Log.d(
          TAG,
          "facilities count: ${result.value.size}"
        )
        eventMessagesChannel?.trySend(
          "Saving ${result.value.size} facilities"
        )
        dbWrapper.withTransaction { db ->
          val dao = db.facilitiesDao()
          result.value.forEachIndexed { index, apiFacility ->
            dao.upsert(FacilityDao.FacilityUpdate(apiFacility.id, apiFacility.name, index))
          }
        }
      }
      is NetworkResult.Failure -> {
        val message = result.errorValue.decodeToString()
        val error = "Unable to get facilities: HTTP ${result.statusCode} error (message: $message)"
        eventMessagesChannel?.trySend(error)
        return DownloadResult.Invalid(error, result.statusCode)
      }
      is NetworkResult.NetworkException -> {
        val formatted = result.formatErrorMessage(context)
        eventMessagesChannel?.trySend("Unable to get facilities: $formatted")
        return DownloadResult.Exception(result.cause, formatted)
      }
    }

    return DownloadResult.Success
  }

  companion object {
    private const val TAG = "FacilityRepository"
  }
}
