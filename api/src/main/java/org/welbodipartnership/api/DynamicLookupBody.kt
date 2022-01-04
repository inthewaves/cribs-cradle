package org.welbodipartnership.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DynamicLookupBody<T>(
  @Json(name = "PageNumber")
  val pageNumber: Int,
  @Json(name = "PageSize")
  val pageSize: Int,
  @Json(name = "TotalNumberOfPages")
  val totalNumberOfPages: Int,
  @Json(name = "TotalNumberOfRecords")
  val totalNumberOfRecords: Int,
  @Json(name = "NextPageUrl")
  val nextPageUrl: String?,
  @Json(name = "Results")
  val results: List<T>
)
