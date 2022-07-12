package org.welbodipartnership.api.cradle5

import androidx.collection.ArrayMap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.forms.PostOperationId
import org.welbodipartnership.cradle5.util.datetime.FormDate

@JsonClass(generateAdapter = true)
@FormId(117)
@PostOperationId(247)
data class CradleImplementationData(
  @Json(name = "Control2249")
  val recordInsertDate: String?,
  @Json(name = "Control2029")
  val recordLastUpdated: String?,
  @Json(name = "Control2159")
  val district: Int?,
  @Json(name = "Control2018")
  val healthcareFacility: Int?,
  @Json(name = "Control2048")
  val dateOfTraining: FormDate?,
  @Json(name = "Control2020")
  val numOfBpDevicesFunctioning: Int?,
  @Json(name = "Control2187")
  val numOfCradleDevicesFunctioning: Int?,
  @Json(name = "Control2188")
  val numOfCradleDevicesBroken: Int?,
  @Json(name = "Control2190")
  val powerSupplyGenerator: Boolean?,
  @Json(name = "Control2191")
  val powerSupplySolar: Boolean?,
  @Json(name = "Control2192")
  val powerSupplyGrid: Boolean?,
  @Json(name = "Control2193")
  val powerSupplyNone: Boolean?,
  @Json(name = "Control2194")
  val totalStaffWorking: Int?,
  @Json(name = "Control2195")
  val totalStaffProvidingMaternityServices: Int?,

  @Json(name = "Control2196")
  val totalStaffTrainedToday: Int?,
  @Json(name = "Control2197")
  val totalStaffTrainedTodayDoctors: Int?,
  @Json(name = "Control2198")
  val totalStaffTrainedTodayMidwives: Int?,
  @Json(name = "Control2199")
  val totalStaffTrainedTodaySACHOS: Int?,
  @Json(name = "Control2200")
  val totalStaffTrainedTodaySECHNMidwives: Int?,
  @Json(name = "Control2201")
  val totalStaffTrainedTodaySRNs: Int?,
  @Json(name = "Control2202")
  val totalStaffTrainedTodayCHOs: Int?,
  @Json(name = "Control2203")
  val totalStaffTrainedTodayCHAs: Int?,
  @Json(name = "Control2204")
  val totalStaffTrainedTodayCSECHNs: Int?,
  @Json(name = "Control2205")
  val totalStaffTrainedTodayMCHAides: Int?,
  @Json(name = "Control2206")
  val totalStaffTrainedTodayTBA: Int?,
  @Json(name = "Control2207")
  val totalStaffTrainedBefore: Int?,
  @Json(name = "Control2283")
  val totalStaffObservedAndScored: Int?,
  /**
   * How many of the staff trained today scored more than 14/17 on the CRADLE checklist?
   */
  @Json(name = "Control2208")
  val totalStaffTrainedScoredMoreThan14OutOf17: Int?,
  @Json(name = "Control2265")
  val checklistStep1Missed: Boolean?,
  @Json(name = "Control2266")
  val checklistStep2Missed: Boolean?,
  @Json(name = "Control2267")
  val checklistStep3Missed: Boolean?,
  @Json(name = "Control2268")
  val checklistStep4Missed: Boolean?,
  @Json(name = "Control2269")
  val checklistStep5Missed: Boolean?,
  @Json(name = "Control2270")
  val checklistStep6Missed: Boolean?,
  @Json(name = "Control2271")
  val checklistStep7Missed: Boolean?,
  @Json(name = "Control2272")
  val checklistStep8Missed: Boolean?,
  @Json(name = "Control2273")
  val checklistStep9Missed: Boolean?,
  @Json(name = "Control2274")
  val checklistStep10Missed: Boolean?,
  @Json(name = "Control2275")
  val checklistStep11Missed: Boolean?,
  @Json(name = "Control2276")
  val checklistStep12Missed: Boolean?,
  @Json(name = "Control2277")
  val checklistStep13Missed: Boolean?,
  @Json(name = "Control2278")
  val checklistStep14Missed: Boolean?,
  @Json(name = "Control2279")
  val checklistStep15Missed: Boolean?,
  @Json(name = "Control2280")
  val checklistStep16Missed: Boolean?,
  @Json(name = "Control2281")
  val checklistStep17Missed: Boolean?,
) {
  companion object {
    val controlIdToNameMap: Map<String, String> = ArrayMap<String, String>().apply {
      put("Control2029", "Record last updated")
      put("Control2159", "District")
      put("Control2018", "Healthcare facility")
      put("Control2048", "Date of training")
      put("Control2020", "Number of functioning blood pressure devices")

      put("Control2194", "Total number of staff working at this facility")
      put("Control2195", "Total number of staff providing maternity services at this facility")
      put("Control2196", "Total number of staff trained today")
      put("Control2197", "Number of doctors trained today")
      put("Control2198", "Number of midwives trained today")
      put("Control2199", "Number of SACHOS trained today")
      put("Control2200", "Number of SECHN midwives trained today")
      put("Control2201", "Number of SRNs trained today")
      put("Control2202", "Number of CHOs trained today")
      put("Control2203", "Number of CHAs trained today")
      put("Control2204", "Number of CSECHNs trained today")
      put("Control2205", "Number of MCH Aides trained today")
      put("Control2206", "Number of TBA trained today")
      put("Control2207", "How many of the staff trained today had ever been trained in CRADLE before?")
      put("Control2208", "How many of the staff trained today scored more than 14/17 on the CRADLE checklist?")
    }
  }
}
