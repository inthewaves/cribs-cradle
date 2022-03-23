package org.welbodipartnership.api.forms.meta

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Dynamic lookup entry list items, returned as part of the
 * /v0/lookups/dynamic/{ControlId}/{FormId}/{ObjectId} endpoint.
 */
@JsonClass(generateAdapter = true)
data class DynamicLookupListEntry(
  @Json(name = "Id")
  val id: Int,
  @Json(name = "Name")
  val name: String
)
