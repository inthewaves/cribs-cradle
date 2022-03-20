package org.welbodipartnership.api.cradle5

import com.squareup.moshi.JsonAdapter
import org.junit.jupiter.api.Test
import org.welbodipartnership.api.Json
import org.welbodipartnership.api.forms.FormGetResponse
import org.welbodipartnership.api.forms.FormId
import org.welbodipartnership.api.getAdapterForFormType
import kotlin.test.assertEquals

internal class OutcomeTest {
  @Test
  fun testFormIdAnnotation() {
    assertEquals(70, Outcome::class.java.getAnnotation(FormId::class.java)?.id)
  }

  @Test
  fun testParsing() {
    val json = """
      {
        "Data": {
          "Control1931": true,
          "Control1930": "02/03/2022",
          "Control1932": 2,
          "Control1933": 3,
          "Control1934": null,
          "Control2130": 4,
          "Control2132": false,
          "Control2133": false,
          "Control2134": false,
          "Control2135": false,
          "Control2136": false,
          "Control2137": false,
          "Control2138": false,
          "Control2139": false,
          "Control2140": false,
          "Control2141": false,
          "Control2104": "Additional info",
          "Control2106": 1,
          "Control2107": 3,
          "Control1378": false,
          "Control2129": null,
          "Control1551": null,
          "Control1921": true,
          "Control1386": "19/07/2020",
          "Control1557": 4,
          "Control1558": null,
          "Control1559": 1,
          "Control1545": false,
          "Control1546": null,
          "Control1552": null,
          "Control1553": null,
          "Control1925": false,
          "Control1924": null,
          "Control1926": null,
          "Control1927": null
        },
        "Meta": {
          "Title": "Outcomes ",
          "FormId": 70,
          "ObjectId": 2,
          "HistoryNavigation": {
            "FirstRecord": {
              "Url": "https://www.medscinet.com/Cradle5/api/v0/forms/11?historyId=1",
              "Text": " First instance "
            },
            "PreviousRecord": {
              "Url": "https://www.medscinet.com/Cradle5/api/v0/forms/11?historyId=2",
              "Text": " Previous instance "
            },
            "NextRecord": null,
            "LastRecord": null
          },
          "OperationLog": {
            "Inserted": {
              "UserId": 111,
              "User": "test (testuser)",
              "Date": "2022-03-20T10:15:41"
            },
            "Updated": {
              "UserId": 111,
              "User": "test (testuser)",
              "Date": "2022-03-20T10:18:17"
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
                  "Text": "1 - Macerated (antenatal) stillbirth - between 24 weeks and delivery"
                },
                {
                  "Id": "2",
                  "Text": "2 - Fresh (intrapartum) stillbirth - between 24 weeks and delivery"
                },
                {
                  "Id": "3",
                  "Text": "3 - Early neonatal death (1 - 7 days)"
                },
                {
                  "Id": "4",
                  "Text": "4 - Late neonatal death (8 - 28 days)"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control1933",
              "Name": "Cause of perinatal loss:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Maternal hypertensive disorders e.g. preeclampsia / eclampsia"
                },
                {
                  "Id": "2",
                  "Text": "2 - Placental insufficiency"
                },
                {
                  "Id": "3",
                  "Text": "3 - Antepartum haemorrhage e.g. placenta praevia"
                },
                {
                  "Id": "4",
                  "Text": "4 - Antepartum haemorrhage e.g. placental abruption"
                },
                {
                  "Id": "5",
                  "Text": "5 - Cord prolapse"
                },
                {
                  "Id": "6",
                  "Text": "6 - Genetic defect in the baby"
                },
                {
                  "Id": "7",
                  "Text": "7 - Pregnancy related infection / sepsis"
                },
                {
                  "Id": "8",
                  "Text": "8 - Other"
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
            },
            {
              "Id": "Control2130",
              "Name": "Cause of Stillbirth (macerated / fresh):",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "1 - Maternal hypertensive disorder e.g. preeclampsia / eclampsia"
                },
                {
                  "Id": "2",
                  "Text": "2 - Placental insufficiency"
                },
                {
                  "Id": "3",
                  "Text": "3 - Antepartum haemorrhage e.g. placenta praevia"
                },
                {
                  "Id": "4",
                  "Text": "4 - Antepartum haemorrhage e.g. placental abruption"
                },
                {
                  "Id": "5",
                  "Text": "5 - Cord Prolapse"
                },
                {
                  "Id": "6",
                  "Text": "6 - Birth defects in the baby"
                },
                {
                  "Id": "7",
                  "Text": "7 - Pregnancy related infection / sepsis"
                },
                {
                  "Id": "8",
                  "Text": "8 - Prolonged / obstructed labour"
                },
                {
                  "Id": "9",
                  "Text": "9 - Birth asphyxia"
                },
                {
                  "Id": "10",
                  "Text": "10 - IUFD (please select only if no other cause given)"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2132",
              "Name": "Respiratory distress syndrome",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2133",
              "Name": "Birth asphyxia",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2134",
              "Name": "Sepsis",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2135",
              "Name": "Pneumonia",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2136",
              "Name": "Meningitis",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2137",
              "Name": "Malaria",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2138",
              "Name": "Major congenital malformation",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2139",
              "Name": "Prematurity",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2140",
              "Name": "Cause not established",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2141",
              "Name": "Other",
              "DataType": "bool",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2104",
              "Name": "Additional information on reason for perinatal death:",
              "DataType": "string",
              "ControlType": "DataInput",
              "ValueList": null,
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2106",
              "Name": "<b>Birthweight</b>:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "Normal (>=2500g)"
                },
                {
                  "Id": "2",
                  "Text": "Low birthweight (1500 - 2499g)"
                },
                {
                  "Id": "3",
                  "Text": "Very low birthweight (1000g - 1499g)"
                },
                {
                  "Id": "4",
                  "Text": "Extremely low birthweight (<1000g)"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
            {
              "Id": "Control2107",
              "Name": "<b>Gestational age at delivery</b>:",
              "DataType": "int",
              "ControlType": "SingleValueFromList",
              "ValueList": [
                {
                  "Id": "1",
                  "Text": "Term birth (>=37 completed weeks)"
                },
                {
                  "Id": "2",
                  "Text": "Preterm birth (between 32 and 37 weeks)"
                },
                {
                  "Id": "3",
                  "Text": "Very preterm birth (between 28 weeks and <32 weeks)"
                },
                {
                  "Id": "4",
                  "Text": "Extreme preterm birth (between 24 weeks and <28 weeks)"
                }
              ],
              "DynamicLookupProperties": null,
              "FileUploadProperties": null,
              "RequiresCommentWhenValueChanges": false
            },
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
              "Id": "Control2129",
              "Name": "Did the woman fit:",
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
              "Id": "Control1551",
              "Name": "Reported place of first eclamptic fit:",
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
                  "Text": "5 - Pregnancy related infection / sepsis"
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
                },
                {
                  "Id": "5",
                  "Text": "5 - Unknown"
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
            }
          ],
          "TreeUrl": "https://www.medscinet.com/Cradle5/api/v0/tree/2"
        }
      }
    """.trimIndent()

    val adapter: JsonAdapter<FormGetResponse<Outcome>> = Json.buildMoshiInstanceForApi().getAdapterForFormType()

    val parsed = adapter.fromJson(json)
  }
}
