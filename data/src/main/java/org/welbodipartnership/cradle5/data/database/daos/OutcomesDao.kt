package org.welbodipartnership.cradle5.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo

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

  /**
   * @return the number of rows that were updated. Note that WHERE is set to the primary key,
   * so it either returns 1 or 0.
   */
  @Query("UPDATE Outcomes SET nodeId = :nodeId, objectId = :objectId WHERE id = :outcomesId")
  protected abstract suspend fun updateWithServerInfo(
    outcomesId: Long,
    nodeId: Long,
    objectId: Long?
  ): Int

  /**
   * Updates an outcomes entity with new server info. This marks an outcomes entity as uploaded.
   *
   * @return whether the update was successful
   */
  suspend fun updateWithServerInfo(outcomesId: Long, serverInfo: ServerInfo): Boolean {
    return updateWithServerInfo(outcomesId, serverInfo.nodeId, serverInfo.objectId) == 1
  }
}
