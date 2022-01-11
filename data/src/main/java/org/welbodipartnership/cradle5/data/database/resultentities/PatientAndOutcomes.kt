package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient

interface PatientOutcomePair {
  val patient: Patient?
  val outcomes: Outcomes?
}

@Immutable
data class PatientAndOutcomes(
  @Embedded
  override val patient: Patient,
  @Relation(parentColumn = "id", entityColumn = "patientId")
  override val outcomes: Outcomes?
) : PatientOutcomePair

@Immutable
data class OutcomesAndPatient(
  @Embedded
  override val outcomes: Outcomes,
  @Relation(parentColumn = "patientId", entityColumn = "id")
  override val patient: Patient?,
) : PatientOutcomePair
