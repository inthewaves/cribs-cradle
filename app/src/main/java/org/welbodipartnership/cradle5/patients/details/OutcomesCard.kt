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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.data.database.entities.AgeAtDelivery
import org.welbodipartnership.cradle5.data.database.entities.BirthWeight
import org.welbodipartnership.cradle5.data.database.entities.EclampsiaFit
import org.welbodipartnership.cradle5.data.database.entities.HduOrItuAdmission
import org.welbodipartnership.cradle5.data.database.entities.Hysterectomy
import org.welbodipartnership.cradle5.data.database.entities.MaternalDeath
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.PerinatalDeath
import org.welbodipartnership.cradle5.data.database.entities.SurgicalManagementOfHaemorrhage
import org.welbodipartnership.cradle5.data.database.entities.TouchedState
import org.welbodipartnership.cradle5.data.database.entities.embedded.EnumSelection
import org.welbodipartnership.cradle5.data.serverenums.DropdownType
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueForDropdownOrUnknown
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrUnknown
import org.welbodipartnership.cradle5.ui.composables.ValueForDropdownOrUnknown
import org.welbodipartnership.cradle5.ui.composables.forms.MoreInfoIconButton
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
      serverErrorMessage: String?,
      eclampsiaFitTouched: TouchedState,
      eclampsiaFit: EclampsiaFit?,
      hysterectomyTouched: TouchedState,
      hysterectomy: Hysterectomy?,
      hduOrItuAdmissionTouched: TouchedState,
      hduOrItuAdmission: HduOrItuAdmission?,
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
      text = stringResource(R.string.outcomes_eclampsia_label),
      moreInfoText = stringResource(R.string.outcomes_eclampsia_more_info),
    )
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (eclampsiaFit != null) {
      LabelAndValueOrUnknown(stringResource(R.string.form_date_label), eclampsiaFit.date?.toString())
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
      text = stringResource(R.string.outcomes_admission_to_hdu_or_itu_label),
      moreInfoText = stringResource(R.string.outcomes_admission_to_hdu_or_itu_more_info),
    )
    Spacer(Modifier.height(categoryToBodySpacerHeight))
    if (hduOrItuAdmission != null) {
      LabelAndValueOrUnknown(
        stringResource(R.string.form_date_label),
        hduOrItuAdmission.date?.toString()
      )
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
      LabelAndValueOrNone(
        stringResource(R.string.hdu_or_idu_admission_additional_info),
        hduOrItuAdmission.additionalInfo
      )
    } else {
      Text(
        text = if (hduOrItuAdmissionTouched == TouchedState.TOUCHED_ENABLED) {
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

    Spacer(Modifier.height(categoryToCategorySpacerHeight))

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
      LabelAndValueForDropdownOrUnknown(
        dropdownType = DropdownType.MaternalFactorsRelatedToPerinatalLoss,
        label = stringResource(R.string.perinatal_death_related_maternal_factors_label),
        enumValue = perinatalDeath.relatedMaternalFactors,
        enumCollection = enumCollection
      )
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
      outcomes = testOutcomes,
      enumCollection = ServerEnumCollection.defaultInstance,
      modifier = Modifier.verticalScroll(scrollState)
    )
  }
}

val testOutcomes = Outcomes(
  patientId = 5L,
  serverInfo = null,
  serverErrorMessage = "some error",
  eclampsiaFitTouched = TouchedState.TOUCHED_ENABLED,
  eclampsiaFit = EclampsiaFit(
    date = FormDate(20, 4, 2019),
    place = EnumSelection.IdOnly(2)
  ),
  hysterectomyTouched = TouchedState.TOUCHED_ENABLED,
  hysterectomy = Hysterectomy(
    date = FormDate.today(),
    cause = EnumSelection.WithOther(4, "The other string"),
  ),
  hduOrItuAdmissionTouched = TouchedState.TOUCHED_ENABLED,
  hduOrItuAdmission = HduOrItuAdmission(
    date = FormDate.today(),
    cause = EnumSelection.WithOther(4, "This is input for the `other` cause"),
    stayInDays = 5,
    additionalInfo = "Additional info here"
  ),
  maternalDeathTouched = TouchedState.TOUCHED_ENABLED,
  maternalDeath = MaternalDeath(
    date = FormDate.today(),
    underlyingCause = EnumSelection.WithOther(6),
    place = EnumSelection.IdOnly(2),
  ),
  surgicalManagementTouched = TouchedState.TOUCHED_ENABLED,
  surgicalManagement = SurgicalManagementOfHaemorrhage(
    date = FormDate.today(),
    typeOfSurgicalManagement = EnumSelection.WithOther(3)
  ),
  perinatalDeathTouched = TouchedState.TOUCHED_ENABLED,
  perinatalDeath = PerinatalDeath(
    date = FormDate.today(),
    outcome = EnumSelection.IdOnly(2),
    relatedMaternalFactors = EnumSelection.WithOther(8),
    additionalInfo = null,
  ),
  birthWeight = BirthWeight(EnumSelection.IdOnly(1)),
  ageAtDelivery = AgeAtDelivery(EnumSelection.IdOnly(1))
)
