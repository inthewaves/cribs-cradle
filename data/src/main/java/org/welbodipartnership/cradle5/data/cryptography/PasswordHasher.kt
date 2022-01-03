package org.welbodipartnership.cradle5.data.cryptography

import kotlinx.coroutines.withContext
import org.signal.argon2.Argon2
import org.welbodipartnership.cradle5.data.settings.PasswordHash
import org.welbodipartnership.cradle5.data.settings.passwordHash
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import java.security.SecureRandom
import java.text.Normalizer

class PasswordHasher internal constructor(
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  private val argon2: Argon2,
  val saltLength: Int,
) {
  suspend fun verifyPassword(
    password: String,
    hash: PasswordHash,
  ): Boolean {
    val normalized = normalizePassword(password)
    return withContext(appCoroutineDispatchers.default) {
      Argon2.verify(hash.encodedHash, normalized.toByteArray(Charsets.UTF_8))
    }
  }

  suspend fun hashPassword(
    password: String,
    secureRandom: SecureRandom = SecureRandom()
  ): PasswordHash {
    val normalized = normalizePassword(password)

    return passwordHash {
      encodedHash = withContext(appCoroutineDispatchers.default) {
        argon2.hash(
          normalized.toByteArray(Charsets.UTF_8),
          ByteArray(saltLength).apply { secureRandom.nextBytes(this) }
        ).encoded
      }
    }
  }

  private fun normalizePassword(password: String) =
    Normalizer.normalize(password, Normalizer.Form.NFKD)
}
