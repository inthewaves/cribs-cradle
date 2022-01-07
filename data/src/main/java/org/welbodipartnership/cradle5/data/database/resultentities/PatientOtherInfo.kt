package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.compose.runtime.Immutable

@Immutable
data class PatientOtherInfo(
  val localNotes: String?,
  val isDraft: Boolean
)
