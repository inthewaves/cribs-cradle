package org.welbodipartnership.api.cradle5

import com.squareup.moshi.JsonAdapter
import org.junit.jupiter.api.Test
import org.welbodipartnership.api.Json
import org.welbodipartnership.api.forms.FormGetResponse
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.getAdapterForFormType
import org.welbodipartnership.cradle5.util.datetime.FormDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class CradleImplementationDataTest {
  @Test
  fun testFormIdAnnotation() {
    assertEquals(117, CradleImplementationData::class.java.getAnnotation(FormId::class.java)?.id)
  }

  @Test
  fun testParsing() {
    val expected = CradleImplementationData(
      recordLastUpdated = "08/05/2022 04:36",
      district = 7,
      healthcareFacility = 626,
      dateOfTraining = FormDate(1, 1, 2010),
      numOfBpDevicesFunctioning = 4,
      numOfCradleDevicesFunctioning = 6,
      numOfCradleDevicesBroken = 7,
      powerSupplyGenerator = true,
      powerSupplySolar = false,
      powerSupplyGrid = true,
      powerSupplyNone = false,
      totalStaffWorking = 50,
      totalStaffProvidingMaternityServices = 4,
      totalStaffTrainedToday = 50,
      totalStaffTrainedTodayDoctors = 1,
      totalStaffTrainedTodayMidwives = 2,
      totalStaffTrainedTodaySACHOS = 3,
      totalStaffTrainedTodaySECHNMidwives = 4,
      totalStaffTrainedTodaySRNs = 5,
      totalStaffTrainedTodayCHOs = 6,
      totalStaffTrainedTodayCHAs = 7,
      totalStaffTrainedTodayCSECHNs = 8,
      totalStaffTrainedTodayMCHAides = 9,
      totalStaffTrainedTodayTBA = 10,
      totalStaffTrainedBefore = 1,
      totalStaffTrainedScored8 = 1
    )
    val json = """
      {
        "Data": {
          "Control2029": "08/05/2022 04:36",
          "Control2159": 7,
          "Control2018": 626,
          "Control2048": "01/01/2010",
          "Control2020": 4,
          "Control2187": 6,
          "Control2188": 7,
          "Control2190": true,
          "Control2191": false,
          "Control2192": true,
          "Control2193": false,
          "Control2194": 50,
          "Control2195": 4,
          "Control2196": 50,
          "Control2197": 1,
          "Control2198": 2,
          "Control2199": 3,
          "Control2200": 4,
          "Control2201": 5,
          "Control2202": 6,
          "Control2203": 7,
          "Control2204": 8,
          "Control2205": 9,
          "Control2206": 10,
          "Control2207": 1,
          "Control2208": 1
        },
        "Meta": {
          "Title": "CRADLE Implementation Data - Bendu",
          "FormId": 117,
          "ObjectId": 4,
          "HistoryNavigation": {
            "FirstRecord": {
              "Url": "https://www.medscinet.com/Cradle5/api/v0/forms/0?historyId=6",
              "Text": " First instance "
            },
            "PreviousRecord": {
              "Url": "https://www.medscinet.com/Cradle5/api/v0/forms/0?historyId=6",
              "Text": " Previous instance "
            },
            "NextRecord": null,
            "LastRecord": null
          },
          "OperationLog": {
            "Inserted": {
              "UserId": 113,
              "User": "Investigator Test (testinvestigator)",
              "Date": "2022-05-08T05:41:54"
            },
            "Updated": {
              "UserId": 113,
              "User": "Investigator Test (testinvestigator)",
              "Date": "2022-05-08T05:49:56"
            },
            "Signed": null
          },
          "Operations": [
            {
              "Id": 247,
              "Title": "Save",
              "Url": null
            },
            {
              "Id": null,
              "Title": "Cancel",
              "Url": null
            }
          ],
          "Controls": [
            {
              "Id": "Control2029",
              "Name": "Record last updated:",
              "DataType": "string",
              "ControlType": "Text",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2159",
              "Name": "District:",
              "DataType": "int",
              "ControlType": "SingleValueFromDynamicList",
              "ValueList": null,
              "DynamicLookupProperties": {
                "Url": "https://www.medscinet.com/Cradle5/api/v0/lookups/dynamic/Control2159/117/4",
                "MasterControls": null
              },
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2018",
              "Name": "Healthcare facility:",
              "DataType": "int",
              "ControlType": "SingleValueFromDynamicList",
              "ValueList": null,
              "DynamicLookupProperties": {
                "Url": "https://www.medscinet.com/Cradle5/api/v0/lookups/dynamic/Control2018/117/4",
                "MasterControls": [
                  "Control2159"
                ]
              },
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2048",
              "Name": "Date of training:",
              "DataType": "date",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2020",
              "Name": "How many functioning blood pressure devices (including CRADLE) are at this facility?",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2187",
              "Name": "How many functioning CRADLE devices are at this facility?",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2188",
              "Name": "How many broken CRADLE devices are at this facility?",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2190",
              "Name": "Generator",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2191",
              "Name": "Solar",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2192",
              "Name": "Grid",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2193",
              "Name": "None",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2194",
              "Name": "What is the total number of staff working at this facility?",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2195",
              "Name": "What is the total number of staff providing maternity services at this facility?",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2196",
              "Name": "What is the total number of staff you trained today?",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2197",
              "Name": "How many of them were: Doctors",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2198",
              "Name": "How many of them were: Midwives",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2199",
              "Name": "How many of them were: SACHOS",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2200",
              "Name": "How many of them were: SECHN midwives",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2201",
              "Name": "How many of them were: SRNs",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2202",
              "Name": "How many of them were: CHOs",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2203",
              "Name": "How many of them were: CHAs",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2204",
              "Name": "How many of them were: CSECHNs",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2205",
              "Name": "How many of them were: MCH Aides",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2206",
              "Name": "How many of them were: TBA",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2207",
              "Name": "How many of the staff trained today had ever been trained in CRADLE before?",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2208",
              "Name": "How many of the staff trained today scored more than 8/10 on the CRADLE checklist?",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            }
          ],
          "TreeUrl": null
        }
      }
    """.trimIndent()

    val adapter: JsonAdapter<FormGetResponse<CradleImplementationData>> = Json.buildMoshiInstanceForApi().getAdapterForFormType()

    val parsed = assertNotNull(adapter.fromJson(json))
    assertEquals(expected, parsed.data)
  }
}