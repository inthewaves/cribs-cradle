package org.welbodipartnership.cradle5.data.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.protobuf.InvalidProtocolBufferException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a [DataStore] that is encrypted at-rest. Obviously, when the app is active, the contents
 * of the [DataStore] in memory is accessible.
 *
 * The [DataStore] at-rest encryption is handled by the [EncryptedSettingsSerializer].
 */
@Singleton
internal class EncryptedSettingsManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {
  private var dataStore: DataStore<EncryptedSettings>? = null

  fun encryptedSettingsFlow(): Flow<EncryptedSettings> = requireNotNull(dataStore).data

  suspend fun updateData(
    transform: suspend (t: EncryptedSettings) -> EncryptedSettings
  ): EncryptedSettings {
    return requireNotNull(dataStore).updateData(transform)
  }

  /**
   * This must be called before the app can use this.
   */
  internal fun setup(keysetHandle: KeysetHandle) {
    val serializer = EncryptedSettingsSerializer(
      keysetHandle.getPrimitive(StreamingAead::class.java),
      FILENAME
    )

    dataStore = DataStoreFactory.create(
      serializer = serializer,
      corruptionHandler = ReplaceFileCorruptionHandler { ex ->
        Log.wtf(TAG, "cannot read encrypted settings", ex)
        // FIXME: do something sane
        throw ex
        //EncryptedSettings.getDefaultInstance()
      },
      produceFile = { context.dataStoreFile(FILENAME) },
      // these are the default params
      scope = CoroutineScope(appCoroutineDispatchers.io + SupervisorJob())
    )
  }

  companion object {
    private const val FILENAME = "encrypted_settings.pb"
    private const val TAG = "EncryptedSettingsManage"
  }
}

/**
 * A serializer that ensures that the [DataStore] is stored on disk encrypted using
 * [https://developers.google.com/tink/streaming-aead].
 */
internal class EncryptedSettingsSerializer internal constructor(
  private val streamingAead: StreamingAead,
  fileName: String,
) : Serializer<EncryptedSettings> {
  private val fileNameBytes = fileName.toByteArray(Charsets.UTF_8)

  override val defaultValue: EncryptedSettings
    get() = EncryptedSettings.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): EncryptedSettings {
    return streamingAead.newDecryptingStream(input, fileNameBytes).use { decryptingStream ->
      try {
        EncryptedSettings.parseFrom(decryptingStream)
      } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("cannot read proto", exception)
      }
    }
  }

  override suspend fun writeTo(t: EncryptedSettings, output: OutputStream) {
    return streamingAead.newEncryptingStream(output, fileNameBytes).use { encryptingStream ->
      t.writeTo(encryptingStream)
    }
  }

  companion object {
    /**
     * https://developers.google.com/tink/streaming-aead
     *
     * - Size of the main key: 32 bytes
     * - HKDF algo: HMAC-SHA256
     * - Size of AES-GCM derived keys: 32 bytes
     * - Ciphertext segment size: 4096 bytes
     */
    const val ENCRYPTION_SCHEME = "AES256_GCM_HKDF_4KB"

    fun create(keysetHandle: KeysetHandle, fileName: String): EncryptedSettingsSerializer {
      return EncryptedSettingsSerializer(
        keysetHandle.getPrimitive(StreamingAead::class.java),
        fileName
      )
    }
  }
}
