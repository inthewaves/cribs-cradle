package org.welbodipartnership.cradle5.cradleform.details

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.cradleform.CradleFormPreviewClasses
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
import org.welbodipartnership.cradle5.data.database.resultentities.CradleTrainingFormFacilityDistrict
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.ui.composables.AnimatedVisibilityFadingWrapper
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

@Composable
fun CradleFormDetailsScreen(
  onBackPressed: () -> Unit,
  onEdit: (primaryKey: Long) -> Unit,
  onOtherInfoEditPress: (primaryKey: Long) -> Unit,
  viewModel: CradleFormDetailsViewModel = hiltViewModel()
) {
  val state: CradleFormDetailsViewModel.State by viewModel.formDetailsStateFlow.collectAsState()
  Scaffold(
    topBar = {
      TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        contentPadding = WindowInsets.systemBars
          .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
          .asPaddingValues(),
        navigationIcon = {
          IconButton(onClick = onBackPressed) {
            Icon(
              imageVector = Icons.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back_button)
            )
          }
        },
        modifier = Modifier.fillMaxWidth(),
        title = {
          Column {
            Text(text = stringResource(R.string.cradle_form_details_title))
            (state as? CradleFormDetailsViewModel.State.Ready)
              ?.formFacilityDistrict
              ?.facility
              ?.name
              ?.let { facilityName ->
                Text(facilityName, style = MaterialTheme.typography.subtitle2)
              }
          }
        },
      )
    }
  ) { padding ->
    val editState by viewModel.editStateFlow.collectAsState()

    LaunchedEffect(state) {
      Log.d("CradleFormDetailsScreen", "new state $state")
    }
    state.let { formState ->
      when (formState) {
        is CradleFormDetailsViewModel.State.Ready -> {
          CradleFormDetailsScreen(
            formState.formFacilityDistrict,
            editState = editState,
            onFormEditPress = onEdit,
            onOtherInfoEditPress = onOtherInfoEditPress,
            onPatientDeletePress = {
              viewModel.deleteForm(formState.formFacilityDistrict.form)
              onBackPressed()
            },
            contentPadding = padding
          )
        }
        CradleFormDetailsViewModel.State.Failed -> {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("Failed to load form")
          }
        }
        CradleFormDetailsViewModel.State.Loading -> {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator()
          }
        }
      }
    }
  }
}

