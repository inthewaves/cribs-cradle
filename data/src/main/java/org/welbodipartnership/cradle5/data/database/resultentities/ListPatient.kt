package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.room.Embedded
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.datetime.FormDate

/**
 * A shortened view of the patient
 */
data class ListPatient(
  val id: Long,
  @Embedded
  val serverInfo: ServerInfo?,
  val initials: String,
  val dateOfBirth: FormDate,
  val localNotes: String?,
  val isDraft: Boolean,
)
