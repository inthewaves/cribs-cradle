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
import org.welbodipartnership.cradle5.data.database.daos.PatientDao
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

const val TAG = "Cradle5Database"

const val DATABASE_VERSION = 12
const val DATABASE_NAME = "cradle5.db"

@Singleton
class CradleDatabaseWrapper @Inject constructor() {
  var database: Cradle5Database? = null
    private set

  fun patientsDao(): PatientDao = requireNotNull(database).patientDao()
  fun outcomesDao(): OutcomesDao = requireNotNull(database).outcomesDao()
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

private val MIGRATIONS = arrayOf(
  MigrationCreator(2, 3) {
    execSQL("DROP TABLE GpsLocation")
    execSQL(
      """
        CREATE TABLE IF NOT EXISTS `LocationCheckIn` (
          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `isUploaded` INTEGER NOT NULL, 
          `timestamp` INTEGER NOT NULL, `providerName` TEXT NOT NULL, `accuracy` REAL, 
          `latitude` REAL NOT NULL, `longitude` REAL NOT NULL
        )
      """.trimIndent()
    )
  },
)

@Database(
  version = DATABASE_VERSION,
  exportSchema = true,
  entities = [
    Patient::class,
    Outcomes::class,
    Facility::class,
    LocationCheckIn::class,
    District::class,
  ],
  autoMigrations = [
    AutoMigration(from = 1, to = 2),
    AutoMigration(from = 3, to = 4),
    AutoMigration(from = 4, to = 5),
    AutoMigration(from = 5, to = 6),
    AutoMigration(from = 6, to = 7, spec = Cradle5Database.Version6To7::class),
    AutoMigration(from = 7, to = 8),
    AutoMigration(from = 8, to = 9),
    AutoMigration(from = 9, to = 10, spec = Cradle5Database.Version9To10::class),
    AutoMigration(from = 10, to = 11, spec = Cradle5Database.Version10To11::class),
    AutoMigration(from = 11, to = 12, spec = Cradle5Database.Version11To12::class),
  ]
)
@TypeConverters(DbTypeConverters::class)
abstract class Cradle5Database : RoomDatabase() {
  @DeleteColumn(tableName = "Outcomes", columnName = "hysterectomy_additionalInfo")
  internal class Version6To7 : AutoMigrationSpec

  internal class Version9To10 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase): Unit = db.run {
      val defaultDistricts = listOf(
        Pair(1, "1 - Bonthe"),
        Pair(2, "2 - Falaba"),
        Pair(3, "3 - Kailahun"),
        Pair(4, "4 - Karene"),
        Pair(5, "5 - Koinadugu"),
        Pair(6, "6 - Kono"),
        Pair(7, "7 - Moyamba"),
        Pair(8, "8 - Tonkolili"),
      )
      for ((id, districtName) in defaultDistricts) {
        Log.d(TAG, "Version9To10: Inserting district id $id, $districtName")
        execSQL("""INSERT INTO District(id, name) VALUES ($id, "$districtName")""")
      }
    }
  }

  @DeleteColumn(tableName = "Outcomes", columnName = "hdu_itu_admission_date")
  @DeleteColumn(tableName = "Outcomes", columnName = "hduOrItuAdmissionTouched")
  @DeleteColumn(tableName = "Outcomes", columnName = "hdu_itu_admission_additionalInfo")
  @DeleteColumn(tableName = "Outcomes", columnName = "hdu_itu_admission_cause_otherString")
  @DeleteColumn(tableName = "Outcomes", columnName = "hdu_itu_admission_cause_selectionId")
  @DeleteColumn(tableName = "Outcomes", columnName = "hdu_itu_admission_stayInDays")
  @DeleteColumn(tableName = "Outcomes", columnName = "perinatal_death_maternalfactors_selectionId")
  @DeleteColumn(tableName = "Outcomes", columnName = "perinatal_death_maternalfactors_otherString")
  @DeleteColumn(tableName = "Outcomes", columnName = "eclampsia_date")
  internal class Version10To11 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase): Unit = db.run {
      val now = LocalDate.now().let { localDate ->
        fun StringBuilder.padInt(value: Int, length: Int) {
          val strValue = value.toString()
          for (i in length - strValue.length downTo 1) {
            append('0')
          }
          append(strValue)
        }
        buildString {
          padInt(localDate.dayOfMonth, 2)
          append('/')
          padInt(localDate.monthValue, 2)
          append('/')
          padInt(localDate.year, 4)
        }
      }
      Log.d(TAG, "Version10To11: Setting all patient registration dates to $now")
      execSQL("""UPDATE Patient SET registrationDate = ?""", arrayOf(now))
    }
  }

  @DeleteColumn(tableName = "Outcomes", columnName = "surgicalManagementTouched")
  @DeleteColumn(tableName = "Outcomes", columnName = "surgical_mgmt_type_selectionId")
  @DeleteColumn(tableName = "Outcomes", columnName = "surgical_mgmt_type_otherString")
  @DeleteColumn(tableName = "Outcomes", columnName = "surgical_mgmt_date")
  internal class Version11To12 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase): Unit = db.run {
      execSQL("""UPDATE District SET isOther = 1 WHERE name LIKE '%other%'""")
    }
  }

  abstract fun patientDao(): PatientDao
  abstract fun outcomesDao(): OutcomesDao
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
