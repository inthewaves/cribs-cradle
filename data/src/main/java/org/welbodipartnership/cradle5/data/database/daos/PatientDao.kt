package org.welbodipartnership.cradle5.data.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.database.resultentities.ListPatient
import org.welbodipartnership.cradle5.data.database.resultentities.PatientAndOutcomes
import org.welbodipartnership.cradle5.data.database.resultentities.PatientFacilityOutcomes

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

  @Transaction
  @Query("SELECT id, initials, dateOfBirth FROM Patient ORDER BY id ASC")
  abstract fun patientsPagingSource(): PagingSource<Int, ListPatient>

  @Transaction
  @Query("SELECT * FROM Patient WHERE id = :patientPk")
  abstract fun getPatientAndOutcomesFlow(patientPk: Long): Flow<PatientFacilityOutcomes?>

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

  /**
   * Updates a patient with new server info. This marks a patient as uploaded.
   *
   * @return whether the update was successful
   */
  suspend fun updatePatientWithServerInfo(patientId: Long, serverInfo: ServerInfo): Boolean {
    return updatePatientWithServerInfo(patientId, serverInfo.nodeId, serverInfo.objectId) == 1
  }

  @Query("SELECT COUNT(*) FROM Patient WHERE nodeId IS NULL")
  abstract fun countPatientsToUpload(): Flow<Int>

  @Transaction
  @Query("SELECT * FROM Patient WHERE nodeId IS NULL")
  abstract suspend fun getPatientsToUpload(): List<PatientAndOutcomes>
}
