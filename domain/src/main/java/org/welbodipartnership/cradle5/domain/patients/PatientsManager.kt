package org.welbodipartnership.cradle5.domain.patients

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.domain.RestApi
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

  companion object {
    private const val TAG = "PatientsManager"
  }
}
