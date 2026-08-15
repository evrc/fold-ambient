package com.example.foldambient.ambient.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetConfigurationFieldType
import com.example.foldambient.ambient.WidgetConfigurationOption
import com.example.foldambient.ambient.WidgetConfigurationSpec
import com.example.foldambient.ambient.WidgetInstance
import com.example.foldambient.weather.AmbientWeather
import com.example.foldambient.weather.AmbientWeatherResult
import com.example.foldambient.weather.OpenMeteoWeatherRepository
import com.example.foldambient.weather.PhoneWeatherLocationProvider
import com.example.foldambient.weather.PhoneWeatherLocationResult
import com.example.foldambient.weather.WeatherLocationMode
import com.example.foldambient.weather.WeatherLocation
import com.example.foldambient.weather.WeatherTemperatureUnit
import com.example.foldambient.weather.WeatherCondition
import kotlinx.coroutines.delay

class WeatherWidget : AmbientWidget {
  override val type = "weather.current"
  override val displayName = "Weather"
  override val configurationSpec =
    WidgetConfigurationSpec(
      fields =
        listOf(
          WidgetConfigurationField(
            key = "label",
            label = "Label",
            type = WidgetConfigurationFieldType.Text,
            defaultValue = "Weather",
          ),
          WidgetConfigurationField(
            key = "location",
            label = "Location",
            type = WidgetConfigurationFieldType.Location,
            defaultValue = "New York",
          ),
          WidgetConfigurationField(
            key = "temperatureUnit",
            label = "Unit",
            type = WidgetConfigurationFieldType.Option,
            defaultValue = WeatherTemperatureUnit.Fahrenheit.apiValue,
            options =
              listOf(
                WidgetConfigurationOption(WeatherTemperatureUnit.Fahrenheit.apiValue, "Fahrenheit"),
                WidgetConfigurationOption(WeatherTemperatureUnit.Celsius.apiValue, "Celsius"),
              ),
          ),
        ),
    )

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    val context = LocalContext.current
    val repository = remember { OpenMeteoWeatherRepository() }
    val phoneLocationProvider =
      remember(context) { PhoneWeatherLocationProvider(context.applicationContext) }
    val locationMode = instance.configuration.weatherLocationMode()
    val manualLocation = instance.configuration.manualWeatherLocation()
    val temperatureUnit = instance.configuration.weatherTemperatureUnit()
    var result by remember(locationMode, manualLocation, temperatureUnit) {
      mutableStateOf<AmbientWeatherResult?>(null)
    }
    var displayedLocation by remember(locationMode, manualLocation, temperatureUnit) {
      mutableStateOf(manualLocation)
    }

    StartedWidgetEffect(repository, phoneLocationProvider, locationMode, manualLocation, temperatureUnit) {
      while (true) {
        val location =
          when (locationMode) {
            WeatherLocationMode.Manual -> manualLocation
            WeatherLocationMode.Phone ->
              when (val phoneLocation = phoneLocationProvider.currentLocation()) {
                is PhoneWeatherLocationResult.Available ->
                  WeatherLocation(
                    name = "Current location",
                    latitude = phoneLocation.location.latitude,
                    longitude = phoneLocation.location.longitude,
                    temperatureUnit = temperatureUnit,
                  )
                PhoneWeatherLocationResult.MissingPermission -> {
                  result = AmbientWeatherResult.Unavailable("Allow location in Configure")
                  null
                }
                is PhoneWeatherLocationResult.Unavailable -> {
                  result = AmbientWeatherResult.Unavailable(phoneLocation.reason)
                  null
                }
              }
          }

        if (location == null) {
          if (locationMode == WeatherLocationMode.Manual) {
            result = AmbientWeatherResult.Unavailable("Search for a city")
          }
        } else {
          displayedLocation = location
          result = repository.currentWeather(location)
        }
        delay(WeatherRefreshIntervalMillis)
      }
    }

