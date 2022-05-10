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
    const val DROPDOWN_VERSION = 3

    val defaultInstance = ServerEnumCollection(emptySet())
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
