package org.welbodipartnership.cradle5.domain

import org.welbodipartnership.api.cradle5.CradleImplementationData
import org.welbodipartnership.api.cradle5.GpsForm
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.util.datetime.toUnixTimestamp

fun CradleTrainingForm.toApiBody() = CradleImplementationData(
  recordLastUpdated = recordLastUpdated,
  district = district,
  healthcareFacility = healthcareFacility,
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
  totalStaffTrainedTodaySECHNMidwives = totalStaffTrainedTodaySECHNMidwives,
  totalStaffTrainedTodaySRNs = totalStaffTrainedTodaySRNs,
  totalStaffTrainedTodayCHOs = totalStaffTrainedTodayCHOs,
  totalStaffTrainedTodayCHAs = totalStaffTrainedTodayCHAs,
  totalStaffTrainedTodayCSECHNs = totalStaffTrainedTodayCSECHNs,
  totalStaffTrainedTodayMCHAides = totalStaffTrainedTodayMCHAides,
  totalStaffTrainedTodayTBA = totalStaffTrainedTodayTBA,
  totalStaffTrainedBefore = totalStaffTrainedBefore,
  totalStaffTrainedScored8 = totalStaffTrainedScored8
)

fun LocationCheckIn.toApiBody(userId: Int) = GpsForm(
  userId = userId.toString(),
  dateTimeIso8601 = this.timestamp.toUnixTimestamp().formatAsIso8601Date(),
  coordinates = "${this.latitude},${this.longitude}"
)
