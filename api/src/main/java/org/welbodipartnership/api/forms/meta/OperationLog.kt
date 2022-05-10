package org.welbodipartnership.api.forms.meta

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.CustomIsoUtils
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Additional information about who and when inserted/updated/signed a form’s data record.
 */
@JsonClass(generateAdapter = true)
data class OperationLog(
  @Json(name = "Inserted")
  val inserted: Entry?,
  @Json(name = "Updated")
  val updated: Entry?,
  @Json(name = "Signed")
  val signed: Entry?,
) {
  @JsonClass(generateAdapter = true)
  data class Entry(
    /** The user's ID that made the record */
    @Json(name = "UserId")
    val userId: Long?,
    /** Full user's name with username */
    @Json(name = "User")
    val user: String?,
    /** The date with time when the record was made. Parse it with  */
    @Json(name = "Date")
    val date: String?,
  ) {
    val parsedDate: ZonedDateTime? get() = date
      ?.let { ZonedDateTime.ofInstant(CustomIsoUtils.parse(it).toInstant(), ZoneId.of("UTC")) }
  }
}
