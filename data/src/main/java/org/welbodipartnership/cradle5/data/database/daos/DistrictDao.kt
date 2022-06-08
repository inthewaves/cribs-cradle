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

  @Query("SELECT * FROM District WHERE name = :name")
  abstract suspend fun getDistrictByName(name: String): List<District>

  @Query("SELECT * FROM District")
  abstract suspend fun getAllDistricts(): List<District>

  @Query("SELECT COUNT(*) FROM District")
  abstract suspend fun countDistricts(): Int

  /**
   * Updates the [district] or inserts it into the database if the [district] doesn't yet exist.
   */
  @Transaction
  open suspend fun upsert(district: District) {
    val districtToUse = if (
      district.name?.contains("other", ignoreCase = true) == true &&
      !district.isOther
    ) {
      district.copy(isOther = true)
    } else {
      district
    }

    if (update(districtToUse) <= 0) {
      insert(districtToUse)
    }
  }

  @Update(entity = District::class)
  abstract suspend fun update(district: District): Int

  @Insert(entity = District::class)
  protected abstract suspend fun insert(district: District): Long

  @Transaction
  @Query("SELECT * FROM District WHERE isOther = 0 $DEFAULT_ORDER")
  abstract fun districtsPagingSourceNoOther(): PagingSource<Int, District>

  @RawQuery
  protected abstract suspend fun getDistrictIndexWhenOrderedById(
    query: SupportSQLiteQuery
  ): Int?

  /**
   * Gets the index of the [district] when the facilities are sorted by ascending order of name.
   */
  suspend fun getDistrictIndexWhenOrderedById(district: District): Int? {
    return getRowNumberSafe(
      entity = district,
      countTotalBlock = { countDistricts() },
      createRawQueryBlock = {
        SimpleSQLiteQuery(
          "SELECT rowNum FROM (SELECT ROW_NUMBER () OVER ($DEFAULT_ORDER) rowNum, id FROM District) WHERE id = ?;",
          arrayOf(district.id)
        )
      },
      runRawQueryBlock = { query -> getDistrictIndexWhenOrderedById(query) },
      getAllEntitiesBlock = { getAllDistricts() }
    )
  }

  @Query("SELECT COUNT(*) FROM District")
  abstract fun countTotalDistricts(): Flow<Int>

  companion object {
    private const val TAG = "DistrictDao"
    private const val DEFAULT_ORDER = "ORDER BY id COLLATE NOCASE ASC"
  }
}
