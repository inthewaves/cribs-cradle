package org.welbodipartnership.cradle5

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.welbodipartnership.cradle5.data.database.Cradle5Database
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val savedStateHandle: SavedStateHandle,
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
  private val db: CradleDatabaseWrapper,
) : ViewModel() {

  private val _dbFlow = MutableStateFlow<Cradle5Database?>(null)
  val dbFlow: StateFlow<Cradle5Database?> = _dbFlow

  init {
    Log.d(TAG, "created")

    _dbFlow.value = db.database!!
  }

  override fun onCleared() {
    super.onCleared()
    Log.d(TAG, "onCleared()")
  }

  companion object {
    private const val TAG = "MainActivityViewModel"
  }
}
