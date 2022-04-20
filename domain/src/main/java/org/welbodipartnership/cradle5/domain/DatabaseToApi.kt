package org.welbodipartnership.cradle5.domain

import org.welbodipartnership.api.cradle5.GpsForm
import org.welbodipartnership.api.cradle5.Outcome
import org.welbodipartnership.api.cradle5.Registration
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.util.datetime.toUnixTimestamp

fun Patient.toApiBody() = Registration(
  initials = initials,
  presentationDate = presentationDate,
  age = dateOfBirth?.getAgeInYearsFromNow()?.toInt(),
  ageUnknown = isAgeUnknown || dateOfBirth == null,
  address = address,
  healthcareFacility = healthcareFacilityId!!.toInt(),

  wasPatientReferral = referralInfo != null,
  patientReferralFromDistrict = referralInfo?.fromDistrict?.toInt(),
  patientReferralFromFacility = referralInfo?.fromFacility?.toInt(),
  patientReferralFromFacilityCustom = referralInfo?.fromFacilityText,
  patientReferralToDistrict = referralInfo?.toDistrict?.toInt(),
  patientReferralToFacility = referralInfo?.toFacility?.toInt(),
  patientReferralToFacilityCustom = referralInfo?.toFacilityText,
)

fun Outcomes.toApiBody() = Outcome(
  hadPerinatalDeath = perinatalDeath != null,
  perinatalDeathDate = perinatalDeath?.date,
  perinatalOutcome = perinatalDeath?.outcome?.selectionId,
  perinatalCauseOfStillBirth = perinatalDeath?.causeOfStillbirth?.selectionId,
  perinatalNeonatalDeathRespDistressSyndrome = perinatalDeath?.causesOfNeonatalDeath?.respiratoryDistressSyndrome ?: false,
  perinatalNeonatalDeathBirthAsphyxia = perinatalDeath?.causesOfNeonatalDeath?.birthAsphyxia ?: false,
  perinatalNeonatalDeathSepsis = perinatalDeath?.causesOfNeonatalDeath?.sepsis ?: false,
  perinatalNeonatalDeathPneumonia = perinatalDeath?.causesOfNeonatalDeath?.pneumonia ?: false,
  perinatalNeonatalDeathMeningitis = perinatalDeath?.causesOfNeonatalDeath?.meningitis ?: false,
  perinatalNeonatalDeathMalaria = perinatalDeath?.causesOfNeonatalDeath?.malaria ?: false,
  perinatalNeonatalDeathMajorCongenitalMalformation = perinatalDeath?.causesOfNeonatalDeath?.majorCongenitialMalformation ?: false,
  perinatalNeonatalDeathPrematurity = perinatalDeath?.causesOfNeonatalDeath?.prematurity ?: false,
  perinatalNeonatalDeathCauseNotEstablished = perinatalDeath?.causesOfNeonatalDeath?.causeNotEstablished ?: false,
  perinatalNeonatalDeathCauseOther = perinatalDeath?.causesOfNeonatalDeath?.other ?: false,
  perinatalAdditionalInfo = perinatalDeath?.additionalInfo?.ifBlank { null },

  birthWeight = if (birthWeight?.isNotReported == true) null else birthWeight?.birthWeight?.selectionId,
  birthWeightNotReported = birthWeight?.isNotReported == true,
  ageAtDelivery = if (ageAtDelivery?.isNotReported == true) null else ageAtDelivery?.ageAtDelivery?.selectionId,
  ageAtDeliveryNotReported = ageAtDelivery?.isNotReported == true,

  hadFirstEclampsiaFit = eclampsiaFit != null,
  eclampsiaDidTheWomanFit = eclampsiaFit?.didTheWomanFit,
  eclampsiaWhenWasFirstFit = eclampsiaFit?.whenWasFirstFit?.selectionId,
  eclampsiaFitLocation = eclampsiaFit?.place?.selectionId,

  hasHysterectomy = hysterectomy != null,
  hysterectomyDate = hysterectomy?.date,
  hysterectomyCause = hysterectomy?.cause?.selectionId,
  hysterectomyOtherCause = hysterectomy?.cause?.otherString?.ifBlank { null },

  hasMaternalDeath = maternalDeath != null,
  maternalDeathDate = maternalDeath?.date,
  maternalDeathUnderlyingCause = maternalDeath?.underlyingCause?.selectionId,
  maternalDeathOtherCause = maternalDeath?.underlyingCause?.otherString?.ifBlank { null },
  maternalDeathPlace = maternalDeath?.place?.selectionId,
  maternalDeathMdsrFindingsSummary = maternalDeath?.summaryOfMdsrFindings
)

fun LocationCheckIn.toApiBody(userId: Int) = GpsForm(
  userId = userId.toString(),
  dateTimeIso8601 = this.timestamp.toUnixTimestamp().formatAsIso8601Date(),
  coordinates = "${this.latitude},${this.longitude}"
)
