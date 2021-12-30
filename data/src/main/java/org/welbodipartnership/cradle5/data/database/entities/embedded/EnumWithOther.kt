package org.welbodipartnership.cradle5.data.database.entities.embedded

/**
 * Represents an enumeration of values where one of the options corresponds to "Other" which
 * allows for free-form input.
 */
data class EnumWithOther constructor(
  val selectionId: Long,
  val otherString: String?,
) {
  /**
   * Constructor for a pre-defined selection.
   */
  constructor(selectionId: Long) : this(selectionId, null)

  val isOther: Boolean get() = otherString != null
}
