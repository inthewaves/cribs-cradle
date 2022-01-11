package org.welbodipartnership.api.cradle5

import androidx.collection.ArrayMap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.forms.PostOperationId
import org.welbodipartnership.cradle5.util.datetime.FormDate

@JsonClass(generateAdapter = true)
@FormId(70)
@PostOperationId(191)
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
  @Json(name = "Control1728")
  val hduOrItuAdditionalInfo: String? = null,

  @Json(name = "Control1921")
  val hasMaternalDeath: Boolean = false,
  @Json(name = "Control1386")
  val maternalDeathDate: FormDate? = null,
  @Json(name = "Control1557")
  val maternalDeathUnderlyingCause: Int?,
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
  val perinatalOtherMaternalFactors: String? = null,
) {
  companion object {
    val controlIdToNameMap: Map<String, String> = ArrayMap<String, String>().apply {
      put("Control1378", "Eclampsia fit present")
      put("Control1541", "Eclampsia fit date")
      put("Control1551", "Eclampsia fit location")

      put("Control1545", "Hysterectomy present")
      put("Control1546", "Hysterectomy date")
      put("Control1552", "Hysterectomy cause")
      put("Control1553", "Hysterectomy other cause")

      put("Control1720", "HDU/ITU admission present")
      put("Control1555", "HDU/ITU admission date")
      put("Control1554", "HDU/ITU admission cause")
      put("Control1556", "HDU/ITU admission other cause")
      put("Control1918", "HDU/ITU admission stay in days")
      put("Control1728", "HDU/ITU admission additional info")

      put("Control1921", "Maternal death present")
      put("Control1386", "Maternal death date")
      put("Control1557", "Maternal death underlying cause")
      put("Control1558", "Maternal death other cause")
      put("Control1559", "Maternal death place")

      put("Control1925", "Surgical management present")
      put("Control1924", "Surgical management date")
      put("Control1926", "Surgical management type")
      put("Control1927", "Surgical management other cause")

      put("Control1931", "Perinatal death present")
      put("Control1930", "Perinatal death date")
      put("Control1932", "Perinatal death outcome")
      put("Control1933", "Perinatal death maternal factors")
      put("Control1934", "Perinatal death other maternal factors")
    }
  }
}
