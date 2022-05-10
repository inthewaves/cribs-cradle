package org.welbodipartnership.cradle5.data.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.database.resultentities.CradleFormOtherInfo
import org.welbodipartnership.cradle5.data.database.resultentities.CradleTrainingFormFacilityDistrict
import org.welbodipartnership.cradle5.data.database.resultentities.ListCradleTrainingForm

@Dao
abstract class CradleTrainingFormDao {
  /**
   * Updates the [form] or inserts it into the database if the form doesn't yet exist.
   * Returns the primary key of the form. For a new form, the primary key might be populated
   * by Room, as it is autoincrementing.
   */
  @Update
  suspend fun upsert(form: CradleTrainingForm): Long {
    return if (update(form) <= 0) {
      insert(form)
    } else {
      form.id
    }
  }

  /**
   * @return the number of rows updated (i.e., 0 means the given form wasn't in the database,
   * and 1 means the [form] was updated)
   */
  @Update
  abstract suspend fun update(form: CradleTrainingForm): Int

  /**
   * Inserts [form] into the [CradleTrainingForm] table.
   *
   * DO NOT use this to update a [CradleTrainingForm]; use [update] or [upsert] for that.
   * If this is used to update a [CradleTrainingForm] in the database, any dependencies with foreign keys
   * pointing to the "updated" form will cascade and delete themselves, because Room's
   * `OnConflictStrategy.REPLACE` somehow involves deleting the entity and reading it.
   *
   * @return the new SQLite rowId for the inserted [form], or -1 if [form] was not inserted
   * into the database. -1 might occur if the [form] already exists.
   */
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  protected abstract suspend fun insert(form: CradleTrainingForm): Long

  @Delete
  abstract suspend fun delete(form: CradleTrainingForm): Int

  // ---- Patients paging

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM ListCradleTrainingForm ORDER BY id DESC")
  abstract fun cradleFormPagingSource(): PagingSource<Int, ListCradleTrainingForm>

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM ListCradleTrainingForm WHERE objectId IS NULL AND isDraft = 1 ORDER BY id DESC")
  abstract fun cradleFormPagingSourceFilterByDraft(): PagingSource<Int, ListCradleTrainingForm>

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM ListCradleTrainingForm WHERE objectId IS NOT NULL ORDER BY id DESC")
  abstract fun cradleFormPagingSourceFilterByUploaded(): PagingSource<Int, ListCradleTrainingForm>

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM ListCradleTrainingForm WHERE objectId IS NULL AND isDraft = 0 ORDER BY id DESC")
  abstract fun cradleFormPagingSourceFilterByNotUploadedAndNotDraft(): PagingSource<Int, ListCradleTrainingForm>

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM ListCradleTrainingForm WHERE facility_id = :facilityId ORDER BY id DESC")
  abstract fun cradleFormPagingSourceFilterByFacility(facilityId: Long): PagingSource<Int, ListCradleTrainingForm>

  @RewriteQueriesToDropUnusedColumns
  @Transaction
  @Query("SELECT * FROM CradleTrainingForm WHERE CAST(SUBSTR(dateOfTraining, 4, 2) AS INT) = :monthOneBased ORDER BY id DESC")
  abstract fun cradleFormPagingSourceFilterByTrainingMonth(
    monthOneBased: Int
  ): PagingSource<Int, ListCradleTrainingForm>

  // ---- Patient + outcomes observations

  @Transaction
  @Query("SELECT * FROM CradleTrainingForm WHERE id = :formPk")
  abstract fun getFormFlow(formPk: Long): Flow<CradleTrainingFormFacilityDistrict?>

  /*
  @Query("SELECT initials FROM Patient WHERE id = :patientPk")
  abstract fun getPatientInitialsFlow(patientPk: Long): Flow<String?>

  @Query("SELECT nodeId FROM Patient WHERE id = :patientPk")
  abstract fun getPatientNodeIdFlow(patientPk: Long): Flow<Long?>
   */

  @Query("UPDATE CradleTrainingForm SET isDraft = 0 WHERE id = :formPk")
  abstract suspend fun clearDraftStatus(formPk: Long): Int

  @Query("UPDATE CradleTrainingForm SET localNotes = :localNotes WHERE id = :formPk")
  protected abstract suspend fun updateLocalNotesInner(
    formPk: Long,
    localNotes: String?
  ): Int

  suspend fun updateLocalNotesInfo(
    patientPk: Long,
    localNotes: String?
  ): Boolean = updateLocalNotesInner(patientPk, localNotes) == 1

  @RewriteQueriesToDropUnusedColumns
  @Query("SELECT * FROM CradleTrainingForm WHERE id = :formPk")
  abstract suspend fun getOtherInfo(formPk: Long): CradleFormOtherInfo?

  @Transaction
  @Query("SELECT * FROM CradleTrainingForm WHERE id = :formPk")
  abstract suspend fun getFormFacilityDistrict(formPk: Long): CradleTrainingFormFacilityDistrict?

  /**
   * @return the number of rows that were updated. Note that WHERE is set to the primary key,
   * so it either returns 1 or 0.
   */
  @Query("UPDATE CradleTrainingForm SET nodeId = :nodeId, objectId = :objectId, updateTime = :updateTime, createdTime = :createdTime WHERE id = :formId")
  protected abstract suspend fun updateWithServerInfo(
    formId: Long,
    nodeId: Long?,
    objectId: Long,
    updateTime: String?,
    createdTime: String?
  ): Int

  @Query("UPDATE CradleTrainingForm SET serverErrorMessage = :serverErrorMessage WHERE id = :formId")
  abstract suspend fun updateWithServerErrorMessage(
    formId: Long,
    serverErrorMessage: String?
  )

  /**
   * Updates a form with new server info. This marks a form as uploaded.
   *
   * @return whether the update was successful
   */
  suspend fun updateWithServerInfo(formId: Long, serverInfo: ServerInfo): Boolean {
    return updateWithServerInfo(
      formId = formId,
      nodeId = serverInfo.nodeId,
      objectId = serverInfo.objectId,
      updateTime = serverInfo.updateTime,
      createdTime = serverInfo.createdTime
    ) == 1
  }

  @Query("SELECT COUNT(*) FROM CradleTrainingForm")
  abstract fun countTotal(): Flow<Int>

  @Query("SELECT COUNT(*) FROM CradleTrainingForm WHERE objectId IS NULL AND isDraft = 0 AND serverErrorMessage IS NULL")
  abstract fun countFormsToUploadWithoutErrors(): Flow<Int>

  @Query("SELECT COUNT(*) FROM CradleTrainingForm WHERE objectId IS NULL AND isDraft = 0 AND serverErrorMessage IS NOT NULL")
  abstract fun countFormsToUploadWithErrors(): Flow<Int>

  @Transaction
  @Query("SELECT * FROM CradleTrainingForm WHERE objectId IS NULL AND isDraft = 0 ORDER BY id")
  abstract suspend fun getNewFormsToUploadOrderedById(): List<CradleTrainingForm>

  @Query("SELECT COUNT(*) FROM CradleTrainingForm WHERE $WHERE_PARTIAL_FORM_CLAUSE")
  abstract fun countPartialFormsToUpload(): Flow<Int>

  @Transaction
  @Query("SELECT * FROM CradleTrainingForm WHERE $WHERE_PARTIAL_FORM_CLAUSE ORDER BY id")
  abstract suspend fun getFormsWithPartialServerInfoOrderedById(): List<CradleTrainingForm>

  companion object {
    private const val WHERE_PARTIAL_FORM_CLAUSE = "objectId IS NOT NULL AND createdTime IS NULL"
  }
}
