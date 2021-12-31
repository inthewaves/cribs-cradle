package org.welbodipartnership.cradle5.facilities

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FacilitiesList() {
  FacilitiesList(
    viewModel = hiltViewModel(),
  )
}

@Composable
private fun FacilitiesList(
  viewModel: FacilitiesListViewModel
) {
  Text("Facilities list goes here")
}
