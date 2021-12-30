package org.welbodipartnership.api.forms.meta

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Additional information with links to API endpoints to view historical form’s data
 */
@JsonClass(generateAdapter = true)
data class HistoryNavigation(
  /** A link to the API endpoint when the form was saved */
  @Json(name = "Url")
  val url: String?,
  /** The type of the history record */
  @Json(name = "Text")
  val text: String?
)
