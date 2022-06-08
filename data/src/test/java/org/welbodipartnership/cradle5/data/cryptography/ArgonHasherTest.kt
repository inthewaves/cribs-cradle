package org.welbodipartnership.cradle5.data.cryptography

import org.junit.jupiter.api.Test
import org.welbodipartnership.cradle5.data.settings.argonHash
import kotlin.test.assertEquals

internal class ArgonHasherTest {
  @Test
  fun testConcatHashAndSalt() {
    val firstPart = "ABC"
    val secondPart = "CDE"
    val hash = argonHash {
      encodedHash = "\$argon2id\$v=19\$m=4096,t=16,p=1\$$firstPart\$$secondPart"
    }
    assertEquals("$firstPart$secondPart", hash.concatHashAndSalt())
  }
}
