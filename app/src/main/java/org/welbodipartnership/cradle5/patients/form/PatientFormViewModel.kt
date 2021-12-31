package org.welbodipartnership.cradle5.patients.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PatientFormViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle
) : ViewModel() {

}