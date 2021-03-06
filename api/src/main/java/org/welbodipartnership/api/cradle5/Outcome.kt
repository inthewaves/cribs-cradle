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
  @Json(name = "Control1931")
  val hadPerinatalDeath: Boolean = false,
  @Json(name = "Control1930")
  val perinatalDeathDate: FormDate? = null,
  @Json(name = "Control1932")
  val perinatalOutcome: Int? = null,
  @Json(name = "Control2130")
  val perinatalCauseOfStillBirth: Int? = null,
  @Json(name = "Control2132")
  val perinatalNeonatalDeathRespDistressSyndrome: Boolean? = null,
  @Json(name = "Control2133")
  val perinatalNeonatalDeathBirthAsphyxia: Boolean? = null,
  @Json(name = "Control2134")
  val perinatalNeonatalDeathSepsis: Boolean? = null,
  @Json(name = "Control2135")
  val perinatalNeonatalDeathPneumonia: Boolean? = null,
  @Json(name = "Control2136")
  val perinatalNeonatalDeathMeningitis: Boolean? = null,
  @Json(name = "Control2137")
  val perinatalNeonatalDeathMalaria: Boolean? = null,
  @Json(name = "Control2138")
  val perinatalNeonatalDeathMajorCongenitalMalformation: Boolean? = null,
  @Json(name = "Control2139")
  val perinatalNeonatalDeathPrematurity: Boolean? = null,
  @Json(name = "Control2140")
  val perinatalNeonatalDeathCauseNotEstablished: Boolean? = null,
  @Json(name = "Control2141")
  val perinatalNeonatalDeathCauseOther: Boolean? = null,
  @Json(name = "Control2147")
  val perinatalNeonatalDeathCauseNotReported: Boolean?,
  /**
   * Additional information on reason for perinatal death
   */
  @Json(name = "Control2104")
  val perinatalAdditionalInfo: String?,

  @Json(name = "Control1378")
  val hadFirstEclampsiaFit: Boolean = false,
  @Json(name = "Control2129")
  val eclampsiaDidTheWomanFit: Boolean? = false,
  @Json(name = "Control2157")
  val eclampsiaWhenWasFirstFit: Int? = null,
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
  @Json(name = "Control2158")
  val maternalDeathMdsrFindingsSummary: String? = null,

  @Json(name = "Control2106")
  val birthWeight: Int?,
  @Json(name = "Control2149")
  val birthWeightNotReported: Boolean,

  @Json(name = "Control2107")
  val ageAtDelivery: Int?,
  @Json(name = "Control2151")
  val ageAtDeliveryNotReported: Boolean,
) {
  companion object {
    val controlIdToNameMap: Map<String, String> = ArrayMap<String, String>().apply {
      put("Control1931", "Perinatal death present")
      put("Control1930", "Perinatal death date")
      put("Control1932", "Perinatal death outcome")
      put("Control2130", "Cause of Stillbirth (macerated / fresh)")
      // too much of the others
      put("Control2104", "Perinatal death additional info on reason")

      put("Control1378", "Eclampsia fit present")
      put("Control1541", "Eclampsia fit date")
      put("Control2129", "Eclampsia fit: did the woman fit")
      put("Control2157", "Eclampsia fit place")
      put("Control1551", "Eclampsia fit location")

      put("Control1545", "Hysterectomy present")
      put("Control1546", "Hysterectomy date")
      put("Control1552", "Hysterectomy cause")
      put("Control1553", "Hysterectomy other cause")

      put("Control1921", "Maternal death present")
      put("Control1386", "Maternal death date")
      put("Control1557", "Maternal death underlying cause")
      put("Control1558", "Maternal death other cause")
      put("Control1559", "Maternal death place")
      put("Control2158", "Maternal death MDSR findings summary")

      put("Control2106", "Birthweight")
      put("Control2149", "Birthweight not reported")
      put("Control2107", "Age at delivery")
      put("Control2151", "Age at delivery not reported")
    }
  }
}
