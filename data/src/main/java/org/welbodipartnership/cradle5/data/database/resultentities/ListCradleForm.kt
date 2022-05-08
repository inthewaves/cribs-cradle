package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.datetime.FormDate

/**
 * A shortened view of the cradle form
 *
 * @see CradleTrainingForm
 */
data class ListCradleTrainingForm(
  val id: Long,
  @Embedded
  val serverInfo: ServerInfo?,
  @Relation(parentColumn = "district", entityColumn = "id")
  val district: District,
  @Relation(parentColumn = "healthcareFacility", entityColumn = "id")
  val healthcareFacility: Facility?,
  val serverErrorMessage: String?,
  val dateOfTraining: FormDate,
  val localNotes: String?,
  val isDraft: Boolean,
)
