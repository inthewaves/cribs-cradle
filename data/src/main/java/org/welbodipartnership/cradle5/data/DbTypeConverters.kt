package org.welbodipartnership.cradle5.data

import androidx.room.TypeConverter
import org.welbodipartnership.cradle5.util.date.FormDate
import org.welbodipartnership.cradle5.util.date.toFormDateOrThrow

object DbTypeConverters {
  @TypeConverter
  @JvmStatic
  fun toFormDate(value: String?): FormDate? = value?.toFormDateOrThrow()

  @TypeConverter
  @JvmStatic
  fun fromFormDate(value: FormDate?): String? = value?.toString()
}