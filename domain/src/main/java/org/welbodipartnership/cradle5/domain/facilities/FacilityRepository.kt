package org.welbodipartnership.cradle5.domain.facilities

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.firstOrNull
import org.welbodipartnership.api.cradle5.Registration
import org.welbodipartnership.api.forms.meta.DynamicLookupListEntry
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.daos.FacilityDao
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.domain.ControlId
import org.welbodipartnership.cradle5.domain.DefaultNetworkResult
import org.welbodipartnership.cradle5.domain.FormId
import org.welbodipartnership.cradle5.domain.NetworkResult
import org.welbodipartnership.cradle5.domain.ObjectId
import org.welbodipartnership.cradle5.domain.RestApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacilityRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val restApi: RestApi,
  private val dbWrapper: CradleDatabaseWrapper,
  private val appValuesStore: AppValuesStore,
) {
  sealed class DownloadResult {
    object Success : DownloadResult()

    data class Invalid(val errorMessage: String?, val errorCode: Int) : DownloadResult()

    data class Exception(val cause: java.lang.Exception, val errorMessage: String?) : DownloadResult()
  }

  /**
   * TODO: Might be worth streaming this response from the server
   * @param districtId The district to get facilities for. If null, will get for self.
   */
  suspend fun downloadAndSaveFacilities(
    eventMessagesChannel: SendChannel<String>?,
    districtId: Int? = null
  ): DownloadResult {
    val selfDistrictId = appValuesStore.districtIdFlow.firstOrNull()
    if (districtId == null && selfDistrictId == null) {
      return DownloadResult.Exception(
        IOException("Missing district id for current user"),
        "Missing district id for current user"
      )
    }

    val result: DefaultNetworkResult<List<DynamicLookupListEntry>> = restApi
      .getDynamicLookupData(
        valuesClass = DynamicLookupListEntry::class.java,
        controlId = if (districtId == null) {
          REGISTRATION_CONTROL_ID_SELF_FACILITIES
        } else {
          REGISTRATION_CONTROL_ID_OTHER_DISTRICT_FACILITIES
        },
        formId = FormId.fromAnnotationOrThrow<Registration>(),
        objectId = ObjectId.QUERIES,
        masterValues = districtId?.let { listOf(it.toString()) } ?: emptyList()
      )

    when (result) {
      is NetworkResult.Success -> {
        Log.d(TAG, "facilities count: ${result.value.size} for district ${districtId ?: "self"}")
        eventMessagesChannel?.trySend(
          if (districtId == null)
            "Saving ${result.value.size} facilities for our own district"
          else
            "Saving ${result.value.size} facilities for district ID $districtId"
        )

        dbWrapper.withTransaction { db ->
          val dao = db.facilitiesDao()
          result.value.forEachIndexed { index, apiFacilityListItem ->
            val update = if (districtId == null) {
              FacilityDao.FacilityUpdate(
                id = apiFacilityListItem.id,
                name = apiFacilityListItem.name,
                districtId = requireNotNull(selfDistrictId),
                listOrder = index
              )
            } else {
              FacilityDao.FacilityUpdate(
                id = apiFacilityListItem.id,
                name = apiFacilityListItem.name,
                districtId = districtId,
                listOrder = index
              )
            }
            dao.upsert(update)
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

    private val REGISTRATION_CONTROL_ID_SELF_FACILITIES = ControlId("Control2092")
    private val REGISTRATION_CONTROL_ID_OTHER_DISTRICT_FACILITIES = ControlId("Control2126")
  }
}
