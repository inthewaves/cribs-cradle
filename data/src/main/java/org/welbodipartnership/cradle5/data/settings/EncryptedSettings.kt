package org.welbodipartnership.cradle5.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

class EncryptedSettingsSerializer internal constructor(
  private val streamingAead: StreamingAead,
  fileName: String,
) : Serializer<EncryptedSettings>  {
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
    const val FILENAME = "encrypted_settings.pb"
    const val ENCRYPTION_SCHEME = "AES256_GCM_HKDF_4KB"

    fun create(keysetHandle: KeysetHandle): EncryptedSettingsSerializer {
      return EncryptedSettingsSerializer(
        keysetHandle.getPrimitive(StreamingAead::class.java),
        FILENAME
      )
    }
  }
}