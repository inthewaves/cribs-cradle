package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility

@Immutable
data class CradleTrainingFormFacilityDistrict(
  @Embedded
  val form: CradleTrainingForm,
  @Relation(parentColumn = "healthcareFacility", entityColumn = "id")
  val facility: Facility?,
  @Relation(parentColumn = "district", entityColumn = "id")
  val district: District?,
)
