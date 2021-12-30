package org.welbodipartnership.api.forms.meta

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Information how to retrieve data from dynamic lookups.
 *
 * The dynamic lookups are such lookups that have too many values to be listed (e.g. thousands) or
 * when two or more lookups are connected with each other (choosing a value from the first lookup
 * changes available values in the second lookup).
 */
@JsonClass(generateAdapter = true)
data class DynamicLookupProperty(
  /** A link to the API endpoint to be used for value retrieval */
  @Json(name = "Url")
  val url: String,
  /** A list of **Control IDs** if there are multiple dependent dynamic lookups */
  @Json(name = "MasterControls")
  val masterControls: List<String>
)
