package org.welbodipartnership.api.forms

import org.welbodipartnership.api.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PostFailureBodyTest {
  @Test
  fun testPostFailureBodyParsing() {
    val moshi = Json.buildMoshiInstanceForApi()
    val json = """
      {
        "Message": "The request is invalid.",
        "ModelState": {
          "Control1009": ["Missing E-mail"]
        }
      }
    """.trimIndent()

    val adapter = moshi.adapter(PostFailureBody::class.java)

    assertEquals(
      PostFailureBody(
        message = "The request is invalid.",
        modelState = mapOf("Control1009" to listOf("Missing E-mail"))
      ),
      adapter.fromJson(json)
    )
  }
}
