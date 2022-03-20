package org.welbodipartnership.api.cradle5

import androidx.collection.ArrayMap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.forms.PostOperationId
import org.welbodipartnership.cradle5.util.datetime.FormDate

@JsonClass(generateAdapter = true)
@FormId(63)
@PostOperationId(173)
data class Registration(
  @Json(name = "Control1336")
  val initials: String,
  @Json(name = "Control1646")
  val presentationDate: FormDate?,
  @Json(name = "Control1648")
  val age: Int?,
  @Json(name = "Control2123")
  val ageUnknown: Boolean,
  @Json(name = "Control2121")
  val address: String?,
  /** This is a dynamic lookup value */
  @Json(name = "Control2092")
  val healthcareFacility: Int?,
  @Json(name = "Control2124")
  val wasPatientReferral: Boolean,
  /** This is a dynamic lookup value */
  @Json(name = "Control2125")
  val patientReferralFromDistrict: Int?,
  /** This is a dynamic lookup value */
  @Json(name = "Control2126")
  val patientReferralFromFacility: Int?,
  @Json(name = "Control2127")
  val patientReferralToDistrict: Int?,
  /** This is a dynamic lookup value */
  @Json(name = "Control2128")
  val patientReferralToFacility: Int?,
) {
  companion object {
    val controlIdToNameMap: Map<String, String> = ArrayMap<String, String>(11).apply {
      put("Control1336", "Initials")
      put("Control1646", "Presentation date")
      put("Control1648", "Age")
      put("Control2123", "Age unknown")
      put("Control2121", "Patient address")
      put("Control2092", "Healthcare facility")

      put("Control2124", "Was this patient a referral?")
      put("Control2125", "Which district was the patient referred from?")
      put("Control2126", "Which facility was the patient referred from?")
      put("Control2127", "Which district was the patient referred to?")
      put("Control2128", "Which facility was the patient referred to?")
    }
  }
}
