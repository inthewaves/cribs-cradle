package org.welbodipartnership.cradle5.data

import android.util.Base64
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

class Base64BytesAdapter : JsonAdapter<ByteArray>() {
  override fun fromJson(reader: JsonReader): ByteArray {
    return Base64.decode(reader.nextString(), Base64.NO_WRAP)
  }

  override fun toJson(writer: JsonWriter, value: ByteArray?) {
    writer.value(Base64.encodeToString(value, Base64.NO_WRAP))
  }
}
