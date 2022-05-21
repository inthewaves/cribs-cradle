package org.welbodipartnership.cradle5.data.database.entities

import androidx.room.Embedded

/**
 * Note: These are stored in the database by ordinal.
 */
enum class TouchedState(
  /**
   * The enabled state to use for a null @[Embedded] value for a nested field.
   */
  val nullEnabledState: Boolean?
) {
  /**
   * When checkbox has not been touched yet
   */
  NOT_TOUCHED(null),

  /**
   * When checkbox has been touched, and nullability should be interpreted as "No"
   */
  TOUCHED(false),

  /**
   * When checkbox has been touched, and the checkbox was set to yes, so nullability does not mean
   * "No"
   */
  TOUCHED_ENABLED(true);

  companion object {
    /** Corresponds to [TOUCHED], the ordinal of the enum. */
    const val DEFAULT_VALUE = "1"
  }
}
