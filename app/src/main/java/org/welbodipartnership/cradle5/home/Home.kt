/*
 * Parts of this file are derived from Tivi (https://github.com/chrisbanes/tivi). License:
 *
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.welbodipartnership.cradle5.home

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.BottomDrawer
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.BottomNavigation
import org.welbodipartnership.cradle5.LeafScreen
import org.welbodipartnership.cradle5.LoggedInNavigation
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.Screen
import org.welbodipartnership.cradle5.domain.auth.AuthState

@Composable
fun LoggedInHome(
  navController: NavHostController,
  authState: AuthState.LoggedInUnlocked,
  districtName: String?,
  onLogout: () -> Unit,
  onLock: () -> Unit,
  modifier: Modifier = Modifier,
  homeViewModel: HomeViewModel = hiltViewModel(),
) {
  val homeManager = rememberSaveable(saver = HomeManager.Saver()) {
    HomeManager(BottomDrawerValue.Closed)
  }
  val drawerState = homeManager.bottomDrawerState
  var showLogoutConfirmDialog by rememberSaveable { mutableStateOf(false) }
  if (showLogoutConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirmDialog = false },
      title = { Text(stringResource(id = R.string.logout_dialog_title)) },
      text = { Text(stringResource(id = R.string.logout_dialog_body)) },
      confirmButton = {
        TextButton(
          onClick = onLogout,
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error)
        ) {
          Text(stringResource(id = R.string.logout_dialog_delete_and_logout_button))
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirmDialog = false }) {
          Text(stringResource(id = R.string.cancel))
        }
      }
    )
  }

  BottomDrawer(
    drawerContent = {
      val padding = PaddingValues(bottom = LocalWindowInsets.current.navigationBars.bottom.dp)
      val textPadding = 16.dp
      val interTextPadding = 8.dp
      LazyColumn(contentPadding = padding) {
        item {
          Text(
            stringResource(R.string.bottom_drawer_logged_in_as_s, authState.username),
            modifier = Modifier.padding(
              start = textPadding,
              top = textPadding,
              end = textPadding,
              bottom = interTextPadding
            )
          )
        }
        item {
          Text(
            districtName?.let { stringResource(R.string.district_label_s, districtName) }
              ?: stringResource(R.string.unknown_district),
            modifier = Modifier.padding(
              start = textPadding,
              top = 0.dp,
              end = textPadding,
              bottom = textPadding
            )
          )
        }
        item { Divider() }
        item {
          ListItem(
            text = {
              Text(stringResource(R.string.lock_app_button))
            },
            secondaryText = {
              if (!drawerState.isClosed || drawerState.isAnimationRunning) {
                val lockAppButtonText by homeViewModel.lockAppButtonSubtitleTextWithTimeLeftFlow
                  .collectAsState()
                Text(lockAppButtonText)
              }
            },
            icon = {
              Icon(
                Icons.Default.Lock,
                contentDescription = stringResource(R.string.lock_app_button)
              )
            },
            modifier = Modifier.clickable(onClick = onLock)
          )
        }

        item {
          CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.error) {
            ListItem(
              text = { Text(stringResource(R.string.bottom_sheet_logout_button)) },
              icon = {
                Icon(
                  Icons.Default.Logout,
                  contentDescription = stringResource(R.string.bottom_sheet_logout_button)
                )
              },
              // TODO: add confirmation
              modifier = Modifier.clickable(onClick = { showLogoutConfirmDialog = true })
            )
          }
        }
      }
    },
    drawerState = drawerState,
    gesturesEnabled = drawerState.isOpen,
  ) {
    CompositionLocalProvider(LocalHomeManager provides homeManager) {
      Scaffold(
        bottomBar = {
          val currentSelectedItemPair by navController.currentScreenAndLeafAsState()
          val (screen, leaf) = currentSelectedItemPair
          val isVisible = leaf?.hideBottomBar == null || !leaf.hideBottomBar
          /*
          AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + expandIn(animationSpec = tween(), expandFrom = Alignment.BottomCenter),
            exit = shrinkOut(animationSpec = tween(), shrinkTowards = Alignment.BottomCenter) + fadeOut(),
          ) {

           */
          if (isVisible) {
            HomeBottomNavigation(
              selectedNavigation = screen,
              onNavigationSelected = { selected ->
                if (isVisible) {
                  navController.navigate(selected.route) {
                    // https://developer.android.com/jetpack/compose/navigation#bottom-nav
                    // Avoid multiple copies of the same destination when reselecting the same item
                    launchSingleTop = true
                    // Restore state when reselecting a previously selected item
                    restoreState = true
                    // Pop up to the start destination of the graph to avoid building up a large stack of
                    // destinations on the back stack as users select items
                    popUpTo(navController.graph.findStartDestination().id) {
                      saveState = true
                    }
                  }
                } else {
                  Log.w("Home", "trying to navigate to $selected but not visible")
                }
              },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
      ) {
        LoggedInNavigation(
          navController,
          modifier = Modifier
            .padding(it)
            .fillMaxHeight()
        )
      }
    }
  }
}

