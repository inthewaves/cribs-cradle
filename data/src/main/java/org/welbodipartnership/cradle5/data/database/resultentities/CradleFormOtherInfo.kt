package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.compose.runtime.Immutable

@Immutable
data class CradleFormOtherInfo(
  val localNotes: String?,
  val isDraft: Boolean
)
