package org.welbodipartnership.cradle5.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.welbodipartnership.cradle5.data.database.entities.Outcomes

@Dao
abstract class OutcomesDao {
  /**
   * Updates the [outcomes] or inserts it into the database if the [outcomes] doesn't yet exist.
   */
  @Update
  suspend fun upsert(outcomes: Outcomes) {
    if (update(outcomes) <= 0) {
      insert(outcomes)
    }
  }

  @Update
  abstract suspend fun update(outcomes: Outcomes): Int

  @Insert
  protected abstract suspend fun insert(outcomes: Outcomes): Long

  @Query("SELECT * FROM Outcomes WHERE id = :id")
  abstract suspend fun get(id: Long): Outcomes?
}
