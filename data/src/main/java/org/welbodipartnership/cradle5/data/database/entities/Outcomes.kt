package org.welbodipartnership.cradle5.data.database.entities

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.verification.Verifiable
import org.welbodipartnership.cradle5.util.datetime.FormDate
import kotlin.reflect.KProperty1

@Entity(
  indices = [
    Index("patientId")
  ],
  foreignKeys = [
    ForeignKey(
      entity = Patient::class,
      parentColumns = ["id"],
      childColumns = ["patientId"],
      onDelete = ForeignKey.CASCADE,
      onUpdate = ForeignKey.CASCADE
    )
  ]
)
@Immutable
data class Outcomes(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  override val id: Long = 0,
  @ColumnInfo(name = "patientId")
  val patientId: Long,
  @Embedded
  override val serverInfo: ServerInfo?,

  @Embedded(prefix = "eclampsia_")
  val eclampsiaFit: EclampsiaFit?,
  @Embedded(prefix = "hysterectomy_")
  val hysterectomy: Hysterectomy?,
  @Embedded(prefix = "hdu_itu_admission_")
  val hduOrItuAdmission: HduOrItuAdmission?,
  @Embedded(prefix = "maternal_death_")
  val maternalDeath: MaternalDeath?,
  @Embedded(prefix = "surgical_mgmt_")
  val surgicalManagement: SurgicalManagementOfHaemorrhage?,
  @Embedded(prefix = "perinatal_death_")
  val perinatalDeath: PerinatalDeath?
) : FormEntity, Verifiable<Outcomes> {
  val isUploadedToServer: Boolean get() = serverInfo?.nodeId != null

  fun isValueForPropertyValid(
    property: KProperty1<out Outcomes, *>,
    value: Any?,
    context: Context?
  ): Verifiable.Result {
    TODO("Not yet implemented")
  }
}

/**
 * Represents the first eclampsia fit
 */
@Immutable
data class EclampsiaFit(
  val date: FormDate,
  /**
   * The server provides enums for location starting at 1
   */
  @get:JvmName("getPlace")
  val place: EnumSelection.IdOnly?,
)

/**
 * Represents the first eclampsia fit
 */
@Immutable
data class Hysterectomy(
  val date: FormDate,
  /**
   * The server provides enums for `cause` starting at 1
   */
  @Embedded(prefix = "cause_")
  val cause: EnumSelection.WithOther?,
  val additionalInfo: String?
)

@Immutable
data class HduOrItuAdmission(
  val date: FormDate,
  /**
   * The server provides enums for `cause` starting at 1
   */
  @Embedded(prefix = "cause_")
  val cause: EnumSelection.WithOther,
  val stayInDays: Int?,
)

@Immutable
data class MaternalDeath(
  val date: FormDate,
  /**
   * The server provides enums for `cause` starting at 1
   */
  @Embedded(prefix = "cause_")
  val underlyingCause: EnumSelection.WithOther?,
  /**
   * The server provides enums for location starting at 1
   */
  @get:JvmName("getPlace")
  val place: EnumSelection.IdOnly?,
)

/**
 * Surgical management of postpartum haemorrhage requiring anaesthesia
 */
@Immutable
data class SurgicalManagementOfHaemorrhage(
  val date: FormDate,
  @Embedded(prefix = "type_")
  val typeOfSurgicalManagement: EnumSelection.WithOther?,
)

/**
 * Surgical management of postpartum haemorrhage requiring anaesthesia
 */
@Immutable
data class PerinatalDeath(
  val date: FormDate,
  @get:JvmName("getOutcome")
  val outcome: EnumSelection.IdOnly?,
  @Embedded(prefix = "maternalfactors_")
  val relatedMaternalFactors: EnumSelection.WithOther?
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
