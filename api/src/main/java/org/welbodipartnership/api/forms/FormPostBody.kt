package org.welbodipartnership.api.forms

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FormPostBody<T>(
  @Json(name = "Data")
  val data: T,
  @Json(name = "OperationId")
  val operationId: Int
) {
  companion object {
    inline fun <reified T> create(data: T): FormPostBody<T> {
      val operationId = requireNotNull(T::class.java.getAnnotation(PostOperationId::class.java)) {
        "${T::class.java} is missing PostOperationId annotation"
      }.id
      return FormPostBody(data, operationId)
    }
  }
}
