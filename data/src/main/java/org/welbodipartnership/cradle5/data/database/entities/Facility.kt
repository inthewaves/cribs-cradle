package org.welbodipartnership.cradle5.data.database.entities

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Represents a facility from the server.
 */
@Entity
@Parcelize
@Immutable
data class Facility(
  @PrimaryKey
  @ColumnInfo(name = "id")
  override val id: Long,
  val name: String?,
  @ColumnInfo(defaultValue = DEFAULT_DISTRICT_ID_STRING)
  val districtId: Long = DEFAULT_DISTRICT_ID,
  @ColumnInfo(defaultValue = "0")
  val listOrder: Int,
  @ColumnInfo(defaultValue = "0")
  val hasVisited: Boolean,
  /**
   * Local notes the user may have saved for the patient. This is not uploaded to the server.
   */
  val localNotes: String? = null
) : Parcelable, AppEntity {
  companion object {
    const val DEFAULT_DISTRICT_ID = -1L
    private const val DEFAULT_DISTRICT_ID_STRING = "$DEFAULT_DISTRICT_ID"
  }
}
