package org.welbodipartnership.api.cradle5

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Healthcare facility as a list item in a dynamic lookup. Can query Control2092, FormId 63,
 * objectId = 0 for these items.
 */
@JsonClass(generateAdapter = true)
data class HealthcareFacilityLookupEntry(
  @Json(name = "Id")
  val id: Long,
  @Json(name = "Name")
  val name: String
)
