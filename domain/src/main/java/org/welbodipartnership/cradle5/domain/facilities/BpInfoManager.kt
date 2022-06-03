package org.welbodipartnership.cradle5.domain.facilities

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.welbodipartnership.api.cradle5.FacilityBpData
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.daos.FacilityBpInfoDao
import org.welbodipartnership.cradle5.data.database.entities.FacilityBpInfo
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.domain.RestApi
import org.welbodipartnership.cradle5.domain.getErrorMessageOrNull
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BpInfoManager @Inject constructor(
  private val restApi: RestApi,
  private val dbWrapper: CradleDatabaseWrapper,
  private val syncRepository: SyncRepository,
  @ApplicationContext private val context: Context,
) {
  sealed class UploadResult {
    data class Failure(
      val error: RestApi.PostResult,
      val serverErrorMessage: String?
    ) : UploadResult()

    object NoMetaInfoFailure : UploadResult()
    object Success : UploadResult()
  }

  suspend fun uploadForm(form: FacilityBpInfo): UploadResult {
    val dao: FacilityBpInfoDao = dbWrapper.bpInfoDao()
    Log.d(TAG, "uploadCradleForm() with form primary key ${form.id}")

    var isMetaMissing = false
    val serverErrorMessage: String?
    val serverInfo: ServerInfo = when (
      val result = restApi.multiStagePostBpInfoForm(form)
    ) {
      is RestApi.PostResult.AllFailed -> {
        serverErrorMessage = result.failureBody?.modelState?.asSequence()
          ?.map { (controlId, errorMessageList) ->
            val fieldName =
              FacilityBpData.controlIdToNameMap.getOrDefault(controlId, controlId)
            val errorMessages = errorMessageList.joinToString()
            "$fieldName: $errorMessages"
          }
          ?.joinToString("\n\n")
          ?: result.failOrException?.getErrorMessageOrNull(context)

        if (serverErrorMessage != null) {
          Log.w(TAG, "received error message from server; inserting")
        }
        return UploadResult.Failure(result, serverErrorMessage)
      }
      is RestApi.PostResult.MetaInfoRetrievalFailed -> {
        Log.w(TAG, "only got partial patient info")
        serverErrorMessage = result.failOrException?.getErrorMessageOrNull(context)
        dao.updateWithServerErrorMessage(form.id, "Failed to get form created date")
        isMetaMissing = true
        result.partialServerInfo
      }
      is RestApi.PostResult.Success -> {
        serverErrorMessage = null
        result.serverInfo
      }
      is RestApi.PostResult.AlreadyUploaded -> {
        serverErrorMessage = null
        result.serverInfo
      }
    }

    dbWrapper.withTransaction {
      dao.apply {
        updateWithServerInfo(form.id, serverInfo)
        updateWithServerErrorMessage(form.id, serverErrorMessage)
        clearDraftStatus(form.id)
      }
    }
    return if (!isMetaMissing) UploadResult.Success else UploadResult.NoMetaInfoFailure
  }

  companion object {
    private const val TAG = "BpInfoManager"
  }
}
