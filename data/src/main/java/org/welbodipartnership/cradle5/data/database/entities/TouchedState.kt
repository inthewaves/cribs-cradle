package org.welbodipartnership.cradle5.data.database.entities

import androidx.room.Embedded
enum class TouchedState(
  /**
   * The enabled state to use for a null @[Embedded] value for one of the nested fields for
   * [Outcomes].
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
}
