package org.welbodipartnership.cradle5.home

import androidx.activity.compose.BackHandler
import androidx.compose.material.BottomDrawerState
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.welbodipartnership.cradle5.R

@Composable
fun AccountInfoButton(
  modifier: Modifier = Modifier,
  scope: CoroutineScope = rememberCoroutineScope()
) {
  val homeManager = LocalHomeManager.current
  BackHandler(enabled = homeManager.bottomDrawerState.isOpen) {
    scope.launch {
      homeManager.bottomDrawerState.close()
    }
  }
  IconButton(
    onClick = {
      scope.launch { homeManager.toggleBottomSheet() }
    },
    modifier
  ) {
    Icon(
      Icons.Filled.AccountCircle,
      contentDescription = stringResource(R.string.home_nav_bar_account_info_button_cd)
    )
  }
}

@Stable
class HomeManager(initialValue: BottomDrawerValue) {
  val bottomDrawerState = BottomDrawerState(initialValue)

  suspend fun toggleBottomSheet() {
    if (bottomDrawerState.isClosed) {
      bottomDrawerState.open()
    } else if (bottomDrawerState.isOpen) {
      bottomDrawerState.close()
    }
  }

  companion object {
    fun Saver() = Saver<HomeManager, BottomDrawerValue>(
      save = { it.bottomDrawerState.currentValue },
      restore = { HomeManager(initialValue = it) }
    )
  }
}

val LocalHomeManager: ProvidableCompositionLocal<HomeManager> =
  compositionLocalOf {
    error("needs to be provided by root")
  }