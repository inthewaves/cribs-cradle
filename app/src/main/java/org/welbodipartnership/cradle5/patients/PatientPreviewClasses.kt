package org.welbodipartnership.cradle5.patients

import org.welbodipartnership.cradle5.data.database.entities.AgeAtDelivery
import org.welbodipartnership.cradle5.data.database.entities.BirthWeight
import org.welbodipartnership.cradle5.data.database.entities.CausesOfNeonatalDeath
import org.welbodipartnership.cradle5.data.database.entities.EclampsiaFit
import org.welbodipartnership.cradle5.data.database.entities.FacilityBpInfo
import org.welbodipartnership.cradle5.data.database.entities.Hysterectomy
import org.welbodipartnership.cradle5.data.database.entities.MaternalDeath
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.PatientReferralInfo
import org.welbodipartnership.cradle5.data.database.entities.PerinatalDeath
import org.welbodipartnership.cradle5.data.database.entities.TouchedState
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.datetime.FormDate

object PatientPreviewClasses {
  const val FROM_DISTRICT_ID = 2L
  const val FROM_FACILITY_ID = 34L
  const val TO_DISTRICT_ID = 3L
  const val TO_FACILITY_ID = 145L

  fun createTestReferralInfo() = PatientReferralInfo(
    fromDistrict = FROM_DISTRICT_ID,
    fromFacility = FROM_FACILITY_ID,
    fromFacilityText = null,
    toDistrict = TO_DISTRICT_ID,
    toFacility = TO_FACILITY_ID,
    toFacilityText = "Custom to facility"
  )

  fun createTestPatient(
    serverInfo: ServerInfo? = null,
    serverErrorMessage: String? = null,
    isDraft: Boolean = false,
  ) = Patient(
    facilityBpInfoTodayTouched = TouchedState.TOUCHED,
    facilityBpInfoToday = FacilityBpInfo(
      numBpReadingsTakenInFacilitySinceLastVisit = 5,
      numBpReadingsEndIn0Or5 = 2,
      numBpReadingsWithColorAndArrow = 1
    ),
    initials = "AA",
    serverInfo = serverInfo,
    serverErrorMessage = serverErrorMessage,
    presentationDate = FormDate(day = 10, month = 2, year = 1995),
    dateOfBirth = FormDate(day = 19, month = 8, year = 1989),
    isAgeUnknown = false,
    address = "Test address",
    healthcareFacilityId = 50,
    lastUpdatedTimestamp = 162224953,
    isDraft = isDraft,
    referralInfoTouched = TouchedState.TOUCHED,
    referralInfo = createTestReferralInfo()
  )

  fun createTestOutcomes() = Outcomes(
    patientId = 5L,
    serverInfo = null,
    serverErrorMessage = "some error",
    eclampsiaFitTouched = TouchedState.TOUCHED_ENABLED,
    eclampsiaFit = EclampsiaFit(
      didTheWomanFit = true,
      whenWasFirstFit = EnumSelection.IdOnly(1),
      place = EnumSelection.IdOnly(2)
    ),
    hysterectomyTouched = TouchedState.TOUCHED_ENABLED,
    hysterectomy = Hysterectomy(
      date = FormDate.today(),
      cause = EnumSelection.WithOther(4, "The other string"),
    ),
    maternalDeathTouched = TouchedState.TOUCHED_ENABLED,
    maternalDeath = MaternalDeath(
      date = FormDate.today(),
      underlyingCause = EnumSelection.WithOther(6),
      place = EnumSelection.IdOnly(2),
      summaryOfMdsrFindings = "MDSR findings"
    ),
    perinatalDeathTouched = TouchedState.TOUCHED_ENABLED,
    perinatalDeath = PerinatalDeath(
      date = FormDate.today(),
      outcome = EnumSelection.IdOnly(2),
      causeOfStillbirth = EnumSelection.IdOnly(4),
      causesOfNeonatalDeath = CausesOfNeonatalDeath(respiratoryDistressSyndrome = true),
      additionalInfo = null,
    ),
    birthWeight = BirthWeight(EnumSelection.IdOnly(1)),
    ageAtDelivery = AgeAtDelivery(EnumSelection.IdOnly(1))
  )
}
