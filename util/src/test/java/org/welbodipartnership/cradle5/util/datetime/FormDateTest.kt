package org.welbodipartnership.cradle5.util.datetime

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import kotlin.math.sign
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

  data class BirthdayTestSpec(
    val birthday: FormDate,
    val theTodayToUse: FormDate,
    val expectedAge: Int,
  )
  internal class BirthdayArgsProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?) = sequenceOf(
      BirthdayTestSpec(
        birthday = FormDate(day = 5, month = 5, year = 2000),
        theTodayToUse = FormDate(day = 5, month = 4, year = 2020),
        expectedAge = 19
      ),
      BirthdayTestSpec(
        birthday = FormDate(day = 5, month = 5, year = 2000),
        theTodayToUse = FormDate(day = 5, month = 6, year = 2020),
        // one month after means it's fine
        expectedAge = 20
      ),
    ).map { Arguments.of(it) }.asStream()
  }
  @ParameterizedTest
  @ArgumentsSource(BirthdayArgsProvider::class)
  fun testBirthdays(birthdayTestSpec: BirthdayTestSpec) {
    val (birthday, theTodayToUse, expectedAge) = birthdayTestSpec
    assertEquals(
      expectedAge.toLong(),
      birthday.getAgeInYearsFromDate(theTodayToUse)
    )
  }

  internal class LenientDateArgumentsProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?) = sequenceOf(
      "20021990" to FormDate(20, 2, 1990),
      "29022000" to FormDate(29, 2, 2000),
      "28022001" to FormDate(28, 2, 2001),
    ).map { (dateString, dateObject) -> Arguments.of(dateString, dateObject) }
      .asStream()
  }
  @ParameterizedTest
  @ArgumentsSource(LenientDateArgumentsProvider::class)
  fun testFormDateLenientParsing(expectedDateString: String, expectedDate: FormDate) {
    val parsedDate: FormDate = expectedDateString.toFormDateFromNoSlashesOrThrow()
    assertEquals(expectedDate, parsedDate)
    assertEquals(expectedDateString, parsedDate.toString(withSlashes = false))
  }

  internal class DateArgumentsProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?) = sequenceOf(
      "20/02/1990" to FormDate(20, 2, 1990),
      "29/02/2000" to FormDate(29, 2, 2000),
      "28/02/2001" to FormDate(28, 2, 2001),
      "22/12/2021" to FormDate(22, 12, 2021),
    ).map { (dateString, dateObject) -> Arguments.of(dateString, dateObject) }
      .asStream()
  }
  @ParameterizedTest
  @ArgumentsSource(DateArgumentsProvider::class)
  fun testFormDate(expectedDateString: String, expectedDate: FormDate) {
    val parsedDate = expectedDateString.toFormDateOrThrow()
    assertEquals(expectedDate, parsedDate)
    assertEquals(expectedDateString, parsedDate.toString())
  }

  @ParameterizedTest
  @ArgumentsSource(DateArgumentsProvider::class)
  fun testFormDateWithMoshiAdapter(expectedDateString: String, expectedDate: FormDate) {
    val json = """{"Control1006":"$expectedDateString"}"""
    val adapter = moshi.adapter(SampleData::class.java)
    val parsed = adapter.fromJson(json)
    assertNotNull(parsed)
    assertEquals(expectedDate, parsed.date)
    assertEquals(expectedDateString, parsed.date.toString())
    val jsonAgain = adapter.toJson(parsed)
    assertEquals(json, jsonAgain)
  }

  enum class Ordering(val sign: Int) { LESS(-1), EQUAL(0), GREATER(1) }
  data class OrderingTestSpec(
    val first: FormDate,
    val expectedOrdering: Ordering,
    val second: FormDate,
  ) {
    constructor(first: String, expectedOrdering: Ordering, second: String) : this(
      first.toFormDateOrThrow(),
      expectedOrdering,
      second.toFormDateOrThrow()
    )
  }
  internal class DateComparisonArgumentsProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?) = sequenceOf(
      OrderingTestSpec("20/02/1990", Ordering.LESS, "21/02/1990"),
      OrderingTestSpec("21/02/1990", Ordering.EQUAL, "21/02/1990"),
      OrderingTestSpec("22/02/1990", Ordering.GREATER, "21/02/1990"),

      OrderingTestSpec("20/02/1990", Ordering.LESS, "21/02/1995"),
    ).map { Arguments.of(it) }
      .asStream()
  }
  @ParameterizedTest
  @ArgumentsSource(DateComparisonArgumentsProvider::class)
  fun testFormDateWithMoshiAdapter(testSpec: OrderingTestSpec) {
    val (first, expectedOrdering, second) = testSpec
    assertEquals(expectedOrdering.sign, first.compareTo(second).sign)
  }

  @Test
  fun testFormDateAge() {
    val today = FormDate(3,1, 2022)
    val oneYearBefore = today.copy(year = today.year - 1)
    assertEquals(1, oneYearBefore.getAgeInYearsFromDate(today))
    val oneYearAfter = today.copy(year = today.year + 1)
    assertEquals(-1, oneYearAfter.getAgeInYearsFromDate(today))
  }
}