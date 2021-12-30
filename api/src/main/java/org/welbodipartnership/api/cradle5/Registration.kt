package org.welbodipartnership.api.cradle5

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.Verifiable
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.cradle5.util.date.FormDate

@JsonClass(generateAdapter = true)
@FormId(63)
data class Registration(
  @Json(name = "Control1336")
  val initials: String,
  @Json(name = "Control1646")
  val presentationDate: FormDate,
  @Json(name = "Control1337")
  val birthdateDate: FormDate?,
  @Json(name = "Control1648")
  val age: Int,
) : Verifiable {
  override fun verify() {
    TODO("Not yet implemented")
  }
}
