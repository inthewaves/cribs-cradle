package org.welbodipartnership.cradle5.patients.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Card
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
import org.welbodipartnership.cradle5.data.serverenums.ServerEnum
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueForDropdownOrUnknown
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrNone
import org.welbodipartnership.cradle5.ui.composables.LabelAndValueOrUnknown
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.date.FormDate

@Composable
fun OutcomesCard(
  outcomes: Outcomes,
  enumCollection: ServerEnumCollection,
  modifier: Modifier = Modifier
) {
  Card(modifier) {
    Column(Modifier.fillMaxWidth()) {
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
      val categoryToCategorySpacerHeight = 12.dp

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
          hduOrItuAdmission.stayInDays.toString()
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
          label = stringResource(R.string.maternal_dealth_underlying_cause_label),
          enumValue = maternalDeath.underlyingCause,
          enumCollection = enumCollection
        )
        LabelAndValueForDropdownOrUnknown(
          dropdownType = DropdownType.Place,
          label = stringResource(R.string.maternal_dealth_place_label),
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
    OutcomesCard(
      outcomes = Outcomes(
        patientId = 5L,
        eclampsiaFit = EclampsiaFit(
          date = FormDate(20, 4, 2019),
          place = EnumSelection.IdOnly(2)
        ),
        hysterectomy = Hysterectomy(
          date = FormDate.today(),
          cause = EnumSelection.WithOther(4, "The other string")
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
        )
      ),
      enumCollection = ServerEnumCollection(
        listOf(
          ServerEnum(
            DropdownType.Place,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Community",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Peripheral Health Unit",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Hospital",
                listOrder = 3
              ),
            )
          ),
          ServerEnum(
            DropdownType.CauseOfHysterectomy,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Haemorrhage",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Sepsis",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Ruptured uterus",
                listOrder = 3
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Other",
                listOrder = 99
              ),
            )
          ),
          ServerEnum(
            DropdownType.CauseForHduOrItuAdmission,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Hypertensive disorders e.g. preeclampsia / eclampsia",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Antepartum haemorrhage e.g. placenta praevia",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Antepartum haemorrhage e.g. placental abruption",
                listOrder = 3
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Postpartum haemorrhage",
                listOrder = 4
              ),
              ServerEnum.Entry(
                id = 5,
                code = 5,
                name = "Pregnancy-related infection",
                listOrder = 5
              ),
              ServerEnum.Entry(
                id = 6,
                code = 6,
                name = "Other source of sepsis",
                listOrder = 6
              ),
              ServerEnum.Entry(
                id = 7,
                code = 7,
                name = "Stroke",
                listOrder = 7
              ),
              ServerEnum.Entry(
                id = 8,
                code = 8,
                name = "Complications of abortive pregnancy outcome",
                listOrder = 8
              ),
              ServerEnum.Entry(
                id = 9,
                code = 9,
                name = "Other",
                listOrder = 99
              ),
            )
          ),
          ServerEnum(
            DropdownType.UnderlyingCauseOfDeath,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Hypertensive disorders e.g. preeclampsia / eclampsia",
                listOrder = 1
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Antepartum haemorrhage e.g. placenta praevia",
                listOrder = 2
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Antepartum haemorrhage e.g. placental abruption",
                listOrder = 3
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Postpartum haemorrhage",
                listOrder = 4
              ),
              ServerEnum.Entry(
                id = 5,
                code = 5,
                name = "Pregnancy-related infection",
                listOrder = 5
              ),
              ServerEnum.Entry(
                id = 6,
                code = 6,
                name = "Other source of sepsis",
                listOrder = 6
              ),
              ServerEnum.Entry(
                id = 7,
                code = 7,
                name = "Stroke",
                listOrder = 7
              ),
              ServerEnum.Entry(
                id = 8,
                code = 8,
                name = "Complications of abortive pregnancy outcome",
                listOrder = 8
              ),
              ServerEnum.Entry(
                id = 9,
                code = 9,
                name = "Other",
                listOrder = 99
              ),
            )
          ),
          ServerEnum(
            DropdownType.PerinatalOutcome,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Macerated stilbirth",
                listOrder = 10
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Fresh stillbirth",
                listOrder = 20
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Early neonatal death",
                listOrder = 30
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Late neonatal death",
                listOrder = 40
              ),
            )
          ),
          ServerEnum(
            DropdownType.MaternalFactorsRelatedToPerinatalLoss,
            listOf(
              ServerEnum.Entry(
                id = 1,
                code = 1,
                name = "Maternal hypertensive disorders e.g. preeclampsia/eclampsia",
                listOrder = 10
              ),
              ServerEnum.Entry(
                id = 2,
                code = 2,
                name = "Placental insufficiency",
                listOrder = 20
              ),
              ServerEnum.Entry(
                id = 3,
                code = 3,
                name = "Haemorrhage before or during labour",
                listOrder = 30
              ),
              ServerEnum.Entry(
                id = 4,
                code = 4,
                name = "Placental abruption",
                listOrder = 40
              ),
              ServerEnum.Entry(
                id = 5,
                code = 5,
                name = "Preeclampsia/Eclampsia",
                listOrder = 50
              ),
              ServerEnum.Entry(
                id = 6,
                code = 6,
                name = "Cord prolapse",
                listOrder = 60
              ),
              ServerEnum.Entry(
                id = 7,
                code = 7,
                name = "Genetic defect in the baby",
                listOrder = 70
              ),
              ServerEnum.Entry(
                id = 8,
                code = 8,
                name = "Pregnancy related infection that also affected the baby",
                listOrder = 80
              ),
              ServerEnum.Entry(
                id = 9,
                code = 9,
                name = "Other",
                listOrder = 90
              ),
            )
          ),
        ).map(ServerEnum::toDynamicServerEnum)
      )
    )
  }
}
