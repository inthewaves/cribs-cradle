package org.welbodipartnership.cradle5.data.database.entities

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.datetime.FormDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

@Entity(
  indices = [
    Index("district"),
    Index("healthcareFacility")
  ]
)
@Immutable
data class CradleTrainingForm(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  override val id: Long = 0,
  @Embedded
  override val serverInfo: ServerInfo?,
  val serverErrorMessage: String?,
  /**
   * A GMT date string that can be parsed using [recordLastUpdatedFormatter].
   */
  val recordLastUpdated: String?,
  val district: Long?,
  val healthcareFacility: Long?,
  val dateOfTraining: FormDate?,
  val numOfBpDevicesFunctioning: Int?,
  val numOfCradleDevicesFunctioning: Int?,
  val numOfCradleDevicesBroken: Int?,
  @Embedded(prefix = "powersupply_")
  val powerSupply: PowerSupply?,
  val totalStaffWorking: Int?,
  val totalStaffProvidingMaternityServices: Int?,
  val totalStaffTrainedToday: Int?,
  val totalStaffTrainedTodayDoctors: Int?,
  val totalStaffTrainedTodayMidwives: Int?,
  val totalStaffTrainedTodaySACHOS: Int?,
  val totalStaffTrainedTodaySECHNMidwives: Int?,
  val totalStaffTrainedTodaySRNs: Int?,
  val totalStaffTrainedTodayCHOs: Int?,
  val totalStaffTrainedTodayCHAs: Int?,
  val totalStaffTrainedTodayCSECHNs: Int?,
  val totalStaffTrainedTodayMCHAides: Int?,
  val totalStaffTrainedTodayTBA: Int?,
  /**
   * How many of the staff trained today had ever been trained in CRADLE before?
   */
  val totalStaffTrainedBefore: Int?,
  val totalStaffTrainedScoredMoreThan8: Int?,

  /**
   * A Unix timestamp of when this was last updated
   */
  val lastUpdatedTimestamp: Long = System.currentTimeMillis() / 1000L,
  /**
   * Local notes the user may have saved for the patient. This is not uploaded to the server.
   */
  val localNotes: String? = null,
  /**
   * Whether the user has marked this patient as uploaded.
   */
  @ColumnInfo(defaultValue = "0")
  val isDraft: Boolean
) : FormEntity {
  val parsedRecordLastUpdated: ZonedDateTime
    get() = ZonedDateTime.parse(recordLastUpdated, recordLastUpdatedFormatter)

  companion object {
    fun parseRecordLastUpdatedString(dateString: String): ZonedDateTime =
      ZonedDateTime.parse(dateString, recordLastUpdatedFormatter)

    val friendlyDateFormatterForRecordLastUpdated: DateTimeFormatter =
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, hh:mm a")

    val recordLastUpdatedFormatter: DateTimeFormatter = DateTimeFormatter
      .ofPattern("dd/MM/yyyy HH:mm", Locale.ENGLISH)
      .withZone(ZoneId.of("GMT"))
    fun formatTimeAsLastUpdatedDateString(temporalAccessor: TemporalAccessor): String =
      recordLastUpdatedFormatter.format(temporalAccessor)
    fun formatNowAsLastUpdatedDateString(): String = formatTimeAsLastUpdatedDateString(Instant.now())
  }
}

@Immutable
@Parcelize
data class PowerSupply(
  @ColumnInfo(defaultValue = "0") val generator: Boolean = false,
  @ColumnInfo(defaultValue = "0") val solar: Boolean = false,
  @ColumnInfo(defaultValue = "0") val grid: Boolean = false,
  @ColumnInfo(defaultValue = "0") val none: Boolean = false
) : Parcelable {
  @IgnoredOnParcel
  @Ignore
  val areAllFieldsFalse = !generator && !solar && !grid && !none
}
