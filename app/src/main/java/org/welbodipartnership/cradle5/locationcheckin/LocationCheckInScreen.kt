package org.welbodipartnership.cradle5.locationcheckin

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.home.AccountInfoButton
import org.welbodipartnership.cradle5.patients.details.BaseDetailsCard
import org.welbodipartnership.cradle5.ui.composables.screenlists.ScreenListItem
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp

@Composable
fun LocationCheckInScreen(onOpenSettingsForApp: () -> Unit,) {
  LocationCheckInScreen(viewModel = hiltViewModel(), onOpenSettingsForApp)
}

@Composable
private fun LocationCheckInScreen(
  viewModel: LocationCheckInViewModel,
  onOpenSettingsForApp: () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }

  val undoState by viewModel.undoCheckIn.collectAsState()
  LaunchedEffect(undoState) {
    val current = undoState
    if (current != null) {
      try {
        val timestamp = UnixTimestamp(current.timestamp).formatAsConciseDate()
        val result = snackbarHostState.showSnackbar(
          "Deleted entry that was made at $timestamp",
          actionLabel = "Undo",
          SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
          viewModel.handleUndo()
        }
      } finally {
        viewModel.clearUndoCache()
      }
    }
  }

  Scaffold(
    scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
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
    val lazyItems = viewModel.checkInsPagerFlow.collectAsLazyPagingItems()
    val isDeleteEnabled by viewModel.canDelete.collectAsState()
    LazyColumn(contentPadding = padding) {
      item {
        val multiLocationPermsState = rememberMultiplePermissionsState(
          listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
          )
        )
        BaseDetailsCard(
          title = null,
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          when {
            multiLocationPermsState.allPermissionsGranted -> {
              val state = viewModel.screenState.collectAsState().value
              when (state) {
                is LocationCheckInViewModel.ScreenState.Error -> {
                  Text(state.errorMessage)
                }
                LocationCheckInViewModel.ScreenState.GettingLocation -> {
                  Text("Getting location")
                  Spacer(Modifier.height(8.dp))
                  CircularProgressIndicator()
                }
                LocationCheckInViewModel.ScreenState.Success -> {
                  Text("Successfully created location check in")
                }
                LocationCheckInViewModel.ScreenState.Ready -> {}
              }

              if (state !is LocationCheckInViewModel.ScreenState.GettingLocation) {
                Button(onClick = { viewModel.submitLocationRequest() }) {
                  Text("Create location check in")
                }
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
              !multiLocationPermsState.permissionRequested ||
              Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> {

              Text("The app requires the precise location permission to perform location check ins.")

              Spacer(modifier = Modifier.height(8.dp))

              Button(onClick = { multiLocationPermsState.launchMultiplePermissionRequest() }) {
                Text("Request permissions")
              }
            }
            else -> {
              Text(
                "Precise location permissions have been denied. This permission is required" +
                  "to perform location check ins. " +
                  "Permissions have to be given through the app settings."
              )
              Spacer(modifier = Modifier.height(8.dp))
              Button(onClick = onOpenSettingsForApp) {
                Text("Open app settings")
              }
            }
          }
        }
      }

      items(lazyItems) { checkIn ->
        if (checkIn != null) {
          CheckInListItem(
            checkIn = checkIn,
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
              .animateItemPlacement(),
            columnModifier = Modifier.padding(16.dp),
            isDeleteEnabled = isDeleteEnabled,
            onDeletePressed = viewModel::delete
          )
        } else {
          CheckInListItemPlaceholder()
        }
      }
    }
  }
}

@Composable
fun CheckInListItemPlaceholder(modifier: Modifier = Modifier) {
  ScreenListItem(
    minHeight = 48.dp,
    onClick = null,
    modifier = modifier,
    horizontalArrangement = Arrangement.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
fun CheckInListItem(
  checkIn: LocationCheckIn,
  isDeleteEnabled: Boolean,
  onDeletePressed: (LocationCheckIn) -> Unit,
  modifier: Modifier = Modifier,
  columnModifier: Modifier = Modifier,
) {
  Card(modifier = modifier) {
    Column(columnModifier) {
      Text(
        UnixTimestamp(checkIn.timestamp).formatAsConciseDate(),
        style = MaterialTheme.typography.h6
      )

      Text(
        "(${checkIn.providerName})",
        style = MaterialTheme.typography.subtitle1
      )
      SelectionContainer {
        Text(
          "${checkIn.latitude}, ${checkIn.longitude}",
          style = MaterialTheme.typography.body1
        )
      }
      val uploadedText = if (checkIn.isUploaded) {
        "Uploaded"
      } else {
        "Not yet uploaded"
      }
      Text(
        uploadedText,
        style = MaterialTheme.typography.body1
      )
      Spacer(modifier = Modifier.height(16.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (!checkIn.isUploaded) {
          TextButton(
            onClick = { onDeletePressed(checkIn) },
            enabled = isDeleteEnabled,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error)
          ) {
            Text("Delete")
          }
        }
        val context = LocalContext.current
        TextButton(
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW)
              .setData(Uri.parse("${checkIn.latitude},${checkIn.longitude}"))
            if (intent.resolveActivity(context.packageManager) != null) {
              context.startActivity(intent)
            } else {
              Toast
                .makeText(context, "Unable to find a maps application", Toast.LENGTH_SHORT)
                .show()
            }
          }
        ) {
          Text("Show in maps app")
        }
      }
    }
  }
}

@Preview
@Composable
fun CheckInListItemPreview() {
  CradleTrialAppTheme {
    Surface {
      Column(Modifier.padding(16.dp)) {
        CheckInListItem(
          checkIn = LocationCheckIn(
            isUploaded = false,
            timestamp = UnixTimestamp.now().timestamp,
            providerName = "gps",
            accuracy = 12.207414365634316,
            latitude = -50.12345678,
            longitude = -150.1234567
          ),
          isDeleteEnabled = false,
          onDeletePressed = {}
        )
      }
    }
  }
}

