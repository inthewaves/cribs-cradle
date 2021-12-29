package org.welbodipartnership.cradle5.data.cryptography

import com.squareup.moshi.Moshi
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.welbodipartnership.cradle5.data.Base64BytesAdapter
import javax.crypto.KeyGenerator
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class AesGcmCiphertextTest {
  @BeforeEach
  fun setup() {
    mockkStatic(android.util.Base64::class)
    every { android.util.Base64.encodeToString(any(), any()) } answers {
      java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
    }
    every { android.util.Base64.decode(any<String>(), any()) } answers {
      java.util.Base64.getDecoder().decode(firstArg<String>())
    }
  }

  @AfterEach
  fun cleanUp() {
    clearStaticMockk(android.util.Base64::class)
  }

  @Test
  fun testEncryptDecrypt() {
    val myPlaintext = "This is my string"

    val secretKey = KeyGenerator.getInstance("AES").run {
      init(256)
      generateKey()
    }
    val ciphertext = AesGcmCiphertext.encrypt(secretKey, Plaintext(myPlaintext.encodeToByteArray()))
    val decrypted = ciphertext.decrypt(secretKey).bytes.decodeToString()
    assertEquals(myPlaintext, decrypted)
  }

  @Test
  fun testEncryptSerializeAndDecrypt() {
    val myPlaintext = "This is my string"
    val secretKey = KeyGenerator.getInstance("AES").run {
      init(128)
      generateKey()
    }
    val ciphertext = AesGcmCiphertext.encrypt(secretKey, Plaintext(myPlaintext.encodeToByteArray()))

    val adapter = Moshi.Builder()
      .add(ByteArray::class.java, Base64BytesAdapter().nullSafe())
      .build()
      .adapter(AesGcmCiphertext::class.java)
    val json = adapter.toJson(ciphertext)
    val parsed = adapter.fromJson(json)
    assertNotNull(parsed)
    val decrypted = parsed.decrypt(secretKey).bytes.decodeToString()
    assertEquals(myPlaintext, decrypted)
  }
}