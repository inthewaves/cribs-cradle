package org.welbodipartnership.cradle5.util.datetime

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Immutable
@JvmInline
value class UnixTimestamp(val timestamp: Long) : Comparable<UnixTimestamp> {
  operator fun plus(duration: Duration): UnixTimestamp =
    UnixTimestamp((timestamp.seconds + duration).inWholeSeconds)

  operator fun minus(duration: Duration): UnixTimestamp =
    UnixTimestamp((timestamp.seconds - duration).inWholeSeconds)

  fun toDuration() = timestamp.seconds

  override fun compareTo(other: UnixTimestamp): Int = timestamp.compareTo(other.timestamp)

  operator fun minus(other: UnixTimestamp) = UnixTimestamp(this.timestamp - other.timestamp)

  infix fun durationBetween(other: UnixTimestamp): Duration {
    return abs(this.timestamp - other.timestamp).seconds
  }

  fun formatAsIso8601Date(): String {
    val thisAsZonedDateTime: ZonedDateTime = ZonedDateTime.ofInstant(
      Instant.ofEpochSecond(timestamp),
      ZoneId.of("UTC")
    )
    return thisAsZonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }

  fun formatAsConciseDate(): String {
    val thisAsZonedDateTime: ZonedDateTime = ZonedDateTime.ofInstant(
      Instant.ofEpochSecond(timestamp),
      ZoneId.systemDefault()
    )
    val now = ZonedDateTime.now()
    val pattern = when {
      now.toLocalDate() == thisAsZonedDateTime.toLocalDate() -> "h:mm a"
      now.year == thisAsZonedDateTime.year -> "MMM d '@' h a"
      else -> "MMM d, yyyy"
    }
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return thisAsZonedDateTime.format(formatter)
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

fun Long.toUnixTimestamp() = UnixTimestamp(this)
