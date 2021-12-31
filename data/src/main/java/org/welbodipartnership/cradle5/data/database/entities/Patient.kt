package org.welbodipartnership.cradle5.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.date.FormDate

/**
 * Represents a patient on the server. Note that this representation is different compared to how
 * the server sees patient. This class associates a patient with the Registration information
 * (because on the web app, one cannot create a patient without first entering their registration
 * information)
 */
@Entity
data class Patient(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Long = 0,
  @Embedded
  val serverInfo: ServerInfo? = null,
  val initials: String,
  @get:JvmName("getPresentationDate")
  val presentationDate: FormDate,
  @get:JvmName("getDateOfBirth")
  val dateOfBirth: FormDate,
  /**
   * Represents whether [dateOfBirth] is exact or provided from an age.
   */
  val isExactDateOfBirth: Boolean,
  /**
   * A Unix timestamp of when this was last updated
   */
  val lastUpdatedTimestamp: Long,
  /**
   * Local notes the user may have saved for the patient. This is not uploaded to the server.
   */
  val localNotes: String? = null
) {
  val serverPatientId: Long? get() = serverInfo?.objectId
}
