package org.welbodipartnership.cradle5.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.room.withTransaction
import net.sqlcipher.database.SupportFactory
import org.welbodipartnership.cradle5.data.database.daos.FacilityDao
import org.welbodipartnership.cradle5.data.database.daos.LocationCheckInDao
import org.welbodipartnership.cradle5.data.database.daos.OutcomesDao
import org.welbodipartnership.cradle5.data.database.daos.PatientDao
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import javax.inject.Inject
import javax.inject.Singleton

const val DATABASE_VERSION = 3
const val DATABASE_NAME = "cradle5.db"

@Singleton
class CradleDatabaseWrapper @Inject constructor() {
  var database: Cradle5Database? = null
    private set

  fun patientsDao(): PatientDao = requireNotNull(database).patientDao()
  fun outcomesDao(): OutcomesDao = requireNotNull(database).outcomesDao()
  fun facilitiesDao(): FacilityDao = requireNotNull(database).facilitiesDao()
  fun locationCheckInDao(): LocationCheckInDao = requireNotNull(database).locationCheckInDao()

  suspend fun <T> withTransaction(block: suspend (db: Cradle5Database) -> T): T {
    return database!!.withTransaction {
      block(database!!)
    }
  }

  internal fun setup(context: Context) {
    if (database != null) {
      return
    }
    synchronized(CradleDatabaseWrapper::class.java) {
      if (database != null) {
        return
      }
      database = Room.databaseBuilder(context, Cradle5Database::class.java, DATABASE_NAME)
        .build()
    }
  }

  internal fun setup(context: Context, supportFactory: SupportFactory) {
    if (database != null) {
      return
    }
    synchronized(CradleDatabaseWrapper::class.java) {
      if (database != null) {
        return
      }
      database = Room.databaseBuilder(context, Cradle5Database::class.java, DATABASE_NAME)
        .openHelperFactory(supportFactory)
        .addMigrations(*MIGRATIONS)
        .build()
    }
  }
}

private val MIGRATIONS = arrayOf(
  Migration(2, 3) { db ->
    db.execSQL("DROP TABLE GpsLocation")
    db.execSQL(
      """
        CREATE TABLE IF NOT EXISTS `LocationCheckIn` (
          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `isUploaded` INTEGER NOT NULL, 
          `timestamp` INTEGER NOT NULL, `providerName` TEXT NOT NULL, `accuracy` REAL, 
          `latitude` REAL NOT NULL, `longitude` REAL NOT NULL
        )
      """.trimIndent()
    )
  }
)

@Database(
  version = DATABASE_VERSION,
  exportSchema = true,
  entities = [
    Patient::class,
    Outcomes::class,
    Facility::class,
    LocationCheckIn::class,
  ],
  autoMigrations = [
    AutoMigration(from = 1, to = 2),
  ]
)
@TypeConverters(DbTypeConverters::class)
abstract class Cradle5Database : RoomDatabase() {
  abstract fun patientDao(): PatientDao
  abstract fun outcomesDao(): OutcomesDao
  abstract fun facilitiesDao(): FacilityDao
  abstract fun locationCheckInDao(): LocationCheckInDao
}
