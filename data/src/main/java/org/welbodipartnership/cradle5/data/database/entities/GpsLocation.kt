package org.welbodipartnership.cradle5.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class GpsLocation(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: Long,
  val timestamp: Long,
  val gpsCoordinates: String,
)
