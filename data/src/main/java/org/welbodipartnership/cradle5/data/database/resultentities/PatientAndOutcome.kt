package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.room.Embedded
import androidx.room.Relation
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient

data class PatientAndOutcomes(
  @Embedded
  val patient: Patient,
  @Relation(
    parentColumn = "id",
    entityColumn = "patientId"
  )
  val outcomes: Outcomes?
)