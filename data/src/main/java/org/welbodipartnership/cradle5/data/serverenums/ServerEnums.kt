package org.welbodipartnership.cradle5.data.serverenums

import androidx.collection.ArrayMap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
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
}

enum class DropdownType(val serverLookupId: Int) {
  Place(14),
  CauseOfHysterectomy(15),
  CauseForHduOrItuAdmission(16),
  UnderlyingCauseOfDeath(17),
  TypeOfSurgicalManagement(18),
  PerinatalOutcome(19),
  MaternalFactorsRelatedToPerinatalLoss(20);
}

/**
 * The [type] property means this will represent a known dropdown. We opted to make the values in
 * each dropdown dynamic, because an admin can freely add more values. However, we made the type
 * itself sealed, because the form structure itself is assumed to be fixed in the app.
 */
@Stable
class ServerEnum constructor(
  val type: DropdownType,
  unsortedValues: List<Entry>
) {
  val sortedValues = unsortedValues.sortedBy { it.listOrder }

  fun getValueFromId(id: Int): Entry? = sortedValues.find { it.id == id }

  fun toDynamicServerEnum(): DynamicServerEnum = DynamicServerEnum.newBuilder()
    .setId(type.serverLookupId)
    .addAllValues(
      sortedValues.asSequence()
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

  data class Entry(
    val id: Int,
    val code: Int?,
    val name: String,
    val listOrder: Int,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ServerEnum

    if (type != other.type) return false
    if (sortedValues != other.sortedValues) return false

    return true
  }

  override fun hashCode(): Int {
    var result = type.hashCode()
    result = 31 * result + sortedValues.hashCode()
    return result
  }

  override fun toString(): String {
    return "ServerEnum(type=$type, values=$sortedValues)"
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
