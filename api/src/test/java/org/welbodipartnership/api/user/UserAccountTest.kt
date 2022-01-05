package org.welbodipartnership.api.user

import org.welbodipartnership.api.Json
import org.welbodipartnership.api.getAdapterForFormType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class UserAccountTest {
  @Test
  fun testEntireFormParse() {
    val json = """
      {
        "Data": {
          "Control1271": "TestUser",
          "Control1330": 2,
          "Control1331": 15,
          "Control1291": 2,
          "Control1272": false,
          "Control1293": false,
          "Control1294": false,
          "Control1295": false,
          "Control1281": null,
          "Control1274": "Anothe",
          "Control1275": "User",
          "Control1273": "notanemail@example.com",
          "Control1284": null,
          "Control1285": null,
          "Control1289": null,
          "Control1277": null,
          "Control1276": null,
          "Control1286": null,
          "Control1278": null,
          "Control1279": null,
          "Control1280": null,
          "Control1298": 66,
          "Control1299": "04/01/2022",
          "Control1300": null
        },
        "Meta": {
          "Title": "User account",
          "FormId": 16,
          "ObjectId": 79,
          "HistoryNavigation": null,
          "OperationLog": {
            "Inserted": {
              "UserId": 77,
              "User": "MedSciNet Support (support)",
              "Date": "2021-12-19T23:00:00"
            },
            "Updated": null,
            "Signed": null
          },
          "Operations": [
            {
              "Id": 165,
              "Title": "Save",
              "Url": null
            },
            {
              "Id": null,
              "Title": "Set password",
              "Url": "https://www.medscinet.com/Cradle5Test/api/v0/forms/17/79?="
            },
            {
              "Id": null,
              "Title": "Cancel",
              "Url": "https://www.medscinet.com/Cradle5Test/api/v0/lists/14?="
            }
          ],
          "Controls": [
            {
              "Id": "Control1271",
              "Name": "Login name:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1330",
              "Name": "District:",
              "DataType": "int",
              "ControlType": "SingleValueFromDynamicList",
              "ValueList": null,
              "DynamicLookupProperties": {
                "Url": "https://www.medscinet.com/Cradle5Test/api/v0/lookups/dynamic/Control1330/16/79",
                "MasterControls": null
              },
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1331",
              "Name": "User role:",
              "DataType": "int",
              "ControlType": "SingleValueFromDynamicList",
              "ValueList": null,
              "DynamicLookupProperties": {
                "Url": "https://www.medscinet.com/Cradle5Test/api/v0/lookups/dynamic/Control1331/16/79",
                "MasterControls": [
                  "Control1330"
                ]
              },
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1291",
              "Name": "Time zone:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "W. Europe Standard Time-(GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna"
                },
                {
                  "Id": "2",
                  "Text": "GMT Standard Time-(GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1272",
              "Name": "Account is disabled",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1293",
              "Name": "Password never expires",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1294",
              "Name": "User must change password at next logon",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1295",
              "Name": "User cannot change password",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1281",
              "Name": "Title:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1274",
              "Name": "First name:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1275",
              "Name": "Last name:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1273",
              "Name": "E-mail:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1284",
              "Name": "Centre:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1285",
              "Name": "Street address:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1289",
              "Name": "City:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1277",
              "Name": "Zip Code:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1276",
              "Name": "State/Province:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1286",
              "Name": "Country:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1278",
              "Name": "Phone 1:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1279",
              "Name": "Phone 2:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1280",
              "Name": "Fax:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1298",
              "Name": "Login count:",
              "DataType": "int",
              "ControlType": "Text",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1299",
              "Name": "Last login:",
              "DataType": "date",
              "ControlType": "Text",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1300",
              "Name": "<script language=\"javascript\" type=\"text/javascript\"><!--document.all['Control1297'].value=document.all['Control1272'].checked;// --></script>",
              "DataType": "string",
              "ControlType": "Text",
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

    val formAdapter = Json.buildMoshiInstance().getAdapterForFormType<UserAccount>()

    val parsed = formAdapter.fromJson(json)
    assertNotNull(parsed)
    assertEquals("notanemail@example.com", parsed.data.email)
  }

  @Test
  fun testJsonParse() {
    val moshi = Json.buildMoshiInstance()
    val adapter = moshi.adapter(UserAccount::class.java)

    val json = """
      {
        "Control1271": "TestUser",
        "Control1330": 1,
        "Control1331": 21,
        "Control1291": 2,
        "Control1272": false,
        "Control1293": false,
        "Control1294": false,
        "Control1295": false,
        "Control1281": null,
        "Control1274": "My first name",
        "Control1275": "User",
        "Control1273": "testemail@example.com",
        "Control1284": null,
        "Control1285": null,
        "Control1289": null,
        "Control1277": null,
        "Control1276": null,
        "Control1286": null,
        "Control1278": null,
        "Control1279": null,
        "Control1280": null,
        "Control1298": 63,
        "Control1299": "04/01/2022",
        "Control1300": null
      }
    """.trimIndent()

    val parsed = adapter.fromJson(json)
    assertNotNull(parsed)
    assertEquals("testemail@example.com", parsed.email)
  }
}
