package com.example.foldambient.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
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
    runCatching {
      getCurrentLocation(
        provider,
        cancellationSignal,
        { command -> command.run() },
      ) { location ->
        continuation.resume(
          (location ?: fallback)
            ?.let(PhoneWeatherLocationResult::Available)
            ?: PhoneWeatherLocationResult.Unavailable("Location unavailable"),
        )
      }
    }.onFailure { error ->
      continuation.resume(
        fallback?.let(PhoneWeatherLocationResult::Available)
          ?: PhoneWeatherLocationResult.Unavailable(error.message ?: "Location unavailable"),
      )
    }
    continuation.invokeOnCancellation { cancellationSignal.cancel() }
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
