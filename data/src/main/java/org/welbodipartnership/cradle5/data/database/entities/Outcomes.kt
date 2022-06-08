package org.welbodipartnership.cradle5.data.database.entities

import android.content.Context
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
  val maternalDeathTouched: TouchedState,
  @Embedded(prefix = "maternal_death_")
  val maternalDeath: MaternalDeath?,

  @ColumnInfo(defaultValue = "1")
  val perinatalDeathTouched: TouchedState,
  @Embedded(prefix = "perinatal_death_")
  val perinatalDeath: PerinatalDeath?,

  @Embedded(prefix = "birthweight_")
  val birthWeight: BirthWeight?,

  @Embedded(prefix = "age_at_delivery_")
  val ageAtDelivery: AgeAtDelivery?,
) : TreeFormEntity, Verifiable<Outcomes>, HasRequiredFields {

  override fun requiredFieldsPresent(): Boolean {
    return when {
      eclampsiaFit?.requiredFieldsPresent() == false ||
        hysterectomy?.requiredFieldsPresent() == false ||
        maternalDeath?.requiredFieldsPresent() == false ||
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
 * Represents Pre-eclampsia/severe pre-eclampsia/ eclampsia
 */
@Immutable
data class EclampsiaFit(
  @Required
  val didTheWomanFit: Boolean?,
  /**
   * The server provides enums for when the fit occurred starting at 1
   */
  val whenWasFirstFit: EnumSelection.IdOnly?,
  /**
   * The server provides enums for location starting at 1
   */
  val place: EnumSelection.IdOnly?,
) : HasRequiredFields {
  override fun requiredFieldsPresent() = didTheWomanFit != null
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
  val summaryOfMdsrFindings: String?,
) : HasRequiredFields {
  override fun requiredFieldsPresent() = date != null && underlyingCause != null
}

/**
 * Surgical management of postpartum haemorrhage requiring anaesthesia
 */
@Immutable
data class PerinatalDeath(
  @Required
  val date: FormDate?,
  val outcome: EnumSelection.IdOnly?,
  @Embedded(prefix = "cause_of_stillbirth_")
  val causeOfStillbirth: EnumSelection.IdOnly?,
  @Embedded(prefix = "cause_of_neonatal_death_")
  val causesOfNeonatalDeath: CausesOfNeonatalDeath?,
  val additionalInfo: String?
) : HasRequiredFields {
  override fun requiredFieldsPresent() = date != null
}

@Immutable
@Parcelize
data class CausesOfNeonatalDeath(
  @ColumnInfo(defaultValue = "0") val respiratoryDistressSyndrome: Boolean = false,
  @ColumnInfo(defaultValue = "0") val birthAsphyxia: Boolean = false,
  @ColumnInfo(defaultValue = "0") val sepsis: Boolean = false,
  @ColumnInfo(defaultValue = "0") val pneumonia: Boolean = false,
  @ColumnInfo(defaultValue = "0") val meningitis: Boolean = false,
  @ColumnInfo(defaultValue = "0") val malaria: Boolean = false,
  @ColumnInfo(defaultValue = "0") val majorCongenitialMalformation: Boolean = false,
  @ColumnInfo(defaultValue = "0") val prematurity: Boolean = false,
  @ColumnInfo(defaultValue = "0") val causeNotEstablished: Boolean = false,
  @ColumnInfo(defaultValue = "0") val other: Boolean = false,
  @ColumnInfo(defaultValue = "0") val notReported: Boolean = false
) : Parcelable {
  @IgnoredOnParcel
  @Ignore
  val areAllFieldsFalse = !respiratoryDistressSyndrome &&
    !birthAsphyxia &&
    !sepsis && !pneumonia && !meningitis && !malaria && !majorCongenitialMalformation &&
    !prematurity && !causeNotEstablished && !other
}

@Immutable
data class BirthWeight(
  val birthWeight: EnumSelection.IdOnly?,
  val isNotReported: Boolean = false,
)

@Immutable
data class AgeAtDelivery(
  val ageAtDelivery: EnumSelection.IdOnly?,
  val isNotReported: Boolean = false,
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
