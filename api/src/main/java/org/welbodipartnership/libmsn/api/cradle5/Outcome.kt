package org.welbodipartnership.libmsn.api.cradle5

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.cradle5.util.date.FormDate
import org.welbodipartnership.libmsn.api.forms.FormId


@JsonClass(generateAdapter = true)
@FormId(70)
data class Outcome(
  @Json(name = "Control1378")
  val hadFirstEclampsiaFit: Boolean = false,
  @Json(name = "Control1541")
  val eclampsiaFitDate: FormDate? = null,
  @Json(name = "Control1551")
  val eclampsiaFitLocation: Int? = null,

  @Json(name = "Control1545")
  val hasHysterectomy: Boolean = false,
  @Json(name = "Control1546")
  val hysterectomyDate: FormDate? = null,
  @Json(name = "Control1552")
  val hysterectomyCause: Int? = null,
  @Json(name = "Control1553")
  val hysterectomyOtherCause: String? = null,
  @Json(name = "Control1728")
  val hysterectomyAdditionalInfo: String? = null,

  @Json(name = "Control1720")
  val isAdmittedToHduOrItu: Boolean = false,
  @Json(name = "Control1555")
  val hduOrItuAdmissionDate: FormDate? = null,
  @Json(name = "Control1554")
  val hduOrItuAdmissionCause: Int? = null,
  @Json(name = "Control1556")
  val hduOrItuAdmissionOtherCause: String? = null,
  @Json(name = "Control1918")
  val hduOrItuStayDays: Int? = null,

  @Json(name = "Control1921")
  val hasMaternalDeath: Boolean = false,
  @Json(name = "Control1386")
  val maternalDeathDate: FormDate? = null,
  @Json(name = "Control1557")
  val maternalDeathUnderlyingCause: Int? = null,
  @Json(name = "Control1558")
  val maternalDeathOtherCause: String? = null,
  @Json(name = "Control1559")
  val maternalDeathPlace: Int? = null,

  /**
   * Surgical management of postpartum haemorrhage requiring anaesthesia
   */
  @Json(name = "Control1925")
  val hadSurgicalMgmtOfPostpartumHaemorrhageAndAnaesthesia: Boolean = false,
  @Json(name = "Control1924")
  val surgicalManagementDate: FormDate? = null,
  @Json(name = "Control1926")
  val surgicalManagementType: Int? = null,
  @Json(name = "Control1927")
  val surgicalManagementOtherType: String? = null,
  @Json(name = "Control1931")
  val hadPerinatalDeath: Boolean = false,
  @Json(name = "Control1930")
  val perinatalDeathDate: FormDate? = null,
  @Json(name = "Control1932")
  val perinatalOutcome: Int? = null,
  @Json(name = "Control1933")
  val perinatalMaternalFactors: Int? = null,
  @Json(name = "Control1934")
  val perinatalOtherMaternalFactors: Int? = null,
)
