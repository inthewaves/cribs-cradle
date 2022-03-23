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
    val id: Int,
    val name: String?,
    val districtId: Int,
    val listOrder: Int,
  )

  @Query("SELECT * FROM Facility WHERE id = :facilityPk")
  abstract fun getFacilityFlow(facilityPk: Int): Flow<Facility?>

  @Query("SELECT * FROM Facility WHERE id = :facilityPk")
  abstract suspend fun getFacility(facilityPk: Int): Facility?

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
    facilityPk: Int,
    hasVisited: Boolean,
    localNotes: String?
  ): Int

  suspend fun updateFacilityOtherInfo(
    facilityPk: Int,
    hasVisited: Boolean,
    localNotes: String?
  ): Boolean = updateFacilityOtherInfoInner(facilityPk, hasVisited, localNotes) == 1

  @Insert(entity = Facility::class)
  protected abstract suspend fun insert(facility: FacilityUpdate): Long

  @Transaction
  @Query("SELECT * FROM Facility WHERE districtId = :districtId $DEFAULT_ORDER")
  abstract fun facilitiesPagingSource(
    districtId: Int = Facility.DEFAULT_DISTRICT_ID
  ): PagingSource<Int, Facility>

  @Transaction
  @Query("SELECT * FROM Facility WHERE hasVisited = :visited AND districtId = :districtId $DEFAULT_ORDER")
  abstract fun facilitiesPagingSourceFilterByVisited(
    visited: Boolean,
    districtId: Int = Facility.DEFAULT_DISTRICT_ID
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
    facilityId: Int,
    districtId: Int = Facility.DEFAULT_DISTRICT_ID
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
