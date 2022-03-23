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

import androidx.annotation.Keep
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation
import org.welbodipartnership.cradle5.facilities.details.FacilityDetailsScreen
import org.welbodipartnership.cradle5.facilities.list.FacilitiesListScreen
import org.welbodipartnership.cradle5.facilities.otherinfoform.FacilityOtherInfoFormScreen
import org.welbodipartnership.cradle5.locationcheckin.LocationCheckInScreen
import org.welbodipartnership.cradle5.patients.details.PatientDetailsScreen
import org.welbodipartnership.cradle5.patients.form.PatientForm
import org.welbodipartnership.cradle5.patients.form.otherinfo.PatientOtherInfoFormScreen
import org.welbodipartnership.cradle5.patients.list.PatientsListScreen
import org.welbodipartnership.cradle5.sync.SyncScreen

@Keep
internal enum class Screen(val route: String, val startLeaf: LeafScreen) {
  Patients("patients", LeafScreen.Patients),
  Sync("sync", LeafScreen.Sync),
  Location("location", LeafScreen.Location),
  Facilities("facilities", LeafScreen.Facilities);

  // The lazy is needed to prevent nulls (https://youtrack.jetbrains.com/issue/KT-25957)
  companion object {
    val defaultStartRoute: Screen by lazy { Patients }
  }
}

internal sealed class LeafScreen(private val route: String) {
  protected abstract val matchRegex: Regex

  fun doesRouteMatch(route: String): Boolean = matchRegex.matches(route)

  fun createRouteMatcherWithNoArgs() = Regex("^.*$route$")

  fun createRouteMatcherWithOneNumericalArg() = "^.*$route$"
    .replace(Regex("""\{[^}]+\}"""), "[0-9]+")
    .let { Regex(it) }

  fun createRoute(root: Screen) = "${root.route}/$route"

  object Patients : LeafScreen("patients") {
    override val matchRegex = createRouteMatcherWithNoArgs()
  }

  object PatientDetails : LeafScreen("patients/view/{patientPk}") {
    const val ARG_PATIENT_PRIMARY_KEY = "patientPk"
    override val matchRegex = createRouteMatcherWithOneNumericalArg()
    fun createRoute(root: Screen, patientPrimaryKey: Long): String {
      return "${root.route}/patients/view/$patientPrimaryKey"
    }
  }
  object PatientCreate : LeafScreen("patients/create") {
    override val matchRegex = createRouteMatcherWithNoArgs()
  }
  object PatientEdit : LeafScreen("patients/edit/{patientEditPk}") {
    const val ARG_PATIENT_PRIMARY_KEY = "patientEditPk"
    override val matchRegex = createRouteMatcherWithOneNumericalArg()
    fun createRoute(root: Screen, existingPatientPrimaryKey: Long): String {
      return "${root.route}/patients/edit/$existingPatientPrimaryKey"
    }
  }
  object PatientOtherInfoEdit : LeafScreen(
    "patients/other_info/edit/{patientEditPk}",
  ) {
    const val ARG_PATIENT_PRIMARY_KEY = "patientEditPk"
    override val matchRegex = createRouteMatcherWithOneNumericalArg()
    fun createRoute(root: Screen, patientEditPk: Long): String {
      return "${root.route}/patients/other_info/edit/$patientEditPk"
    }
  }

  object Location : LeafScreen("location") {
    override val matchRegex = createRouteMatcherWithNoArgs()
  }

  object Sync : LeafScreen("sync") {
    override val matchRegex = createRouteMatcherWithNoArgs()
  }

  object Facilities : LeafScreen("facilities") {
    override val matchRegex = createRouteMatcherWithNoArgs()
  }
  object FacilityDetails : LeafScreen("facilities/view/{facilityPk}") {
    const val ARG_FACILITY_PRIMARY_KEY = "facilityPk"
    override val matchRegex = createRouteMatcherWithOneNumericalArg()
    fun createRoute(root: Screen, facilityPrimaryKey: Int): String {
      return "${root.route}/facilities/view/$facilityPrimaryKey"
    }
  }
  object FacilityOtherInfoEdit : LeafScreen(
    "facilities/other_info/edit/{facilityPk}",
  ) {
    const val ARG_FACILITY_PRIMARY_KEY = "facilityPk"
    override val matchRegex = createRouteMatcherWithOneNumericalArg()
    fun createRoute(root: Screen, facilityEditPk: Int): String {
      return "${root.route}/facilities/other_info/edit/$facilityEditPk"
    }
  }

