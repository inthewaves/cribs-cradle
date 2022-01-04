package org.welbodipartnership.cradle5.data.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
  @Query("SELECT * FROM Facility ORDER BY id ASC")
  abstract fun patientsPagingSource(): PagingSource<Int, Facility>
}
