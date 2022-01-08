package org.welbodipartnership.cradle5.locationcheckin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.home.AccountInfoButton

@Composable
fun LocationCheckInScreen(onOpenSettingsForApp: () -> Unit,) {
  LocationCheckInScreen(viewModel = hiltViewModel(), onOpenSettingsForApp)
}

@Composable
private fun LocationCheckInScreen(
  viewModel: LocationCheckInViewModel,
  onOpenSettingsForApp: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        contentPadding = rememberInsetsPaddingValues(
          insets = LocalWindowInsets.current.systemBars,
          applyBottom = false,
        ),
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.location_checkin_title)) },
        actions = { AccountInfoButton() }
      )
    }
  ) { padding ->
    Column(Modifier.padding(padding)) {
      Text(text = "hi")

      var dontShowRationale by rememberSaveable { mutableStateOf(false) }

      val multiLocationPermsState = rememberMultiplePermissionsState(
        listOf(
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )

      when {
        // If all permissions are granted, then show screen with the feature enabled
        multiLocationPermsState.allPermissionsGranted -> {
          val location by viewModel.lastLocation.collectAsState()
          Text(text = location)
          Button(onClick = { viewModel.getLocation() }) {
            Text("Get location")
          }
        }
        // If the user denied any permission but a rationale should be shown, or the user sees
        // the permissions for the first time, explain why the feature is needed by the app and
        // allow the user decide if they don't want to see the rationale any more.
        //
        // "Starting in Android 11 (API level 30), if the user taps Deny for a specific permission
        // more than once during your app's lifetime of installation on a device, the user doesn't
        // see the system permissions dialog if your app requests that permission again. The user's
        // action implies "don't ask again." On previous versions, users would see the system
        // permissions dialog each time your app requested a permission, unless the user had
        // previously selected a "don't ask again" checkbox or option"
        // - https://developer.android.com/training/permissions/requesting#handle-denial
        multiLocationPermsState.shouldShowRationale ||
          !multiLocationPermsState.permissionRequested ->
        {
          if (dontShowRationale) {
            Text("Feature not available")
          } else {
            Column {
              val revokedPermissionsText = getPermissionsText(
                multiLocationPermsState.revokedPermissions
              )
              Text(
                "$revokedPermissionsText important. " +
                  "Please grant all of them for the app to function properly."
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row {
                Button(
                  onClick = {
                    multiLocationPermsState.launchMultiplePermissionRequest()
                  }
                ) {
                  Text("Request permissions")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { dontShowRationale = true }) {
                  Text("Don't show rationale again")
                }
              }
            }
          }
        }
        // If the criteria above hasn't been met, the user denied some permission. Let's present
        // the user with a FAQ in case they want to know more and send them to the Settings screen
        // to enable them the future there if they want to.
        else -> {
          Column {
            val revokedPermissionsText = getPermissionsText(
              multiLocationPermsState.revokedPermissions
            )
            Text(
              "$revokedPermissionsText denied. See this FAQ with " +
                "information about why we need this permission. Please, grant us " +
                "access on the Settings screen."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onOpenSettingsForApp) {
              Text("Open app settings")
            }
          }
        }
      }


    }
  }
}

/**
 * https://github.com/google/accompanist/blob/main/sample/src/main/java/com/google/accompanist/sample/permissions/RequestMultiplePermissionsSample.kt
 */
private fun getPermissionsText(permissions: List<PermissionState>): String {
  val revokedPermissionsSize = permissions.size
  if (revokedPermissionsSize == 0) return ""

  val textToShow = StringBuilder().apply {
    append("The ")
  }

  for (i in permissions.indices) {
    textToShow.append(permissions[i].permission)
    when {
      revokedPermissionsSize > 1 && i == revokedPermissionsSize - 2 -> {
        textToShow.append(", and ")
      }
      i == revokedPermissionsSize - 1 -> {
        textToShow.append(" ")
      }
      else -> {
        textToShow.append(", ")
      }
    }
  }
  textToShow.append(if (revokedPermissionsSize == 1) "permission is" else "permissions are")
  return textToShow.toString()
}