    WeatherWidgetContent(
      label = instance.configuration.text("label", displayName),
      location = displayedLocation,
      result = result,
      modifier = modifier,
    )
  }

  @Composable
  override fun PreviewContent(instance: WidgetInstance, modifier: Modifier) {
    val unit = instance.configuration.weatherTemperatureUnit()
    val locationName = instance.configuration.text("locationName", "New York")
    WeatherWidgetContent(
      label = instance.configuration.text("label", displayName),
      location =
        WeatherLocation(
          name = locationName,
          latitude = 40.7128,
          longitude = -74.0060,
          temperatureUnit = unit,
        ),
      result =
        AmbientWeatherResult.Available(
          AmbientWeather(
            locationName = locationName,
            temperature = if (unit == WeatherTemperatureUnit.Fahrenheit) 72.0 else 22.0,
            apparentTemperature = null,
            temperatureUnit = unit,
            condition = WeatherCondition(code = 1, isDay = true, label = "Partly cloudy"),
            windSpeed = 6.0,
            windSpeedUnit = "mph",
            observedAt = null,
          ),
        ),
      modifier = modifier,
    )
  }
}

@Composable
private fun WeatherWidgetContent(
  label: String,
  location: WeatherLocation?,
  result: AmbientWeatherResult?,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
  ) {
    WidgetLabel(label)
    when (result) {
      null -> WeatherMessage(title = "Weather", detail = "Loading")
      is AmbientWeatherResult.Available -> CurrentWeather(result.weather)
      is AmbientWeatherResult.Unavailable ->
        WeatherMessage(
          title = "Unavailable",
          detail = result.reason ?: location?.name ?: "Weather",
        )
    }
  }
}

@Composable
private fun CurrentWeather(weather: AmbientWeather) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth > maxHeight
    val temperatureSize = if (isWide) 54.sp else 62.sp
    val conditionStyle =
      if (isWide) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.Start,
    ) {
      Text(
        text = weather.temperatureText,
        color = Color.White,
        fontSize = temperatureSize,
        lineHeight = temperatureSize,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        overflow = TextOverflow.Clip,
      )
      Text(
        text = weather.condition.label,
        color = WeatherAccent,
        style = conditionStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = weather.locationName,
        color = Muted,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      weather.apparentTemperatureText?.let { apparent ->
        Text(
          text = "Feels $apparent",
          color = Muted,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      weather.windText?.let { wind ->
        Text(
          text = "Wind $wind",
          color = Muted,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun WeatherMessage(
  title: String,
  detail: String,
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.Start,
  ) {
    Text(
      text = title,
      color = Color.White,
      style = MaterialTheme.typography.headlineMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = detail,
      color = Muted,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Start,
    )
  }
}

private fun com.example.foldambient.ambient.WidgetConfiguration.manualWeatherLocation(): WeatherLocation? {
  val latitude = text("latitude", DefaultLatitude).toDoubleOrNull() ?: return null
  val longitude = text("longitude", DefaultLongitude).toDoubleOrNull() ?: return null
  if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null

  return WeatherLocation(
    name = text("locationName", "Location").ifBlank { "Location" },
    latitude = latitude,
    longitude = longitude,
    temperatureUnit = weatherTemperatureUnit(),
  )
}

private fun com.example.foldambient.ambient.WidgetConfiguration.weatherLocationMode(): WeatherLocationMode =
  WeatherLocationMode.fromValue(text("locationMode", WeatherLocationMode.Manual.value))

private fun com.example.foldambient.ambient.WidgetConfiguration.weatherTemperatureUnit(): WeatherTemperatureUnit =
  WeatherTemperatureUnit.fromValue(
    text("temperatureUnit", WeatherTemperatureUnit.Fahrenheit.apiValue),
  )

private val Muted = Color(0xFF9CA3AF)
private val WeatherAccent = Color(0xFF7DD3FC)
private const val DefaultLatitude = "40.7128"
private const val DefaultLongitude = "-74.0060"
private const val WeatherRefreshIntervalMillis = 15 * 60 * 1_000L
