package org.welbodipartnership.libmsn.api.util

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.streams.asStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class FormDateTest {
  private val moshi = Moshi.Builder()
    .add(FormDate::class.java, FormDate.Adapter())
    .build()

  @JsonClass(generateAdapter = true)
  data class SampleData(
    @Json(name = "Control1006")
    val date: FormDate,
  )

  internal class DateArgumentsProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?) = sequenceOf(
      "20/02/1990" to Triple(20, 2, 1990),
      "29/02/2000" to Triple(29, 2, 2000),
      "28/02/2001" to Triple(28, 2, 2001),
    ).map { (dateString, ddMmYyyyTriple) ->
      val expectedDate = GregorianCalendar(TimeZone.getTimeZone("GMT")).run {
        clear()
        set(Calendar.DAY_OF_MONTH, ddMmYyyyTriple.first)
        set(Calendar.MONTH, ddMmYyyyTriple.second - 1)
        set(Calendar.YEAR, ddMmYyyyTriple.third)
        time
      }
      Arguments.of(dateString, expectedDate)
    }.asStream()
  }
  @ParameterizedTest
  @ArgumentsSource(DateArgumentsProvider::class)
  fun testFormDate(expectedDateString: String, expectedDate: Date) {
    val json = """{"Control1006":"$expectedDateString"}"""
    val adapter = moshi.adapter(SampleData::class.java)
    val parsed = adapter.fromJson(json)
    assertNotNull(parsed)
    assertEquals(expectedDate, parsed.date.date)
    val jsonAgain = adapter.toJson(parsed)
    assertEquals(json, jsonAgain)
  }
}
