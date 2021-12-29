package org.welbodipartnership.cradle5.util.date

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import org.welbodipartnership.libmsn.api.util.fromDdMmYyyyDateOrThrow
import org.welbodipartnership.libmsn.api.util.toDdMmYyyySlashString
import java.util.Date

/**
 * Represents the date string for form input
 */
@JvmInline
@JsonClass(generateAdapter = true)
value class FormDate(val date: Date) {
  override fun toString(): String = date.toDdMmYyyySlashString()

  class Adapter : JsonAdapter<FormDate>() {
    override fun fromJson(reader: JsonReader): FormDate? {
      if (reader.peek() == JsonReader.Token.NULL) {
        return reader.nextNull()
      }
      val dateString = reader.nextString()!!
      val date = try {
        dateString.fromDdMmYyyyDateOrThrow()
      } catch (e: NumberFormatException) {
        throw JsonDataException("invalid form date", e)
      }
      return FormDate(date)
    }

    override fun toJson(writer: JsonWriter, value: FormDate?) {
      if (value != null) {
        writer.value(value.date.toDdMmYyyySlashString())
      } else {
        writer.nullValue()
      }
    }
  }
}

@Throws(NumberFormatException::class)
fun String.toFormDateOrThrow() = FormDate(this.fromDdMmYyyyDateOrThrow())
