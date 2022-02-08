package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource

@Composable
fun MoreInfoIconButton(
  moreInfoText: String,
  modifier: Modifier = Modifier,
) {
  var showInfoDialog by rememberSaveable { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = { showInfoDialog = false },
      confirmButton = {
        TextButton(onClick = { showInfoDialog = false }) {
          Text(stringResource(android.R.string.ok))
        }
      },
      title = { Text("Info") },
      text = { Text(moreInfoText) }
    )
  }

  IconButton(
    onClick = {
      focusManager.clearFocus()
      showInfoDialog = true
    },
    modifier = modifier
  ) {
    Icon(imageVector = Icons.Outlined.HelpOutline, contentDescription = "Help button icon")
  }
}
