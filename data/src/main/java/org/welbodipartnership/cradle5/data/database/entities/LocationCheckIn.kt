package org.welbodipartnership.cradle5.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import javax.annotation.concurrent.Immutable

@Entity
@Immutable
data class LocationCheckIn(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Long = 0,
  val isUploaded: Boolean,
  val timestamp: Long,
  val providerName: String,
  val accuracy: Double?,
  val latitude: Double,
  val longitude: Double,
)
