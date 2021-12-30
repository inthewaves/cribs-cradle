package org.welbodipartnership.cradle5.data.settings

import androidx.datastore.core.DataStore
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

internal class EncryptedSettingsSerializerTest {
  companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
      StreamingAeadConfig.register()
    }
  }

  @Test
  fun testEncryptedSettings() {
    runBlocking {
      val simpleDataStore = object : DataStore<UnencryptedSettings> {
        var instance = MutableStateFlow(UnencryptedSettings.getDefaultInstance())
        override val data: Flow<UnencryptedSettings> = instance

        override suspend fun updateData(transform: suspend (t: UnencryptedSettings) -> UnencryptedSettings): UnencryptedSettings {
          return transform(instance.value)
            .also { instance.value = it }
        }
      }

      val keysetHandle = KeysetHandle.generateNew(
        KeyTemplates.get(EncryptedSettingsSerializer.ENCRYPTION_SCHEME)
      )

      val encryptedSettingsSerializer = EncryptedSettingsSerializer(
        keysetHandle.getPrimitive(StreamingAead::class.java),
        "abc"
      )

      val instance = EncryptedSettings.newBuilder()
        .setUsername("Test user")
        .build()
      val bos = ByteArrayOutputStream()
      encryptedSettingsSerializer.writeTo(instance, bos)
      val bytes = bos.toByteArray()
      val input = bytes.inputStream()

      val decryptedAndParsed = encryptedSettingsSerializer.readFrom(input)

      assertEquals(instance, decryptedAndParsed)
    }
  }
}
