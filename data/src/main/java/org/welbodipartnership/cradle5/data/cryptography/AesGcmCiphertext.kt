package org.welbodipartnership.cradle5.data.cryptography

import com.google.protobuf.ByteString
import com.squareup.moshi.JsonClass
import org.welbodipartnership.cradle5.data.settings.UnencryptedSettings
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@JsonClass(generateAdapter = true)
class AesGcmCiphertext internal constructor(val data: ByteArray, val iv: ByteArray) {
  @Throws(GeneralSecurityException::class)
  fun decrypt(secretKey: SecretKey): Plaintext {
    val cipher = Cipher.getInstance(AES_GCM_CIPHER_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
    return Plaintext(cipher.doFinal(data))
  }

  fun toProtoAesGcmCiphertext() = UnencryptedSettings.AesGcmCiphertext.newBuilder()
    .setData(ByteString.copyFrom(data))
    .setIv(ByteString.copyFrom(iv))
    .build()!!

  companion object {
    // AES-GCM uses a 12 byte IV and a 16 byte tag.
    private const val AES_GCM_CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val AES_GCM_IV_SIZE_IN_BYTES = 12
    private const val AES_GCM_TAG_SIZE_IN_BYTES = 16

    @Throws(GeneralSecurityException::class)
    fun encrypt(secretKey: SecretKey, plaintext: Plaintext): AesGcmCiphertext {
      // Check that ciphertext is not longer than the max. size of a Java array.
      if (
        plaintext.size > Integer.MAX_VALUE - AES_GCM_IV_SIZE_IN_BYTES - AES_GCM_TAG_SIZE_IN_BYTES
      ) {
        throw GeneralSecurityException("Plaintext too long")
      }

      val cipher = Cipher.getInstance(AES_GCM_CIPHER_TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)

      val iv: ByteArray = cipher.iv
      val data: ByteArray = cipher.doFinal(plaintext.bytes)
      return AesGcmCiphertext(data, iv)
    }
  }
}

internal fun UnencryptedSettings.AesGcmCiphertext.toAppAesGcmCiphertext() = AesGcmCiphertext(
  data.toByteArray(),
  iv.toByteArray()
)
