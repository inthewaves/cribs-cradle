package org.welbodipartnership.cradle5.data.database

import android.content.Context
import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.welbodipartnership.cradle5.data.database.daos.DistrictDao
import org.welbodipartnership.cradle5.data.database.daos.FacilityDao
import org.welbodipartnership.cradle5.data.database.daos.LocationCheckInDao
import org.welbodipartnership.cradle5.data.database.daos.OutcomesDao
import org.welbodipartnership.cradle5.data.database.daos.CradleTrainingFormDao
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

const val TAG = "Cradle5Database"

const val DATABASE_VERSION = 1
const val DATABASE_NAME = "cradle5.db"

@Singleton
class CradleDatabaseWrapper @Inject constructor() {
  var database: Cradle5Database? = null
    private set

  fun cradleTrainingFormDao(): CradleTrainingFormDao = requireNotNull(database).cradleTrainingFormDao()
  fun facilitiesDao(): FacilityDao = requireNotNull(database).facilitiesDao()
  fun locationCheckInDao(): LocationCheckInDao = requireNotNull(database).locationCheckInDao()
  fun districtDao(): DistrictDao = requireNotNull(database).districtDao()

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

private val MIGRATIONS = arrayOf<Migration>()

@Database(
  version = DATABASE_VERSION,
  exportSchema = true,
  entities = [
    CradleTrainingForm::class,
    Facility::class,
    LocationCheckIn::class,
    District::class,
  ],
  autoMigrations = []
)
@TypeConverters(DbTypeConverters::class)
abstract class Cradle5Database : RoomDatabase() {
  abstract fun cradleTrainingFormDao(): CradleTrainingFormDao
  abstract fun facilitiesDao(): FacilityDao
  abstract fun locationCheckInDao(): LocationCheckInDao
  abstract fun districtDao(): DistrictDao
}

/**
 * Creates [Migration] from [startVersion] to [endVersion] that runs [migrate] to perform
 * the necessary migrations.
 *
 * A migration can handle more than 1 version (e.g. if you have a faster path to choose when
 * going version 3 to 5 without going to version 4). If Room opens a database at version
 * 3 and latest version is < 5, Room will use the migration object that can migrate from
 * 3 to 5 instead of 3 to 4 and 4 to 5.
 *
 * If there are not enough migrations provided to move from the current version to the latest
 * version, Room will clear the database and recreate so even if you have no changes between 2
 * versions, you should still provide a Migration object to the builder.
 *
 * [migrate] cannot access any generated Dao in this method.
 *
 * [migrate] is already called inside a transaction and that transaction
 * might actually be a composite transaction of all necessary `Migration`s.
 */
private fun MigrationCreator(
  startVersion: Int,
  endVersion: Int,
  migrate: SupportSQLiteDatabase.() -> Unit
): Migration = object : Migration(startVersion, endVersion) {
  override fun migrate(database: SupportSQLiteDatabase) {
    migrate(database)
  }
}
