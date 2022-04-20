package org.welbodipartnership.cradle5.data.database.entities

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Represents a district from the server.
 */
@Entity
@Parcelize
@Immutable
data class District(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: Long,
  val name: String?,
  @ColumnInfo(defaultValue = "0")
  val isOther: Boolean = false,
) : Parcelable
