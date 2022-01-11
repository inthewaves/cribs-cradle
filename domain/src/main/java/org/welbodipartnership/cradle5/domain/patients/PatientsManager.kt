package org.welbodipartnership.cradle5.domain.patients

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.welbodipartnership.api.cradle5.Outcome
import org.welbodipartnership.api.cradle5.Registration
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.domain.ObjectId
import org.welbodipartnership.cradle5.domain.RestApi
import org.welbodipartnership.cradle5.domain.getErrorMessageOrNull
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientsManager @Inject constructor(
  private val restApi: RestApi,
  private val dbWrapper: CradleDatabaseWrapper,
  private val syncRepository: SyncRepository,
  @ApplicationContext private val context: Context,
) {

  val editPatientsOutcomesState = syncRepository.editFormState

  sealed class UploadResult {
    data class PatientFailure(
      val error: RestApi.PostResult,
      val serverErrorMessage: String?
    ) : UploadResult()
    object NoPatientObjectIdFailure : UploadResult()
    data class OutcomesFailure(
      val error: RestApi.PostResult,
      val serverErrorMessage: String?
    ) : UploadResult()
    object NoOutcomesObjectIdFailure : UploadResult()
    object Success : UploadResult()
  }

  suspend fun uploadPatientAndOutcomes(
    patient: Patient,
    outcomes: Outcomes
  ): UploadResult {
    Log.d(
      TAG,
      "uploadPatientAndOutcomes() with " +
        "patient with id ${patient.id}, outcomes id ${outcomes.id}"
    )
    require(outcomes.patientId == patient.id) {
      "outcomes patientId doesn't match patient's id"
    }

    val patientServerInfo: ServerInfo = when (
      val patientResult = restApi.multiStagePostPatient(patient)
    ) {
      is RestApi.PostResult.AllFailed -> {
        val serverErrorMessage = patientResult.failureBody?.modelState?.asSequence()
          ?.map { (controlId, errorMessageList) ->
            val fieldName = Registration.controlIdToNameMap.getOrDefault(controlId, controlId)
            val errorMessages = errorMessageList.joinToString()
            "$fieldName: $errorMessages"
          }
          ?.joinToString("\n\n")
          ?: patientResult.failOrException?.getErrorMessageOrNull(context)

        if (serverErrorMessage != null) {
          Log.w(TAG, "received error message from server; inserting")
          dbWrapper.patientsDao().updatePatientWithServerErrorMessage(
            patient.id,
            serverErrorMessage
          )
        }

        return UploadResult.PatientFailure(patientResult, serverErrorMessage)
      }
      is RestApi.PostResult.ObjectIdRetrievalFailed -> {
        Log.w(TAG, "only got partial patient info")

        dbWrapper.patientsDao().updatePatientWithServerErrorMessage(
          patient.id,
          "Failed to get patient's ID"
        )

        patientResult.partialServerInfo
      }
      is RestApi.PostResult.Success -> patientResult.serverInfo
      is RestApi.PostResult.AlreadyUploaded -> patientResult.serverInfo
    }
    dbWrapper.patientsDao().updatePatientWithServerInfo(patient.id, patientServerInfo)
    val patientObjectId = patientServerInfo.objectId
    if (patientObjectId == null) {
      Log.e(TAG, "Unable to post outcomes because we are missing the patient's ObjectId")
      return UploadResult.NoPatientObjectIdFailure
    } else {
      dbWrapper.withTransaction {
        dbWrapper.patientsDao().updatePatientWithServerErrorMessage(patient.id, null)
        dbWrapper.patientsDao().clearPatientDraftStatus(patient.id)
      }
    }

    val outcomesServerInfo: ServerInfo = when (
      val outcomesResult = restApi.multiStagePostOutcomes(outcomes, ObjectId(patientObjectId.toInt()))
    ) {
      is RestApi.PostResult.AllFailed -> {
        val serverErrorMessage = outcomesResult.failureBody?.modelState?.asSequence()
          ?.map { (controlId, errorMessageList) ->
            val fieldName = Outcome.controlIdToNameMap.getOrDefault(controlId, controlId)
            val errorMessages = errorMessageList.joinToString()
            "$fieldName: $errorMessages"
          }
          ?.joinToString("\n\n")
          ?: outcomesResult.failOrException?.getErrorMessageOrNull(context)

        if (serverErrorMessage != null) {
          Log.w(TAG, "received error message from server; inserting")
          dbWrapper.outcomesDao().updateOutcomesWithServerErrorMessage(
            outcomes.id,
            serverErrorMessage
          )
        }

        return UploadResult.OutcomesFailure(outcomesResult, serverErrorMessage)
      }
      is RestApi.PostResult.ObjectIdRetrievalFailed -> {
        dbWrapper.outcomesDao().updateOutcomesWithServerErrorMessage(
          outcomes.id,
          "Failed to get server ObjectID"
        )

        outcomesResult.partialServerInfo
      }
      is RestApi.PostResult.Success -> outcomesResult.serverInfo
      is RestApi.PostResult.AlreadyUploaded -> outcomesResult.serverInfo
    }
    return dbWrapper.withTransaction {
      dbWrapper.outcomesDao().updateWithServerInfo(outcomes.id, outcomesServerInfo)
      if (outcomesServerInfo.objectId == null) {
        Log.e(TAG, "missing outcomes objectId")
        UploadResult.NoOutcomesObjectIdFailure
      } else {
        dbWrapper.outcomesDao().updateOutcomesWithServerErrorMessage(outcomes.id, null)
        UploadResult.Success
      }
    }
  }

  companion object {
    private const val TAG = "PatientsManager"
  }
}
