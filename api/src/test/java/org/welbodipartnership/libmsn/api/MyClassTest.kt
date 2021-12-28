package org.welbodipartnership.libmsn.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MyClassTest {
  private val moshi = Moshi.Builder().build()

  @Test
  fun testBasicSerialization() {
    val adapter = moshi.adapter(Something::class.java)

    val instance = Something("hi", 5)
    val json = adapter.toJson(instance)
    assertEquals(json, "abc")
  }
}

@JsonClass(generateAdapter = true)
data class Something(val a: String, val b: Int) {

}