  companion object {
    private val allLeaves by lazy {
      // A really hacky way to get all the sealed classes without needing to use reflection.
      // This will cause a compiler error in 1.7 if a new subtype is added without updating this.
      @Suppress("USELESS_CAST")
      when (Patients as LeafScreen) {
        PatientCreate -> {}
        PatientDetails -> {}
        PatientEdit -> {}
        Patients -> {}
        Location -> {}
        Sync -> {}
        PatientOtherInfoEdit -> {}
        Facilities -> {}
        FacilityDetails -> {}
        FacilityOtherInfoEdit -> {}
      }

      listOf(
        PatientCreate,
        PatientDetails,
        PatientEdit,
        PatientOtherInfoEdit,
        Patients,
        Location,
        Sync,
        Facilities,
        FacilityDetails,
        FacilityOtherInfoEdit
      )
    }

    @Throws(IllegalArgumentException::class)
    fun matchRouteOrThrow(route: String): LeafScreen =
      allLeaves.find { route.endsWith(it.route) || it.doesRouteMatch(route) }
        ?: throw IllegalArgumentException("route $route doesn't match any known LeafScreen!")
  }
}

/**
 * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav event.
 *
 * This is used to de-duplicate navigation events.
 */
private fun NavBackStackEntry.lifecycleIsResumed() =
  this.lifecycle.currentState == Lifecycle.State.RESUMED

private inline fun NavController.withDebouncedAction(
  action: NavController.() -> Unit
) {
  val lifecycleResumed = currentBackStackEntry?.lifecycleIsResumed()
  if (lifecycleResumed == null || lifecycleResumed == true) {
    action()
  }
}

@Composable
internal fun LoggedInNavigation(
  navController: NavHostController,
  onOpenSettingsForApp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedNavHost(
    navController = navController,
    startDestination = Screen.defaultStartRoute.route,
    enterTransition = { defaultEnterTransition() },
    exitTransition = { defaultExitTransition() },
    popEnterTransition = { defaultPopEnterTransition() },
    popExitTransition = { defaultPopExitTransition() },
    modifier = modifier,
  ) {
    addPatientsTopLevel(navController)
    addSyncTopLevel(navController)
    addLocationCheckInTopLevel(navController, onOpenSettingsForApp)
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
    addPatientCreate(navController, Screen.Patients)
    addPatientEdit(navController, Screen.Patients)
    addPatientOtherInfoEdit(navController, Screen.Patients)
  }
}

private fun NavGraphBuilder.addPatientsList(
  navController: NavController,
  root: Screen,
) {
  composable(route = LeafScreen.Patients.createRoute(root)) {
    PatientsListScreen(
      onOpenNewPatientCreation = {
        navController.navigate(LeafScreen.PatientCreate.createRoute(root))
      },
      onOpenPatientDetails = { patientPk ->
        navController.withDebouncedAction {
          navigate(LeafScreen.PatientDetails.createRoute(root, patientPk))
        }
      }
    )
  }
}

private fun NavGraphBuilder.addPatientDetails(
  navController: NavController,
  root: Screen,
) {
  composable(
    route = LeafScreen.PatientDetails.createRoute(root),
    arguments = listOf(
      navArgument(LeafScreen.PatientDetails.ARG_PATIENT_PRIMARY_KEY) { type = NavType.LongType }
    )
  ) {
    PatientDetailsScreen(
      onBackPressed = { navController.navigateUp() },
      onPatientEdit = { patientPrimaryKey ->
        navController.navigate(LeafScreen.PatientEdit.createRoute(root, patientPrimaryKey))
      },
      onPatientOtherInfoEditPress = { patientPrimaryKey ->
        navController.navigate(LeafScreen.PatientOtherInfoEdit.createRoute(root, patientPrimaryKey))
      }
    )
  }
}

private fun NavGraphBuilder.addPatientCreate(
  navController: NavController,
  root: Screen,
) {
  composable(
    route = LeafScreen.PatientCreate.createRoute(root),
  ) {
    // TODO: Use an ambient?
    PatientForm(
      LocalServerEnumCollection.current,
      onNavigateBack = { navController.navigateUp() },
      onNavigateToPatient = { patientPrimaryKey ->
        navController.withDebouncedAction {
          popBackStack()
          navigate(
            LeafScreen.PatientDetails.createRoute(root, patientPrimaryKey)
          )
        }
      }
    )
  }
}

private fun NavGraphBuilder.addPatientEdit(
  navController: NavController,
  root: Screen,
) {
  composable(
    route = LeafScreen.PatientEdit.createRoute(root),
    arguments = listOf(
      navArgument(LeafScreen.PatientEdit.ARG_PATIENT_PRIMARY_KEY) { type = NavType.LongType }
    )
  ) {
    // TODO: Use an ambient?
    PatientForm(
      LocalServerEnumCollection.current,
      onNavigateBack = { navController.navigateUp() },
      onNavigateToPatient = { navController.navigateUp() }
    )
  }
}

private fun NavGraphBuilder.addPatientOtherInfoEdit(
  navController: NavController,
  root: Screen,
) {
  composable(
    route = LeafScreen.PatientOtherInfoEdit.createRoute(root),
    arguments = listOf(
      navArgument(LeafScreen.PatientOtherInfoEdit.ARG_PATIENT_PRIMARY_KEY) {
        type = NavType.LongType
      }
    )
  ) {
    PatientOtherInfoFormScreen(
      onNavigateBack = { navController.navigateUp() },
    )
  }
}

