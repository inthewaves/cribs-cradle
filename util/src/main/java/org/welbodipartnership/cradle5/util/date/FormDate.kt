package org.welbodipartnership.cradle5.util.date

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.LocalDate

private const val YEAR_LENGTH = 4
private const val MONTH_WITH_PADDING_LENGTH = 2
private const val DAY_WITH_PADDING_LENGTH = 2
private const val SLASH_LENGTH = 1

private const val DD_MM_YYYY_STRING_LENGTH = DAY_WITH_PADDING_LENGTH +
  SLASH_LENGTH +
  MONTH_WITH_PADDING_LENGTH +
  SLASH_LENGTH +
  YEAR_LENGTH

/**
 * Represents the date string for form input as dd/mm/yyyy string.
 *
 * Note that we do not perform validations for determining if all quantities are strictly positive.
 * The database uses 00/00/{year} to indicate that a birthday is not exact.
 */
@JsonClass(generateAdapter = true)
data class FormDate(val day: Int, val month: Int, val year: Int) : Comparable<FormDate> {
  val isExact: Boolean get() = day != 0 && month != 0

  override fun toString(): String = buildString(DD_MM_YYYY_STRING_LENGTH) {
    padInt(day, DAY_WITH_PADDING_LENGTH)
    append('/')
    padInt(month, MONTH_WITH_PADDING_LENGTH)
    append('/')
    padInt(year, YEAR_LENGTH)
  }

  override fun compareTo(other: FormDate): Int {
    val yearComparison = this.year.compareTo(other.year)
    if (yearComparison != 0) return yearComparison
    val monthComparison = this.month.compareTo(other.month)
    if (monthComparison != 0) return monthComparison
    return this.day.compareTo(other.day)
  }

  private fun StringBuilder.padInt(value: Int, length: Int) {
    val strValue = value.toString()
    for (i in length - strValue.length downTo 1) {
      append('0')
    }
    append(strValue)
  }

  class Adapter : JsonAdapter<FormDate>() {
    override fun fromJson(reader: JsonReader): FormDate? {
      if (reader.peek() == JsonReader.Token.NULL) {
        return reader.nextNull()
      }
      val dateString = reader.nextString()!!
      return try {
        dateString.toFormDateOrThrow()
      } catch (e: NumberFormatException) {
        throw JsonDataException("invalid form date", e)
      }
    }

    override fun toJson(writer: JsonWriter, value: FormDate?) {
      if (value != null) {
        writer.value(value.toString())
      } else {
        writer.nullValue()
      }
    }
  }

  companion object {
    fun today(): FormDate {
      val now = LocalDate.now()
      return FormDate(
        day = now.dayOfMonth,
        month = now.monthValue,
        year = now.year
      )
    }
  }
}

@Throws(NumberFormatException::class)
fun String.toFormDateOrThrow(): FormDate {
  // Logic from https://github.com/square/moshi/blob/master/moshi-adapters/src/main/java/com/squareup/moshi/adapters/Iso8601Utils.java,
  // which itself is derived from Jackson / FasterXML
  var offset = 0
  val day = parseInt(this, offset, offset + DAY_WITH_PADDING_LENGTH)
  offset += DAY_WITH_PADDING_LENGTH
  if (checkOffset(this, offset, '/')) {
    offset += SLASH_LENGTH;
  }

  val month = parseInt(this, offset, offset + MONTH_WITH_PADDING_LENGTH)
  offset += MONTH_WITH_PADDING_LENGTH
  if (checkOffset(this, offset, '/')) {
    offset += SLASH_LENGTH;
  }

  val year = parseInt(this, offset, offset + YEAR_LENGTH)

  return FormDate(day, month, year)
}

/**
 * Check if the expected character exist at the given offset in the value.
 *
 * https://github.com/square/moshi/blob/master/moshi-adapters/src/main/java/com/squareup/moshi/adapters/Iso8601Utils.java
 *
 * @param value the string to check at the specified offset
 * @param offset the offset to look for the expected character
 * @param expected the expected character
 * @return true if the expected character exist at the given offset
 */
private fun checkOffset(value: String, offset: Int, expected: Char): Boolean {
  return offset < value.length && value[offset] == expected
}

/**
 * Parse an integer located between 2 given offsets in a string
 *
 * From https://github.com/square/moshi/blob/master/moshi-adapters/src/main/java/com/squareup/moshi/adapters/Iso8601Utils.java
 *
 * @param value the string to parse
 * @param beginIndex the start index for the integer in the string
 * @param endIndex the end index for the integer in the string
 * @return the int
 * @throws NumberFormatException if the value is not a number
 */
@Throws(NumberFormatException::class)
private fun parseInt(value: String, beginIndex: Int, endIndex: Int): Int {
  if (beginIndex < 0 || endIndex > value.length || beginIndex > endIndex) {
    throw NumberFormatException(value)
  }

  // use same logic as in Integer.parseInt() but less generic we're not supporting negative values
  var i = beginIndex
  var result = 0
  var digit: Int
  if (i < endIndex) {
    digit = Character.digit(value[i++], 10)
    if (digit < 0) {
      throw NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex))
    }
    result = -digit
  }
  while (i < endIndex) {
    digit = Character.digit(value[i++], 10)
    if (digit < 0) {
      throw NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex))
    }
    result *= 10
    result -= digit
  }
  return -result
}
