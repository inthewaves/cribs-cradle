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
import org.welbodipartnership.cradle5.data.database.entities.Facility

@Dao
abstract class FacilityDao {
  /**
   * Updates the [facility] or inserts it into the database if the [facility] doesn't yet exist.
   */
  @Update
  suspend fun upsert(facility: Facility) {
    if (update(facility) <= 0) {
      insert(facility)
    }
  }

  @Update
  abstract suspend fun update(facility: Facility): Int

  @Insert
  protected abstract suspend fun insert(facility: Facility): Long

  @Transaction
  @Query("SELECT * FROM Facility $NAME_ORDER")
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
          SELECT ROW_NUMBER () OVER ($NAME_ORDER) rowNum, id FROM Facility
        ) WHERE id = ?;
      """.trimIndent(),
      arrayOf(facilityId)
    )
    // ROW_NUMBER is 1-based
    return getFacilityIndexWhenOrderedByName(query)?.let { (it - 1).coerceAtLeast(0L) }
  }

  companion object {
    private const val NAME_ORDER = "ORDER BY name COLLATE NOCASE ASC"
  }
}
