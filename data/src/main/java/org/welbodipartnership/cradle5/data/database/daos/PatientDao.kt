package org.welbodipartnership.cradle5.data.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.database.resultentities.ListPatientAndOutcomeError
import org.welbodipartnership.cradle5.data.database.resultentities.PatientAndOutcomes
import org.welbodipartnership.cradle5.data.database.resultentities.PatientFacilityOutcomes
import org.welbodipartnership.cradle5.data.database.resultentities.PatientOtherInfo

@Dao
abstract class PatientDao {
  /**
   * Updates the [patient] or inserts it into the database if the patient doesn't yet exist.
   * Returns the primary key of the patient. For a new patient, the primary key might be populated
   * by Room, as it is autoincrementing.
   */
  @Update
  suspend fun upsert(patient: Patient): Long {
    return if (update(patient) <= 0) {
      insert(patient)
    } else {
      patient.id
    }
  }

  /**
   * @return the number of rows updated (i.e., 0 means the given patient wasn't in the database,
   * and 1 means the [patient] was updated)
   */
  @Update
  abstract suspend fun update(patient: Patient): Int

  /**
   * Inserts [patient] into the [Patient] table.
   *
   * DO NOT use this to update a [Patient]; use [update] or [upsert] for that.
   * If this is used to update a [Patient] in the database, any [Outcomes] with foreign keys
   * pointing to the "updated" Patient will cascade and delete themselves, because Room's
   * `OnConflictStrategy.REPLACE` somehow involves deleting the entity and reading it.
   *
   * @return the new SQLite rowId for the inserted [patient], or -1 if [patient] was not inserted
   * into the database. -1 might occur if the [patient] already exists.
   */
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  protected abstract suspend fun insert(patient: Patient): Long

  // ---- Patients paging

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM Patient ORDER BY id DESC")
  abstract fun patientsPagingSource(): PagingSource<Int, ListPatientAndOutcomeError>

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM Patient WHERE nodeId IS NULL AND isDraft = 1 ORDER BY id DESC")
  abstract fun patientsPagingSourceFilterByDraft(): PagingSource<Int, ListPatientAndOutcomeError>

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM Patient WHERE nodeId IS NOT NULL ORDER BY id DESC")
  abstract fun patientsPagingSourceFilterByUploaded(): PagingSource<Int, ListPatientAndOutcomeError>

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM Patient WHERE nodeId IS NULL AND isDraft = 0 ORDER BY id DESC")
  abstract fun patientsPagingSourceFilterByNotUploadedAndNotDraft(): PagingSource<Int, ListPatientAndOutcomeError>

  // ---- Patient + outcomes observations

  @Transaction
  @Query("SELECT * FROM Patient WHERE id = :patientPk")
  abstract fun getPatientAndOutcomesFlow(patientPk: Long): Flow<PatientFacilityOutcomes?>

  @Query("SELECT initials FROM Patient WHERE id = :patientPk")
  abstract fun getPatientInitialsFlow(patientPk: Long): Flow<String?>

  @Query("SELECT nodeId FROM Patient WHERE id = :patientPk")
  abstract fun getPatientNodeIdFlow(patientPk: Long): Flow<Long?>

  @Query("UPDATE Patient SET isDraft = 0 WHERE id = :patientPk")
  abstract suspend fun clearPatientDraftStatus(patientPk: Long): Int

  @Query("UPDATE Patient SET localNotes = :localNotes WHERE id = :patientPk")
  protected abstract suspend fun updatePatientLocalNotesInner(
    patientPk: Long,
    localNotes: String?
  ): Int

  suspend fun updatePatientLocalNotesInfo(
    patientPk: Long,
    localNotes: String?
  ): Boolean = updatePatientLocalNotesInner(patientPk, localNotes) == 1

  @RewriteQueriesToDropUnusedColumns
  @Query("SELECT * FROM Patient WHERE id = :patientPk")
  abstract suspend fun getPatientOtherInfo(patientPk: Long): PatientOtherInfo?

  @Transaction
  @Query("SELECT * FROM Patient WHERE id = :patientPk")
  abstract suspend fun getPatientFacilityAndOutcomes(patientPk: Long): PatientFacilityOutcomes?

  /**
   * @return the number of rows that were updated. Note that WHERE is set to the primary key,
   * so it either returns 1 or 0.
   */
  @Query("UPDATE Patient SET nodeId = :nodeId, objectId = :objectId WHERE id = :patientId")
  protected abstract suspend fun updatePatientWithServerInfo(
    patientId: Long,
    nodeId: Long,
    objectId: Long?
  ): Int

  @Query("UPDATE Patient SET serverErrorMessage = :serverErrorMessage WHERE id = :patientId")
  abstract suspend fun updatePatientWithServerErrorMessage(
    patientId: Long,
    serverErrorMessage: String?
  )

  /**
   * Updates a patient with new server info. This marks a patient as uploaded.
   *
   * @return whether the update was successful
   */
  suspend fun updatePatientWithServerInfo(patientId: Long, serverInfo: ServerInfo): Boolean {
    return updatePatientWithServerInfo(patientId, serverInfo.nodeId, serverInfo.objectId) == 1
  }

  @Query("SELECT COUNT(*) FROM Patient")
  abstract fun countTotalPatients(): Flow<Int>

  @Query("SELECT COUNT(*) FROM Patient WHERE nodeId IS NULL AND isDraft = 0")
  abstract fun countPatientsToUpload(): Flow<Int>

  @Transaction
  @Query("SELECT * FROM Patient WHERE nodeId IS NULL AND isDraft = 0 ORDER BY id")
  abstract suspend fun getNewPatientsToUploadOrderedById(): List<PatientAndOutcomes>

  @Query("SELECT COUNT(*) FROM Patient WHERE nodeId IS NOT NULL AND objectId IS NULL")
  abstract fun countPartialPatientsToUpload(): Flow<Int>

  @Transaction
  @Query(
    """
    SELECT * FROM Patient WHERE nodeId IS NOT NULL AND objectId IS NULL ORDER BY id
    """
  )
  abstract suspend fun getPatientsWithPartialServerInfoOrderedById(): List<PatientAndOutcomes>
}
