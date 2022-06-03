package org.welbodipartnership.api.forms.meta

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Provides additional basic information about the form, its variables and available operations.
 * This is part of the response from the `/{VERSION}/FORMS/{FORMID}/{OBJECTID}` v0 endpoint.
 *
 * To allow multiple row entry in the same form, CTF uses a special control called MRE
 * (MultiRecordEdit). This control has child controls assigned to it and in CTF applications is
 * displayed as a table. For such controls, the control IDs are constructed by combining:
 *
 * - MRE control ID
 * - "_" symbol
 * - MRE row number (starting from 0)
 * - "_" symbol
 * - Child control ID
 *
 * For example, **Control1234_0_4567**.
 */
@JsonClass(generateAdapter = true)
data class Meta(
  @Json(name = "Title")
  val title: String,
  @Json(name = "FormId")
  val formId: Long,
  @Json(name = "ObjectId")
  val objectId: Long,
  /** Additional information with links to API endpoints to view historical form’s data. */
  @Json(name = "HistoryNavigation")
  val historyNavigation: HistoryNavigation?,
  /** Additional information about who and when inserted/updated/signed form’s data record */
  @Json(name = "OperationLog")
  val operationLog: OperationLog,
  /** information about the actions that can be done with the form (e.g. "Save draft" or "Save") */
  @Json(name = "Operations")
  val operations: List<Operation>,
  /** Information about form’s variables */
  @Json(name = "Controls")
  val controls: List<Control>,
  /** A link to the API endpoint to be used to retrieve a patient's form in hierarchical view */
  @Json(name = "TreeUrl")
  val treeUrl: String?
) {
  /**
   * Variant of [Meta] with only minimal info
   */
  @JsonClass(generateAdapter = true)
  data class MinimalInfo(
    @Json(name = "Title")
    val title: String,
    @Json(name = "FormId")
    val formId: Long,
    @Json(name = "ObjectId")
    val objectId: Long,
    /** Additional information with links to API endpoints to view historical form’s data. */
    @Json(name = "OperationLog")
    val operationLog: OperationLog,
  )
}
