package org.welbodipartnership.cradle5.domain

import org.welbodipartnership.api.cradle5.Outcome
import org.welbodipartnership.api.cradle5.Registration
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient

fun Patient.toApiBody() = Registration(
  initials = initials,
  presentationDate = presentationDate,
  birthdateDate = dateOfBirth,
  age = if (dateOfBirth.isExact) null else dateOfBirth.getAgeInYearsFromNow().toInt(),
  healthcareFacility = healthcareFacilityId.toInt()
)

fun Outcomes.toApiBody() = Outcome(
  hadFirstEclampsiaFit = eclampsiaFit != null,
  eclampsiaFitDate = eclampsiaFit?.date,
  eclampsiaFitLocation = eclampsiaFit?.place?.selectionId,

  hasHysterectomy = hysterectomy != null,
  hysterectomyDate = hysterectomy?.date,
  hysterectomyCause = hysterectomy?.cause?.selectionId,
  hysterectomyOtherCause = hysterectomy?.cause?.otherString?.ifBlank { null },
  hysterectomyAdditionalInfo = hysterectomy?.additionalInfo,

  isAdmittedToHduOrItu = hduOrItuAdmission != null,
  hduOrItuAdmissionDate = hduOrItuAdmission?.date,
  hduOrItuAdmissionCause = hduOrItuAdmission?.cause?.selectionId,
  hduOrItuAdmissionOtherCause = hduOrItuAdmission?.cause?.otherString?.ifBlank { null },
  hduOrItuStayDays = hduOrItuAdmission?.stayInDays,

  hasMaternalDeath = maternalDeath != null,
  maternalDeathDate = maternalDeath?.date,
  maternalDeathUnderlyingCause = maternalDeath?.underlyingCause?.selectionId,
  maternalDeathOtherCause = maternalDeath?.underlyingCause?.otherString?.ifBlank { null },
  maternalDeathPlace = maternalDeath?.place?.selectionId,

  hadSurgicalMgmtOfPostpartumHaemorrhageAndAnaesthesia = surgicalManagement != null,
  surgicalManagementDate = surgicalManagement?.date,
  surgicalManagementType = surgicalManagement?.typeOfSurgicalManagement?.selectionId,
  surgicalManagementOtherType = surgicalManagement?.typeOfSurgicalManagement?.otherString?.ifBlank { null },

  hadPerinatalDeath = perinatalDeath != null,
  perinatalDeathDate = perinatalDeath?.date,
  perinatalOutcome = perinatalDeath?.outcome?.selectionId,
  perinatalMaternalFactors = perinatalDeath?.relatedMaternalFactors?.selectionId,
  perinatalOtherMaternalFactors = perinatalDeath?.relatedMaternalFactors?.otherString
)
