package org.welbodipartnership.cradle5.data.cryptography

import kotlinx.coroutines.withContext
import org.signal.argon2.Argon2
import org.welbodipartnership.cradle5.data.settings.ArgonHash
import org.welbodipartnership.cradle5.data.settings.argonHash
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import java.security.SecureRandom
import java.text.Normalizer

class ArgonHasher internal constructor(
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  private val argon2: Argon2,
  private val argon2Username: Argon2,
  val saltLength: Int,
) {
  suspend fun verifyPassword(
    password: String,
    hash: ArgonHash,
  ): Boolean {
    val normalized = normalizePassword(password)
    return withContext(appCoroutineDispatchers.default) {
      Argon2.verify(hash.encodedHash, normalized.toByteArray(Charsets.UTF_8))
    }
  }

  suspend fun hash(
    forPassword: Boolean,
    string: String,
    secureRandom: SecureRandom = SecureRandom()
  ): ArgonHash {
    val normalized = normalizePassword(string)

    return argonHash {
      encodedHash = withContext(appCoroutineDispatchers.default) {
        val argonInstance = if (forPassword) argon2 else argon2Username
        argonInstance.hash(
          normalized.toByteArray(Charsets.UTF_8),
          ByteArray(saltLength).apply { secureRandom.nextBytes(this) }
        ).encoded
      }
    }
  }

  private fun normalizePassword(password: String) =
    Normalizer.normalize(password, Normalizer.Form.NFKD)
}

fun ArgonHash.concatHashAndSalt(): String {
  val secondPartIndex = encodedHash.lastIndexOf('$')
  if (secondPartIndex <= 0 || secondPartIndex == encodedHash.lastIndex) return encodedHash
  val firstPartIndex = encodedHash.lastIndexOf('$', secondPartIndex - 1)
  if (firstPartIndex == -1) return encodedHash.substring(secondPartIndex + 1)
  return encodedHash.substring(firstPartIndex + 1, secondPartIndex) + encodedHash.substring(secondPartIndex + 1)
}
