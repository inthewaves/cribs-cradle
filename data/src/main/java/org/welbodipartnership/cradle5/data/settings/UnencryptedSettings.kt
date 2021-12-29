package org.welbodipartnership.cradle5.data.settings

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.protobuf.InvalidProtocolBufferException
import org.welbodipartnership.cradle5.data.cryptography.KeyStoreWrapper
import org.welbodipartnership.cradle5.data.cryptography.Plaintext
import org.welbodipartnership.cradle5.data.cryptography.toAppAesGcmCiphertext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

object UnencryptedSettingsSerializer : Serializer<UnencryptedSettings> {
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

suspend fun UnencryptedSettings.unwrapEncryptedSettingsKeyset(
  keyStoreWrapper: KeyStoreWrapper
): KeysetHandle? {
  if (!hasWrappedSettingsKey()) {
    return null
  }

  val plaintextKey = keyStoreWrapper.decrypt(wrappedSettingsKey.toAppAesGcmCiphertext())
  return CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(plaintextKey.bytes))
}

suspend fun DataStore<UnencryptedSettings>.createEncryptedSettingsKeysetHandle(
  keyStoreWrapper: KeyStoreWrapper
) {
  this.updateData { data ->
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

    val encrypted = keyStoreWrapper.encrypt(Plaintext(keysetBytes))

    data.toBuilder()
      .setWrappedDatabaseKey(encrypted.toProtoAesGcmCiphertext())
      .build()
  }
}

val Context.unencryptedSettingsDataStore: DataStore<UnencryptedSettings> by dataStore(
  "unencrypted_settings.pb",
  serializer = UnencryptedSettingsSerializer
)
