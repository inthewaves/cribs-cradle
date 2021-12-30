package org.welbodipartnership.api.forms.meta

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Operation(
  /** Operation's ID (will be needed for data submission) */
  @Json(name = "ID")
  val id: String?,
  /** Operation's title (e.g. "Save draft") */
  @Json(name = "Title")
  val title: String,
  /**
   * The link when operation does not perform POST but rather is used to open another endpoint/URL
   */
  @Json(name = "Url")
  val url: String?,
)
