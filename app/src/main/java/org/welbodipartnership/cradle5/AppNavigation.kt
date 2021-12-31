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

package org.welbodipartnership.cradle5

import android.util.Log
import androidx.collection.ArrayMap
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation
import org.welbodipartnership.cradle5.facilities.FacilitiesList
import org.welbodipartnership.cradle5.patients.list.PatientsList

internal sealed class Screen(val route: String) {
  object Patients : Screen("patients")
  object Sync : Screen("sync")
  object Facilities : Screen("facilities")

  // The lazy is needed to prevent nulls (https://youtrack.jetbrains.com/issue/KT-25957)
  companion object {
    val defaultStartRoute: Screen by lazy { Patients }

    val routesToScreenMap: Map<String, Screen> by lazy {
      Screen::class.sealedSubclasses
        .asSequence()
        .filterNotNull()
        .mapNotNull { it.objectInstance }
        .associateByTo(ArrayMap(Screen::class.sealedSubclasses.size)) { it.route }
    }
  }
}

private sealed class LeafScreen(private val route: String,) {

  fun createRoute(root: Screen) = "${root.route}/$route"

  object Patients : LeafScreen("patients")
  object PatientDetails : LeafScreen("patients/view/{patientPk}") {
    fun createRoute(root: Screen, patientPrimaryKey: Long): String {
      return "${root.route}/patients/view/$patientPrimaryKey"
    }
  }
  object PatientCreate : LeafScreen("patients/create")
  object PatientEdit : LeafScreen("patients/edit/{patientPk}") {
    fun createRoute(root: Screen, existingPatientPrimaryKey: Long): String {
      return "${root.route}/patients/edit/$existingPatientPrimaryKey"
    }
  }

  object Facilities : LeafScreen("facilities")

  companion object {
    // val bottomBarRoutes = setOf()
  }
}

@Composable
internal fun LoggedInNavigation(
  navController: NavHostController,
  modifier: Modifier = Modifier,
) {
  AnimatedNavHost(
    navController = navController,
    startDestination = Screen.defaultStartRoute.route,
    enterTransition = { defaultEnterTransition(initialState, targetState) },
    exitTransition = { defaultExitTransition(initialState, targetState) },
    popEnterTransition = { defaultPopEnterTransition() },
    popExitTransition = { defaultPopExitTransition() },
    modifier = modifier,
  ) {
    addPatientsTopLevel(navController)
    addFacilitiesTopLevel(navController)
  }
}

private fun NavGraphBuilder.addPatientsTopLevel(
  navController: NavController,
) {
  navigation(
    route = Screen.Patients.route,
    startDestination = LeafScreen.Patients.createRoute(Screen.Patients),
  ) {
    addPatientsList(navController, Screen.Patients)
    addPatientDetails(navController, Screen.Patients)
  }
}

private fun NavGraphBuilder.addPatientsList(
  navController: NavController,
  root: Screen,
) {
  composable(route = LeafScreen.Patients.createRoute(root)) {
    LaunchedEffect(null) {
      Log.d("PatientsList", "Route is ${navController.currentBackStackEntry?.destination}")
    }

    PatientsList(
      onOpenNewPatientCreation = {
      },
      onOpenPatientDetails = {
      }
    )
  }
}

private fun NavGraphBuilder.addPatientDetails(
  navController: NavController,
  root: Screen,
) {
  /*
  composable(
    route = LeafScreen.PatientView.createRoute(root),
    arguments = listOf(
      navArgument("patientPk") { type = NavType.LongType }
    )
  ) {

  }

   */
}

private fun NavGraphBuilder.addFacilitiesTopLevel(
  navController: NavController,
) {
  navigation(
    route = Screen.Facilities.route,
    startDestination = LeafScreen.Facilities.createRoute(Screen.Facilities),
  ) {
    addFacilitiesList(navController, Screen.Facilities)
  }
}

private fun NavGraphBuilder.addFacilitiesList(
  navController: NavController,
  root: Screen,
) {
  composable(route = LeafScreen.Facilities.createRoute(root)) {
    FacilitiesList()
  }
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultEnterTransition(
  initial: NavBackStackEntry,
  target: NavBackStackEntry,
): EnterTransition {
  val initialNavGraph = initial.destination.hostNavGraph
  val targetNavGraph = target.destination.hostNavGraph
  // If we're crossing nav graphs (bottom navigation graphs), we crossfade
  if (initialNavGraph.id != targetNavGraph.id) {
    return fadeIn()
  }
  // Otherwise we're in the same nav graph, we can imply a direction
  return fadeIn() + slideIntoContainer(AnimatedContentScope.SlideDirection.Start)
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultExitTransition(
  initial: NavBackStackEntry,
  target: NavBackStackEntry,
): ExitTransition {
  val initialNavGraph = initial.destination.hostNavGraph
  val targetNavGraph = target.destination.hostNavGraph
  // If we're crossing nav graphs (bottom navigation graphs), we crossfade
  if (initialNavGraph.id != targetNavGraph.id) {
    return fadeOut()
  }
  // Otherwise we're in the same nav graph, we can imply a direction
  return fadeOut() + slideOutOfContainer(AnimatedContentScope.SlideDirection.Start)
}

private val NavDestination.hostNavGraph: NavGraph
  get() = hierarchy.first { it is NavGraph } as NavGraph

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultPopEnterTransition(): EnterTransition {
  return fadeIn() + slideIntoContainer(AnimatedContentScope.SlideDirection.End)
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultPopExitTransition(): ExitTransition {
  return fadeOut() + slideOutOfContainer(AnimatedContentScope.SlideDirection.End)
}
