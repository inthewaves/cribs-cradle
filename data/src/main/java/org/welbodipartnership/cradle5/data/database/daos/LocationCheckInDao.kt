package org.welbodipartnership.cradle5.data.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn

@Dao
abstract class LocationCheckInDao {
  @Insert
  abstract suspend fun insertCheckIn(locationCheckIn: LocationCheckIn): Long

  @Transaction
  @Query("SELECT * FROM LocationCheckIn ORDER BY timestamp DESC")
  abstract fun checkInsPagingSource(): PagingSource<Int, LocationCheckIn>

  @Transaction
  @Query("SELECT * FROM LocationCheckIn WHERE isUploaded = 0")
  abstract suspend fun getCheckInsForUpload(): List<LocationCheckIn>

  @Query("SELECT COUNT(*) FROM LocationCheckIn WHERE isUploaded = 0")
  abstract fun countCheckInsForUpload(): Flow<Int>

  @Query("UPDATE LocationCheckIn SET isUploaded = 1 WHERE id = :checkInId")
  protected abstract suspend fun markAsUploadedInner(checkInId: Long): Int

  suspend fun markAsUploaded(checkInId: Long) = markAsUploadedInner(checkInId) == 1

  @Delete
  abstract suspend fun deleteCheckIn(locationCheckIn: LocationCheckIn)
}
