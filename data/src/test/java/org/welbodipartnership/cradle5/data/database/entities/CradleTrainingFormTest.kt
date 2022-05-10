package org.welbodipartnership.cradle5.data.database.entities

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.test.assertEquals

internal class CradleTrainingFormTest {
  @Test
  fun testTrainingDate() {
    assertEquals(
      "30/04/2011 17:59",
      CradleTrainingForm.recordLastUpdatedFormatter.format(
        GregorianCalendar(2011, 3 /* zero-based month */, 30, 17, 59)
          .apply { timeZone = TimeZone.getTimeZone("GMT") }
          .toInstant()
      )
    )
    assertEquals(
      "09/05/2022 21:32",
      CradleTrainingForm.formatTimeAsLastUpdatedDateString(Instant.ofEpochMilli(1652131965298))
    )
  }
}
