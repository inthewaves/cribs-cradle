package org.welbodipartnership.api.forms

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.EnumJsonAdapter
import org.junit.jupiter.api.Test
import org.welbodipartnership.api.cradle5.Registration
import org.welbodipartnership.api.forms.meta.ControlType
import org.welbodipartnership.api.forms.meta.DataType
import kotlin.test.assertEquals

class FormTest {
  private val moshi = Moshi.Builder()
    .add(DataType::class.java, EnumJsonAdapter.create(DataType::class.java))
    .add(ControlType::class.java, EnumJsonAdapter.create(ControlType::class.java))
    .build()

  @JsonClass(generateAdapter = true)
  data class SampleData(
    @field:Json(name = "Control1006")
    val loginName: String,
    @Json(name = "Control1016")
    val title: String,
    @Json(name = "Control1007")
    val firstName: String,
    @Json(name = "Control1008")
    val lastName: String,
    @Json(name = "Control1009")
    val email: String,
    @Json(name = "Control1012")
    val center: String,
    @Json(name = "Control1013")
    val street: String,
    @Json(name = "Control1015")
    val city: String,
    @Json(name = "Control1014")
    val postalCode: Int,
    @Json(name = "Control1017")
    val state: String,
    @Json(name = "Control1018")
    val country: String,
    @Json(name = "Control1010")
    val phoneNumber: String,
    @Json(name = "Control1019")
    val something: String?,
    @Json(name = "Control1011")
    val somethingElse: String?
  )

  @Test
  fun testFormIdAnnotation() {
    val formIdAnnotationValue = Registration::class.java.getAnnotation(FormId::class.java)
      ?.id
    assertEquals(63, formIdAnnotationValue)
  }

  @Test
  fun testSampleRequest() {
    val json = """
      {
        "Data": {
          "Control1006": "user",
          "Control1016": "Mr.",
          "Control1007": "Unit",
          "Control1008": "User",
          "Control1009": "support@medscinet.com",
          "Control1012": "Test centet",
          "Control1013": "Test street",
          "Control1015": "Vilnius",
          "Control1014": "12345",
          "Control1017": "Test state",
          "Control1018": "Lithuania",
          "Control1010": "132456789",
          "Control1019": null,
          "Control1011": null
      },
      "Meta": {
        "Title": "User account",
        "FormId": 11,
        "ObjectId": 20,
        "HistoryNavigation": null,
        "OperationLog": {
          "Inserted": {
            "UserId": 1,
            "User": "User Support1 (support)",
            "Date": "2000-01-01T00:00:00"
          },
          "Updated": {
            "UserId": 20,
            "User": "User Unit (user)",
            "Date": "2011-05-18T11:44:12"
          },
          "Signed": null
        },
        "Operations": [
          {
            "Id": 100,
            "Title": "Save",
            "Url": null
          },
          {
            "Id": null,
            "Title": "Change password",
            "Url": "https://www.medscinet.com/{study}/api/{version}/forms/12/20"
          }
        ],
        "Controls": [
          {
            "Id": "Control1006",
            "Name": "Login name:",
            "DataType": "string",
            "ControlType": "Text",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1016",
            "Name": "Title:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1007",
            "Name": "First name:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1008",
            "Name": "Last name:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1009",
            "Name": "E-mail:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1012",
            "Name": "Centre:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1013",
            "Name": "Street address:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1015",
            "Name": "City:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1014",
            "Name": "Zip Code:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1017",
            "Name": "State/Province:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1018",
            "Name": "Country:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1010",
            "Name": "Phone 1:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1019",
            "Name": "Phone 2:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          },
          {
            "Id": "Control1011",
            "Name": "Fax:",
            "DataType": "string",
            "ControlType": "DataInput",
            "ValueList": null,
            "DynamicLookupProperties": null
          }
        ],
        "TreeUrl": null
      }
    }
    """.trimIndent()

    val adapter = moshi.adapter<Form<SampleData>>(
      Types.newParameterizedType(Form::class.java, SampleData::class.java)
    )

    adapter.fromJson(json)
  }

  @Test
  fun testReflection() {
    val controlIdFromAnnotation = SampleData::class.java.declaredFields.asSequence()
      .flatMap { it.annotations.asIterable() }
      .mapNotNull { (it as? Json)?.name }
      .toList()
    assertEquals(listOf("Control1006"), controlIdFromAnnotation)

    /*
    error(
      SampleData::class.java.constructors.asSequence()
        .single()
        .parameterAnnotations
        .asList()
        .map { it.asList() }
    )

    error(
      SampleData::class.java.constructors.asSequence()
        .single()
        .parameterAnnotations
        .flatMap { it.asIterable() }
        .mapNotNull { (it as? Json)?.name }
        .toList()
    )
     */
  }
}
