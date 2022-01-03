package org.welbodipartnership.cradle5.patients.details

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.EclampsiaFit
import org.welbodipartnership.cradle5.data.database.entities.HduOrItuAdmission
import org.welbodipartnership.cradle5.data.database.entities.Hysterectomy
import org.welbodipartnership.cradle5.data.database.entities.MaternalDeath
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.PerinatalDeath
import org.welbodipartnership.cradle5.data.database.entities.SurgicalManagementOfHaemorrhage
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueForDropdownOrUnknown
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrUnknown
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate

@Composable
fun OutcomesCard(
  outcomes: Outcomes?,
  enumCollection: ServerEnumCollection,
  modifier: Modifier = Modifier
) {
  BaseDetailsCard(title = stringResource(R.string.outcomes_card_title), modifier = modifier) {
    if (outcomes == null) {
      Text(stringResource(R.string.outcomes_card_no_outcomes))

      return@BaseDetailsCard
    }

    val (
      _,
      _,
      _,
      eclampsiaFit,
      hysterectomy,
      hduOrItuAdmission,
      maternalDeath,
      surgicalManagement,
      perinatalDeath
    ) = outcomes

    val categoryToBodySpacerHeight = 0.dp
    val categoryToCategorySpacerHeight = 16.dp

    CategoryHeader(text = stringResource(R.string.outcomes_eclampsia_label))
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (eclampsiaFit != null) {
      LabelAndValueOrNone(stringResource(R.string.form_date_label), eclampsiaFit.date.toString())
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.Place,
        label = stringResource(R.string.place_of_first_eclamptic_fit_label),
        enumValue = eclampsiaFit.place,
        enumCollection = enumCollection
      )
    } else {
      Text(
        text = stringResource(R.string.none),
        style = MaterialTheme.typography.body2,
      )
    }

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(text = stringResource(R.string.outcomes_hysterectomy_label))
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (hysterectomy != null) {
      LabelAndValueOrNone(stringResource(R.string.form_date_label), hysterectomy.date.toString())
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.CauseOfHysterectomy,
        label = stringResource(R.string.hysterectomy_cause_label),
        enumValue = hysterectomy.cause,
        enumCollection = enumCollection
      )
      LabelAndValueOrNone(
        stringResource(R.string.hysterectomy_additional_info_label),
        hysterectomy.additionalInfo
      )
    } else {
      Text(
        text = stringResource(R.string.no),
        style = MaterialTheme.typography.body2,
      )
    }

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(text = stringResource(R.string.outcomes_admission_to_hdu_or_itu_label))
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (hduOrItuAdmission != null) {
      LabelAndValueOrNone(stringResource(R.string.form_date_label), hduOrItuAdmission.date.toString())
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.CauseForHduOrItuAdmission,
        label = stringResource(R.string.hdu_or_idu_admission_cause_label),
        enumValue = hduOrItuAdmission.cause,
        enumCollection = enumCollection
      )
      LabelAndValueOrUnknown(
        stringResource(R.string.hdu_or_idu_admission_length_stay_days_if_known_label),
        hduOrItuAdmission.stayInDays?.toString()
      )
    } else {
      Text(
        text = stringResource(R.string.none),
        style = MaterialTheme.typography.body2,
      )
    }

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(text = stringResource(R.string.outcomes_maternal_death_label))
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (maternalDeath != null) {
      LabelAndValueOrNone(stringResource(R.string.form_date_label), maternalDeath.date.toString())
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.UnderlyingCauseOfDeath,
        label = stringResource(R.string.maternal_death_underlying_cause_label),
        enumValue = maternalDeath.underlyingCause,
        enumCollection = enumCollection
      )
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.Place,
        label = stringResource(R.string.maternal_death_place_label),
        enumValue = maternalDeath.place,
        enumCollection = enumCollection
      )
    } else {
      Text(
        text = stringResource(R.string.none),
        style = MaterialTheme.typography.body2,
      )
    }

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(text = stringResource(R.string.outcomes_surgical_management_label))
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (surgicalManagement != null) {
      LabelAndValueOrNone(stringResource(R.string.form_date_label), surgicalManagement.date.toString())
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.TypeOfSurgicalManagement,
        label = stringResource(R.string.surgical_management_type_label),
        enumValue = surgicalManagement.typeOfSurgicalManagement,
        enumCollection = enumCollection
      )
    } else {
      Text(
        text = stringResource(R.string.none),
        style = MaterialTheme.typography.body2,
      )
    }

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(text = stringResource(R.string.outcomes_perinatal_death_label))
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (perinatalDeath != null) {
      LabelAndValueOrNone(stringResource(R.string.form_date_label), perinatalDeath.date.toString())
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.PerinatalOutcome,
        label = stringResource(R.string.perinatal_death_outcome_label),
        enumValue = perinatalDeath.outcome,
        enumCollection = enumCollection
      )
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.MaternalFactorsRelatedToPerinatalLoss,
        label = stringResource(R.string.perinatal_death_related_maternal_factors_label),
        enumValue = perinatalDeath.relatedMaternalFactors,
        enumCollection = enumCollection
      )
    } else {
      Text(
        text = stringResource(R.string.none),
        style = MaterialTheme.typography.body2,
      )
    }
  }
}

@Composable
fun CategoryHeader(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = MaterialTheme.typography.h6,
    modifier = modifier,
  )
}

@Preview
@Composable
fun OutcomesCardPreview() {
  CradleTrialAppTheme {
    val scrollState = rememberScrollState()
    OutcomesCard(
      outcomes = testOutcomes,
      enumCollection = ServerEnumCollection.defaultInstance,
      modifier = Modifier.verticalScroll(scrollState)
    )
  }
}

val testOutcomes = Outcomes(
  patientId = 5L,
  eclampsiaFit = EclampsiaFit(
    date = FormDate(20, 4, 2019),
    place = EnumSelection.IdOnly(2)
  ),
  hysterectomy = Hysterectomy(
    date = FormDate.today(),
    cause = EnumSelection.WithOther(4, "The other string"),
    additionalInfo = "Additional info here"
  ),
  hduOrItuAdmission = HduOrItuAdmission(
    date = FormDate.today(),
    cause = EnumSelection.WithOther(4, "This is input for the `other` cause"),
    stayInDays = 5
  ),
  maternalDeath = MaternalDeath(
    date = FormDate.today(),
    underlyingCause = EnumSelection.WithOther(6),
    place = EnumSelection.IdOnly(2),
  ),
  surgicalManagement = SurgicalManagementOfHaemorrhage(
    date = FormDate.today(),
    typeOfSurgicalManagement = EnumSelection.WithOther(3)
  ),
  perinatalDeath = PerinatalDeath(
    date = FormDate.today(),
    outcome = EnumSelection.IdOnly(2),
    relatedMaternalFactors = EnumSelection.WithOther(8)
  ),
)
