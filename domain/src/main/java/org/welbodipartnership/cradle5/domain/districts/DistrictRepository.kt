package org.welbodipartnership.cradle5.domain.districts

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import org.welbodipartnership.api.cradle5.CradleImplementationData
import org.welbodipartnership.api.forms.meta.DynamicLookupListEntry
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.domain.ControlId
import org.welbodipartnership.cradle5.domain.DefaultNetworkResult
import org.welbodipartnership.cradle5.domain.FormId
import org.welbodipartnership.cradle5.domain.NetworkResult
import org.welbodipartnership.cradle5.domain.ObjectId
import org.welbodipartnership.cradle5.domain.RestApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DistrictRepository @Inject constructor(
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
  suspend fun downloadAndSaveDistricts(
    eventMessagesChannel: SendChannel<String>?
  ): DownloadResult = coroutineScope {
    when (
      val result: DefaultNetworkResult<List<DynamicLookupListEntry>> = restApi
        .getDynamicLookupData(
          DynamicLookupListEntry::class.java,
          ControlId("Control2159"),
          FormId.fromAnnotationOrThrow<CradleImplementationData>(),
          ObjectId.QUERIES
        )
    ) {
      is NetworkResult.Success -> {
        ensureActive()
        Log.d(TAG, "districts count: ${result.value.size}")
        eventMessagesChannel?.trySend("Saving ${result.value.size} districts")
        dbWrapper.withTransaction { db ->
          val dao = db.districtDao()

          result.value.forEachIndexed { _, apiFacilityListItem ->
            ensureActive()
            dao.upsert(District(apiFacilityListItem.id.toLong(), apiFacilityListItem.name))
          }
        }
      }
      is NetworkResult.Failure -> {
        val message = result.errorValue.decodeToString()
        val error = "Unable to get districts: HTTP ${result.statusCode} error (message: $message)"
        eventMessagesChannel?.trySend(error)
        return@coroutineScope DownloadResult.Invalid(error, result.statusCode)
      }
      is NetworkResult.NetworkException -> {
        val formatted = result.formatErrorMessage(context)
        val errorMessage = "Unable to get districts: $formatted"
        eventMessagesChannel?.trySend(errorMessage)
        return@coroutineScope DownloadResult.Exception(result.cause, errorMessage)
      }
    }

    return@coroutineScope DownloadResult.Success
  }

  companion object {
    private const val TAG = "DistrictRepository"
  }
}
