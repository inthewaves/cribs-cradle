package org.welbodipartnership.cradle5.data.settings

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.protobuf.InvalidProtocolBufferException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.welbodipartnership.cradle5.data.cryptography.KeyStoreHelper
import org.welbodipartnership.cradle5.data.cryptography.Plaintext
import org.welbodipartnership.cradle5.data.cryptography.toAppAesGcmCiphertext
import org.welbodipartnership.cradle5.data.database.DatabaseSecret
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages settings in a [DataStore] that is not stored encrypted on disk. Usually this is used to
 * store keys that are wrapped with a key stored in the Android KeyStore.
 */
@Singleton
internal class UnencryptedSettingsManager @Inject constructor(
  @ApplicationContext val context: Context,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  private val keyStoreHelper: KeyStoreHelper
) {

  private val dataStoreInitLock = Mutex()
  private var dataStoreAndJobPair: Pair<DataStore<UnencryptedSettings>, CompletableJob>? = null

  /**
   * Closes the [DataStore]. The [DataStore] will reinitialize when accessor functions are
   * called again.
   */
  fun closeDataStore() {
    if (!dataStoreInitLock.tryLock()) return
    try {
      dataStoreAndJobPair?.second?.cancel(CancellationException("closeDataStore() was called"))
      dataStoreAndJobPair = null
    } finally {
      dataStoreInitLock.unlock()
    }
  }

  suspend fun getOrCreateKeysetForEncryptedSettings(): KeysetHandle {
    val unencryptedSettings = getOrCreateDatastore().data.first()
    val key = unencryptedSettings.decryptWrappedSettingsKeyIfPresent()
    if (key != null) {
      return key
    } else {
      val newSnapshot = getOrCreateDatastore().updateData { data ->
        if (data.hasWrappedSettingsKey()) {
          return@updateData data
        }

        val keysetHandle = KeysetHandle.generateNew(
          KeyTemplates.get(EncryptedSettingsSerializer.ENCRYPTION_SCHEME)
        )
        val keysetBytes: ByteArray = ByteArrayOutputStream()
          .apply {
            CleartextKeysetHandle.write(keysetHandle, BinaryKeysetWriter.withOutputStream(this))
          }
          .toByteArray()
        val encryptedSettingsKeyset = keyStoreHelper.encrypt(Plaintext(keysetBytes))
        data.toBuilder()
          .setWrappedSettingsKey(encryptedSettingsKeyset.toProtoAesGcmCiphertext())
          .build()
      }
      return newSnapshot.decryptWrappedSettingsKeyIfPresent()!!
    }
  }

  suspend fun getOrCreateDatabaseKey(): DatabaseSecret {
    val unencryptedSettings = getOrCreateDatastore().data.first()
    val key = unencryptedSettings.decryptWrappedDatabaseKeyIfPresent()
    if (key != null) {
      return key
    } else {
      val newSnapshot = getOrCreateDatastore().updateData { data ->
        if (data.hasWrappedDatabaseKey()) {
          return@updateData data
        }

        val generatedDatabaseKey = DatabaseSecret.generate(SecureRandom())
        val encryptedDbSecret = keyStoreHelper.encrypt(Plaintext(generatedDatabaseKey.secretBytes))
        data.toBuilder()
          .setWrappedDatabaseKey(encryptedDbSecret.toProtoAesGcmCiphertext())
          .build()
      }
      return newSnapshot.decryptWrappedDatabaseKeyIfPresent()!!
    }
  }

  private suspend fun getOrCreateDatastore(): DataStore<UnencryptedSettings> {
    return dataStoreAndJobPair?.first ?: dataStoreInitLock.withLock {
      val dataStore = dataStoreAndJobPair?.first
      if (dataStore != null) {
        dataStore
      } else {
        val job = SupervisorJob()
        val newStore = DataStoreFactory.create(
          serializer = UnencryptedSettingsSerializer,
          produceFile = { context.dataStoreFile(UNENCRYPTED_SETTINGS_DATASTORE_FILE_NAME) },
          // these are the default params
          scope = CoroutineScope(appCoroutineDispatchers.io + job)
        )
        dataStoreAndJobPair = newStore to job
        newStore
      }
    }
  }

  private suspend fun UnencryptedSettings.decryptWrappedSettingsKeyIfPresent(): KeysetHandle? {
    if (!hasWrappedSettingsKey()) {
      return null
    }

    val plaintextKey = keyStoreHelper.decrypt(wrappedSettingsKey.toAppAesGcmCiphertext())
    return CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(plaintextKey.bytes))
  }

  private suspend fun UnencryptedSettings.decryptWrappedDatabaseKeyIfPresent(): DatabaseSecret? {
    if (!hasWrappedDatabaseKey()) {
      return null
    }
    val plaintextKey = keyStoreHelper.decrypt(wrappedDatabaseKey.toAppAesGcmCiphertext())
    return DatabaseSecret(plaintextKey.bytes)
  }

  companion object {
    private const val UNENCRYPTED_SETTINGS_DATASTORE_FILE_NAME = "unencrypted_settings.pb"
  }
}

private object UnencryptedSettingsSerializer : Serializer<UnencryptedSettings> {
  override val defaultValue: UnencryptedSettings
    get() = UnencryptedSettings.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): UnencryptedSettings {
    return try {
      UnencryptedSettings.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException("cannot read proto", exception)
    }
  }

  override suspend fun writeTo(t: UnencryptedSettings, output: OutputStream) {
    t.writeTo(output)
  }
}
