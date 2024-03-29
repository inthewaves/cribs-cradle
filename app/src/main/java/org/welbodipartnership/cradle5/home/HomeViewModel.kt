package org.welbodipartnership.cradle5.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.domain.auth.AuthRepository
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val authRepository: AuthRepository,
  @ApplicationContext private val context: Context,
) : ViewModel() {
  private val tickerFLow = flow<Unit> {
    while (currentCoroutineContext().isActive) {
      emit(Unit)
      delay(1.seconds)
    }
  }

  val lockAppButtonSubtitleTextWithTimeLeftFlow: StateFlow<String> = combine(
    authRepository.nextLockTimeFlow,
    tickerFLow
  ) { nextExpiryTime, _ ->
    if (nextExpiryTime != null) {
      val now = UnixTimestamp.now()
      if (now >= nextExpiryTime) {
        0.seconds
      } else {
        UnixTimestamp.now() durationBetween nextExpiryTime
      }
    } else {
      null
    }
  }.map { timeUntilLock ->
    timeUntilLock?.let {
      if (it > 0.seconds) {
        context.getString(R.string.lock_app_button_subtitle_with_next_lock_time_s, it.toString())
      } else {
        context.getString(R.string.lock_app_button_subtitle_with_next_lock_time_on_background)
      }
    } ?: context.getString(R.string.lock_app_button)
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(stopTimeoutMillis = 0L),
    initialValue = ""
  )
}
