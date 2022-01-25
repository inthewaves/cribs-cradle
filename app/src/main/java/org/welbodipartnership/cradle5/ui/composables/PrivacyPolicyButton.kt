package org.welbodipartnership.cradle5.ui.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.util.launchPrivacyPolicyWebIntent

@Composable
fun PrivacyPolicyButton(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  TextButton(
    onClick = { context.launchPrivacyPolicyWebIntent() },
    modifier = modifier
  ) {
    Text(stringResource(R.string.privacy_policy_button))
    Icon(
      imageVector = Icons.Default.OpenInNew,
      contentDescription = stringResource(R.string.privacy_policy_button_cd),
      modifier = Modifier.padding(horizontal = 4.dp)
    )
  }
}
