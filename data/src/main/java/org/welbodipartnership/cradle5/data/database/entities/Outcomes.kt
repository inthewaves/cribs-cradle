package org.welbodipartnership.cradle5.data.database.entities

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.verification.HasRequiredFields
import org.welbodipartnership.cradle5.data.verification.Verifiable
import org.welbodipartnership.cradle5.util.datetime.FormDate
import kotlin.reflect.KProperty1

@Entity(
  indices = [
    Index("patientId")
  ],
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
  val serverErrorMessage: String?,

  /** Whether nullability is from the user not selected an option and saving as draft */
  @ColumnInfo(defaultValue = "1")
  val eclampsiaFitTouched: TouchedState,
  @Embedded(prefix = "eclampsia_")
  val eclampsiaFit: EclampsiaFit?,

  @ColumnInfo(defaultValue = "1")
  val hysterectomyTouched: TouchedState,
  @Embedded(prefix = "hysterectomy_")
  val hysterectomy: Hysterectomy?,

  @ColumnInfo(defaultValue = "1")
  val hduOrItuAdmissionTouched: TouchedState,
  @Embedded(prefix = "hdu_itu_admission_")
  val hduOrItuAdmission: HduOrItuAdmission?,

  @ColumnInfo(defaultValue = "1")
  val maternalDeathTouched: TouchedState,
  @Embedded(prefix = "maternal_death_")
  val maternalDeath: MaternalDeath?,

  @ColumnInfo(defaultValue = "1")
  val surgicalManagementTouched: TouchedState,
  @Embedded(prefix = "surgical_mgmt_")
  val surgicalManagement: SurgicalManagementOfHaemorrhage?,

  @ColumnInfo(defaultValue = "1")
  val perinatalDeathTouched: TouchedState,
  @Embedded(prefix = "perinatal_death_")
  val perinatalDeath: PerinatalDeath?
) : FormEntity, Verifiable<Outcomes>, HasRequiredFields {

  override fun requiredFieldsPresent(): Boolean {
    return when {
      eclampsiaFit?.requiredFieldsPresent() == false ||
        hysterectomy?.requiredFieldsPresent() == false ||
        hduOrItuAdmission?.requiredFieldsPresent() == false ||
        maternalDeath?.requiredFieldsPresent() == false ||
        surgicalManagement?.requiredFieldsPresent() == false ||
        perinatalDeath?.requiredFieldsPresent() == false -> false
      else -> true
    }
  }

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
  @Required
  val date: FormDate?,
  /**
   * The server provides enums for location starting at 1
   */
  val place: EnumSelection.IdOnly?,
) : HasRequiredFields {
  override fun requiredFieldsPresent() = date != null
}

/**
 * Represents the first eclampsia fit
 */
@Immutable
data class Hysterectomy(
  @Required
  val date: FormDate?,
  /**
   * The server provides enums for `cause` starting at 1
   */
  @Embedded(prefix = "cause_")
  val cause: EnumSelection.WithOther?,
) : HasRequiredFields {
  override fun requiredFieldsPresent() = date != null
}

@Immutable
data class HduOrItuAdmission(
  @Required
  val date: FormDate?,
  /**
   * The server provides enums for `cause` starting at 1
   */
  @Required
  @Embedded(prefix = "cause_")
  val cause: EnumSelection.WithOther?,
  val stayInDays: Int?,
  val additionalInfo: String?
) : HasRequiredFields {
  override fun requiredFieldsPresent() = date != null && cause != null
}

@Immutable
data class MaternalDeath(
  @Required
  val date: FormDate?,
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
) : HasRequiredFields {
  override fun requiredFieldsPresent() = date != null && underlyingCause != null
}

/**
 * Surgical management of postpartum haemorrhage requiring anaesthesia
 */
@Immutable
data class SurgicalManagementOfHaemorrhage(
  @Required
  val date: FormDate?,
  @Embedded(prefix = "type_")
  val typeOfSurgicalManagement: EnumSelection.WithOther?,
) : HasRequiredFields {
  override fun requiredFieldsPresent() = date != null
}

/**
 * Surgical management of postpartum haemorrhage requiring anaesthesia
 */
@Immutable
data class PerinatalDeath(
  @Required
  val date: FormDate?,
  val outcome: EnumSelection.IdOnly?,
  @Embedded(prefix = "maternalfactors_")
  val relatedMaternalFactors: EnumSelection.WithOther?
) : HasRequiredFields {
  override fun requiredFieldsPresent() = date != null
}

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
