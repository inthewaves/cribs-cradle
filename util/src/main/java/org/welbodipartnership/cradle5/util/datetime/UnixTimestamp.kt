package org.welbodipartnership.cradle5.util.datetime

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Immutable
@JvmInline
value class UnixTimestamp(val timestamp: Long) : Comparable<UnixTimestamp> {
  operator fun plus(duration: Duration) =
    UnixTimestamp((timestamp.seconds + duration).inWholeSeconds)

  fun toDuration() = timestamp.seconds

  override fun compareTo(other: UnixTimestamp): Int = timestamp.compareTo(other.timestamp)

  operator fun minus(other: UnixTimestamp) = UnixTimestamp(this.timestamp - other.timestamp)

  infix fun durationBetween(other: UnixTimestamp): Duration {
    return abs(this.timestamp - other.timestamp).seconds
  }

  companion object {
    fun now() = UnixTimestamp(System.currentTimeMillis() / 1000)

    @Throws(DateTimeParseException::class)
    fun fromDateTimeString(dateTime: String, formatter: DateTimeFormatter) =
      LocalDateTime.parse(dateTime, formatter)
        .toEpochSecond(ZoneOffset.UTC)
        .let { UnixTimestamp(it) }
  }
}