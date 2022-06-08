package org.welbodipartnership.api.forms.meta

import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class OperationLogTest {
  @Test
  fun testDSTParsing() {
    val bstString = "2022-05-10T09:58:38"
    val logEntry = OperationLog.Entry(null, null, date = bstString)
    // one-hour behind
    val nonDaylightSavingsDate = ZonedDateTime.of(2022, 5, 10, 8, 58, 38, 0, ZoneId.of("GMT"))
    assertEquals(
      nonDaylightSavingsDate,
      assertNotNull(logEntry.parsedDate).withZoneSameInstant(ZoneId.of("GMT"))
    )
  }
}
