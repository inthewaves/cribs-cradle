package org.welbodipartnership.cradle5.data.database

import androidx.room.TypeConverter
import org.welbodipartnership.cradle5.data.database.entities.TouchedState
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.util.datetime.FormDate
import org.welbodipartnership.cradle5.util.datetime.toFormDateOrThrow
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Suppress("UNUSED")
object DbTypeConverters {
  @TypeConverter
  @JvmStatic
  @JvmName("toZonedDateTime")
  fun toZonedDateTime(epochSecond: Long?): ZonedDateTime? =
    epochSecond?.let { ZonedDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneOffset.UTC) }

  @TypeConverter
  @JvmStatic
  @JvmName("fromZonedDateTime")
  fun fromZonedDateTime(date: ZonedDateTime?): Long? = date?.toEpochSecond()

  @TypeConverter
  @JvmStatic
  @JvmName("toFormDate")
  fun toFormDate(value: String?): FormDate? = value?.toFormDateOrThrow()

  @TypeConverter
  @JvmStatic
  @JvmName("fromFormDate")
  fun fromFormDate(value: FormDate?): String? = value?.toString()

  @TypeConverter
  @JvmStatic
  @JvmName("toEnumIdOnly")
  fun toEnumIdOnly(value: Int?): EnumSelection.IdOnly? = value?.let { EnumSelection.IdOnly(it) }

  @TypeConverter
  @JvmStatic
  @JvmName("fromEnumIdOnly")
  fun fromEnumIdOnly(value: EnumSelection.IdOnly?): Int? = value?.selectionId

  @TypeConverter
  @JvmStatic
  fun toTouchedState(value: Int?): TouchedState? = value?.let { TouchedState.values()[value] }

  @TypeConverter
  @JvmStatic
  fun fromTouchedState(touchedState: TouchedState?): Int? = touchedState?.ordinal
}
