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
    val listOrder: Int,
  )

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

  @Insert(entity = Facility::class)
  protected abstract suspend fun insert(facility: FacilityUpdate): Long

  @Transaction
  @Query("SELECT * FROM Facility $DEFAULT_ORDER")
  abstract fun facilitiesPagingSource(): PagingSource<Int, Facility>

  @RawQuery
  protected abstract suspend fun getFacilityIndexWhenOrderedByName(
    query: SupportSQLiteQuery
  ): Long?

  /**
   * Gets the index of the facility with the given [facilityId] when the facilities are sorted
   * by ascending order of name.
   */
  suspend fun getFacilityIndexWhenOrderedByName(facilityId: Long): Long? {
    // Room doesn't support this type of query
    val query = SimpleSQLiteQuery(
      """
        SELECT rowNum FROM (
          SELECT ROW_NUMBER () OVER ($DEFAULT_ORDER) rowNum, id FROM Facility
        ) WHERE id = ?;
      """.trimIndent(),
      arrayOf(facilityId)
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
