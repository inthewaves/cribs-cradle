package org.welbodipartnership.api.cradle5

import com.squareup.moshi.JsonAdapter
import org.junit.jupiter.api.Test
import org.welbodipartnership.api.Json
import org.welbodipartnership.api.forms.Form
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.getAdapterForFormType
import kotlin.test.assertEquals

internal class OutcomeTest {
  @Test
  fun testFormIdAnnotation() {
    assertEquals(70L, Outcome::class.java.getAnnotation(FormId::class.java)?.id)
  }

  @Test
  fun testParsing() {
    val json = """
      {
        "Data": {
          "Control1378": true,
          "Control1541": "01/12/2021",
          "Control1551": 2,
          "Control1545": true,
          "Control1546": "08/12/2021",
          "Control1552": 4,
          "Control1553": "Test other",
          "Control1728": "Test",
          "Control1720": true,
          "Control1555": "04/11/2021",
          "Control1554": 8,
          "Control1556": null,
          "Control1918": null,
          "Control1921": true,
          "Control1386": "08/10/2021",
          "Control1557": 9,
          "Control1558": "Test cause of death",
          "Control1559": 2,
          "Control1925": false,
          "Control1924": null,
          "Control1926": null,
          "Control1927": null,
          "Control1931": true,
          "Control1930": "07/10/2021",
          "Control1932": 2,
          "Control1933": 6,
          "Control1934": null
        },
        "Meta": {
          "Title": "Outcomes ",
          "FormId": 70,
          "ObjectId": 4,
          "HistoryNavigation": {
            "FirstRecord": {
              "Url": "https://www.medscinet.com/Cradle5Test/api/v0/forms/17?historyId=4",
              "Text": " First instance "
            },
            "PreviousRecord": {
              "Url": "https://www.medscinet.com/Cradle5Test/api/v0/forms/17?historyId=5",
              "Text": " Previous instance "
            },
            "NextRecord": null,
            "LastRecord": null
          },
          "OperationLog": {
            "Inserted": {
              "UserId": 83,
              "User": "District4User Test (testUser)",
              "Date": "2021-12-29T04:15:16"
            },
            "Updated": {
              "UserId": 83,
              "User": "District4User Test (testUser)",
              "Date": "2021-12-29T07:05:22"
            },
            "Signed": null
          },
          "Operations": [
            {
              "Id": 191,
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
              "Id": "Control1378",
              "Name": null,
              "DataType": "bool",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "true",
                  "Text": "Yes"
                },
                {
                  "Id": "false",
                  "Text": "No"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1541",
              "Name": "Date of event:",
              "DataType": "date",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1551",
              "Name": "Place of  first eclamptic fit:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Community"
                },
                {
                  "Id": "2",
                  "Text": "2 - Peripheral Health Unit"
                },
                {
                  "Id": "3",
                  "Text": "3 - Hospital"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1545",
              "Name": null,
              "DataType": "bool",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "true",
                  "Text": "Yes"
                },
                {
                  "Id": "false",
                  "Text": "No"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1546",
              "Name": "Date of event:",
              "DataType": "date",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1552",
              "Name": "Cause of hysterectomy:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Haemorrhage"
                },
                {
                  "Id": "2",
                  "Text": "2 - Sepsis"
                },
                {
                  "Id": "3",
                  "Text": "3 - Ruptured uterus"
                },
                {
                  "Id": "4",
                  "Text": "4 - Other"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1553",
              "Name": "If <i>Other</i>, please specify:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1728",
              "Name": "Additional information on<br>the cause of hysterectomy:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1720",
              "Name": null,
              "DataType": "bool",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "true",
                  "Text": "Yes"
                },
                {
                  "Id": "false",
                  "Text": "No"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1555",
              "Name": "Date of event:",
              "DataType": "date",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1554",
              "Name": "Cause for HDU / ITU admission:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Hypertensive disorders e.g. preeclampsia / eclampsia"
                },
                {
                  "Id": "2",
                  "Text": "2 - Antepartum haemorrhage e.g. placenta praevia"
                },
                {
                  "Id": "3",
                  "Text": "3 - Antepartum haemorrhage e.g. placental abruption"
                },
                {
                  "Id": "4",
                  "Text": "4 - Postpartum haemorrhage"
                },
                {
                  "Id": "5",
                  "Text": "5 - Pregnancy-related infection"
                },
                {
                  "Id": "6",
                  "Text": "6 - Other source of sepsis"
                },
                {
                  "Id": "7",
                  "Text": "7 - Stroke"
                },
                {
                  "Id": "8",
                  "Text": "8 - Complications of abortive pregnancy outcome"
                },
                {
                  "Id": "9",
                  "Text": "9 - Other"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1556",
              "Name": "If <i>Other</i>, please specify:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1918",
              "Name": "Length of stay in ITU / HDU in days:",
              "DataType": "int",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1921",
              "Name": null,
              "DataType": "bool",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "true",
                  "Text": "Yes"
                },
                {
                  "Id": "false",
                  "Text": "No"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1386",
              "Name": "Date of event:",
              "DataType": "date",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1557",
              "Name": "Underlying cause of death:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Hypertensive disorders e.g. preeclampsia / eclampsia"
                },
                {
                  "Id": "2",
                  "Text": "2 - Antepartum haemorrhage e.g. placenta praevia"
                },
                {
                  "Id": "3",
                  "Text": "3 - Antepartum haemorrhage e.g. placental abruption"
                },
                {
                  "Id": "4",
                  "Text": "4 - Postpartum haemorrhage"
                },
                {
                  "Id": "5",
                  "Text": "5 - Pregnancy-related infection"
                },
                {
                  "Id": "6",
                  "Text": "6 - Other source of sepsis"
                },
                {
                  "Id": "7",
                  "Text": "7 - Stroke"
                },
                {
                  "Id": "8",
                  "Text": "8 - Complications of abortive pregnancy outcome"
                },
                {
                  "Id": "9",
                  "Text": "9 - Other"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1558",
              "Name": "If <i>Other</i>, please specify:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1559",
              "Name": "Place of death:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Community"
                },
                {
                  "Id": "2",
                  "Text": "2 - Peripheral Health Unit"
                },
                {
                  "Id": "3",
                  "Text": "3 - Hospital"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1925",
              "Name": null,
              "DataType": "bool",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "true",
                  "Text": "Yes"
                },
                {
                  "Id": "false",
                  "Text": "No"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1924",
              "Name": "Date of event:",
              "DataType": "date",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1926",
              "Name": "Type of surgical management:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Examination under anaesthesia"
                },
                {
                  "Id": "2",
                  "Text": "2 - Laparotomy without added procedures"
                },
                {
                  "Id": "3",
                  "Text": "3 - Uterine compression sutures"
                },
                {
                  "Id": "4",
                  "Text": "4 - Balloon tamponade"
                },
                {
                  "Id": "5",
                  "Text": "5 - Artery ligation"
                },
                {
                  "Id": "6",
                  "Text": "6 - Hysterectomy"
                },
                {
                  "Id": "7",
                  "Text": "7 - Other"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1927",
              "Name": "If <i>Other</i>, please specify:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1931",
              "Name": null,
              "DataType": "bool",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "true",
                  "Text": "Yes"
                },
                {
                  "Id": "false",
                  "Text": "No"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1930",
              "Name": "Date of event:",
              "DataType": "date",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1932",
              "Name": "Please define the perinatal outcome:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Macerated stillbirth"
                },
                {
                  "Id": "2",
                  "Text": "2 - Fresh stillbirth"
                },
                {
                  "Id": "3",
                  "Text": "3 - Early neonatal death"
                },
                {
                  "Id": "4",
                  "Text": "4 - Late neonatal death"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1933",
              "Name": "Maternal factors related to perinatal loss:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Maternal hypertensive disorders e.g. preeclampsia/eclampsia"
                },
                {
                  "Id": "2",
                  "Text": "2 - Placental insufficiency"
                },
                {
                  "Id": "3",
                  "Text": "3 - Haemorrhage before or during labour"
                },
                {
                  "Id": "4",
                  "Text": "4 - Placental abruption"
                },
                {
                  "Id": "5",
                  "Text": "5 - Preeclampsia/Eclampsia"
                },
                {
                  "Id": "6",
                  "Text": "6 - Cord prolapse"
                },
                {
                  "Id": "7",
                  "Text": "7 - Genetic defect in the baby"
                },
                {
                  "Id": "8",
                  "Text": "8 - Pregnancy related infection that also affected the baby"
                },
                {
                  "Id": "9",
                  "Text": "9 - Other"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1934",
              "Name": "If <i>Other</i>, please specify:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            }
          ],
          "TreeUrl": "https://www.medscinet.com/Cradle5Test/api/v0/tree/6"
        }
      }
    """.trimIndent()

    val adapter: JsonAdapter<Form<Outcome>> = Json.buildMoshiInstance().getAdapterForFormType()

    val parsed = adapter.fromJson(json)
  }
}
