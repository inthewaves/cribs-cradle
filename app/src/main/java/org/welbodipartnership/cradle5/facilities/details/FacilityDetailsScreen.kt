package org.welbodipartnership.cradle5.facilities.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.Facility

@Composable
fun FacilityDetailsScreen(
  onBackPressed: () -> Unit,
  onFacilityOtherInfoEditPress: (facilityPrimaryKey: Long) -> Unit,
  viewModel: FacilityDetailsViewModel = hiltViewModel()
) {
  Scaffold(
    topBar = {
      TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        contentPadding = WindowInsets.statusBars
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
        title = { Text(text = stringResource(R.string.facility_details_title)) },
      )
    }
  ) { padding ->
    val state by viewModel.facilityStateFlow.collectAsState()

    state.let { facilityState ->
      when (facilityState) {
        is FacilityDetailsViewModel.State.Ready -> {
          FacilityDetailsScreen(
            facilityState.facility,
            onFacilityOtherInfoEditPress = onFacilityOtherInfoEditPress,
            contentPadding = padding
          )
        }
        FacilityDetailsViewModel.State.Failed -> {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("Failed to load facility")
          }
        }
        FacilityDetailsViewModel.State.Loading -> {
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
private fun FacilityDetailsScreen(
  facility: Facility,
  onFacilityOtherInfoEditPress: (facilityPrimaryKey: Long) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues()
) {
  LazyColumn(modifier = modifier, contentPadding = contentPadding) {
    item {
      OtherFacilityInfoCard(
        hasVisited = facility.hasVisited,
        localNotes = facility.localNotes,
        onEditOtherInfoButtonClick = { onFacilityOtherInfoEditPress(facility.id) },
        modifier = Modifier.padding(16.dp)
      )
    }

    item { Spacer(Modifier.height(8.dp)) }

    item {
      FacilityCard(facility = facility, modifier = Modifier.padding(16.dp))
    }
  }
}
