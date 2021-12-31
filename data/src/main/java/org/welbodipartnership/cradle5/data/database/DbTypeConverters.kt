package org.welbodipartnership.cradle5.data.database

import androidx.room.TypeConverter
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.util.date.FormDate
import org.welbodipartnership.cradle5.util.date.toFormDateOrThrow

@Suppress("UNUSED")
object DbTypeConverters {
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
}
