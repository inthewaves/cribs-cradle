package org.welbodipartnership.cradle5.locationcheckin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
import java.io.IOException
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class LocationCheckInViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val executors: AppExecutors,
  private val dbWrapper: CradleDatabaseWrapper,
  private val syncRepository: SyncRepository,
) : ViewModel() {
  @Immutable
  sealed class ScreenState {
    object Ready : ScreenState()
    @Immutable
    data class GettingLocation(val providerName: String) : ScreenState()
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

  private val _locationProviderListText = MutableStateFlow("")
  val locationProviderListText = _locationProviderListText.asStateFlow()
  init {
    viewModelScope.launch {
      updateLocationProviderList()
    }
  }

  private fun updateLocationProviderList() {
    if (locationManager == null) {
      _locationProviderListText.value = "No location manager available"
      return
    }

    val allProviders: List<String> = try {
      locationManager.allProviders
    } catch (e: Exception) {
      Log.e(TAG, "failed to get all providers from location manager", e)
      _locationProviderListText.value =
        "Unable to get location provider list: ${e::class.java}: ${e.localizedMessage}"
      return
    }

    _locationProviderListText.value = allProviders.asSequence()
      .map {
        // isProviderEnabled should not throw on Android >= 5
        try {
          if (locationManager.isProviderEnabled(it)) {
            it
          } else {
            "$it (disabled)"
          }
        } catch (e: Exception) {
          Log.e(TAG, "exception trying to query provider enabled status for $it", e)
          "$it (unknown status: ${e::class.java}: ${e.localizedMessage})"
        }
      }
      .joinToString(", ")
  }

  private val locationCriteria = Criteria().apply { accuracy = Criteria.ACCURACY_FINE }

  private fun hasEnabledProviderSafe(providerName: String): Boolean {
    if (locationManager == null) {
      return false
    }

    return try {
      LocationManagerCompat.hasProvider(locationManager, providerName) &&
        locationManager.isProviderEnabled(providerName)
    } catch (e: Exception) {
      Log.e(TAG, "error trying to determine if $providerName is present and enabled")
      false
    }
  }

  @SuppressLint("MissingPermission")
  private val locationRequestChannel = viewModelScope.actor<Unit>(capacity = Channel.RENDEZVOUS) {
    var resetStateJob: Job? = null
    consumeEach {
      updateLocationProviderList()

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

      val provider: String = try {
        locationManager.getBestProvider(locationCriteria, true)
      } catch (e: Exception) {
        Log.e(TAG, "failed to run getBestProvider", e)
        null
      } ?: when {
        hasEnabledProviderSafe(LocationManager.FUSED_PROVIDER) &&
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> LocationManager.FUSED_PROVIDER
        hasEnabledProviderSafe(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        hasEnabledProviderSafe(LocationManager.NETWORK_PROVIDER) -> {
          LocationManager.NETWORK_PROVIDER
        }
        else -> {
          // this provider is always present
          LocationManager.PASSIVE_PROVIDER
        }
      }

      val isLocationEnabled = LocationManagerCompat.isLocationEnabled(locationManager)
      Log.d(TAG, "provider = $provider, isLocationEnabled = $isLocationEnabled")

      val locationType: LocationType? = getLocationWithPlayServices()
        ?: getLocationWithoutPlayServices(provider)

      _screenState.value = if (locationType != null) {
        val typeName = when (locationType) {
          is LocationType.GooglePlay -> "Google Play"
          is LocationType.System -> "System"
        }
        val checkIn = with(locationType) {
          LocationCheckIn(
            isUploaded = false,
            timestamp = UnixTimestamp.now().timestamp,
            providerName = "${location.provider ?: provider} ($typeName)",
            accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else null,
            latitude = location.latitude,
            longitude = location.longitude,
          )
        }
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

  sealed class LocationType {
    abstract val location: Location

    data class GooglePlay(override val location: Location) : LocationType()
    data class System(override val location: Location) : LocationType()
  }

  @RequiresPermission(
    allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]
  )
  private suspend fun getLocationWithPlayServices(): LocationType.GooglePlay? {
    Log.d(TAG, "Getting location with FusedLocationProvider")
    _screenState.value = ScreenState.GettingLocation("Google Play services")
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()

    val task: Task<Location> = fusedLocationClient.getCurrentLocation(
      LocationRequest.PRIORITY_HIGH_ACCURACY,
      cts.token
    )
    return try {
      if (task.isComplete) {
        if (task.isSuccessful) {
          LocationType.GooglePlay(task.result)
        } else {
          throw task.exception ?: IOException("failed to get location and task had no error")
        }
      } else {
        suspendCancellableCoroutine { cont ->
          cont.invokeOnCancellation { cts.cancel() }
          task
            .addOnSuccessListener { location ->
              Log.d(TAG, "Successfully retrieved location from FusedLocationProvider (provider ${location.provider})")
              cont.resume(LocationType.GooglePlay(location))
            }
            .addOnFailureListener { cause -> cont.resumeWithException(cause) }
        }
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.e(TAG, "failed to get location with Play services", e)
      null
    }
  }

  @RequiresPermission(
    allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]
  )
  private suspend fun getLocationWithoutPlayServices(provider: String): LocationType.System? {
    Log.d(TAG, "Getting location without Google Play services")
    _screenState.value = ScreenState.GettingLocation(provider)
    if (locationManager == null) {
      Log.w(TAG, "location manager is missing")
      return null
    }
    return try {
      suspendCancellableCoroutine { cont ->
        val cancellationSignal = CancellationSignal()
        try {
          LocationManagerCompat.getCurrentLocation(
            locationManager,
            provider,
            cancellationSignal,
            executors.locationExecutor,
          ) { location ->
            cont.resume(LocationType.System(location), null)
          }
        } catch (e: Exception) {
          Log.w(TAG, "${e::class.java.simpleName} while getting location", e)
          cont.resumeWithException(e)
        }
        cont.invokeOnCancellation { cancellationSignal.cancel() }
      }
    } catch (e: Exception) {
      Log.e(TAG, "${e::class.java.simpleName} while getting location, outside", e)
      coroutineContext.ensureActive()
      null
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
