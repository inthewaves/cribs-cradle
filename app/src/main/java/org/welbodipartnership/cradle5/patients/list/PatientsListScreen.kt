package org.welbodipartnership.cradle5.patients.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.compose.rememberFlowWithLifecycle
import org.welbodipartnership.cradle5.data.database.resultentities.ListPatient
import org.welbodipartnership.cradle5.ui.composables.forms.DateOutlinedTextField
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.date.FormDate

@Composable
fun PatientsListScreen(
  onOpenPatientDetails: (patientPrimaryKey: Long) -> Unit,
  onOpenNewPatientCreation: () -> Unit,
) {
  PatientsListScreen(
    viewModel = hiltViewModel(),
    onOpenPatientDetails = onOpenPatientDetails,
    onOpenNewPatientCreation = onOpenNewPatientCreation
  )
}

/**
 * Returns whether the lazy list is currently scrolling up.
 * From Google's Animation code lab.
 */
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
  var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
  var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
  return remember(this) {
    derivedStateOf {
      if (previousIndex != firstVisibleItemIndex) {
        previousIndex > firstVisibleItemIndex
      } else {
        previousScrollOffset >= firstVisibleItemScrollOffset
      }.also {
        previousIndex = firstVisibleItemIndex
        previousScrollOffset = firstVisibleItemScrollOffset
      }
    }
  }.value
}

@Composable
private fun PatientsListScreen(
  viewModel: PatientsListViewModel,
  onOpenPatientDetails: (patientPrimaryKey: Long) -> Unit,
  onOpenNewPatientCreation: () -> Unit,
) {
  val lazyListState = rememberLazyListState()
  Scaffold(
    topBar = {
      TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        contentPadding = rememberInsetsPaddingValues(
          insets = LocalWindowInsets.current.systemBars,
          applyBottom = false,
        ),
        modifier = Modifier.fillMaxWidth(),
        title = { Text(text = stringResource(R.string.patients_title)) },
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = onOpenNewPatientCreation) {
        Row(Modifier.padding(16.dp)) {
          Icon(Icons.Filled.Add, stringResource(R.string.patients_list_add_new_button))

          AnimatedVisibility(visible = lazyListState.isScrollingUp()) {
            Text(
              text = stringResource(R.string.patients_list_add_new_button),
              modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
          }
        }
      }
    }
  ) { padding ->
    Column(Modifier.padding(padding)) {
      /*
      Text("Patients")

      val scope = rememberCoroutineScope()
      OutlinedButton(
        onClick = {
          Log.d("PatientsList", "")
          scope.launch {
            viewModel.addPatient(
              Patient(
                initials = "AB",
                presentationDate = FormDate(day = 5, month = 4, year = 2010),
                dateOfBirth = FormDate(day = 5, month = 4, year = 2010),
                isExactDateOfBirth = false,
                lastUpdatedTimestamp = System.currentTimeMillis() / 1000
              )
            )
          }
        }
      ) {
        Text("Add patient")
      }

       */

      var selectedDate: MutableState<FormDate?> = remember { mutableStateOf(null) }
      DateOutlinedTextField(
        date = selectedDate.value,
        onDatePicked = {
          selectedDate.value = it
        }
      )

      val lazyPagingItems = rememberFlowWithLifecycle(viewModel.pager.flow)
        .collectAsLazyPagingItems()

      PatientListHeader()
      LazyColumn(state = lazyListState) {
        itemsIndexed(lazyPagingItems) { index, listPatient ->
          if (listPatient != null) {
            PatientListItem(
              listPatient,
              onClick = { onOpenPatientDetails(it.id) }
            )
          } else {
            Text("No patients")
          }
        }
      }
    }
  }
}

@Composable
fun PatientListHeader(modifier: Modifier = Modifier) {
  Card(elevation = 1.dp) {
    BasePatientListItem(
      first = stringResource(R.string.patient_list_header_id),
      second = stringResource(R.string.patient_list_header_initials),
      third = stringResource(R.string.patient_list_header_date_of_birth),
      minHeight = 24.dp,
      textStyle = MaterialTheme.typography.subtitle2,
      onClick = null,
      modifier = modifier
    )
  }
}

@Composable
fun PatientListItem(
  listPatient: ListPatient,
  onClick: (listPatient: ListPatient) -> Unit,
  modifier: Modifier = Modifier
) {
  BasePatientListItem(
    first = listPatient.id.toString(),
    second = listPatient.initials,
    third = listPatient.dateOfBirth.toString(),
    minHeight = 48.dp,
    textStyle = MaterialTheme.typography.body2,
    onClick = { onClick(listPatient) },
    modifier = modifier
  )
}

@Composable
private fun BasePatientListItem(
  first: String,
  second: String,
  third: String,
  minHeight: Dp,
  zHeight: Dp = 0.dp,
  textStyle: TextStyle,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier
) {
  Column {
    val outlineModifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
    Row(
      modifier = modifier
        .then(
          if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .heightIn(min = minHeight),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        first,
        modifier = Modifier
          .weight(0.2f)
          .align(Alignment.CenterVertically),
        style = textStyle
      )
      Text(
        second,
        modifier = Modifier
          .weight(0.2f)
          .align(Alignment.CenterVertically),
        style = textStyle
      )
      Text(
        third,
        modifier = Modifier
          .weight(0.3f)
          .align(Alignment.CenterVertically),
        style = textStyle
      )
    }
    Box(outlineModifier)
  }
}

@Preview
@Composable
fun PatientListItemPreview() {
  CradleTrialAppTheme {
    Surface {
      PatientListItem(
        listPatient = ListPatient(0L, "AA", FormDate.today()),
        onClick = {}
      )
    }
  }
}