package org.welbodipartnership.cradle5.data.database.resultentities

import org.welbodipartnership.cradle5.util.datetime.FormDate

/**
 * A shortened view of the patient
 */
data class ListPatient(
  val id: Long,
  val initials: String,
  @get:JvmName("getDateOfBirth")
  val dateOfBirth: FormDate,
)
