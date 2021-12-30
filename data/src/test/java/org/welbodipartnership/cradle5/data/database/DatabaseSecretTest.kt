package org.welbodipartnership.cradle5.data.database

import org.junit.jupiter.api.Test
import java.security.SecureRandom

internal class DatabaseSecretTest {
  @Test
  fun testDbSecretGeneration() {
    DatabaseSecret.generate(SecureRandom())
  }
}
