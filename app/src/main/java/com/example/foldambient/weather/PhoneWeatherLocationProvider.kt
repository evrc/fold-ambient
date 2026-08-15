package com.example.foldambient.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class PhoneWeatherLocationProvider(
  private val context: Context,
) {
  fun hasLocationPermission(): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED

  suspend fun currentLocation(): PhoneWeatherLocationResult {
    if (!hasLocationPermission()) return PhoneWeatherLocationResult.MissingPermission

    val locationManager =
      context.getSystemService(LocationManager::class.java)
        ?: return PhoneWeatherLocationResult.Unavailable("Location unavailable")
    val fallback = locationManager.latestKnownLocation()
    val provider = locationManager.preferredProvider()

    if (provider == null) {
      return fallback?.let(PhoneWeatherLocationResult::Available)
        ?: PhoneWeatherLocationResult.Unavailable("Location disabled")
    }

    return locationManager.currentLocation(provider, fallback)
  }
}

sealed interface PhoneWeatherLocationResult {
  data class Available(val location: Location) : PhoneWeatherLocationResult
  data object MissingPermission : PhoneWeatherLocationResult
  data class Unavailable(val reason: String) : PhoneWeatherLocationResult
}

@SuppressLint("MissingPermission")
private suspend fun LocationManager.currentLocation(
  provider: String,
  fallback: Location?,
): PhoneWeatherLocationResult =
  suspendCancellableCoroutine { continuation ->
    val cancellationSignal = CancellationSignal()
    val completion = CallbackCompletionGate { cancellationSignal.cancel() }
    continuation.invokeOnCancellation { completion.cancel() }

    runCatching {
      getCurrentLocation(
        provider,
        cancellationSignal,
        { command -> command.run() },
      ) { location ->
        val result =
          (location ?: fallback)
            ?.let(PhoneWeatherLocationResult::Available)
            ?: PhoneWeatherLocationResult.Unavailable("Location unavailable")
        if (completion.tryComplete()) {
          continuation.resume(result)
        }
      }
    }.onFailure { error ->
      val result =
        fallback?.let(PhoneWeatherLocationResult::Available)
          ?: PhoneWeatherLocationResult.Unavailable(error.message ?: "Location unavailable")
      if (completion.tryComplete()) {
        continuation.resume(result)
      }
    }
  }

internal class CallbackCompletionGate(
  private val onCancel: () -> Unit = {},
) {
  private val completed = AtomicBoolean(false)

  fun tryComplete(): Boolean = completed.compareAndSet(false, true)

  fun cancel(): Boolean {
    val didCancel = completed.compareAndSet(false, true)
    if (didCancel) {
      onCancel()
    }
    return didCancel
  }
}

@SuppressLint("MissingPermission")
private fun LocationManager.latestKnownLocation(): Location? =
  getProviders(true)
    .mapNotNull { provider ->
      runCatching { getLastKnownLocation(provider) }.getOrNull()
    }
    .maxByOrNull { it.time }

private fun LocationManager.preferredProvider(): String? {
  val enabledProviders = getProviders(true)
  return when {
    LocationManager.NETWORK_PROVIDER in enabledProviders -> LocationManager.NETWORK_PROVIDER
    LocationManager.PASSIVE_PROVIDER in enabledProviders -> LocationManager.PASSIVE_PROVIDER
    enabledProviders.isNotEmpty() -> enabledProviders.first()
    else -> null
  }
}
