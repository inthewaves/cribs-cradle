package org.welbodipartnership.cradle5.patients.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.AgeAtDelivery
import org.welbodipartnership.cradle5.data.database.entities.BirthWeight
import org.welbodipartnership.cradle5.data.database.entities.EclampsiaFit
import org.welbodipartnership.cradle5.data.database.entities.Hysterectomy
import org.welbodipartnership.cradle5.data.database.entities.MaternalDeath
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.PerinatalDeath
import org.welbodipartnership.cradle5.data.database.entities.SurgicalManagementOfHaemorrhage
import org.welbodipartnership.cradle5.data.database.entities.TouchedState
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.patients.PatientPreviewClasses
import org.welbodipartnership.cradle5.patients.form.PerinatalNeonatalDeathList
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueForDropdownOrUnknown
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrUnknown
import org.welbodipartnership.cradle5.ui.composables.ValueForDropdownOrUnknown
import org.welbodipartnership.cradle5.ui.composables.forms.MoreInfoIconButton
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme

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
      serverErrorMessage: String?,
      eclampsiaFitTouched: TouchedState,
      eclampsiaFit: EclampsiaFit?,
      hysterectomyTouched: TouchedState,
      hysterectomy: Hysterectomy?,
      maternalDeathTouched: TouchedState,
      maternalDeath: MaternalDeath?,
      surgicalManagementTouched: TouchedState,
      surgicalManagement: SurgicalManagementOfHaemorrhage?,
      perinatalDeathTouched: TouchedState,
      perinatalDeath: PerinatalDeath?,
      birthWeight: BirthWeight?,
      ageAtDelivery: AgeAtDelivery?,
    ) = outcomes

    val categoryToBodySpacerHeight = 0.dp
    val categoryToCategorySpacerHeight = 16.dp

    if (serverErrorMessage != null) {
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.error) {
        LabelAndValueOrNone(
          label = stringResource(R.string.errors_from_sync_label),
          value = serverErrorMessage
        )
      }
      Spacer(Modifier.height(categoryToCategorySpacerHeight))
    }

    CategoryHeader(
      text = stringResource(R.string.outcomes_perinatal_death_label),
      moreInfoText = stringResource(R.string.outcomes_maternal_death_more_info),
    )
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (perinatalDeath != null) {
      LabelAndValueOrNone(stringResource(R.string.form_date_label), perinatalDeath.date?.toString())
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.PerinatalOutcome,
        label = stringResource(R.string.perinatal_death_outcome_label),
        enumValue = perinatalDeath.outcome,
        enumCollection = enumCollection
      )

      if (perinatalDeath.causeOfStillbirth != null) {
        LabelAndValueForDropdownOrUnknown(
          dropdownType = DropdownType.CauseOfStillbirth,
          label = stringResource(R.string.perinatal_death_cause_of_stillbirth_label),
          enumValue = perinatalDeath.causeOfStillbirth,
          enumCollection = enumCollection
        )
      }

      if (perinatalDeath.causesOfNeonatalDeath != null) {
        Column {
          Text(
            text = stringResource(R.string.perinatal_death_cause_of_neonatal_death_label),
            style = MaterialTheme.typography.subtitle1,
          )
          PerinatalNeonatalDeathList(
            causesOfNeonatalDeath = perinatalDeath.causesOfNeonatalDeath,
            onCausesChanged = {},
            enabled = false,
          )
        }
      }

      LabelAndValueOrNone(
        stringResource(R.string.perinatal_death_additional_info_label),
        perinatalDeath.additionalInfo
      )
    } else {
      Text(
        text = if (perinatalDeathTouched == TouchedState.TOUCHED_ENABLED) {
          stringResource(R.string.outcomes_card_enabled_but_missing_details_from_draft)
        } else {
          stringResource(R.string.none)
        },
        style = MaterialTheme.typography.body2,
      )
    }

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(text = stringResource(R.string.outcomes_birthweight_label))
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    ValueForDropdownOrUnknown(
      dropdownType = DropdownType.Birthweight,
      enumValue = birthWeight?.birthWeight,
      enumCollection = enumCollection
    )

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(text = stringResource(R.string.outcomes_age_at_delivery_label))
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    ValueForDropdownOrUnknown(
      dropdownType = DropdownType.AgeAtDelivery,
      enumValue = ageAtDelivery?.ageAtDelivery,
      enumCollection = enumCollection
    )

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(
      text = stringResource(R.string.outcomes_eclampsia_label),
      moreInfoText = stringResource(R.string.outcomes_eclampsia_more_info),
    )
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (eclampsiaFit != null) {
      LabelAndValueOrUnknown(
        stringResource(R.string.outcomes_eclampsia_did_woman_fit_label),
        when (eclampsiaFit.didTheWomanFit) {
          true -> stringResource(R.string.yes)
          false -> stringResource(R.string.no)
          null -> null
        }
      )

      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.Place,
        label = stringResource(R.string.place_of_first_eclamptic_fit_label),
        enumValue = eclampsiaFit.place,
        enumCollection = enumCollection
      )
    } else {
      Text(
        text = if (eclampsiaFitTouched == TouchedState.TOUCHED_ENABLED) {
          stringResource(R.string.outcomes_card_enabled_but_missing_details_from_draft)
        } else {
          stringResource(R.string.none)
        },
        style = MaterialTheme.typography.body2,
      )
    }

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(
      text = stringResource(R.string.outcomes_hysterectomy_label),
      moreInfoText = stringResource(R.string.outcomes_hysterectomy_more_info),
    )
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (hysterectomy != null) {
      LabelAndValueOrUnknown(
        stringResource(R.string.form_date_label),
        hysterectomy.date?.toString()
      )
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.CauseOfHysterectomy,
        label = stringResource(R.string.hysterectomy_cause_label),
        enumValue = hysterectomy.cause,
        enumCollection = enumCollection
      )
    } else {
      Text(
        text = if (hysterectomyTouched == TouchedState.TOUCHED_ENABLED) {
          stringResource(R.string.outcomes_card_enabled_but_missing_details_from_draft)
        } else {
          stringResource(R.string.none)
        },
        style = MaterialTheme.typography.body2,
      )
    }

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(
      text = stringResource(R.string.outcomes_maternal_death_label),
      moreInfoText = stringResource(R.string.outcomes_maternal_death_more_info),
    )
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (maternalDeath != null) {
      LabelAndValueOrUnknown(stringResource(R.string.form_date_label), maternalDeath.date?.toString())
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.UnderlyingCauseOfMaternalDeath,
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
        text = if (maternalDeathTouched == TouchedState.TOUCHED_ENABLED) {
          stringResource(R.string.outcomes_card_enabled_but_missing_details_from_draft)
        } else {
          stringResource(R.string.none)
        },
        style = MaterialTheme.typography.body2,
      )
    }

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

    CategoryHeader(
      text = stringResource(R.string.outcomes_surgical_management_label),
      moreInfoText = stringResource(R.string.outcomes_surgical_management_more_info),
    )
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (surgicalManagement != null) {
      LabelAndValueOrNone(stringResource(R.string.form_date_label), surgicalManagement.date?.toString())
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.TypeOfSurgicalManagement,
        label = stringResource(R.string.surgical_management_type_label),
        enumValue = surgicalManagement.typeOfSurgicalManagement,
        enumCollection = enumCollection
      )
    } else {
      Text(
        text = if (surgicalManagementTouched == TouchedState.TOUCHED_ENABLED) {
          stringResource(R.string.outcomes_card_enabled_but_missing_details_from_draft)
        } else {
          stringResource(R.string.none)
        },
        style = MaterialTheme.typography.body2,
      )
    }
  }
}

