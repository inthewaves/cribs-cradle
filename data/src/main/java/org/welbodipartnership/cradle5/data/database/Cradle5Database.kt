package org.welbodipartnership.cradle5.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import net.sqlcipher.database.SupportFactory
import org.welbodipartnership.cradle5.data.database.daos.FacilityDao
import org.welbodipartnership.cradle5.data.database.daos.OutcomesDao
import org.welbodipartnership.cradle5.data.database.daos.PatientDao
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.GpsLocation
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import javax.inject.Inject
import javax.inject.Singleton

const val DATABASE_VERSION = 1
const val DATABASE_NAME = "cradle5.db"

@Singleton
class CradleDatabaseWrapper @Inject constructor() {
  var database: Cradle5Database? = null
    private set

  public suspend fun <T> withTransaction(block: suspend (db: Cradle5Database) -> T): T {
    return database!!.withTransaction {
      block(database!!)
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
        .build()
    }
  }
}

@Database(
  version = DATABASE_VERSION,
  exportSchema = true,
  entities = [
    Patient::class,
    Outcomes::class,
    Facility::class,
    GpsLocation::class,
  ]
)
@TypeConverters(DbTypeConverters::class)
abstract class Cradle5Database : RoomDatabase() {
  abstract fun patientDao(): PatientDao
  abstract fun outcomesDao(): OutcomesDao
  abstract fun facilitiesDao(): FacilityDao
}
