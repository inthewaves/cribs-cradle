package org.welbodipartnership.cradle5.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.database.entities.forms.NonTreeFormEntity
import org.welbodipartnership.cradle5.util.datetime.FormDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Entity
data class FacilityBpInfo(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  override val id: Long = 0,
  @Embedded
  override val serverInfo: ServerInfo?,
  val serverErrorMessage: String?,

  val dataCollectionDate: FormDate?,
  val district: Long?,
  val facility: Long?,
  val numBpReadingsTakenInFacilitySinceLastVisit: Int?,
  val numBpReadingsEndIn0Or5: Int?,
  val numBpReadingsWithColorAndArrow: Int?,

  val recordLastUpdated: ZonedDateTime?,
  /**
   * Local notes the user may have saved for the patient. This is not uploaded to the server.
   */
  val localNotes: String? = null,
  /**
   * Whether the user has marked this patient as uploaded.
   */
  @ColumnInfo(defaultValue = "0")
  val isDraft: Boolean,
) : NonTreeFormEntity {
  companion object {
    val friendlyDateFormatterForRecordLastUpdated: DateTimeFormatter =
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, hh:mm a")
  }
}
