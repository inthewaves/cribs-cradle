package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.datetime.FormDate

/**
 * A shortened view of the patient
 *
 * @see Patient
 */
data class ListPatient(
  val id: Long,
  @Embedded
  val serverInfo: ServerInfo?,
  val serverErrorMessage: String?,

  val initials: String,
  val dateOfBirth: FormDate?,
  val localNotes: String?,
  val isDraft: Boolean,
)

@Immutable
data class ListPatientAndOutcomeError(
  @Embedded
  val listPatient: ListPatient,
  @Relation(entity = Outcomes::class, parentColumn = "id", entityColumn = "patientId")
  val outcomeError: OutcomeErrorOnly?
)

@Immutable
data class OutcomeErrorOnly(
  val id: Long,
  val patientId: Long,
  @Embedded
  val serverInfo: ServerInfo?,
  val serverErrorMessage: String?,
)
