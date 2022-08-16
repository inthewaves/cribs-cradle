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
   * Convert to a string using [recordLastUpdatedFormatter].
   */
  val recordLastUpdated: ZonedDateTime?,
  val recordCreated: ZonedDateTime?,
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
  val totalStaffTrainedTodaySRNs: Int?,
  val totalStaffTrainedTodaySECHNs: Int?,
  val totalStaffTrainedTodayCHOs: Int?,
  val totalStaffTrainedTodayCHAs: Int?,
  val totalStaffTrainedTodayMCHAides: Int?,
  val totalStaffTrainedTodayTBA: Int?,
  val totalStaffTrainedTodayVolunteers: Int?,
  /**
   * How many of the staff trained today had ever been trained in CRADLE before?
   */
  val totalStaffTrainedBefore: Int?,
  val totalStaffObservedAndScored: Int?,
  val totalStaffTrainedScoredMoreThan14: Int?,
  @Embedded(prefix = "checklist_")
  val checklistMissed: ChecklistMissed?,
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
  val recordCreatedDateString: String? get() = recordCreated
    ?.withZoneSameInstant(ZoneId.of("Europe/London"))
    ?.format(recordLastUpdatedFormatter)

  val recordLastUpdatedString: String? get() = recordLastUpdated
    ?.withZoneSameInstant(ZoneId.of("Europe/London"))
    ?.format(recordLastUpdatedFormatter)

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

@Immutable
@Parcelize
data class ChecklistMissed(
  @ColumnInfo(defaultValue = "0") val missed1: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed2: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed3: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed4: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed5: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed6: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed7: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed8: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed9: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed10: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed11: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed12: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed13: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed14: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed15: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed16: Boolean = false,
  @ColumnInfo(defaultValue = "0") val missed17: Boolean = false,
) : Parcelable
