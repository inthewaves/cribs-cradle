package org.welbodipartnership.cradle5.cradleform.form.otherinfo

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.cradleform.form.OtherCard
import org.welbodipartnership.cradle5.cradleform.form.SaveButtonCard

@Composable
fun CradleOtherInfoFormScreen(
  onNavigateBack: () -> Unit,
  viewModel: CradleOtherInfoFormViewModel = hiltViewModel()
) {
  var errorDialogText: String? by rememberSaveable { mutableStateOf(null) }
  errorDialogText?.let { currentError ->
    if (currentError.isNotBlank()) {
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
        is CradleOtherInfoFormViewModel.FormState.Error -> {
          errorDialogText = currentState.errorMessage
        }
        CradleOtherInfoFormViewModel.FormState.SaveSuccess -> {
          onNavigateBack()
        }
        CradleOtherInfoFormViewModel.FormState.Loaded,
        CradleOtherInfoFormViewModel.FormState.Loading,
        CradleOtherInfoFormViewModel.FormState.Saving -> {}
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
        contentPadding = WindowInsets.systemBars
          .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
          .asPaddingValues(),
        modifier = Modifier.fillMaxWidth(),
        title = {
          Column {
            Text(stringResource(R.string.edit_cradle_form_other_info_title))
            val formInfo by viewModel.existingInfo.collectAsState()
            formInfo?.facility?.name?.let { facilityName ->
              Text(facilityName, style = MaterialTheme.typography.subtitle2)
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
      modifier = Modifier.navigationBarsPadding().imePadding()
    ) {
      item {
        val (localNotes, setLocalNotes) = viewModel.localNotesState
        OtherCard(
          hideDraft = true,
          isDraft = null,
          onIsDraftChange = {},
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
