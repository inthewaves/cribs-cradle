package org.welbodipartnership.cradle5.data.database.resultentities

import androidx.room.DatabaseView
import androidx.room.Embedded
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.datetime.FormDate
import java.time.ZonedDateTime

/**
 * A shortened view of the CRADLE form
 *
 * @see CradleTrainingForm
 */
@DatabaseView(
  value = """
    SELECT
      form.id,
      
      form.nodeId,
      form.objectId,
      form.updateTime,
      form.createdTime,
      
      d.id AS district_id,
      d.name AS district_name,
      d.isOther AS district_isOther,
      
      fac.id AS facility_id,
      fac.name AS facility_name,

      form.serverErrorMessage,
      form.dateOfTraining,
      form.recordLastUpdated,
      form.localNotes,
      form.isDraft
    FROM
      CradleTrainingForm AS form
      LEFT JOIN District AS d on form.district = d.id
      LEFT JOIN Facility AS fac on form.healthcareFacility = fac.id
  """
)
data class ListCradleTrainingForm(
  val id: Long = -1,
  @Embedded
  val serverInfo: ServerInfo?,
  @Embedded(prefix = "district_")
  val district: District?,
  @Embedded(prefix = "facility_")
  val healthcareFacility: FacilityIdAndName?,
  val serverErrorMessage: String?,
  val dateOfTraining: FormDate?,
  val recordLastUpdated: String?,
  val localNotes: String?,
  val isDraft: Boolean,
) {
  val parsedLastUpdated: ZonedDateTime?
    get() = recordLastUpdated?.let { CradleTrainingForm.parseRecordLastUpdatedString(it) }
}