@Stable
@Composable
private fun NavController.currentScreenAndLeafAsState(): State<Pair<Screen, LeafScreen?>> {
  val selectedItem: MutableState<Pair<Screen, LeafScreen?>> =
    remember { mutableStateOf(Screen.defaultStartRoute to Screen.defaultStartRoute.startLeaf) }

  DisposableEffect(this) {
    val listener = NavController.OnDestinationChangedListener { controller, destination, _ ->
      val screen: Screen = destination.hierarchy
        .mapNotNull { dest ->
          Screen.values().find { it.route == dest.route }
        }
        .firstOrNull()
        ?: return@OnDestinationChangedListener

      val leaf: LeafScreen? = try {
        destination.route?.let { LeafScreen.matchRouteOrThrow(it) }
      } catch (e: IllegalArgumentException) {
        Log.e("Home", "Failed to match route ${destination.route}", e)
        null
      }

      selectedItem.value = Pair(screen, leaf)
    }
    addOnDestinationChangedListener(listener)

    onDispose {
      removeOnDestinationChangedListener(listener)
    }
  }

  return selectedItem
}

/*
@Stable
@Composable
private fun NavController.currentScreenAsState(): State<Screen?> {
  val navBackStackEntry by currentBackStackEntryAsState()
  val selectedItem = remember {
    derivedStateOf {
      navBackStackEntry?.destination?.hierarchy
        ?.mapNotNull { Screen.routesToScreenMap[it.route] }
        ?.firstOrNull()
        ?: Screen.default
    }
  }
  return selectedItem
}

 */

@Composable
internal fun HomeBottomNavigation(
  selectedNavigation: Screen,
  onNavigationSelected: (Screen) -> Unit,
  modifier: Modifier = Modifier,
) {
  BottomNavigation(
    backgroundColor = MaterialTheme.colors.surface,
    contentColor = contentColorFor(MaterialTheme.colors.surface),
    contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars),
    modifier = modifier
  ) {
    HomeNavigationItems.forEach { item ->
      BottomNavigationItem(
        icon = {
          HomeNavigationItemIcon(
            item = item,
            selected = selectedNavigation == item.screen
          )
        },
        label = { Text(text = stringResource(item.labelResId)) },
        selected = selectedNavigation == item.screen,
        onClick = { onNavigationSelected(item.screen) },
      )
    }
  }
}

@Composable
private fun HomeNavigationItemIcon(item: HomeNavigationItem, selected: Boolean) {
  val painter = when (item) {
    is HomeNavigationItem.ResourceIcon -> painterResource(item.iconResId)
    is HomeNavigationItem.ImageVectorIcon -> rememberVectorPainter(item.iconImageVector)
  }
  val selectedPainter = when (item) {
    is HomeNavigationItem.ResourceIcon -> item.selectedIconResId?.let { painterResource(it) }
    is HomeNavigationItem.ImageVectorIcon -> item.selectedImageVector?.let { rememberVectorPainter(it) }
  }

  if (selectedPainter != null) {
    Crossfade(targetState = selected) {
      Icon(
        painter = if (it) selectedPainter else painter,
        contentDescription = stringResource(item.contentDescriptionResId),
      )
    }
  } else {
    Icon(
      painter = painter,
      contentDescription = stringResource(item.contentDescriptionResId),
    )
  }
}

private sealed class HomeNavigationItem(
  val screen: Screen,
  @StringRes val labelResId: Int,
  @StringRes val contentDescriptionResId: Int,
) {
  class ResourceIcon(
    screen: Screen,
    @StringRes labelResId: Int,
    @StringRes contentDescriptionResId: Int,
    @DrawableRes val iconResId: Int,
    @DrawableRes val selectedIconResId: Int? = null,
  ) : HomeNavigationItem(screen, labelResId, contentDescriptionResId)

  class ImageVectorIcon(
    screen: Screen,
    @StringRes labelResId: Int,
    @StringRes contentDescriptionResId: Int,
    val iconImageVector: ImageVector,
    val selectedImageVector: ImageVector? = null,
  ) : HomeNavigationItem(screen, labelResId, contentDescriptionResId)
}

private val HomeNavigationItems = listOf(
  HomeNavigationItem.ImageVectorIcon(
    screen = Screen.Patients,
    labelResId = R.string.patients_title,
    contentDescriptionResId = R.string.cd_patients_nav_button,
    iconImageVector = Icons.Outlined.Groups,
    selectedImageVector = Icons.Default.Groups,
  ),
  HomeNavigationItem.ImageVectorIcon(
    screen = Screen.Sync,
    labelResId = R.string.sync_title,
    contentDescriptionResId = R.string.cd_sync_nav_button,
    iconImageVector = Icons.Outlined.Sync,
    selectedImageVector = Icons.Default.Sync,
  ),
  HomeNavigationItem.ImageVectorIcon(
    screen = Screen.Facilities,
    labelResId = R.string.facilities_title,
    contentDescriptionResId = R.string.cd_facilities_nav_button,
    iconImageVector = Icons.Outlined.LocationCity,
    selectedImageVector = Icons.Default.LocationCity,
  ),
)
