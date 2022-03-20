package org.welbodipartnership.cradle5.patients

import org.welbodipartnership.cradle5.data.database.entities.AgeAtDelivery
import org.welbodipartnership.cradle5.data.database.entities.BirthWeight
import org.welbodipartnership.cradle5.data.database.entities.EclampsiaFit
import org.welbodipartnership.cradle5.data.database.entities.HduOrItuAdmission
import org.welbodipartnership.cradle5.data.database.entities.Hysterectomy
import org.welbodipartnership.cradle5.data.database.entities.MaternalDeath
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.PatientReferralInfo
import org.welbodipartnership.cradle5.data.database.entities.PerinatalDeath
import org.welbodipartnership.cradle5.data.database.entities.SurgicalManagementOfHaemorrhage
import org.welbodipartnership.cradle5.data.database.entities.TouchedState
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.datetime.FormDate

object PatientPreviewClasses {
  fun createTestPatient(
    serverInfo: ServerInfo? = null,
    serverErrorMessage: String? = null,
    isDraft: Boolean = false,
  ) = Patient(
    initials = "AA",
    serverInfo = serverInfo,
    serverErrorMessage = serverErrorMessage,
    presentationDate = FormDate(day = 10, month = 2, year = 1995),
    dateOfBirth = FormDate(day = 19, month = 8, year = 1989),
    isAgeKnown = TouchedState.TOUCHED_ENABLED,
    address = "Test address",
    healthcareFacilityId = 50L,
    lastUpdatedTimestamp = 162224953,
    isDraft = isDraft,
    referralInfoTouched = TouchedState.TOUCHED,
    referralInfo = PatientReferralInfo(
      fromDistrict = 2L,
      fromFacility = 34L,
      toDistrict = 3L,
      toFacility = 145L
    )
  )

  fun createTestOutcomes() = Outcomes(
    patientId = 5L,
    serverInfo = null,
    serverErrorMessage = "some error",
    eclampsiaFitTouched = TouchedState.TOUCHED_ENABLED,
    eclampsiaFit = EclampsiaFit(
      date = FormDate(20, 4, 2019),
      place = EnumSelection.IdOnly(2)
    ),
    hysterectomyTouched = TouchedState.TOUCHED_ENABLED,
    hysterectomy = Hysterectomy(
      date = FormDate.today(),
      cause = EnumSelection.WithOther(4, "The other string"),
    ),
    hduOrItuAdmissionTouched = TouchedState.TOUCHED_ENABLED,
    hduOrItuAdmission = HduOrItuAdmission(
      date = FormDate.today(),
      cause = EnumSelection.WithOther(4, "This is input for the `other` cause"),
      stayInDays = 5,
      additionalInfo = "Additional info here"
    ),
    maternalDeathTouched = TouchedState.TOUCHED_ENABLED,
    maternalDeath = MaternalDeath(
      date = FormDate.today(),
      underlyingCause = EnumSelection.WithOther(6),
      place = EnumSelection.IdOnly(2),
    ),
    surgicalManagementTouched = TouchedState.TOUCHED_ENABLED,
    surgicalManagement = SurgicalManagementOfHaemorrhage(
      date = FormDate.today(),
      typeOfSurgicalManagement = EnumSelection.WithOther(3)
    ),
    perinatalDeathTouched = TouchedState.TOUCHED_ENABLED,
    perinatalDeath = PerinatalDeath(
      date = FormDate.today(),
      outcome = EnumSelection.IdOnly(2),
      relatedMaternalFactors = EnumSelection.WithOther(8),
      additionalInfo = null,
    ),
    birthWeight = BirthWeight(EnumSelection.IdOnly(1)),
    ageAtDelivery = AgeAtDelivery(EnumSelection.IdOnly(1))
  )
}