private fun NavGraphBuilder.addFacilitiesTopLevel(
  navController: NavController,
) {
  navigation(
    route = Screen.Facilities.route,
    startDestination = LeafScreen.Facilities.createRoute(Screen.Facilities),
  ) {
    addFacilitiesList(navController, Screen.Facilities)
    addFacilityDetails(navController, Screen.Facilities)
    addFacilityOtherInfoEdit(navController, Screen.Facilities)
  }
}

private fun NavGraphBuilder.addFacilitiesList(
  navController: NavController,
  root: Screen,
) {
  composable(route = LeafScreen.Facilities.createRoute(root)) {
    FacilitiesListScreen(
      onOpenFacilityDetails = { facilityPk ->
        navController.navigate(LeafScreen.FacilityDetails.createRoute(root, facilityPk))
      }
    )
  }
}

private fun NavGraphBuilder.addFacilityDetails(
  navController: NavController,
  root: Screen,
) {
  composable(
    route = LeafScreen.FacilityDetails.createRoute(root),
    arguments = listOf(
      navArgument(LeafScreen.FacilityDetails.ARG_FACILITY_PRIMARY_KEY) { type = NavType.LongType }
    )
  ) {
    FacilityDetailsScreen(
      onBackPressed = { navController.navigateUp() },
      onFacilityOtherInfoEditPress = { facilityPrimaryKey ->
        navController.navigate(LeafScreen.FacilityOtherInfoEdit.createRoute(root, facilityPrimaryKey))
      }
    )
  }
}

private fun NavGraphBuilder.addFacilityOtherInfoEdit(
  navController: NavController,
  root: Screen,
) {
  composable(
    route = LeafScreen.FacilityOtherInfoEdit.createRoute(root),
    arguments = listOf(
      navArgument(LeafScreen.FacilityOtherInfoEdit.ARG_FACILITY_PRIMARY_KEY) {
        type = NavType.LongType
      }
    )
  ) {
    FacilityOtherInfoFormScreen(
      onNavigateBack = { navController.navigateUp() },
    )
  }
}

private fun NavGraphBuilder.addSyncTopLevel(
  navController: NavController,
) {
  navigation(
    route = Screen.Sync.route,
    startDestination = LeafScreen.Sync.createRoute(Screen.Sync),
  ) {
    addSyncScreen(navController, Screen.Sync)
  }
}

private fun NavGraphBuilder.addSyncScreen(
  navController: NavController,
  root: Screen,
) {
  composable(route = LeafScreen.Sync.createRoute(root)) {
    SyncScreen()
  }
}

private fun NavGraphBuilder.addLocationCheckInTopLevel(
  navController: NavController,
  onOpenSettingsForApp: () -> Unit,
) {
  navigation(
    route = Screen.Location.route,
    startDestination = LeafScreen.Location.createRoute(Screen.Location),
  ) {
    addLocationCheckInScreen(navController, Screen.Location, onOpenSettingsForApp)
  }
}

private fun NavGraphBuilder.addLocationCheckInScreen(
  navController: NavController,
  root: Screen,
  onOpenSettingsForApp: () -> Unit,
) {
  composable(route = LeafScreen.Location.createRoute(root)) {
    LocationCheckInScreen(onOpenSettingsForApp)
  }
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<NavBackStackEntry>.defaultEnterTransition(): EnterTransition {
  val initialNavGraph = initialState.destination.hostNavGraph
  val targetNavGraph = targetState.destination.hostNavGraph
  // If we're crossing nav graphs (bottom navigation graphs), we crossfade
  if (initialNavGraph.id != targetNavGraph.id) {
    return fadeIn()
  }
  // Otherwise we're in the same nav graph, we can imply a direction
  return fadeIn() + slideIntoContainer(AnimatedContentScope.SlideDirection.Start)
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<NavBackStackEntry>.defaultExitTransition(): ExitTransition {
  val initialNavGraph = initialState.destination.hostNavGraph
  val targetNavGraph = targetState.destination.hostNavGraph
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
private fun AnimatedContentScope<NavBackStackEntry>.defaultPopEnterTransition(): EnterTransition {
  val initialNavGraph = initialState.destination.hostNavGraph
  val targetNavGraph = targetState.destination.hostNavGraph
  // If we're crossing nav graphs (bottom navigation graphs), we crossfade
  if (initialNavGraph.id != targetNavGraph.id) {
    return fadeIn()
  }
  return fadeIn() + slideIntoContainer(AnimatedContentScope.SlideDirection.End)
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<NavBackStackEntry>.defaultPopExitTransition(): ExitTransition {
  val initialNavGraph = initialState.destination.hostNavGraph
  val targetNavGraph = targetState.destination.hostNavGraph
  // If we're crossing nav graphs (bottom navigation graphs), we crossfade
  if (initialNavGraph.id != targetNavGraph.id) {
    return fadeOut()
  }
  return fadeOut() + slideOutOfContainer(AnimatedContentScope.SlideDirection.End)
}
