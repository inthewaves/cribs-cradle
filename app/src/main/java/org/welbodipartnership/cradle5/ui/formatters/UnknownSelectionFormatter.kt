package org.welbodipartnership.cradle5.ui.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum

/**
 * Formats a selection id that doesn't belong in an [ServerEnum]
 */
@Composable
@ReadOnlyComposable
fun unknownSelectionFormatter(unknownSelectionId: Int): String {
  return stringResource(R.string.unknown_dropdown_value_d, unknownSelectionId)
}
