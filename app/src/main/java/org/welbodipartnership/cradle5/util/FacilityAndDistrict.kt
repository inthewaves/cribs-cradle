package org.welbodipartnership.cradle5.util

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility

@Immutable
@Parcelize
data class FacilityAndPosition(
  val facility: Facility,
  val position: Int?
) : Parcelable

@Immutable
@Parcelize
data class DistrictAndPosition(
  val district: District,
  val position: Int?
) : Parcelable