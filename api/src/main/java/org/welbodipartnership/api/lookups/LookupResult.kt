package org.welbodipartnership.api.lookups

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The result of a /v0/lookups/{id} call
 */
@JsonClass(generateAdapter = true)
data class LookupResult(
  @Json(name = "Values")
  val values: List<Value>,
  @Json(name = "Id")
  val id: Int,
  @Json(name = "Name")
  val name: String,
  @Json(name = "Url")
  val url: String,
) {
  @JsonClass(generateAdapter = true)
  data class Value(
    @Json(name = "Id")
    val id: Int,
    @Json(name = "Code")
    val code: String,
    @Json(name = "Name")
    val name: String,
    @Json(name = "ListOrder")
    val listOrder: Int,
  )
}