@Composable
private fun CradleFormDetailsScreen(
  formAndRelatedInfo: CradleTrainingFormFacilityDistrict,
  editState: SyncRepository.FormEditState?,
  onFormEditPress: (patientPrimaryKey: Long) -> Unit,
  onOtherInfoEditPress: (patientPrimaryKey: Long) -> Unit,
  onPatientDeletePress: (patientPrimaryKey: Long) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues()
) {
  val form: CradleTrainingForm = formAndRelatedInfo.form

  var isDeleteConfirmDialogShowing by rememberSaveable { mutableStateOf(false) }
  if (isDeleteConfirmDialogShowing) {
    AlertDialog(
      onDismissRequest = { isDeleteConfirmDialogShowing = false },
      title = { Text(stringResource(id = R.string.delete_info_dialog_title)) },
      text = { Text(stringResource(id = R.string.delete_info_dialog_body)) },
      confirmButton = {
        TextButton(
          onClick = { onPatientDeletePress(form.id) },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error)
        ) {
          Text(stringResource(id = R.string.delete_info_dialog_confirm_button))
        }
      },
      dismissButton = {
        TextButton(onClick = { isDeleteConfirmDialogShowing = false }) {
          Text(stringResource(id = R.string.cancel))
        }
      }
    )
  }

  LazyColumn(modifier = modifier, contentPadding = contentPadding) {
    item {
      BaseDetailsCard(title = null, modifier = modifier) {
        // Don't allow editing forms already uploaded.

        val canEdit = !form.isUploadedToServer && editState?.canEdit == true
        OutlinedButton(
          enabled = canEdit,
          onClick = { onFormEditPress(form.id) }
        ) {
          Text(stringResource(R.string.cradle_form_details_screen_edit_button))
        }

        OutlinedButton(
          enabled = canEdit,
          onClick = { isDeleteConfirmDialogShowing = true },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error),
        ) {
          Text(stringResource(R.string.delete_info_button))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          val icon: ImageVector
          val contentDescription: String
          val text: String
          val isError: Boolean
          when {
            form.isUploadedToServer -> {
              icon = Icons.Default.Lock
              contentDescription = "Patient locked"
              text = "Form has been uploaded to MedSciNet and is locked for editing on the app"
              isError = false
            }
            form.isDraft -> {
              icon = Icons.Outlined.Edit
              contentDescription = "Patient marked as draft"
              text = "Form is marked as draft and won't be included in the next sync"
              isError = false
            }
            else -> {
              icon = Icons.Default.LockOpen
              contentDescription = "Patient ready for upload"
              text = "Form is ready for upload"
              isError = false
            }
          }

          val color = if (isError) MaterialTheme.colors.error else LocalContentColor.current
          CompositionLocalProvider(LocalContentColor provides color) {
            Icon(imageVector = icon, contentDescription = contentDescription)
            Spacer(Modifier.width(4.dp))
            Text(text)
          }
        }

        AnimatedVisibilityFadingWrapper(
          visible = editState?.canEdit == false && !form.isUploadedToServer
        ) {
          Spacer(Modifier.width(4.dp))
          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            val text = when (editState) {
              SyncRepository.FormEditState.CANT_EDIT_SYNC_ENQUEUED -> {
                "Unable to make edits when sync is enqueued"
              }
              SyncRepository.FormEditState.CANT_EDIT_SYNC_IN_PROGRESS -> {
                "Unable to make edits while sync is running"
              }
              SyncRepository.FormEditState.CAN_EDIT, null -> ""
            }
            Text(text = text)
          }
        }
      }
    }

    item { Spacer(Modifier.height(8.dp)) }

    item {
      OtherInfoCard(
        isUploadedToServer = form.isUploadedToServer,
        isDraft = form.isDraft,
        localNotes = form.localNotes,
        onEditOtherInfoButtonClick = { onOtherInfoEditPress(form.id) },
        modifier = Modifier.padding(16.dp)
      )
    }

    item { Spacer(Modifier.height(8.dp)) }

    item { CradleFormCard(formAndRelatedInfo, modifier = Modifier.padding(16.dp)) }
  }
}

@Preview
@Composable
fun PatientDetailsScreenNotUploadedPreview() {
  CradleTrialAppTheme {
    Surface {
      CradleFormDetailsScreen(
        CradleTrainingFormFacilityDistrict(
          CradleFormPreviewClasses.createTestCradleForm(),
          CradleFormPreviewClasses.createTestFacility(),
          CradleFormPreviewClasses.createTestDistrict(),
        ),
        editState = SyncRepository.FormEditState.CAN_EDIT,
        onFormEditPress = {},
        onOtherInfoEditPress = {},
        onPatientDeletePress = {}
      )
    }
  }
}

@Preview
@Composable
fun PatientDetailsScreenUploadedPreview() {
  CradleTrialAppTheme {
    Surface {
      CradleFormDetailsScreen(
        CradleTrainingFormFacilityDistrict(
          CradleFormPreviewClasses.createTestCradleForm(serverInfo = ServerInfo(nodeId = null, 5L, null, null)),
          CradleFormPreviewClasses.createTestFacility(),
          CradleFormPreviewClasses.createTestDistrict(),
        ),
        editState = SyncRepository.FormEditState.CAN_EDIT,
        onFormEditPress = {},
        onOtherInfoEditPress = {},
        onPatientDeletePress = {}
      )
    }
  }
}

@Preview
@Composable
fun PatientDetailsScreenSyncingPreview() {
  CradleTrialAppTheme {
    Surface {
      CradleFormDetailsScreen(
        CradleTrainingFormFacilityDistrict(
          CradleFormPreviewClasses.createTestCradleForm(),
          CradleFormPreviewClasses.createTestFacility(),
          CradleFormPreviewClasses.createTestDistrict(),
        ),
        editState = SyncRepository.FormEditState.CANT_EDIT_SYNC_IN_PROGRESS,
        onFormEditPress = {},
        onOtherInfoEditPress = {},
        onPatientDeletePress = {},
      )
    }
  }
}
