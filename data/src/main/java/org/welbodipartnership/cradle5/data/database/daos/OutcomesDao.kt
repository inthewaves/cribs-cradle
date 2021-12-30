package org.welbodipartnership.cradle5.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.welbodipartnership.cradle5.data.database.entities.Outcomes

@Dao
abstract class OutcomesDao {
  @Insert
  abstract suspend fun insert(outcomes: Outcomes): Long

  @Query("SELECT * FROM Outcomes WHERE id = :id")
  abstract suspend fun get(id: Long): Outcomes?
}
