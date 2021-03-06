package org.welbodipartnership.cradle5.data.appinit

import android.app.Application
import android.util.Log
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import kotlinx.coroutines.flow.firstOrNull
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import org.acra.ACRA
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.settings.EncryptedSettingsManager
import org.welbodipartnership.cradle5.data.settings.UnencryptedSettingsManager
import org.welbodipartnership.cradle5.util.appinit.AppInitTask
import javax.inject.Inject
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class DataAndEncryptionSetupTask @Inject internal constructor(
  private val unencryptedSettings: UnencryptedSettingsManager,
  private val encryptedSettings: EncryptedSettingsManager,
  private val databaseWrapper: CradleDatabaseWrapper,
) : AppInitTask {

  override val order: ULong = 1u

  override suspend fun init(application: Application) {
    StreamingAeadConfig.register()

    Log.d(TAG, "Decrypting encrypted settings")
    val encryptedSettingsTime = measureTime {
      encryptedSettings.setup(unencryptedSettings.getOrCreateKeysetForEncryptedSettings())
    }
    Log.d(TAG, "Done decrypting encrypted settings in $encryptedSettingsTime")

    Log.d(TAG, "Opening / decrypting database")
    val (db, dbOpenTime) = measureTimedValue {
      val databaseSecret = unencryptedSettings.getOrCreateDatabaseKey()
      val supportFactory = databaseSecret.createSupportFactory(object : SQLiteDatabaseHook {
        override fun preKey(database: SQLiteDatabase) {
          database.apply {
            rawExecSQL("PRAGMA cipher_default_kdf_iter = 1;")
            rawExecSQL("PRAGMA cipher_default_page_size = 4096;")
          }
        }

        override fun postKey(database: SQLiteDatabase) {
          database.apply {
            rawExecSQL("PRAGMA cipher_compatibility = 3;")
            rawExecSQL("PRAGMA cipher_memory_security = OFF;")
            rawExecSQL("PRAGMA kdf_iter = '1';")
            rawExecSQL("PRAGMA cipher_page_size = 4096;")
            enableWriteAheadLogging()
            setForeignKeyConstraintsEnabled(true)
          }
        }
      })
      databaseWrapper.setup(application, supportFactory)
    }
    Log.d(TAG, "Done opening database in $dbOpenTime")
    ACRA.errorReporter.putCustomData("db_open_time", dbOpenTime.toString())

    // We don't actually need the unencrypted settings anymore, as it just stores the keys.
    unencryptedSettings.closeDataStore()

    // If an exception occurs due to inconsisten schema (Room validates schema), this will be caught
    // by the AppInitManager
    Log.d(TAG, "Checking database integrity")
    Log.d(TAG, "There are ${db.districtDao().countTotalDistricts().firstOrNull()} districts")
  }

  companion object {
    private const val TAG = "DataSetupTask"
  }
}
