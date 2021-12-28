package org.welbodipartnership.libmsn.api.forms

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.libmsn.api.forms.meta.Meta

@JsonClass(generateAdapter = true)
data class Form<T>(
  @Json(name = "Data")
  val data: T,
  @Json(name = "Meta")
  val meta: Meta
)
