package org.welbodipartnership.cradle5.data.serverenums

import android.util.Log
import androidx.collection.ArrayMap
import androidx.compose.runtime.Immutable
import org.welbodipartnership.cradle5.data.database.TAG
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.settings.DynamicServerEnum

/**
 * Immutable collection of enums. Should be treated as a singleton and updated appropriately.
 */
@Immutable
@JvmInline
value class ServerEnumCollection private constructor(
  private val map: ArrayMap<DropdownType, ServerEnum>,
) {
  constructor(enumsFromServer: Collection<DynamicServerEnum>) : this(
    // TODO: merge duplicates?
    enumsFromServer.asSequence()
      .mapNotNull { it.toAppServerEnumOrNull() }
      .associateByTo(ArrayMap(DropdownType.values().size)) { it.type }
  )

  operator fun get(type: DropdownType): ServerEnum? = map[type]

  val keys get() = map.keys

  companion object {
    /**
     * The current version of the default instance. This is kept track of in order to force an
     * update when the app updates.
     */
    const val DROPDOWN_VERSION = 4

    val defaultInstance by lazy {
      ServerEnumCollection(
        listOf(
          ServerEnum(
            DropdownType.Place,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = "1",
                name = "Community",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = "2",
                name = "Peripheral Health Unit",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = "3",
                name = "Hospital",
                listOrder = 3
              ),
            )
          ),
          ServerEnum(
            DropdownType.CauseOfHysterectomy,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = "1",
                name = "Haemorrhage",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = "2",
                name = "Sepsis",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = "3",
                name = "Ruptured uterus",
                listOrder = 3
              ),
              ServerEnum.Entry(
                id = 4,
                code = "4",
                name = "Other",
                listOrder = 99
              ),
              ServerEnum.Entry(
                id = 5,
                code = "5",
                name = "Unknown",
                listOrder = 100
              ),
            )
          ),
          ServerEnum(
            DropdownType.CauseOfStillbirth,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = "1",
                name = "Maternal hypertensive disorder e.g. preeclampsia / eclampsia",
                listOrder = 10,
              ),
              ServerEnum.Entry(
                id = 2,
                code = "2",
                name = "Placental insufficiency",
                listOrder = 20,
              ),
              ServerEnum.Entry(
                id = 3,
                code = "3",
                name = "Antepartum haemorrhage e.g. placenta praevia",
                listOrder = 30,
              ),
              ServerEnum.Entry(
                id = 4,
                code = "4",
                name = "Antepartum haemorrhage e.g. placental abruption",
                listOrder = 40,
              ),
              ServerEnum.Entry(
                id = 5,
                code = "5",
                name = "Cord Prolapse",
                listOrder = 50,
              ),
              ServerEnum.Entry(
                id = 6,
                code = "6",
                name = "Birth defects in the baby",
                listOrder = 60,
              ),
              ServerEnum.Entry(
                id = 7,
                code = "7",
                name = "Pregnancy related infection / sepsis",
                listOrder = 70,
              ),
              ServerEnum.Entry(
                id = 8,
                code = "8",
                name = "Prolonged / obstructed labour",
                listOrder = 80,
              ),
              ServerEnum.Entry(
                id = 9,
                code = "9",
                name = "Birth asphyxia",
                listOrder = 90,
              ),
              ServerEnum.Entry(
                id = 10,
                code = "10",
                name = "IUFD (please select only if no other cause given)",
                listOrder = 100,
              ),
              ServerEnum.Entry(
                id = 11,
                code = "11",
                name = "Not reported",
                listOrder = 110,
              ),
            )
          ),
          ServerEnum(
            DropdownType.UnderlyingCauseOfMaternalDeath,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = "1",
                name = "Hypertensive disorders e.g. preeclampsia / eclampsia",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = "2",
                name = "Antepartum haemorrhage e.g. placenta praevia",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = "3",
                name = "Antepartum haemorrhage e.g. placental abruption",
                listOrder = 3
              ),
              ServerEnum.Entry(
                id = 4,
                code = "4",
                name = "Postpartum haemorrhage",
                listOrder = 4
              ),
              ServerEnum.Entry(
                id = 5,
                code = "5",
                name = "Pregnancy related infection / sepsi",
                listOrder = 5
              ),
              ServerEnum.Entry(
                id = 6,
                code = "6",
                name = "Other source of sepsis",
                listOrder = 6
              ),
              ServerEnum.Entry(
                id = 7,
                code = "7",
                name = "Stroke",
                listOrder = 7
              ),
              ServerEnum.Entry(
                id = 8,
                code = "8",
                name = "Complications of abortive pregnancy outcome",
                listOrder = 8
              ),
              ServerEnum.Entry(
                id = 9,
                code = "9",
                name = "Other",
                listOrder = 99
              ),
            )
          ),
          ServerEnum(
            DropdownType.TypeOfSurgicalManagement,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = "1",
                name = "Examination under anaesthesia",
                listOrder = 10
              ),
              ServerEnum.Entry(
                id = 2,
                code = "2",
                name = "Laparotomy without added procedures",
                listOrder = 20
              ),
              ServerEnum.Entry(
                id = 3,
                code = "3",
                name = "Uterine compression sutures",
                listOrder = 30
              ),
              ServerEnum.Entry(
                id = 4,
                code = "4",
                name = "Balloon tamponade",
                listOrder = 40
              ),
              ServerEnum.Entry(
                id = 5,
                code = "5",
                name = "Artery ligation",
                listOrder = 50
              ),
              ServerEnum.Entry(
                id = 6,
                code = "6",
                name = "Hysterectomy",
                listOrder = 60
              ),
              ServerEnum.Entry(
                id = 7,
                code = "7",
                name = "Other",
                listOrder = 99
              ),
            )
          ),
          ServerEnum(
            DropdownType.PerinatalOutcome,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = "1",
                name = "Macerated (antenatal) stillbirth - between 24 weeks and delivery",
                listOrder = 10
              ),
              ServerEnum.Entry(
                id = 2,
                code = "2",
                name = "Fresh (intrapartum) stillbirth - between 24 weeks and delivery",
                listOrder = 20
              ),
              ServerEnum.Entry(
                id = 3,
                code = "3",
                name = "Early neonatal death (1 - 7 days)",
                listOrder = 30
              ),
              ServerEnum.Entry(
                id = 4,
                code = "4",
                name = "Late neonatal death (8 - 28 days)",
                listOrder = 40
              ),
              ServerEnum.Entry(
                id = 5,
                code = "5",
                name = "Other",
                listOrder = 50
              ),
            )
          ),
          ServerEnum(
            DropdownType.Birthweight,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = "1",
                name = "Normal (>2500g)",
                listOrder = 10,
              ),
              ServerEnum.Entry(
                id = 2,
                code = "2",
                name = "Low birthweight (1500 - 2499g)",
                listOrder = 20,
              ),
              ServerEnum.Entry(
                id = 3,
                code = "3",
                name = "Very low birthweight (1000g - 1499g)",
                listOrder = 30,
              ),
              ServerEnum.Entry(
                id = 4,
                code = "4",
                name = "Extremely low birthweight (<1000g)",
                listOrder = 40,
              ),
            ),
          ),
          ServerEnum(
            DropdownType.AgeAtDelivery,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = "1",
                name = "Term birth (>=37 completed weeks)",
                listOrder = 10,
              ),
              ServerEnum.Entry(
                id = 2,
                code = "2",
                name = "Preterm birth (between 32 and <37 weeks)",
                listOrder = 20,
              ),
              ServerEnum.Entry(
                id = 3,
                code = "3",
                name = "Very preterm birth (between 28 weeks and <32 weeks)",
                listOrder = 30,
              ),
              ServerEnum.Entry(
                id = 4,
                code = "4",
                name = "Extreme preterm birth (between 24 weeks and <28 weeks)",
                listOrder = 40,
              ),
            ),
          ),
          ServerEnum(
            DropdownType.EclampticFitTime,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = "1",
                name = "Before delivery",
                listOrder = 10,
              ),
              ServerEnum.Entry(
                id = 2,
                code = "2",
                name = "After delivery",
                listOrder = 20,
              ),
            ),
          ),
        ).associateByTo(ArrayMap(DropdownType.values().size)) { it.type }
      )
    }
  }
}

