package org.welbodipartnership.cradle5.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.data.database.entities.FacilityBpInfo
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.database.resultentities.BpInfoFacilityDistrict
import java.time.ZonedDateTime

@Dao
abstract class FacilityBpInfoDao {
  /**
   * Updates the [bpInfo] or inserts it into the database if it doesn't yet exist.
   * Returns the primary key of the bp Info. For a new form, the primary key might be populated
   * by Room, as it is autoincrementing.
   */
  @Update
  @Transaction
  open suspend fun upsert(bpInfo: FacilityBpInfo): Long {
    return if (update(bpInfo) <= 0) {
      insert(bpInfo)
    } else {
      bpInfo.id
    }
  }

  /**
   * @return the number of rows updated (i.e., 0 means the given entity wasn't in the database,
   * and 1 means the entity was updated)
   */
  @Update
  abstract suspend fun update(bpInfo: FacilityBpInfo): Int

  /**
   * Inserts [bpInfo] into the [FacilityBpInfo] table.
   *
   * DO NOT use this to update an entity; use [update] or [upsert] for that.
   * If this is used to update an entity in the database, any foreign keys pointing to the "updated"
   * entity will cascade and delete themselves, because Room's `OnConflictStrategy.REPLACE` somehow
   * involves deleting the entity and reading it.
   *
   * @return the new SQLite rowId for the inserted [bpInfo], or -1 if [bpInfo] was not inserted
   * into the database. -1 might occur if the [bpInfo] already exists.
   */
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  protected abstract suspend fun insert(bpInfo: FacilityBpInfo): Long

  @Delete
  abstract suspend fun delete(bpInfo: FacilityBpInfo): Int


  //@Transaction
  //@Query("SELECT * FROM FacilityBpInfo WHERE id = :formPk")
  //abstract fun getFormFlow(formPk: Long): Flow<BpInfoFacilityDistrict?>

  /*
  @Query("SELECT initials FROM Patient WHERE id = :patientPk")
  abstract fun getPatientInitialsFlow(patientPk: Long): Flow<String?>

  @Query("SELECT nodeId FROM Patient WHERE id = :patientPk")
  abstract fun getPatientNodeIdFlow(patientPk: Long): Flow<Long?>
   */

  @Query("UPDATE FacilityBpInfo SET isDraft = 0 WHERE id = :formPk")
  abstract suspend fun clearDraftStatus(formPk: Long): Int

  @Query("UPDATE FacilityBpInfo SET localNotes = :localNotes WHERE id = :formPk")
  protected abstract suspend fun updateLocalNotesInner(
    formPk: Long,
    localNotes: String?
  ): Int

  suspend fun updateLocalNotesInfo(
    patientPk: Long,
    localNotes: String?
  ): Boolean = updateLocalNotesInner(patientPk, localNotes) == 1

  //@RewriteQueriesToDropUnusedColumns
  //@Query("SELECT * FROM FacilityBpInfo WHERE id = :formPk")
  //abstract suspend fun getOtherInfo(formPk: Long): CradleFormOtherInfo?

  @Transaction
  @Query("SELECT * FROM FacilityBpInfo WHERE id = :formPk")
  abstract suspend fun getFormFacilityDistrict(formPk: Long): BpInfoFacilityDistrict?

  /**
   * @return the number of rows that were updated. Note that WHERE is set to the primary key,
   * so it either returns 1 or 0.
   */
  @Query("UPDATE FacilityBpInfo SET nodeId = :nodeId, objectId = :objectId, updateTime = :updateTime, createTime = :createTime WHERE id = :formId")
  protected abstract suspend fun updateWithServerInfo(
    formId: Long,
    nodeId: Long?,
    objectId: Long?,
    updateTime: ZonedDateTime?,
    createTime: ZonedDateTime?
  ): Int

  @Query("UPDATE FacilityBpInfo SET recordLastUpdated = :lastUpdated WHERE id = :formId")
  protected abstract suspend fun updateRecordLastUpdatedString(
    formId: Long,
    lastUpdated: ZonedDateTime
  ): Int

  @Query("UPDATE FacilityBpInfo SET serverErrorMessage = :serverErrorMessage WHERE id = :formId")
  abstract suspend fun updateWithServerErrorMessage(
    formId: Long,
    serverErrorMessage: String?
  )

  /**
   * Updates a form with new server info. This marks a form as uploaded.
   *
   * @return whether the update was successful
   */
  @Transaction
  open suspend fun updateWithServerInfo(formId: Long, serverInfo: ServerInfo): Boolean {
    if (
      updateWithServerInfo(
        formId = formId,
        nodeId = serverInfo.nodeId,
        objectId = serverInfo.objectId,
        updateTime = serverInfo.updateTime,
        createTime = serverInfo.createTime
      ) != 1
    ) {
      return false
    }
    val newUpdateTime: ZonedDateTime = serverInfo.updateTime ?: serverInfo.createTime ?: return true
    return updateRecordLastUpdatedString(formId, newUpdateTime) == 1
  }

  @Query("SELECT COUNT(*) FROM FacilityBpInfo")
  abstract fun countTotal(): Flow<Int>

  @Query("SELECT COUNT(*) FROM FacilityBpInfo WHERE objectId IS NULL AND isDraft = 0 AND serverErrorMessage IS NULL")
  abstract fun countFormsToUploadWithoutErrors(): Flow<Int>

  @Query("SELECT COUNT(*) FROM FacilityBpInfo WHERE objectId IS NULL AND isDraft = 0 AND serverErrorMessage IS NOT NULL")
  abstract fun countFormsToUploadWithErrors(): Flow<Int>

  @Transaction
  @Query("SELECT * FROM FacilityBpInfo WHERE objectId IS NULL AND isDraft = 0 ORDER BY recordLastUpdated DESC")
  abstract suspend fun getNewFormsToUploadOrderedById(): List<FacilityBpInfo>

  @Query("SELECT COUNT(*) FROM FacilityBpInfo WHERE $WHERE_PARTIAL_FORM_CLAUSE")
  abstract fun countPartialFormsToUpload(): Flow<Int>

  @Transaction
  @Query("SELECT * FROM FacilityBpInfo WHERE $WHERE_PARTIAL_FORM_CLAUSE ORDER BY recordLastUpdated DESC")
  abstract suspend fun getFormsWithPartialServerInfoOrderedById(): List<FacilityBpInfo>

  companion object {
    private const val WHERE_PARTIAL_FORM_CLAUSE = "objectId IS NOT NULL AND createdTime IS NULL"
  }
}
