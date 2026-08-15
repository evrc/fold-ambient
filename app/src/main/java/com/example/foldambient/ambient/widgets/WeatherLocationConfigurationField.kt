package com.example.foldambient.ambient.widgets

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetInstance
import com.example.foldambient.weather.OpenMeteoGeocodingRepository
import com.example.foldambient.weather.PhoneWeatherLocationProvider
import com.example.foldambient.weather.PhoneWeatherLocationResult
import com.example.foldambient.weather.WeatherLocationMode
import com.example.foldambient.weather.WeatherLocationSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun WeatherLocationConfigurationField(
  widget: WidgetInstance,
  field: WidgetConfigurationField,
  onValuesChange: (Map<String, String>) -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val phoneLocationProvider =
    remember(context) { PhoneWeatherLocationProvider(context.applicationContext) }
  val geocodingRepository = remember { OpenMeteoGeocodingRepository() }
  val locationMode =
    WeatherLocationMode.fromValue(
      widget.configuration.text("locationMode", WeatherLocationMode.Manual.value),
    )
  var query by remember(widget.id) {
    mutableStateOf(widget.configuration.text("locationName", field.defaultValue))
  }
  var searchResults by remember { mutableStateOf(emptyList<WeatherLocationSearchResult>()) }
  var searchStatus by remember { mutableStateOf<String?>(null) }
  var phoneStatus by remember { mutableStateOf<String?>(null) }

  fun savePhoneLocation(location: Location) {
    onValuesChange(
      mapOf(
        "locationMode" to WeatherLocationMode.Phone.value,
        "locationName" to "Current location",
        "latitude" to location.latitude.toString(),
        "longitude" to location.longitude.toString(),
      ),
    )
  }

  fun resolvePhoneLocation() {
    coroutineScope.launch {
      phoneStatus = "Locating"
      when (val result = phoneLocationProvider.currentLocation()) {
        is PhoneWeatherLocationResult.Available -> {
          savePhoneLocation(result.location)
          phoneStatus = null
        }
        PhoneWeatherLocationResult.MissingPermission -> {
          phoneStatus = "Location permission needed"
        }
        is PhoneWeatherLocationResult.Unavailable -> {
          phoneStatus = result.reason
        }
      }
    }
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        resolvePhoneLocation()
      } else {
        phoneStatus = "Location denied"
        onValuesChange(mapOf("locationMode" to WeatherLocationMode.Manual.value))
      }
    }

  LaunchedEffect(query, locationMode) {
    if (locationMode != WeatherLocationMode.Manual) return@LaunchedEffect
    val trimmedQuery = query.trim()
    if (trimmedQuery.length < 2) {
      searchResults = emptyList()
      searchStatus = null
      return@LaunchedEffect
    }

    searchStatus = "Searching"
    delay(LocationSearchDebounceMillis)
    searchResults = geocodingRepository.searchLocations(trimmedQuery)
    searchStatus = if (searchResults.isEmpty()) "No matches" else null
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = field.label,
      color = Color.White,
      style = MaterialTheme.typography.bodyLarge,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Use phone location",
        color = if (locationMode == WeatherLocationMode.Phone) Color.White else WeatherConfigMuted,
        style = MaterialTheme.typography.bodyMedium,
      )
      Switch(
        checked = locationMode == WeatherLocationMode.Phone,
        onCheckedChange = { usePhoneLocation ->
          if (usePhoneLocation) {
            onValuesChange(
              mapOf(
                "locationMode" to WeatherLocationMode.Phone.value,
                "locationName" to "Current location",
              ),
            )
            if (phoneLocationProvider.hasLocationPermission()) {
              resolvePhoneLocation()
            } else {
              locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
          } else {
            phoneStatus = null
            onValuesChange(mapOf("locationMode" to WeatherLocationMode.Manual.value))
          }
        },
      )
    }

    phoneStatus?.let { status ->
      Text(
        text = status,
        color = WeatherConfigMuted,
        style = MaterialTheme.typography.labelLarge,
      )
    }

    if (locationMode == WeatherLocationMode.Manual) {
      TextField(
        value = query,
        onValueChange = { value -> query = value },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Search city") },
      )
      searchStatus?.let { status ->
        Text(
          text = status,
          color = WeatherConfigMuted,
          style = MaterialTheme.typography.labelLarge,
        )
      }
      searchResults.forEach { result ->
        TextButton(
          onClick = {
            query = result.displayName
            searchResults = emptyList()
            searchStatus = null
            onValuesChange(
              mapOf(
                "locationMode" to WeatherLocationMode.Manual.value,
                "locationName" to result.displayName,
                "latitude" to result.latitude.toString(),
                "longitude" to result.longitude.toString(),
              ),
            )
          },
        ) {
          Text(
            text = result.displayName,
            color = Color.White,
          )
        }
      }
    } else {
      Text(
        text = widget.configuration.text("locationName", "Current location"),
        color = WeatherConfigMuted,
        style = MaterialTheme.typography.labelLarge,
      )
    }
  }
}

private const val LocationSearchDebounceMillis = 350L
private val WeatherConfigMuted = Color(0xFF9CA3AF)
