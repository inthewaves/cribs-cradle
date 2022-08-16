package org.welbodipartnership.cradle5.domain

import org.welbodipartnership.api.cradle5.CradleImplementationData
import org.welbodipartnership.api.cradle5.GpsForm
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.util.datetime.toUnixTimestamp

fun CradleTrainingForm.toApiBody() = CradleImplementationData(
  recordInsertDate = recordCreatedDateString,
  recordLastUpdated = null,
  district = district?.toInt(),
  healthcareFacility = healthcareFacility?.toInt(),
  dateOfTraining = dateOfTraining,
  numOfBpDevicesFunctioning = numOfBpDevicesFunctioning,
  numOfCradleDevicesFunctioning = numOfCradleDevicesFunctioning,
  numOfCradleDevicesBroken = numOfCradleDevicesBroken,
  powerSupplyGenerator = powerSupply?.generator,
  powerSupplySolar = powerSupply?.solar,
  powerSupplyGrid = powerSupply?.grid,
  powerSupplyNone = powerSupply?.none,

  totalStaffWorking = totalStaffWorking,
  totalStaffProvidingMaternityServices = totalStaffProvidingMaternityServices,
  totalStaffTrainedToday = totalStaffTrainedToday,
  totalStaffTrainedTodayDoctors = totalStaffTrainedTodayDoctors,
  totalStaffTrainedTodayMidwives = totalStaffTrainedTodayMidwives,
  totalStaffTrainedTodaySACHOS = totalStaffTrainedTodaySACHOS,
  totalStaffTrainedTodaySRNs = totalStaffTrainedTodaySRNs,
  totalStaffTrainedTodaySECHNs = totalStaffTrainedTodaySECHNs,
  totalStaffTrainedTodayCHOs = totalStaffTrainedTodayCHOs,
  totalStaffTrainedTodayCHAs = totalStaffTrainedTodayCHAs,
  totalStaffTrainedTodayMCHAides = totalStaffTrainedTodayMCHAides,
  totalStaffTrainedTodayTBA = totalStaffTrainedTodayTBA,
  totalStaffTrainedTodayVolunteers = totalStaffTrainedTodayVolunteers,
  totalStaffTrainedBefore = totalStaffTrainedBefore,
  totalStaffObservedAndScored = totalStaffObservedAndScored,
  totalStaffTrainedScoredMoreThan14OutOf17 = totalStaffTrainedScoredMoreThan14,
  checklistStep1Missed = checklistMissed?.missed1,
  checklistStep2Missed = checklistMissed?.missed2,
  checklistStep3Missed = checklistMissed?.missed3,
  checklistStep4Missed = checklistMissed?.missed4,
  checklistStep5Missed = checklistMissed?.missed5,
  checklistStep6Missed = checklistMissed?.missed6,
  checklistStep7Missed = checklistMissed?.missed7,
  checklistStep8Missed = checklistMissed?.missed8,
  checklistStep9Missed = checklistMissed?.missed9,
  checklistStep10Missed = checklistMissed?.missed10,
  checklistStep11Missed = checklistMissed?.missed11,
  checklistStep12Missed = checklistMissed?.missed12,
  checklistStep13Missed = checklistMissed?.missed13,
  checklistStep14Missed = checklistMissed?.missed14,
  checklistStep15Missed = checklistMissed?.missed15,
  checklistStep16Missed = checklistMissed?.missed16,
  checklistStep17Missed = checklistMissed?.missed17,
)

fun LocationCheckIn.toApiBody(userId: Int) = GpsForm(
  userId = userId.toString(),
  dateTimeIso8601 = this.timestamp.toUnixTimestamp().formatAsIso8601Date(),
  coordinates = "${this.latitude},${this.longitude}"
)
