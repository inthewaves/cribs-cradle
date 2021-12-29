package org.welbodipartnership.cradle5.data.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.welbodipartnership.cradle5.data.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.util.date.FormDate

@Entity
data class Patient(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo
  val id: Long = 0,
  @Embedded
  val serverInfo: ServerInfo? = null,
  @get:JvmName("getPresentationDate")
  val presentationDate: FormDate,
  @get:JvmName("getDateOfBirth")
  val dateOfBirth: FormDate,
  val isExactDateOfBirth: Boolean,
)
