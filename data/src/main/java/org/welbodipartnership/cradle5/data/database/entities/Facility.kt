package org.welbodipartnership.cradle5.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a facility from the server.
 */
@Entity
data class Facility(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: Long,
  val name: String?
)
