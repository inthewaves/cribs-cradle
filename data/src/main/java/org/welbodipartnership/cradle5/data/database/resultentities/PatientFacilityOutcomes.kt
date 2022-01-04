package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient

@Immutable
data class PatientFacilityOutcomes(
  @Embedded
  val patient: Patient,
  @Relation(parentColumn = "healthcareFacilityId", entityColumn = "id")
  val facility: Facility,
  @Relation(parentColumn = "id", entityColumn = "patientId")
  val outcomes: Outcomes?
)
