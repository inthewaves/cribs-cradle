package org.welbodipartnership.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@JsonClass(generateAdapter = true)
data class ApiAuthToken(
  @Json(name = "access_token")
  val accessToken: String,
  @Json(name = "token_type")
  val tokenType: String,
  @Json(name = "expires_in")
  val expiresIn: Long,
  @Json(name = "userName")
  val username: String,
  @Json(name = ".issued")
  val issued: String,
  @Json(name = ".expires")
  val expires: String,
) {
  companion object {
    /**
     * HTTP format time using RFC 2616 for the [issued] and [expires] fields
     */
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter
      .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
      .withZone(ZoneId.of("GMT"))
  }
}

@JsonClass(generateAdapter = true)
data class LoginErrorMessage(
  val error: String,
  @Json(name = "error_description")
  val errorDescription: String,
)
