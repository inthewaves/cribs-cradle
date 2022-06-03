package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.FacilityBpInfo

@Immutable
data class BpInfoFacilityDistrict(
  @Embedded
  val bpInfo: FacilityBpInfo,
  @Relation(parentColumn = "district", entityColumn = "id")
  val district: District?,
  @Relation(parentColumn = "facility", entityColumn = "id")
  val facility: Facility?,
)