@Composable
fun CategoryHeader(
  text: String,
  modifier: Modifier = Modifier,
  moreInfoText: String? = null,
  textModifier: Modifier = Modifier
) {
  Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = text,
      style = MaterialTheme.typography.h6,
      modifier = textModifier.weight(1f),
    )
    moreInfoText?.let {
      MoreInfoIconButton(
        moreInfoText = it,
      )
    }
  }
}

@Preview
@Composable
fun CategoryHeaderPreview() {
  CradleTrialAppTheme {
    Surface {
      Column {
        CategoryHeader(
          text = stringResource(R.string.outcomes_surgical_management_label),
          moreInfoText = "More info"
        )

        CategoryHeader(
          text = stringResource(R.string.outcomes_surgical_management_label),
        )

        CategoryHeader(
          text = stringResource(R.string.outcomes_maternal_death_label),
          moreInfoText = stringResource(R.string.outcomes_maternal_death_more_info),
        )
      }
    }
  }
}

@Preview(heightDp = 1000)
@Composable
fun OutcomesCardPreview() {
  CradleTrialAppTheme {
    val scrollState = rememberScrollState()
    OutcomesCard(
      outcomes = PatientPreviewClasses.createTestOutcomes(),
      enumCollection = ServerEnumCollection.defaultInstance,
      modifier = Modifier.verticalScroll(scrollState)
    )
  }
}
