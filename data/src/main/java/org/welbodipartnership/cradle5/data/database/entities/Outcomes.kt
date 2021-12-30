package org.welbodipartnership.cradle5.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumWithOther
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.date.FormDate

@Entity(
  indices = [
    Index("patientId")
  ],
  foreignKeys = [
    ForeignKey(
      entity = Patient::class,
      parentColumns = ["id"],
      childColumns = ["patientId"],
      onDelete = ForeignKey.CASCADE
    )
  ]
)
data class Outcomes(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Long = 0,
  @ColumnInfo(name = "patientId")
  val patientId: Long,
  @Embedded
  val serverInfo: ServerInfo? = null,

  @Embedded(prefix = "eclampsia_")
  val eclampsiaFit: EclampsiaFit? = null,
)

sealed class Location(val serverId: Long) {
  /**
   * An unknown location type
   */
  class Unknown(serverId: Long) : Location(serverId)

  /**
   * Community is defined as anywhere outside a healthcare facility,
   */
  class Community : Location(1)

  /**
   * Peripheral health unit is defined as any primary healthcare facility e.g. maternal health post
   * or clinic,
   */
  class PeripheralHealthUnit : Location(2)

  /**
   * Hospital is defined as any secondary or tertiary level healthcare facility e.g. district
   * hospital or referral hospital.
   */
  class Hospital : Location(3)
}

/**
 * Represents the first eclampsia fit
 */
data class EclampsiaFit(
  val date: FormDate,
  /**
   * The server provides enums for location starting at 1
   */
  @Embedded
  val locationId: EnumWithOther,
)
