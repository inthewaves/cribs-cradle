package org.welbodipartnership.api.cradle5

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.forms.PostOperationId

@JsonClass(generateAdapter = true)
@FormId(122)
@PostOperationId(239)
data class GpsForm(
  @Json(name = "Control2095")
  val userId: String,
  @Json(name = "Control2096")
  val dateTimeIso8601: String,
  @Json(name = "Control2097")
  val coordinates: String,
)
