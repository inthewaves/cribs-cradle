package org.welbodipartnership.api.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.FormId

@FormId(16)
@JsonClass(generateAdapter = true)
data class UserAccount(
  @Json(name = "Control1271")
  val loginName: String,
  /**
   * The district id for the user. This is a dynamic lookup value
   */
  @Json(name = "Control1330")
  val districtId: Int?,
  /**
   * The user's role. This is a dynamic lookup value, using "Control1330" as a master control.
   */
  @Json(name = "Control1331")
  val userRole: Int?,
  /**
   * For Cradle5,
   * "ValueList": [
   *   {
   *     "Id": "1",
   *     "Text": "W. Europe Standard Time-(GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna"
   *   },
   *   {
   *     "Id": "2",
   *     "Text": "GMT Standard Time-(GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London"
   *   }
   * ],
   */
  @Json(name = "Control1291")
  val timeZone: Int?,
  @Json(name = "Control1272")
  val accountDisabled: Boolean,
  @Json(name = "Control1293")
  val passwordNeverExpires: Boolean,
  @Json(name = "Control1294")
  val userMustChangePasswordAtNextLogin: Boolean,
  @Json(name = "Control1295")
  val userCannotChangePassword: Boolean,
  @Json(name = "Control1281")
  val title: String?,
  @Json(name = "Control1274")
  val firstName: String?,
  @Json(name = "Control1275")
  val lastName: String?,
  @Json(name = "Control1273")
  val email: String?,
  @Json(name = "Control1284")
  val centre: String?,
  @Json(name = "Control1285")
  val streetAddress: String?,
  @Json(name = "Control1289")
  val city: String?,
  @Json(name = "Control1277")
  val zipCode: String?,
  @Json(name = "Control1276")
  val stateOrProvince: String?,
  @Json(name = "Control1286")
  val country: String?,
  @Json(name = "Control1278")
  val phone1: String?,
  @Json(name = "Control1279")
  val phone2: String?,
  @Json(name = "Control1280")
  val fax: String?,
  @Json(name = "Control1298")
  val loginCount: String?,
  @Json(name = "Control1299")
  val lastLoginDate: String?,
  // Control1300 is just some embedded HTML?
)
