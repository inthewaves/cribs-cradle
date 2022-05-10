package org.welbodipartnership.cradle5.cradleform.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable

@Stable
sealed class BaseFields {
  abstract fun forceShowErrors()
}

@Stable
sealed class FieldsWithCheckbox : BaseFields() {
  abstract val isEnabled: MutableState<Boolean?>

  /**
   * Resets the error state on the forms, using the [newEnabledState] for the checkbox state (null
   * means no selection).
   */
  abstract fun clearFormsAndSetCheckbox(newEnabledState: Boolean?)
}
