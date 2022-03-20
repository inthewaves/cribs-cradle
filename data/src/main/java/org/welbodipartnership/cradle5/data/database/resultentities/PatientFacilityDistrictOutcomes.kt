package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient

@Immutable
data class PatientFacilityDistrictOutcomes(
  @Embedded
  val patient: Patient,
  @Relation(parentColumn = "healthcareFacilityId", entityColumn = "id")
  val facility: Facility?,
  @Relation(parentColumn = "patient_referral_fromDistrict", entityColumn = "id")
  val referralFromDistrict: District?,
  @Relation(parentColumn = "patient_referral_fromFacility", entityColumn = "id")
  val referralFromFacility: Facility?,
  @Relation(parentColumn = "patient_referral_toDistrict", entityColumn = "id")
  val referralToDistrict: District?,
  @Relation(parentColumn = "patient_referral_toFacility", entityColumn = "id")
  val referralToFacility: Facility?,
  @Relation(parentColumn = "id", entityColumn = "patientId")
  val outcomes: Outcomes?
)
