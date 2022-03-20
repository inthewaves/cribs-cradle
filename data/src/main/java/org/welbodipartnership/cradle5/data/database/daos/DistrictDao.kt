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
import org.welbodipartnership.cradle5.data.database.entities.District

@Dao
abstract class DistrictDao {
  @Query("SELECT * FROM District WHERE id = :districtPk")
  abstract fun getDistrictFlow(districtPk: Long): Flow<District?>

  @Query("SELECT * FROM District WHERE id = :districtPk")
  abstract suspend fun getDistrict(districtPk: Long): District?

  /**
   * Updates the [district] or inserts it into the database if the [district] doesn't yet exist.
   */
  @Transaction
  open suspend fun upsert(district: District) {
    if (update(district) <= 0) {
      insert(district)
    }
  }

  @Update(entity = District::class)
  abstract suspend fun update(district: District): Int

  @Insert(entity = District::class)
  protected abstract suspend fun insert(district: District): Long

  @Transaction
  @Query("SELECT * FROM District $DEFAULT_ORDER")
  abstract fun districtsPagingSource(): PagingSource<Int, District>

  @RawQuery
  protected abstract suspend fun getDistrictIndexWhenOrderedById(
    query: SupportSQLiteQuery
  ): Long?

  /**
   * Gets the index of the district with the given [districtId] when the facilities are sorted
   * by ascending order of name.
   */
  suspend fun getDistrictIndexWhenOrderedById(districtId: Long): Long? {
    // Room doesn't support this type of query
    val query = SimpleSQLiteQuery(
      """
        SELECT rowNum FROM (
          SELECT ROW_NUMBER () OVER ($DEFAULT_ORDER) rowNum, id FROM District
        ) WHERE id = ?;
      """.trimIndent(),
      arrayOf(districtId)
    )
    // ROW_NUMBER is 1-based
    return getDistrictIndexWhenOrderedById(query)?.let { (it - 1).coerceAtLeast(0L) }
  }

  @Query("SELECT COUNT(*) FROM District")
  abstract fun countTotalDistricts(): Flow<Int>

  companion object {
    private const val DEFAULT_ORDER = "ORDER BY id COLLATE NOCASE ASC"
  }
}
