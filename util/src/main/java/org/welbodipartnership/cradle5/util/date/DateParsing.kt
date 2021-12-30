package org.welbodipartnership.api.util

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

private val gmtTimezone = TimeZone.getTimeZone("GMT")

private const val YEAR_LENGTH = 4
private const val MONTH_WITH_PADDING_LENGTH = 2
private const val DAY_WITH_PADDING_LENGTH = 2
private const val SLASH_LENGTH = 1

private const val DD_MM_YYYY_STRING_LENGTH = DAY_WITH_PADDING_LENGTH +
  SLASH_LENGTH +
  MONTH_WITH_PADDING_LENGTH +
  SLASH_LENGTH +
  YEAR_LENGTH

@Throws(NumberFormatException::class)
fun String.fromDdMmYyyyDateOrThrow(): Date {
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

  return with(GregorianCalendar(gmtTimezone)) {
    isLenient = false
    clear()

    set(Calendar.YEAR, year)
    set(Calendar.MONTH, month - 1)
    set(Calendar.DAY_OF_MONTH, day)

    time
  }
}

fun Date.toDdMmYyyySlashString(): String {
  val calendar: Calendar = GregorianCalendar(gmtTimezone, Locale.US)
  calendar.clear()
  calendar.time = this

  return buildString(DD_MM_YYYY_STRING_LENGTH) {
    padInt(this, calendar[Calendar.DAY_OF_MONTH], DAY_WITH_PADDING_LENGTH)
    append('/')
    padInt(this, calendar[Calendar.MONTH] + 1, MONTH_WITH_PADDING_LENGTH)
    append('/')
    padInt(this, calendar[Calendar.YEAR], YEAR_LENGTH)
  }
}

/**
 * Zero pad a number to a specified length
 *
 * https://github.com/square/moshi/blob/master/moshi-adapters/src/main/java/com/squareup/moshi/adapters/Iso8601Utils.java
 *
 * @param buffer buffer to use for padding
 * @param value the integer value to pad if necessary.
 * @param length the length of the string we should zero pad
 */
private fun padInt(buffer: StringBuilder, value: Int, length: Int) {
  val strValue = Integer.toString(value)
  for (i in length - strValue.length downTo 1) {
    buffer.append('0')
  }
  buffer.append(strValue)
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
 * https://github.com/square/moshi/blob/master/moshi-adapters/src/main/java/com/squareup/moshi/adapters/Iso8601Utils.java
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
