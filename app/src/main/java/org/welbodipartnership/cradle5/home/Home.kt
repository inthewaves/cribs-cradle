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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.BottomNavigation
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.welbodipartnership.cradle5.LoggedInNavigation
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.Screen

@Composable
fun Home() {
  val navController = rememberAnimatedNavController()
  Scaffold(
    bottomBar = {
      /*
      TODO: Hide bottom bar on screens that don't need it
      val showBottomBar = navController.currentBackStackEntryAsState()
        .value
        ?.destination?.

       */

      val currentSelectedItem by navController.currentScreenAsState()
      HomeBottomNavigation(
        selectedNavigation = currentSelectedItem,
        onNavigationSelected = { selected ->
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
        },
        modifier = Modifier.fillMaxWidth()
      )
    }
  ) { innerPadding ->
    LoggedInNavigation(
      navController,
      modifier = Modifier.padding(innerPadding).fillMaxHeight()
    )
  }
}

@Stable
@Composable
private fun NavController.currentScreenAsState(): State<Screen> {
  val selectedItem = remember { mutableStateOf(Screen.defaultStartRoute) }

  DisposableEffect(this) {
    val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
      destination.hierarchy
        .mapNotNull { Screen.routesToScreenMap[it.route] }
        .firstOrNull()
        ?.let { selectedItem.value = it }
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
