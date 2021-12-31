package org.welbodipartnership.cradle5.data.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.resultentities.ListPatient

@Dao
abstract class PatientDao {
  @Insert
  abstract suspend fun insert(patient: Patient): Long

  @Transaction
  @Query("SELECT id, initials, dateOfBirth FROM Patient ORDER BY id DESC")
  abstract fun patientsPagingSource(): PagingSource<Int, ListPatient>
}
