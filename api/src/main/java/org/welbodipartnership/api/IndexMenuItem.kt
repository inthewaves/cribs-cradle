package org.welbodipartnership.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response list items from the / (index endpoint)
 */
@JsonClass(generateAdapter = true)
data class IndexMenuItem(
  @Json(name = "Title")
  val title: String,
  @Json(name = "Url")
  val url: String
)
