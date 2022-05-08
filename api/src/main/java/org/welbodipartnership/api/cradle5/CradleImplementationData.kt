package org.welbodipartnership.api.cradle5

import androidx.collection.ArrayMap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.forms.PostOperationId

@JsonClass(generateAdapter = true)
@FormId(70)
@PostOperationId(191)

data class CradleImplementationData(
  @Json(name = "Control2029")
  val recordLastUpdated: String?,
  @Json(name = "Control2159")
  val district: Int?,
  @Json(name = "Control2018")
  val healthcareFacility: Int?,
  @Json(name = "Control2048")
  val dateOfTraining: String?,
) {
  companion object {
    val controlIdToNameMap: Map<String, String> = ArrayMap<String, String>().apply {
      put("Control2029", "Record last updated")
      put("Control2159", "District")
      put("Control2018", "Healthcare facility")
    }
  }
}