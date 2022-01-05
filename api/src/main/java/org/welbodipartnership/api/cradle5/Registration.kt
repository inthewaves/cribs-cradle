package org.welbodipartnership.api.cradle5

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.forms.PostOperationId
import org.welbodipartnership.cradle5.util.datetime.FormDate

@JsonClass(generateAdapter = true)
@FormId(63)
@PostOperationId(173)
data class Registration(
  @Json(name = "Control1336")
  val initials: String,
  @Json(name = "Control1646")
  val presentationDate: FormDate?,
  @Json(name = "Control1337")
  val birthdateDate: FormDate?,
  @Json(name = "Control1648")
  val age: Int?,
  /**
   * This is a dynamic lookup value
   */
  @Json(name = "Control2092")
  val healthcareFacility: Int?,
)
