package org.welbodipartnership.cradle5.locationcheckin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.os.OperationCanceledException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.welbodipartnership.cradle5.data.database.CradleDatabaseWrapper
import org.welbodipartnership.cradle5.data.database.entities.LocationCheckIn
import org.welbodipartnership.cradle5.domain.sync.SyncRepository
import org.welbodipartnership.cradle5.util.datetime.UnixTimestamp
import org.welbodipartnership.cradle5.util.executors.AppExecutors
import javax.annotation.concurrent.Immutable
import javax.inject.Inject

@HiltViewModel
class LocationCheckInViewModel @Inject constructor(
  @ApplicationContext context: Context,
  private val executors: AppExecutors,
  private val dbWrapper: CradleDatabaseWrapper,
  private val syncRepository: SyncRepository,
) : ViewModel() {
  @Immutable
  sealed class ScreenState {
    object Ready : ScreenState()
    object GettingLocation : ScreenState()
    @Immutable
    data class Error(val errorMessage: String) : ScreenState()
    object ErrorLocationDisabled : ScreenState()
    object Success : ScreenState()
  }

  private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Ready)
  val screenState: StateFlow<ScreenState> = _screenState

  val canDelete: StateFlow<Boolean> = syncRepository.editFormState
    .map { it.canEdit }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

  val checkInsPagerFlow: Flow<PagingData<LocationCheckIn>> = Pager(
    PagingConfig(
      pageSize = 60,
      enablePlaceholders = true,
      maxSize = 200
    )
  ) { dbWrapper.locationCheckInDao().checkInsPagingSource() }
    .flow
    .cachedIn(viewModelScope)

  private val locationManager = ContextCompat.getSystemService(context, LocationManager::class.java)

  @SuppressLint("MissingPermission")
  private val locationRequestChannel = viewModelScope.actor<Unit>(capacity = Channel.RENDEZVOUS) {
    var resetStateJob: Job? = null
    consumeEach {
      val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
      val hasCoarseLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED

      if (!hasFineLocation || !hasCoarseLocation) {
        Log.w(
          TAG,
          "missing location permissions, hasFineLocation $hasFineLocation, " +
            "hasCoarseLocation: $hasCoarseLocation"
        )
        _screenState.value = ScreenState.Error(
          if (!hasFineLocation) {
            "Missing fine location permissions"
          } else {
            "Missing (coarse) location permission"
          }
        )
        return@consumeEach
      }

      if (locationManager == null) {
        Log.w(TAG, "unable to get location manager")
        _screenState.value = ScreenState.Error("Missing location manager")
        return@consumeEach
      }

      val provider = if (
        LocationManagerCompat.hasProvider(locationManager, LocationManager.GPS_PROVIDER)
      ) {
        LocationManager.GPS_PROVIDER
      } else {
        Log.w(TAG, "no GPS provider")

        if (LocationManagerCompat.hasProvider(locationManager, LocationManager.NETWORK_PROVIDER)) {
          LocationManager.NETWORK_PROVIDER
        } else {
          // this provider is always present
          LocationManager.PASSIVE_PROVIDER
        }
      }
      _screenState.value = ScreenState.GettingLocation
      val isLocationEnabled = LocationManagerCompat.isLocationEnabled(locationManager)
      Log.d(TAG, "provider = $provider, isLocationEnabled = $isLocationEnabled")

      val location = suspendCancellableCoroutine<Location?> { cont ->
        val cancellationSignal = CancellationSignal()
        try {
          LocationManagerCompat.getCurrentLocation(
            locationManager,
            provider,
            cancellationSignal,
            executors.locationExecutor,
          ) { location ->
            cont.resume(location, null)
          }
        } catch (e: OperationCanceledException) {
          Log.w(TAG, "OperationCanceledException", e)
        }
        cont.invokeOnCancellation { cancellationSignal.cancel() }
      }

      _screenState.value = if (location != null) {
        val checkIn = LocationCheckIn(
          isUploaded = false,
          timestamp = UnixTimestamp.now().timestamp,
          providerName = location.provider ?: provider,
          accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else null,
          latitude = location.latitude,
          longitude = location.longitude,
        )
        dbWrapper.locationCheckInDao().insertCheckIn(checkIn)

        resetStateJob?.cancel()
        resetStateJob = launch {
          delay(5000L)
          _screenState.getAndUpdate { current ->
            if (current is ScreenState.Success) {
              ScreenState.Ready
            } else {
              current
            }
          }
        }

        ScreenState.Success
      } else {
        if (isLocationEnabled) {
          ScreenState.Error("Failed to get location")
        } else {
          ScreenState.ErrorLocationDisabled
        }
      }
    }
  }

  fun submitLocationRequest() {
    locationRequestChannel.trySend(Unit)
  }

  private val undoCache: MutableStateFlow<LocationCheckIn?> = MutableStateFlow(null)
  val undoCheckIn: StateFlow<LocationCheckIn?> = undoCache.asStateFlow()

  fun handleUndo() {
    Log.d(TAG, "handleUndo()")
    val undoItem = undoCache.value ?: return
    viewModelScope.launch {
      dbWrapper.locationCheckInDao().insertCheckIn(undoItem)
    }
  }

  fun clearUndoCache() {
    Log.d(TAG, "clearUndoCache()")
    undoCache.value = null
  }

  fun delete(checkIn: LocationCheckIn) {
    viewModelScope.launch {
      dbWrapper.locationCheckInDao().deleteCheckIn(checkIn)
      undoCache.value = checkIn
    }
  }

  companion object {
    private const val TAG = "LocationCheckInViewModel"
  }
}
