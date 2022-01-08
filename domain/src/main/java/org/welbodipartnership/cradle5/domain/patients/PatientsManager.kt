package org.welbodipartnership.cradle5.domain.patients

import android.util.Log
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.domain.ObjectId
import org.welbodipartnership.cradle5.domain.RestApi
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientsManager @Inject constructor(
  private val restApi: RestApi,
  private val dbWrapper: CradleDatabaseWrapper,
  private val syncRepository: SyncRepository,
) {

  val editPatientsOutcomesState = syncRepository.editFormState

  sealed class UploadResult {
    data class PatientFailure(val error: RestApi.PostResult) : UploadResult()
    object NoPatientObjectIdFailure : UploadResult()
    data class OutcomesFailure(val error: RestApi.PostResult) : UploadResult()
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
      is RestApi.PostResult.AllFailed -> return UploadResult.PatientFailure(patientResult)
      is RestApi.PostResult.ObjectIdRetrievalFailed -> {
        Log.w(TAG, "only got partial patient info")
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
    }
    val outcomesServerInfo: ServerInfo = when (
      val outcomesResult = restApi.multiStagePostOutcomes(outcomes, ObjectId(patientObjectId.toInt()))
    ) {
      is RestApi.PostResult.AllFailed -> return UploadResult.OutcomesFailure(outcomesResult)
      is RestApi.PostResult.ObjectIdRetrievalFailed -> outcomesResult.partialServerInfo
      is RestApi.PostResult.Success -> outcomesResult.serverInfo
      is RestApi.PostResult.AlreadyUploaded -> outcomesResult.serverInfo
    }
    dbWrapper.outcomesDao().updateWithServerInfo(outcomes.id, outcomesServerInfo)
    return if (outcomesServerInfo.objectId == null) {
      Log.e(TAG, "missing outcomes objectId")
      UploadResult.NoOutcomesObjectIdFailure
    } else {
      UploadResult.Success
    }
  }

  companion object {
    private const val TAG = "PatientsManager"
  }
}
