package org.welbodipartnership.cradle5.locationcheckin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.os.OperationCanceledException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.welbodipartnership.cradle5.util.executors.AppExecutors
import javax.inject.Inject

@HiltViewModel
class LocationCheckInViewModel @Inject constructor(
  @ApplicationContext context: Context,
  private val executors: AppExecutors
) : ViewModel() {
  val lastLocation = MutableStateFlow("unknown")
  private val locationManager = ContextCompat.getSystemService(context, LocationManager::class.java)

  @SuppressLint("MissingPermission")
  private val locationRequestChannel = viewModelScope.actor<Unit>(capacity = Channel.RENDEZVOUS) {
    consumeEach {
      val hasFineLocation = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
      val hasCoarseLocation = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED

      if (!hasFineLocation || !hasCoarseLocation) {
        Log.w(
          TAG,
          "missing location permissions, hasFineLocation $hasFineLocation, " +
            "hasCoarseLocation: $hasCoarseLocation"
        )
        lastLocation.value = "missing location permissions"
        return@consumeEach
      }
      lastLocation.value = "getting location"


      locationManager!!
      if (!LocationManagerCompat.hasProvider(locationManager, LocationManager.GPS_PROVIDER)) {
        Log.w(TAG, "no gps provider")
        return@consumeEach
      }
      val result = suspendCancellableCoroutine<Location?> { cont ->
        val cancellationSignal = CancellationSignal()
        try {
          LocationManagerCompat.getCurrentLocation(
            locationManager,
            LocationManager.GPS_PROVIDER,
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

      lastLocation.value = "lat = ${result?.latitude}, long = ${result?.longitude}"
    }
  }

  fun getLocation() {
    locationRequestChannel.trySend(Unit)
  }

  companion object {
    private const val TAG = "LocationCheckInViewModel"
  }
}