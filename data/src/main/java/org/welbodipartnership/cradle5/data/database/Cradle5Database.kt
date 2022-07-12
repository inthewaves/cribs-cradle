package org.welbodipartnership.cradle5.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.core.database.getLongOrNull
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import org.welbodipartnership.cradle5.data.database.daos.CradleTrainingFormDao
import org.welbodipartnership.cradle5.data.database.daos.DistrictDao
import org.welbodipartnership.cradle5.data.database.daos.FacilityDao
import org.welbodipartnership.cradle5.data.database.daos.LocationCheckInDao
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.data.database.resultentities.ListCradleTrainingForm
import javax.inject.Inject
import javax.inject.Singleton

const val TAG = "Cradle5Database"

const val DATABASE_VERSION = 4
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

  internal fun setup(context: Context): Cradle5Database {
    if (database != null) {
      return database!!
    }
    synchronized(CradleDatabaseWrapper::class.java) {
      if (database != null) {
        return database!!
      }

      val db = Room.databaseBuilder(context, Cradle5Database::class.java, DATABASE_NAME)
        // .openHelperFactory(supportFactory)
        .addMigrations(*MIGRATIONS)
        .build()
      database = db
      return db
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
  views = [ListCradleTrainingForm::class],
  autoMigrations = [
    AutoMigration(from = 1, to = 2, spec = Cradle5Database.Version1To2::class),
    AutoMigration(from = 2, to = 3, spec = Cradle5Database.Version2To3::class),
    AutoMigration(from = 3, to = 4, spec = Cradle5Database.Version3To4::class)
  ]
)
@TypeConverters(DbTypeConverters::class)
abstract class Cradle5Database : RoomDatabase() {
  abstract fun cradleTrainingFormDao(): CradleTrainingFormDao
  abstract fun facilitiesDao(): FacilityDao
  abstract fun locationCheckInDao(): LocationCheckInDao
  abstract fun districtDao(): DistrictDao

  @RenameColumn(
    tableName = "CradleTrainingForm",
    fromColumnName = "totalStaffTrainedScoredMoreThan8",
    toColumnName = "totalStaffTrainedScoredMoreThan14"
  )
  class Version1To2 : AutoMigrationSpec
  class Version2To3 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase): Unit = db.run {
      db.execSQL("UPDATE CradleTrainingForm SET recordCreated = recordLastUpdated")
    }
  }
  class Version3To4 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase): Unit = db.run {
      beginTransaction()
      try {
        val districtQuery = SupportSQLiteQueryBuilder.builder("District")
          .columns(arrayOf("id"))
          .create()
        db.query(districtQuery).use { districtCursor ->
          while (districtCursor != null && districtCursor.moveToNext()) {
            val districtId = districtCursor.getLongOrNull(districtCursor.getColumnIndexOrThrow("id"))

            val allFacilityNames: List<String> = run {
              val facilityQuery = SupportSQLiteQueryBuilder.builder("Facility")
                .columns(arrayOf("name"))
                .selection("districtId = ?", arrayOf(districtId))
                .orderBy("name")
                .create()
              db.query(facilityQuery).use { facilityCursor ->
                buildList<String> {
                  while (facilityCursor != null && facilityCursor.moveToNext()) {
                    val name = facilityCursor.getString(facilityCursor.getColumnIndexOrThrow("name"))
                    add(name.lowercase())
                  }
                }
              }
            }
            val invertedInsertionPoint = allFacilityNames.binarySearch("other")
            val listOrder = if (invertedInsertionPoint < 0) {
              // invertedInsertionPoint =  -insertion point - 1
              -(invertedInsertionPoint + 1)
            } else {
              9999
            }
            Log.d(TAG, "Inserting OTHER facility for districtId $districtId with list order $listOrder")
            val contentValues = contentValuesOf(
              // 656 seems to be the constant ID for the OTHER facility
              "id" to 656,
              "name" to "OTHER",
              "districtId" to districtId,
              "listOrder" to listOrder,
            )
            db.insert("Facility", SQLiteDatabase.CONFLICT_IGNORE, contentValues)
          }
        }
        setTransactionSuccessful()
      } finally {
        endTransaction()
      }
    }
  }
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
