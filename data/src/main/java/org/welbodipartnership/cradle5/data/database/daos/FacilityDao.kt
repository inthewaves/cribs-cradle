package org.welbodipartnership.cradle5.data.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.data.database.entities.Facility

@Dao
abstract class FacilityDao {
  /**
   * A partial version of [Facility] so that the user notes don't get overwritten.
   */
  data class FacilityUpdate(
    val id: Long,
    val name: String?,
    val districtId: Long,
    val listOrder: Int,
  )

  @Query("SELECT * FROM Facility WHERE id = :facilityPk")
  abstract fun getFacilityFlow(facilityPk: Long): Flow<Facility?>

  @Query("SELECT * FROM Facility WHERE id = :facilityPk")
  abstract suspend fun getFacility(facilityPk: Long): Facility?

  /**
   * Updates the [facility] or inserts it into the database if the [facility] doesn't yet exist.
   */
  @Transaction
  open suspend fun upsert(facility: FacilityUpdate) {
    if (update(facility) <= 0) {
      insert(facility)
    }
  }

  @Update(entity = Facility::class)
  abstract suspend fun update(facility: FacilityUpdate): Int

  @Query("UPDATE Facility SET hasVisited = :hasVisited, localNotes = :localNotes WHERE id = :facilityPk")
  protected abstract suspend fun updateFacilityOtherInfoInner(
    facilityPk: Long,
    hasVisited: Boolean,
    localNotes: String?
  ): Int

  suspend fun updateFacilityOtherInfo(
    facilityPk: Long,
    hasVisited: Boolean,
    localNotes: String?
  ): Boolean = updateFacilityOtherInfoInner(facilityPk, hasVisited, localNotes) == 1

  @Insert(entity = Facility::class)
  protected abstract suspend fun insert(facility: FacilityUpdate): Long

  @Transaction
  @Query("SELECT * FROM Facility WHERE districtId = :districtId $DEFAULT_ORDER")
  abstract fun facilitiesPagingSource(
    districtId: Long = Facility.DEFAULT_DISTRICT_ID
  ): PagingSource<Int, Facility>

  @Transaction
  @Query("SELECT * FROM Facility WHERE hasVisited = :visited AND districtId = :districtId $DEFAULT_ORDER")
  abstract fun facilitiesPagingSourceFilterByVisited(
    visited: Boolean,
    districtId: Long = Facility.DEFAULT_DISTRICT_ID
  ): PagingSource<Int, Facility>

  @Transaction
  @Query(
    """
      SELECT 
        f.* 
      FROM 
        Facility AS f
      WHERE
        f.districtId = :districtId AND
        EXISTS (
          SELECT b.id FROM FacilityBpInfo AS b WHERE f.id = b.facility AND b.objectId IS NULL
        )
      $DEFAULT_ORDER
    """
  )
  abstract fun facilitiesPagingSourceFilterByBpInfoNeedsSync(
    districtId: Long = Facility.DEFAULT_DISTRICT_ID
  ): PagingSource<Int, Facility>

  @RawQuery
  protected abstract suspend fun getFacilityIndexWhenOrderedByName(
    query: SupportSQLiteQuery
  ): Long?

  /**
   * Gets the index of the facility with the given [facilityId] when the facilities are sorted
   * by ascending order of name.
   */
  suspend fun getFacilityIndexWhenOrderedByName(
    facilityId: Long,
    districtId: Long = Facility.DEFAULT_DISTRICT_ID
  ): Long? {
    // Room doesn't support this type of query
    val query = SimpleSQLiteQuery(
      """
        SELECT rowNum FROM (
          SELECT ROW_NUMBER () OVER ($DEFAULT_ORDER) rowNum, id FROM Facility WHERE districtId = ?
        ) WHERE id = ?;
      """.trimIndent(),
      arrayOf(districtId, facilityId)
    )
    // ROW_NUMBER is 1-based
    return getFacilityIndexWhenOrderedByName(query)?.let { (it - 1).coerceAtLeast(0L) }
  }

  @Query("SELECT COUNT(*) FROM Facility")
  abstract fun countTotalFacilities(): Flow<Int>

  companion object {
    private const val DEFAULT_ORDER = "ORDER BY listOrder, name COLLATE NOCASE ASC"
  }
}
