package org.welbodipartnership.api.cradle5

import androidx.collection.ArrayMap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.forms.PostOperationId

@JsonClass(generateAdapter = true)
@FormId(127)
@PostOperationId(264)
data class FacilityBpData(
  @Json(name = "Control2229")
  val districtName: String?,
  @Json(name = "Control2230")
  val recordLastUpdated: String?,
  @Json(name = "Control2252")
  val districtId: Int?,
  @Json(name = "Control2219")
  val facilityId: Int?,
  @Json(name = "Control2258")
  val dateOfDataCollection: String?,
  @Json(name = "Control2212")
  val numOfBpReadings: Int?,
  @Json(name = "Control2213")
  val numOfBpReadingsEndInA0Or5: Int?,
  @Json(name = "Control2214")
  val numOfBpReadingsHavingColorAndArrow: Int?,
) {
  companion object {
    val controlIdToNameMap: Map<String, String> = ArrayMap<String, String>(8).apply {
      put("Control2229", "District")
      put("Control2230", "Record last updated")
      put("Control2252", "District id")
      put("Control2219", "Facility id")
      put("Control2258", "Date of data collection")
      put("Control2212", "Number of blood pressure readings")
      put("Control2213", "Number of blood pressure readings ending in 0 or 5")
      put("Control2214", "Number of blood pressure readings having color and arrow")
    }
  }
}
