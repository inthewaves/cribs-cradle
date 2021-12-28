package org.welbodipartnership.libmsn.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MyClass(
  val something: Int = 5
)