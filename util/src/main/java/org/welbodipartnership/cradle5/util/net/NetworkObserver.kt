package org.welbodipartnership.cradle5.util.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import org.welbodipartnership.cradle5.util.ApplicationCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a flow that checks whether the device is connected to the internet.
 *
 * Note: This simply checks whether the NetworkState is connected
 * ([NetworkCapabilities.NET_CAPABILITY_INTERNET]); it does not check whether the NetworkState is
 * validated ([NetworkCapabilities.NET_CAPABILITY_VALIDATED]).
 *
 * Based on androidx.work's NetworkStateTracker
 * (https://github.com/androidx/androidx/blob/androidx-main/work/workmanager/src/main/java/androidx/
 * work/impl/constraints/trackers/NetworkStateTracker.java)
 * and NetworkConnectedController
 * (https://github.com/androidx/androidx/blob/8cb282ccdbb00687dbf253a4419ded0dfc786fb5/work/
 * workmanager/src/main/java/androidx/work/impl/constraints/controllers/
 * NetworkConnectedController.java)
 */
@Singleton
class NetworkObserver @Inject constructor(
  @ApplicationContext context: Context,
  @ApplicationCoroutineScope applicationCoroutineScope: CoroutineScope,
) {
  private val appContext = context.applicationContext

  private val connectivityManager: ConnectivityManager? =
    ContextCompat.getSystemService(context, ConnectivityManager::class.java)

  val networkAvailabilityFlow: StateFlow<Boolean> = callbackFlow {
    if (connectivityManager != null) {
      if (isCallbackSupported) {
        val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
          override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
          ) {
            Log.d(TAG, "onCapabilitiesChanged")
            trySend(isConnected())
          }

          override fun onLost(network: Network) {
            Log.d(TAG, "onLost")
            trySend(isConnected())
          }
        }
        Log.d(TAG, "registering registerDefaultNetworkCallback")
        connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
        awaitClose {
          Log.d(TAG, "unregistering registerDefaultNetworkCallback")
          connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
        }
      } else {
        val connectivityBroadcastReceiver = object : BroadcastReceiver() {
          override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
              Log.d(TAG, "onReceive")
              trySend(isConnected())
            }
          }
        }
        Log.d(TAG, "registering broadcast receiver")
        appContext.registerReceiver(
          connectivityBroadcastReceiver,
          IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
        awaitClose {
          Log.d(TAG, "unregistering broadcast receiver")
          appContext.unregisterReceiver(connectivityBroadcastReceiver)
        }
      }
    } else {
      send(true)
    }
  }.conflate()
    .stateIn(
      applicationCoroutineScope,
      SharingStarted.WhileSubscribed(
        stopTimeoutMillis = 5000L,
        replayExpirationMillis = Long.MAX_VALUE
      ),
      initialValue = connectivityManager.isConnected()
    )

  suspend fun isNetworkAvailable() = networkAvailabilityFlow.first()

  init {
    if (connectivityManager == null) {
      Log.wtf(TAG, "device is missing ConnectivityManager; defaulting to true")
    }
  }

  /**
   * Fall back to true if the ConnectivityManager is unavailable for some reason.
   */
  private fun isConnected(): Boolean = connectivityManager?.isConnected() ?: true

  /**
   * Checks if the device currently has a connection. Note: This simply checks whether the
   * NetworkState is connected ([NetworkCapabilities.NET_CAPABILITY_INTERNET]); it does not check
   * whether the NetworkState is validated ([NetworkCapabilities.NET_CAPABILITY_VALIDATED]).
   */
  private fun ConnectivityManager?.isConnected(): Boolean =
    when {
      this == null -> true
      USE_NON_DEPRECATED_CONNECTIVITY_MANAGER_METHODS -> {
        getNetworkCapabilities(activeNetwork)
          ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          ?: false
      }
      else -> {
        // We're just using the same method as NetworkStateTracker in androidx.work at this
        // point.
        // activeNetworkInfo deprecated in API 29 (Q):
        // https://developer.android.com/reference/android/net/ConnectivityManager#getActiveNetworkInfo()
        // isConnected deprecated in API 29 (Q):
        // https://developer.android.com/reference/android/net/NetworkInfo#isConnected()
        // If these get removed, let's see what androidx.work does.
        activeNetworkInfo?.isConnected ?: false
      }
    }

  companion object {
    private const val TAG = "NetworkObserver"
    /** The defaultNetworkCallback is only available on >= API 24 */
    private const val DEFAULT_NETWORK_CALLBACK_SUPPORTED_API_LEVEL = 24
    @ChecksSdkIntAtLeast(api = DEFAULT_NETWORK_CALLBACK_SUPPORTED_API_LEVEL)
    private val isCallbackSupported =
      Build.VERSION.SDK_INT >= DEFAULT_NETWORK_CALLBACK_SUPPORTED_API_LEVEL

    private const val USE_NON_DEPRECATED_CONNECTIVITY_MANAGER_METHODS = false
  }
}


