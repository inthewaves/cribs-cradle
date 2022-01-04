package org.welbodipartnership.cradle5.util.datetime

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.DateTimeException
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

private const val YEAR_LENGTH = 4
private const val MONTH_WITH_PADDING_LENGTH = 2
private const val DAY_WITH_PADDING_LENGTH = 2
private const val SLASH_LENGTH = 1

/**
 * Represents the date string for form input as dd/mm/yyyy string.
 *
 * Note that we do not perform validations for determining if all quantities are strictly positive.
 * The database uses 00/00/{year} to indicate that a birthday is not exact.
 */
@JsonClass(generateAdapter = true)
@Parcelize
@Immutable
data class FormDate(val day: Int, val month: Int, val year: Int) : Comparable<FormDate>, Parcelable {
  @IgnoredOnParcel
  val isExact: Boolean get() = day != 0 && month != 0

  fun isValid(areNonExactDatesValid: Boolean) = isValid(day, month, year, areNonExactDatesValid)

  /**
   * Checks whether this date would be valid if it was in mm/dd/yyyy format.
   */
  fun isValidIfItWereMmDdYyyyFormat(areNonExactDatesValid: Boolean) =
    isValid(month, day, year, areNonExactDatesValid)

  private fun isValid(day: Int, month: Int, year: Int, areNonExactDatesValid: Boolean): Boolean {
    // Let approximate fields pass validation by assigning them an always-valid value
    val monthToUse = if (areNonExactDatesValid && month == 0) 1 else month
    val dayToUse = if (areNonExactDatesValid && day == 0) 1 else day

    // the logic here is derived from LocalDate's `of` static method
    try {
      ChronoField.YEAR.checkValidValue(year.toLong())
      ChronoField.MONTH_OF_YEAR.checkValidValue(monthToUse.toLong())
      ChronoField.DAY_OF_MONTH.checkValidValue(dayToUse.toLong())
    } catch (e: DateTimeException) {
      return false
    }

    return if (dayToUse > 28) {
      val lastDayOfMonth = when (monthToUse) {
        2 -> if (IsoChronology.INSTANCE.isLeapYear(year.toLong())) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
      }
      dayToUse <= lastDayOfMonth
    } else {
      true
    }
  }

  fun toGmtGregorianCalendar() = GregorianCalendar(TimeZone.getTimeZone("GMT")).apply {
    clear()
    if (day == 0 || month == 0) {
      set(year, 0, 1)
    } else {
      set(year, month - 1, day)
    }
  }

  fun toLocalDate(): LocalDate = LocalDate.of(year, month, day)

  /**
   * Will be negative if this date is after [other]
   */
  fun getAgeInYearsFromDate(other: FormDate): Long {
    val otherDate = other.toGmtGregorianCalendar().toZonedDateTime()
    val thisAsDate = toGmtGregorianCalendar().toZonedDateTime()
    return if (compareDayAndMonth(this, other) < 0) {
      ChronoUnit.YEARS.between(thisAsDate, otherDate)
    } else {
      ChronoUnit.YEARS.between(thisAsDate, otherDate)
    }
  }

  /**
   * Will be negative if this date is after now
   */
  fun getAgeInYearsFromNow(): Long {
    return getAgeInYearsFromDate(today())
  }

  override fun toString(): String = toString(withSlashes = true)

  fun toString(withSlashes: Boolean = true): String {
    return if (withSlashes) {
      buildString(MAX_STRING_LEN_WITH_SLASHES) {
        padInt(day, DAY_WITH_PADDING_LENGTH)
        append('/')
        padInt(month, MONTH_WITH_PADDING_LENGTH)
        append('/')
        padInt(year, YEAR_LENGTH)
      }
    } else {
      buildString(MAX_STRING_LEN_NO_SLASHES) {
        padInt(day, DAY_WITH_PADDING_LENGTH)
        padInt(month, MONTH_WITH_PADDING_LENGTH)
        padInt(year, YEAR_LENGTH)
      }
    }
  }

  override fun compareTo(other: FormDate): Int {
    val yearComparison = this.year.compareTo(other.year)
    if (yearComparison != 0) return yearComparison
    return compareDayAndMonth(this, other)
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
    const val MAX_STRING_LEN_WITH_SLASHES = DAY_WITH_PADDING_LENGTH +
      SLASH_LENGTH +
      MONTH_WITH_PADDING_LENGTH +
      SLASH_LENGTH +
      YEAR_LENGTH

    const val MAX_STRING_LEN_NO_SLASHES = DAY_WITH_PADDING_LENGTH +
      MONTH_WITH_PADDING_LENGTH +
      YEAR_LENGTH

    fun today(): FormDate {
      val now = LocalDate.now()
      return FormDate(
        day = now.dayOfMonth,
        month = now.monthValue,
        year = now.year
      )
    }

    fun fromAgeFromNow(ageInYears: Int): FormDate {
      return fromAgeFromTime(ageInYears, LocalDate.now())
    }

    fun fromAgeFromDate(ageInYears: Int, otherFormDate: FormDate): FormDate {
      return FormDate(
        day = 0,
        month = 0,
        year = otherFormDate.year - ageInYears
      )
    }

    fun fromAgeFromTime(ageInYears: Int, localDate: LocalDate): FormDate {
      return FormDate(
        day = 0,
        month = 0,
        year = localDate.year - ageInYears
      )
    }

    fun fromGmtTimestampMillis(timestampMillis: Long): FormDate {
      val calendar = GregorianCalendar(TimeZone.getTimeZone("GMT"))
        .apply { time = Date(timestampMillis) }
      return FormDate(
        day = calendar[Calendar.DAY_OF_MONTH],
        month = calendar[Calendar.MONTH] + 1,
        year = calendar[Calendar.YEAR]
      )
    }

    private fun compareDayAndMonth(thisFormDate: FormDate, otherFormDate: FormDate): Int {
      val monthComparison = thisFormDate.month.compareTo(otherFormDate.month)
      if (monthComparison != 0) return monthComparison
      return thisFormDate.day.compareTo(otherFormDate.day)
    }
  }
}

fun String.toFormDateFromNoSlashesOrNull(): FormDate? = try {
  toFormDateFromNoSlashesOrThrow()
} catch (e: NumberFormatException) {
  null
}

fun String.toFormDateOrNull(): FormDate? = try {
  toFormDateOrThrow()
} catch (e: NumberFormatException) {
  null
}

@Throws(NumberFormatException::class)
fun String.toFormDateOrThrow(): FormDate {
  if (length < FormDate.MAX_STRING_LEN_WITH_SLASHES) {
    throw NumberFormatException("length too short to be a form date")
  }
  return toFormDateFromNoSlashesOrThrow()
}

@Throws(NumberFormatException::class)
fun String.toFormDateFromNoSlashesOrThrow(): FormDate {
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
