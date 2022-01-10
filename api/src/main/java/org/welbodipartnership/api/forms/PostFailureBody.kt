package org.welbodipartnership.api.forms

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A HTTP 400 error is returned with this body when a form POST request fails some validation.
 */
@JsonClass(generateAdapter = true)
data class PostFailureBody(
  @Json(name = "Message")
  val message: String,
  @Json(name = "ModelState")
  val modelState: Map<String, List<String>>
)
