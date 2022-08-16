package org.welbodipartnership.cradle5.cradleform

import org.welbodipartnership.cradle5.data.database.entities.ChecklistMissed
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.PowerSupply
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.datetime.FormDate
import java.time.ZonedDateTime

object CradleFormPreviewClasses {
  const val DISTRICT_ID = 3L
  const val FACILITY_ID = 145L

  fun createTestDistrict() = District(id = DISTRICT_ID, "1- Test district")

  fun createTestFacility() = Facility(id = FACILITY_ID, name = "Test Facility, In This Place (ABC)", hasVisited = false, listOrder = 1)

  fun createTestCradleForm(
    serverInfo: ServerInfo? = null,
    serverErrorMessage: String? = null,
    isDraft: Boolean = false,
  ) = CradleTrainingForm(
    id = 5L,
    serverInfo = serverInfo,
    serverErrorMessage = serverErrorMessage,
    recordLastUpdated = ZonedDateTime.now(),
    recordCreated = ZonedDateTime.now(),
    district = DISTRICT_ID,
    healthcareFacility = FACILITY_ID,
    dateOfTraining = FormDate.today(),
    numOfBpDevicesFunctioning = 5,
    numOfCradleDevicesFunctioning = 4,
    numOfCradleDevicesBroken = 5,
    powerSupply = PowerSupply(generator = true, grid = true),
    totalStaffWorking = 6,
    totalStaffProvidingMaternityServices = 7,
    totalStaffTrainedToday = 8,
    totalStaffTrainedTodayDoctors = 1,
    totalStaffTrainedTodayMidwives = 2,
    totalStaffTrainedTodaySACHOS = 3,
    totalStaffTrainedTodaySRNs = 5,
    totalStaffTrainedTodaySECHNs = 1,
    totalStaffTrainedTodayCHOs = 6,
    totalStaffTrainedTodayCHAs = 5,
    totalStaffTrainedTodayMCHAides = 4,
    totalStaffTrainedTodayTBA = 1,
    totalStaffTrainedTodayVolunteers = 1,
    totalStaffTrainedBefore = 4,
    totalStaffTrainedScoredMoreThan14 = 2,
    totalStaffObservedAndScored = 17-4,
    checklistMissed = ChecklistMissed(
      missed1 = true,
      missed2 = true,
      missed3 = true,
      missed4 = true,
    ),
    isDraft = isDraft
  )
}
