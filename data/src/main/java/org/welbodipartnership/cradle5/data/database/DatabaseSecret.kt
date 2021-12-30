package org.welbodipartnership.cradle5.data.database

import com.google.crypto.tink.subtle.Hex
import net.sqlcipher.database.SQLiteDatabaseHook
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

@JvmInline
internal value class DatabaseSecret(val secretBytes: ByteArray) {
  fun createSupportFactory(hook: SQLiteDatabaseHook): SupportFactory {
    return SupportFactory(secretBytes, hook, false)
  }

  companion object {
    /**
     * https://www.zetetic.net/sqlcipher/sqlcipher-api/#key
     */
    private const val KEY_LENGTH_BYTES = 32

    fun generate(secureRandom: SecureRandom): DatabaseSecret {
      val randomKeyBytes = ByteArray(KEY_LENGTH_BYTES).apply(secureRandom::nextBytes)
      val keyInHex = Hex.encode(randomKeyBytes)
      check(keyInHex.length == KEY_LENGTH_BYTES * 2)
      return DatabaseSecret("x''$keyInHex''".toByteArray(Charsets.UTF_8))
    }
  }
}
