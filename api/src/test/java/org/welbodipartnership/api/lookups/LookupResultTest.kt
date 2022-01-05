package org.welbodipartnership.api.lookups

import org.welbodipartnership.api.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class LookupResultTest {
  @Test
  fun testParsing() {
    val json = """
      {
        "Values": [
          {
            "Id": 1,
            "Code": "1",
            "Name": "Something",
            "ListOrder": 1
          },
          {
            "Id": 2,
            "Code": "2",
            "Name": "Something else",
            "ListOrder": 2
          },
          {
            "Id": 9,
            "Code": "9",
            "Name": "Other",
            "ListOrder": 99
          }
        ],
        "Id": 55,
        "Name": "The name for this lookup",
        "Url": "https://www.example.com/somewhere/api/v0/lookups/55"
      }
    """.trimIndent()
    val expected = LookupResult(
      values = listOf(
        LookupResult.Value(
          id = 1,
          code = "1",
          name = "Something",
          listOrder = 1,
        ),
        LookupResult.Value(
          id = 2,
          code = "2",
          name = "Something else",
          listOrder = 2,
        ),
        LookupResult.Value(
          id = 9,
          code = "9",
          name = "Other",
          listOrder = 99,
        ),
      ),
      id = 55,
      name = "The name for this lookup",
      url = "https://www.example.com/somewhere/api/v0/lookups/55"
    )

    val moshi = Json.buildMoshiInstance()
    val adapter = moshi.adapter(LookupResult::class.java)
    val parsed = adapter.fromJson(json)
    assertEquals(expected, parsed)
  }
}
