package org.welbodipartnership.libmsn.api.forms.meta

import com.squareup.moshi.Json

enum class DataType {
  @Json(name = "string")
  String,
  Int,
  Decimal,
  Date,
  Bool
}