package org.welbodipartnership.cradle5.data.database.entities

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.text.isDigitsOnly
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.welbodipartnership.cradle5.data.R
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.verification.Verifiable
import org.welbodipartnership.cradle5.util.datetime.FormDate
import kotlin.reflect.KProperty1

/**
 * Represents a patient on the server. Note that this representation is different compared to how
 * the server sees patient. This class associates a patient with the Registration information
 * (because on the web app, one cannot create a patient without first entering their registration
 * information)
 */
@Entity(
  indices = [
    Index("healthcareFacilityId")
  ]
)
@Immutable
data class Patient(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Long = 0,
  @Embedded
  val serverInfo: ServerInfo?,
  val initials: String,
  val presentationDate: FormDate?,
  val dateOfBirth: FormDate,
  val healthcareFacilityId: Long,
  /**
   * A Unix timestamp of when this was last updated
   */
  val lastUpdatedTimestamp: Long = System.currentTimeMillis() / 1000L,
  /**
   * Local notes the user may have saved for the patient. This is not uploaded to the server.
   */
  val localNotes: String? = null
) : Verifiable<Patient> {
  val serverPatientId: Long? get() = serverInfo?.objectId

  fun isValueForPropertyValid(
    property: KProperty1<out Patient, *>,
    value: Any?,
    context: Context?
  ): Verifiable.Result = isValueValid(property, value, context, instance = this)

  companion object : Verifiable.Verifier<Patient> {
    const val INITIALS_MAX_LENGTH = 5

    override fun isValueValid(
      property: KProperty1<out Patient, *>,
      value: Any?,
      context: Context?,
      instance: Patient?,
      currentValues: Map<String, Any?>?
    ): Verifiable.Result = when (property) {
      Patient::initials -> with(value as? String) {
        if (isNullOrBlank() || length !in 1..INITIALS_MAX_LENGTH) {
          return Verifiable.Invalid(
            property, context?.getString(R.string.patient_error_initials_missing)
          )
        }

        if (!isDigitsOnly()) {
          return Verifiable.Invalid(
            property,
            context?.getString(R.string.patient_error_initials_cant_have_digits)
          )
        }

        return Verifiable.Valid
      }
      Patient::presentationDate -> with(value as? FormDate) {
        if (this == null) {
          return Verifiable.Invalid(
            property, "Missing patient presentation date"
          )
        }
        if (this.getAgeInYearsFromNow() < 0) {
          return Verifiable.Invalid(
            property, "Can't be in the future"
          )
        }

        return Verifiable.Valid
      }

      else -> Verifiable.Valid
    }
  }
}
