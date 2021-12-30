package org.welbodipartnership.cradle5.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import org.welbodipartnership.cradle5.data.database.entities.Patient

@Dao
abstract class PatientDao {
  @Insert
  abstract suspend fun insert(patient: Patient): Long
}
