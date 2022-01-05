package org.welbodipartnership.api.lookups

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Each of these is an entry inside of the result returned by the /v0/lookups endpoint that lists
 * all the possible lookups for the server. Each of those IDs correspond to a /v0/lookups/{id} call
 * that has [LookupResult] as the body.
 */
@JsonClass(generateAdapter = true)
data class LookupsEnumerationEntry(
  @Json(name = "Id")
  val id: Int,
  @Json(name = "Name")
  val name: String,
  @Json(name = "Url")
  val url: String,
)
