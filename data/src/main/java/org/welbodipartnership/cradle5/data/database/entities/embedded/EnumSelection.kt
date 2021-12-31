package org.welbodipartnership.cradle5.data.database.entities.embedded

import androidx.compose.runtime.Immutable
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum

/**
 * Represents a selection for a server enum.
 */
sealed class EnumSelection {
  abstract val selectionId: Int

  fun getSelection(serverEnum: ServerEnum): ServerEnum.Entry? =
    serverEnum.getValueFromId(selectionId)

  @Immutable
  data class IdOnly(override val selectionId: Int) : EnumSelection() {
    /**
     * @param unknownFormatter Formats an unknown selection id
     */
    inline fun getSelectionString(
      serverEnum: ServerEnum?,
      unknownFormatter: (unknownSelectionId: Int) -> String
    ): String = serverEnum?.getValueFromId(selectionId)?.name
      ?: unknownFormatter(selectionId)
  }

  /**
   * Represents an enumeration of values where one of the options corresponds to "Other" which
   * allows for free-form input.
   */
  @Immutable
  data class WithOther constructor(
    override val selectionId: Int,
    val otherString: String?,
  ) : EnumSelection() {
    /**
     * Constructor for a pre-defined selection.
     */
    constructor(selectionId: Int) : this(selectionId, null)

    val isOther: Boolean get() = otherString != null

    /**
     * @param unknownFormatter Formats an unknown selection id
     */
    inline fun getSelectionString(
      serverEnum: ServerEnum?,
      unknownFormatter: (unknownSelectionId: Int) -> String
    ): String = otherString
      ?: serverEnum?.getValueFromId(selectionId)?.name
      ?: unknownFormatter(selectionId)
  }
}
