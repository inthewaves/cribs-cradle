package org.welbodipartnership.cradle5.data.serverenums

import androidx.collection.ArrayMap
import androidx.compose.runtime.Immutable
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

  companion object {
    // TODO: Remove this when login in implemented
    val defaultInstance by lazy {
      ServerEnumCollection(
        listOf(
          ServerEnum(
            DropdownType.Place,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Community",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Peripheral Health Unit",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
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
                code = 1,
                name = "Haemorrhage",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Sepsis",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Ruptured uterus",
                listOrder = 3
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Other",
                listOrder = 99
              ),
            )
          ),
          ServerEnum(
            DropdownType.CauseForHduOrItuAdmission,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Hypertensive disorders e.g. preeclampsia / eclampsia",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Antepartum haemorrhage e.g. placenta praevia",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Antepartum haemorrhage e.g. placental abruption",
                listOrder = 3
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Postpartum haemorrhage",
                listOrder = 4
              ),
              ServerEnum.Entry(
                id = 5,
                code = 5,
                name = "Pregnancy-related infection",
                listOrder = 5
              ),
              ServerEnum.Entry(
                id = 6,
                code = 6,
                name = "Other source of sepsis",
                listOrder = 6
              ),
              ServerEnum.Entry(
                id = 7,
                code = 7,
                name = "Stroke",
                listOrder = 7
              ),
              ServerEnum.Entry(
                id = 8,
                code = 8,
                name = "Complications of abortive pregnancy outcome",
                listOrder = 8
              ),
              ServerEnum.Entry(
                id = 9,
                code = 9,
                name = "Other",
                listOrder = 99
              ),
            )
          ),
          ServerEnum(
            DropdownType.UnderlyingCauseOfMaternalDeath,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Hypertensive disorders e.g. preeclampsia / eclampsia",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Antepartum haemorrhage e.g. placenta praevia",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Antepartum haemorrhage e.g. placental abruption",
                listOrder = 3
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Postpartum haemorrhage",
                listOrder = 4
              ),
              ServerEnum.Entry(
                id = 5,
                code = 5,
                name = "Pregnancy-related infection",
                listOrder = 5
              ),
              ServerEnum.Entry(
                id = 6,
                code = 6,
                name = "Other source of sepsis",
                listOrder = 6
              ),
              ServerEnum.Entry(
                id = 7,
                code = 7,
                name = "Stroke",
                listOrder = 7
              ),
              ServerEnum.Entry(
                id = 8,
                code = 8,
                name = "Complications of abortive pregnancy outcome",
                listOrder = 8
              ),
              ServerEnum.Entry(
                id = 9,
                code = 9,
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
                code = 1,
                name = "Examination under anaesthesia",
                listOrder = 10
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Laparotomy without added procedures",
                listOrder = 20
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Uterine compression sutures",
                listOrder = 30
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Balloon tamponade",
                listOrder = 40
              ),
              ServerEnum.Entry(
                id = 5,
                code = 5,
                name = "Artery ligation",
                listOrder = 50
              ),
              ServerEnum.Entry(
                id = 6,
                code = 6,
                name = "Hysterectomy",
                listOrder = 60
              ),
              ServerEnum.Entry(
                id = 7,
                code = 7,
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
                code = 1,
                name = "Macerated stilbirth",
                listOrder = 10
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Fresh stillbirth",
                listOrder = 20
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Early neonatal death",
                listOrder = 30
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Late neonatal death",
                listOrder = 40
              ),
            )
          ),
          ServerEnum(
            DropdownType.MaternalFactorsRelatedToPerinatalLoss,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Maternal hypertensive disorders e.g. preeclampsia/eclampsia",
                listOrder = 10
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Placental insufficiency",
                listOrder = 20
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Haemorrhage before or during labour",
                listOrder = 30
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Placental abruption",
                listOrder = 40
              ),
              ServerEnum.Entry(
                id = 5,
                code = 5,
                name = "Preeclampsia/Eclampsia",
                listOrder = 50
              ),
              ServerEnum.Entry(
                id = 6,
                code = 6,
                name = "Cord prolapse",
                listOrder = 60
              ),
              ServerEnum.Entry(
                id = 7,
                code = 7,
                name = "Genetic defect in the baby",
                listOrder = 70
              ),
              ServerEnum.Entry(
                id = 8,
                code = 8,
                name = "Pregnancy related infection that also affected the baby",
                listOrder = 80
              ),
              ServerEnum.Entry(
                id = 9,
                code = 9,
                name = "Other",
                listOrder = 90
              ),
            )
          ),
        ).associateByTo(ArrayMap()) { it.type }
      )
    }
  }
}

enum class DropdownType(val serverLookupId: Int) {
  Place(14),
  CauseOfHysterectomy(15),
  CauseForHduOrItuAdmission(16),
  UnderlyingCauseOfMaternalDeath(17),
  TypeOfSurgicalManagement(18),
  PerinatalOutcome(19),
  MaternalFactorsRelatedToPerinatalLoss(20);
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

  val otherEntry: Entry? = validSortedValues.asReversed().find { it.name.trim() == "Other" }

  fun getValueFromId(id: Int): Entry? = validSortedValues.find { it.id == id }

  fun getValueFromId(id: EnumSelection): Entry? = getValueFromId(id.selectionId)

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
    val code: Int?,
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

fun DynamicServerEnum.toAppServerEnumOrNull(): ServerEnum? {
  val dropdownType = DropdownType.values().find { it.serverLookupId == this.id } ?: return null

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
