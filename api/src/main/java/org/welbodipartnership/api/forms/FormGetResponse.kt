package org.welbodipartnership.api.forms

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.internal.Util
import org.welbodipartnership.api.forms.meta.Meta

@JsonClass(generateAdapter = true)
data class FormGetResponse<T>(
  @Json(name = "Data")
  val data: T,
  @Json(name = "Meta")
  val meta: Meta
) {
  /**
   * An adapter that only read the ObjectId of the GET request body. This will not consume
   * the entire stream for speed reasons. Note that since OkHttp uses BufferedSources,
   * this doesn't save much in terms of bandwidth.
   *
   * The general format it is trying to read:
   *
   * {
   *   "Data": {...},
   *   "Meta": {
   *     ...
   *     "ObjectId": 5,
   *
   *   },
   * }
   */
  object ObjectIdOnlyAdapter : JsonAdapter<Int>() {
    private val formOptions: JsonReader.Options = JsonReader.Options.of("Meta")
    private val metaOptions: JsonReader.Options = JsonReader.Options.of("ObjectId")

    override fun toString(): String = "ObjectIdOnlyAdapter"

    override fun fromJson(reader: JsonReader): Int {
      reader.beginObject()
      while (reader.hasNext()) {
        when (reader.selectName(formOptions)) {
          0 -> {
            reader.beginObject()
            while (reader.hasNext()) {
              when (reader.selectName(metaOptions)) {
                0 -> {
                  // selectName consume the name token; now we just read the integer and return
                  return reader.nextInt()
                }
                -1 -> {
                  // Unknown name, skip it.
                  reader.skipName()
                  reader.skipValue()
                }
              }
            }
            reader.endObject()
            throw Util.missingProperty("objectId", "ObjectId", reader)
          }
          -1 -> {
            // Unknown name, skip it.
            reader.skipName()
            reader.skipValue()
          }
        }
      }
      reader.endObject()
      throw Util.missingProperty("meta", "Meta", reader)
    }

    override fun toJson(writer: JsonWriter, value_: Int?) {
      throw JsonDataException("toJson is not supported")
    }
  }
}
