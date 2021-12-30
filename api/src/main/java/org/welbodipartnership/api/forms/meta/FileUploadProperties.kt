package org.welbodipartnership.api.forms.meta

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FileUploadProperties(
  /** A link to the API endpoint to be used for file upload */
  @Json(name = "Url")
  val url: String,
  /** Indicates if files can be overwritten */
  @Json(name = "AllowsOverwrite")
  val allowsOverwrite: Boolean,
  /** Indicates if a list of available files to choose from is populated */
  @Json(name = "CanChooseExistingFile")
  val canChooseExistingFile: Boolean
)
