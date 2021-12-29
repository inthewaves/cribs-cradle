package org.welbodipartnership.cradle5.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.welbodipartnership.cradle5.data.entities.Patient

const val DATABASE_VERSION = 1

@Database(
  version = DATABASE_VERSION,
  entities = [
    Patient::class
  ]
)
@TypeConverters(DbTypeConverters::class)
abstract class Cradle5Database : RoomDatabase()
