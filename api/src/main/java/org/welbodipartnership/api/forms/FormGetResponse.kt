package org.welbodipartnership.api.forms

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.internal.Util
import org.welbodipartnership.api.forms.meta.Meta
import org.welbodipartnership.api.forms.meta.OperationLog

@JsonClass(generateAdapter = true)
data class FormGetResponse<T>(
  @Json(name = "Data")
  val data: T,
  @Json(name = "Meta")
  val meta: Meta
) {

  object MetaObjectIdOnlyAdapter : SingleMetaFieldAdapter<Int>(
    metaFieldName = "ObjectId",
    resultParser = { nextInt() }
  )

  object MetaTitleOnlyAdapter : SingleMetaFieldAdapter<String>(
    metaFieldName = "Title",
    resultParser = { nextString() }
  )

  class MetaOperationLogOnlyAdapter(adapter: JsonAdapter<OperationLog>) : SingleMetaFieldAdapter<OperationLog>(
    metaFieldName = "OperationLog",
    resultParser = {
      adapter.fromJson(this)
        ?: throw Util.unexpectedNull("operationLog_", "OperationLog", this)
    }
  )

  /**
   * An adapter that only reads the [metaFieldName] field of the GET request body. This will not
   * consume the entire stream for speed reasons. Note that since OkHttp uses BufferedSources,
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
  open class SingleMetaFieldAdapter<T>(
    private val metaFieldName: String,
    private val resultParser: JsonReader.() -> T
  ) : JsonAdapter<T>() {
    init {
      require(metaFieldName.first().isUpperCase()) {
        "First character in $metaFieldName should be upper cased"
      }
    }
    val nonUpperCasedMetaFieldName = metaFieldName.asSequence()
      .mapIndexed { index, c -> if (index == 0) c.lowercase() else c }
      .joinToString()

    private val formOptions: JsonReader.Options = JsonReader.Options.of("Meta")
    private val metaOptions: JsonReader.Options = JsonReader.Options.of(metaFieldName)

    override fun toString(): String = "SingleMetaFieldAdapter(metaFieldName=$metaFieldName)"

    override fun fromJson(reader: JsonReader): T {
      reader.beginObject()
      while (reader.hasNext()) {
        when (reader.selectName(formOptions)) {
          0 -> {
            reader.beginObject()
            while (reader.hasNext()) {
              when (reader.selectName(metaOptions)) {
                0 -> {
                  // selectName consume the name token; now we just read the integer and return
                  return resultParser.invoke(reader)
                }
                -1 -> {
                  // Unknown name, skip it.
                  reader.skipName()
                  reader.skipValue()
                }
              }
            }
            reader.endObject()
            throw Util.missingProperty(nonUpperCasedMetaFieldName, metaFieldName, reader)
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

    override fun toJson(writer: JsonWriter, value_: T?) {
      throw JsonDataException("toJson is not supported")
    }
  }
}
