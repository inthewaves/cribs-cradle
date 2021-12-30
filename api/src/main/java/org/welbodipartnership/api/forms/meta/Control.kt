package org.welbodipartnership.api.forms.meta

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.welbodipartnership.api.forms.Form

/**
 * Information about one of the variables in a [Form].
 */
@JsonClass(generateAdapter = true)
data class Control(
  /** The variable ID (e.g. "Control1234") */
  @Json(name = "Id")
  val id: String,
  /** The variable label or name */
  @Json(name = "Name")
  val name: String?,
  @Json(name = "DataType")
  val dataType: DataType,
  @Json(name = "ControlType")
  val controlType: ControlType,
  /**
   * a list of possible values when [controlType] is [ControlType.SingleValueFromList],
   * [ControlType.MultiValueFromList] or [ControlType.FileUpload] (when
   * [FileUploadProperties.canChooseExistingFile] is `true`).
   */
  @Json(name = "ValueList")
  val valueList: List<Value>?,
  /** Information how to retrieve data from dynamic lookups */
  @Json(name = "DynamicLookupProperties")
  val dynamicLookupProperties: DynamicLookupProperty?,
  /**
   * Indicates that a reason for a change is required when variable’s value changes. The change
   * should be added as **ControlXXXX_Comment** in the variables list in Data list when sending
   * data.
   */
  @Json(name = "RequiresCommentWhenValueChanges")
  val requiresCommentWhenValueChanges: Boolean?,
  /** Information how to upload files for this control */
  val fileUploadProperties: FileUploadProperties?
) {
  @JsonClass(generateAdapter = true)
  data class Value(
    @Json(name = "Id")
    val id: String,
    @Json(name = "Text")
    val text: String?
  )
}
