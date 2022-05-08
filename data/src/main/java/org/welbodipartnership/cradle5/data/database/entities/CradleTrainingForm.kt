package org.welbodipartnership.cradle5.data.database.entities

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.datetime.FormDate


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

  val recordLastUpdated: String?,
  val district: Int?,
  val healthcareFacility: Int?,
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
  val totalStaffTrainedScored8: Int?,

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
) : FormEntity

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
