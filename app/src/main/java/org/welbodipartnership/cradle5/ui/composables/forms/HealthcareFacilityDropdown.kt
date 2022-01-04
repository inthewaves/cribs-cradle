package org.welbodipartnership.cradle5.ui.composables.forms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.ui.composables.screenlists.ScreenListItem

@Composable
fun HealthcareFacilityDropdown(
  facility: Facility?,
  onFacilitySelected: (Facility) -> Unit,
  facilityPagingItemsFlow: Flow<PagingData<Facility>>,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  label: @Composable (() -> Unit)? = null,
  errorHint: String?,
) {
  var showDialog by remember { mutableStateOf(false) }
  if (showDialog) {
    val lazyItems = facilityPagingItemsFlow.collectAsLazyPagingItems()

    Dialog(onDismissRequest = { showDialog = false }) {
      Surface(
        modifier = Modifier
          .width((LocalConfiguration.current.screenWidthDp * 0.8).dp)
          .height((LocalConfiguration.current.screenHeightDp * 0.8).dp),
        shape = MaterialTheme.shapes.medium
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 18.dp)
        ) {
          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
            Box(Modifier.align(Alignment.Start)) {
              Text(
                stringResource(R.string.patient_registration_health_facility_dialog_title),
                style = MaterialTheme.typography.subtitle1
              )
            }
          }

          Spacer(Modifier.height(18.dp))

          LazyColumn(modifier = Modifier.fillMaxWidth(),) {
            items(lazyItems) { fac ->
              if (fac != null) {
                ScreenListItem(
                  minHeight = 24.dp,
                  onClick = {
                    onFacilitySelected(fac)
                    showDialog = false
                  }
                ) {
                  Text(fac.name ?: stringResource(R.string.unknown))
                }
              } else {
                ScreenListItem(
                  minHeight = 24.dp,
                  onClick = null,
                  horizontalArrangement = Arrangement.Center
                ) {
                  CircularProgressIndicator()
                }
              }
            }
          }
        }
      }
    }
  }

  OutlinedTextFieldWithErrorHint(
    readOnly = true,
    value = facility?.name ?: "",
    onValueChange = {},
    label = label,
    maxLines = 2,
    enabled = enabled,
    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = enabled && showDialog) },
    errorHint = errorHint,
    modifier = modifier,
    textFieldModifier = textFieldModifier,
    interactionSource = remember { MutableInteractionSource() }
      .also { interactionSource ->
        if (enabled) {
          LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect {
              if (it is PressInteraction.Release) {
                showDialog = true
              }
            }
          }
        }
      }
  )
}
