package org.welbodipartnership.cradle5.data.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.resultentities.ListPatient
import org.welbodipartnership.cradle5.data.database.resultentities.PatientAndOutcomes

@Dao
abstract class PatientDao {
  @Insert
  abstract suspend fun insert(patient: Patient): Long

  @Transaction
  @Query("SELECT id, initials, dateOfBirth FROM Patient ORDER BY id ASC")
  abstract fun patientsPagingSource(): PagingSource<Int, ListPatient>

  @Transaction
  @Query("SELECT * FROM Patient WHERE id = :patientPk")
  abstract suspend fun getPatientAndOutcomes(patientPk: Long): PatientAndOutcomes
}
