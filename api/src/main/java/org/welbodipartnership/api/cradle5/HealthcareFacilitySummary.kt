package org.welbodipartnership.api.cradle5

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.FormId

@FormId(113)
@JsonClass(generateAdapter = true)
data class HealthcareFacilitySummary(
  @Json(name = "Control1952")
  val districtName: String
)
