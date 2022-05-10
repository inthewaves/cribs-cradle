package org.welbodipartnership.cradle5.domain.cradletraining

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.welbodipartnership.api.cradle5.CradleImplementationData
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.domain.RestApi
import org.welbodipartnership.cradle5.domain.getErrorMessageOrNull
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CradleTrainingFormManager @Inject constructor(
  private val restApi: RestApi,
  private val dbWrapper: CradleDatabaseWrapper,
  private val syncRepository: SyncRepository,
  @ApplicationContext private val context: Context,
) {

  val editFormOutcomesState = syncRepository.editFormState

  sealed class UploadResult {
    data class Failure(
      val error: RestApi.PostResult,
      val serverErrorMessage: String?
    ) : UploadResult()

    object NoMetaInfoFailure : UploadResult()
    object Success : UploadResult()
  }

  suspend fun uploadCradleForm(form: CradleTrainingForm): UploadResult {
    val dao = dbWrapper.cradleTrainingFormDao()
    Log.d(TAG, "uploadCradleForm() with form primary key ${form.id}")

    var isMetaMissing = false
    val serverInfo: ServerInfo = when (
      val result = restApi.multiStagePostCradleTrainingForm(form)
    ) {
      is RestApi.PostResult.AllFailed -> {
        val serverErrorMessage = result.failureBody?.modelState?.asSequence()
          ?.map { (controlId, errorMessageList) ->
            val fieldName =
              CradleImplementationData.controlIdToNameMap.getOrDefault(controlId, controlId)
            val errorMessages = errorMessageList.joinToString()
            "$fieldName: $errorMessages"
          }
          ?.joinToString("\n\n")
          ?: result.failOrException?.getErrorMessageOrNull(context)

        if (serverErrorMessage != null) {
          Log.w(TAG, "received error message from server; inserting")
          dao.updateWithServerErrorMessage(form.id, serverErrorMessage)
        }
        return UploadResult.Failure(result, serverErrorMessage)
      }
      is RestApi.PostResult.MetaInfoRetrievalFailed -> {
        Log.w(TAG, "only got partial patient info")
        dao.updateWithServerErrorMessage(form.id, "Failed to get form created date")
        isMetaMissing = true
        result.partialServerInfo
      }
      is RestApi.PostResult.Success -> result.serverInfo
      is RestApi.PostResult.AlreadyUploaded -> result.serverInfo
    }
    dbWrapper.withTransaction {
      dao.updateWithServerInfo(form.id, serverInfo)
      if (!isMetaMissing) {
        dao.updateWithServerErrorMessage(form.id, null)
      }
      dao.clearDraftStatus(form.id)
    }
    return if (!isMetaMissing) UploadResult.Success else UploadResult.NoMetaInfoFailure
  }

  companion object {
    private const val TAG = "CradleTrainingFormManager"
  }
}
