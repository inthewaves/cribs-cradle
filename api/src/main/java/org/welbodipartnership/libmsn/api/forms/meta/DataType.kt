package org.welbodipartnership.libmsn.api.forms.meta

import com.squareup.moshi.Json

enum class DataType {
  @Json(name = "string")
  String,
  @Json(name = "int")
  Int,
  @Json(name = "decimal")
  Decimal,
  @Json(name = "date")
  Date,
  @Json(name = "bool")
  Bool
}