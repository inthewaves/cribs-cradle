package org.welbodipartnership.cradle5.patients.form.otherinfo

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.patients.form.OtherCard
import org.welbodipartnership.cradle5.patients.form.SaveButtonCard

@Composable
fun PatientOtherInfoFormScreen(
  onNavigateBack: () -> Unit,
  viewModel: PatientOtherInfoFormViewModel = hiltViewModel()
) {
  var errorDialogText: String? by rememberSaveable { mutableStateOf(null) }
  errorDialogText?.let { currentError ->
    if (!currentError.isNullOrBlank()) {
      AlertDialog(
        onDismissRequest = { errorDialogText = null },
        title = { Text(stringResource(id = R.string.error_dialog_title)) },
        text = { Text(currentError) },
        confirmButton = {
          TextButton(onClick = { errorDialogText = null }) {
            Text(stringResource(id = android.R.string.ok))
          }
        }
      )
    }
  }

  val formState by viewModel.formState.collectAsState()
  LaunchedEffect(formState) {
    formState.let { currentState ->
      when (currentState) {
        is PatientOtherInfoFormViewModel.FormState.Error -> {
          errorDialogText = currentState.errorMessage
        }
        PatientOtherInfoFormViewModel.FormState.SaveSuccess -> {
          onNavigateBack()
        }
        PatientOtherInfoFormViewModel.FormState.Loaded,
        PatientOtherInfoFormViewModel.FormState.Loading,
        PatientOtherInfoFormViewModel.FormState.Saving -> {}
      }
    }
  }

  // TODO: refactor
  var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }
  BackHandler { showUnsavedChangesDialog = true }
  if (showUnsavedChangesDialog) {
    AlertDialog(
      onDismissRequest = { showUnsavedChangesDialog = false },
      title = { Text(stringResource(id = R.string.discard_unsaved_changes_dialog_title)) },
      confirmButton = {
        TextButton(
          onClick = {
            showUnsavedChangesDialog = false
            onNavigateBack()
          }
        ) { Text(stringResource(id = R.string.discard)) }
      },
      dismissButton = {
        TextButton(onClick = { showUnsavedChangesDialog = false }) {
          Text(stringResource(id = R.string.cancel))
        }
      }
    )
  }

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
        title = {
          Column {
            Text(stringResource(R.string.edit_patient_other_info_title))
            val patientInitials by viewModel.patientInitials.collectAsState()
            patientInitials?.let { initials ->
              Text(
                initials,
                style = MaterialTheme.typography.subtitle2
              )
            }
          }
        },
        navigationIcon = {
          val backPressedDispatcher = requireNotNull(
            LocalOnBackPressedDispatcherOwner.current!!, { "failed to get a back pressed dispatcher" }
          ).onBackPressedDispatcher
          IconButton(onClick = backPressedDispatcher::onBackPressed) {
            Icon(
              imageVector = Icons.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back_button)
            )
          }
        },
      )
    },
  ) { padding ->
    LazyColumn(
      contentPadding = padding,
      modifier = Modifier.navigationBarsWithImePadding()
    ) {
      item {
        val (isDraft, setIsDraft) = viewModel.isDraftState
        val (localNotes, setLocalNotes) = viewModel.localNotesState
        val isUploadedToServer by viewModel.isUploadedToServer.collectAsState()
        OtherCard(
          hideDraft = isUploadedToServer,
          isDraft = isDraft,
          onIsDraftChange = setIsDraft,
          localNotes = localNotes,
          onLocalNotesChange = setLocalNotes,
          modifier = Modifier.padding(16.dp)
        )
      }

      item {
        SaveButtonCard(
          onSaveButtonClick = viewModel::submit,
          text = stringResource(R.string.save_button),
        )
      }
    }
  }
}