enum class DropdownType(val serverLookupId: Int, val expectedServerName: String) {
  Place(14, "Place"),
  CauseOfHysterectomy(15, "Cause of hysterectomy"),
  CauseOfStillbirth(16, "Cause of stillbirth (macerated / fresh)"),
  UnderlyingCauseOfMaternalDeath(17, "Underlying cause of death"),
  TypeOfSurgicalManagement(18, "Type of surgical management"),
  PerinatalOutcome(19, "Perinatal outcome"),
  Birthweight(22, "Birthweight"),
  AgeAtDelivery(23, "Age at delivery"),
  EclampticFitTime(25, "Eclamptic fit time"),
}

/**
 * The [type] property means this will represent a known dropdown. We opted to make the values in
 * each dropdown dynamic, because an admin can freely add more values. However, we made the type
 * itself sealed, because the form structure itself is assumed to be fixed in the app.
 */
@Immutable
class ServerEnum constructor(
  val type: DropdownType,
  unsortedValues: List<Entry>
) {
  val validSortedValues: List<Entry> = unsortedValues.sortedBy { it.listOrder }

  val sortedValuesWithEmptyResponse: Sequence<EntryType>
    get() = sequenceOf(EmptyResponseEntry) + validSortedValues

  val otherEntry: Entry? = validSortedValues.asReversed()
    .find { it.name.trim().equals("other", ignoreCase = true) }

  operator fun get(id: Int): Entry? = validSortedValues.find { it.id == id }

  operator fun get(id: EnumSelection): Entry? = get(id.selectionId)

  fun toDynamicServerEnum(): DynamicServerEnum = DynamicServerEnum.newBuilder()
    .setId(type.serverLookupId)
    .addAllValues(
      validSortedValues.asSequence()
        .map { entry ->
          DynamicServerEnum.Value.newBuilder().apply {
            id = entry.id
            entry.code?.let { code = it }
            name = entry.name
            listOrder = entry.listOrder
          }.build()
        }
        .asIterable()
    )
    .build()

  sealed class EntryType

  /**
   * Models the web app having an empty entry
   */
  object EmptyResponseEntry : EntryType()
  data class Entry(
    val id: Int,
    val code: String?,
    val name: String,
    val listOrder: Int,
  ) : EntryType()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ServerEnum

    if (type != other.type) return false
    if (validSortedValues != other.validSortedValues) return false

    return true
  }

  override fun hashCode(): Int {
    var result = type.hashCode()
    result = 31 * result + validSortedValues.hashCode()
    return result
  }

  override fun toString(): String {
    return "ServerEnum(type=$type, values=$validSortedValues)"
  }
}

/**
 * Converts from the proto settings format
 */
fun DynamicServerEnum.toAppServerEnumOrNull(): ServerEnum? {
  val dropdownType = DropdownType.values().find { it.serverLookupId == this.id }
  if (dropdownType == null) {
    Log.w(TAG, "Encountered unknown dropdown: $this")
    return null
  }

  return ServerEnum(
    dropdownType,
    this.valuesList.asSequence()
      .filterNotNull()
      .map {
        ServerEnum.Entry(
          id = it.id,
          code = it.code,
          name = it.name,
          listOrder = it.listOrder
        )
      }
      .toList()
  )
}
