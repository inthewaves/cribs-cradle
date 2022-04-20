package org.welbodipartnership.cradle5.ui.composables.forms

import android.os.Parcelable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import kotlinx.coroutines.flow.Flow
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.ui.composables.screenlists.ScreenListItem

@Composable
fun <DatabaseType : Parcelable> DatabasePagingListDropdown(
  selectedItem: DatabaseType?,
  positionInList: Int?,
  onItemSelected: (index: Int, item: DatabaseType) -> Unit,
  pagingItemFlow: Flow<PagingData<DatabaseType>>,
  formatTextForListItem: (DatabaseType) -> String?,
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  label: @Composable (() -> Unit)? = null,
  errorHint: String? = null,
) {
  var showDialog by rememberSaveable { mutableStateOf(false) }
  if (showDialog) {
    val lazyItems = pagingItemFlow.collectAsLazyPagingItems()

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = positionInList ?: 0)
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
              title()
            }
          }

          Spacer(Modifier.height(18.dp))

          LazyColumn(modifier = Modifier.fillMaxWidth(), listState) {
            itemsIndexed(lazyItems) { idx, item ->
              if (item != null) {
                val isSelected = item == selectedItem
                ScreenListItem(
                  modifier = if (isSelected) {
                    Modifier.background(MaterialTheme.colors.surface.copy(0.24f))
                  } else {
                    Modifier
                  },
                  minHeight = 32.dp,
                  onClick = {
                    onItemSelected(idx, item)
                    showDialog = false
                  }
                ) {
                  Text(
                    formatTextForListItem(item) ?: stringResource(R.string.unknown),
                    fontWeight = if (isSelected) FontWeight.ExtraBold else null,
                  )
                }
              } else {
                ScreenListItem(
                  minHeight = 32.dp,
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
    value = selectedItem?.let { formatTextForListItem(it) } ?: "",
    onValueChange = {},
    label = label,
    maxLines = 2,
    enabled = enabled,
    trailingIcon = {
      ExposedDropdownMenuDefaults.TrailingIcon(
        expanded = enabled && showDialog,
        onIconClick = { showDialog = true }
      )
    },
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
