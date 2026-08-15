package com.example.foldambient.ambient.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.foldambient.ambient.AmbientWidget
import com.example.foldambient.ambient.WidgetConfigurationField
import com.example.foldambient.ambient.WidgetConfigurationFieldType
import com.example.foldambient.ambient.WidgetConfigurationSpec
import com.example.foldambient.ambient.WidgetInstance

class AndroidAppWidgetWidget : AmbientWidget {
  override val type = "android.appwidget"
  override val displayName = "Android Widget"
  override val configurationSpec =
    WidgetConfigurationSpec(
      fields =
        listOf(
          WidgetConfigurationField(
            key = AppWidgetIdKey,
            label = "App widget ID",
            type = WidgetConfigurationFieldType.Text,
            defaultValue = "",
          ),
        ),
    )

  @Composable
  override fun Content(instance: WidgetInstance, modifier: Modifier) {
    val context = LocalContext.current
    val appWidgetId = instance.configuration.text(AppWidgetIdKey, "").toIntOrNull()

    if (appWidgetId == null) {
      AndroidAppWidgetPlaceholder(modifier = modifier)
      return
    }

    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    val providerInfo = remember(appWidgetManager, appWidgetId) {
      appWidgetManager.getAppWidgetInfo(appWidgetId)
    }

    if (providerInfo == null) {
      AndroidAppWidgetPlaceholder(
        value = "Unavailable",
        modifier = modifier,
      )
      return
    }

    val host = remember(context) {
      AppWidgetHost(context.applicationContext, FoldAmbientAppWidgetHostId)
    }

    DisposableEffect(host) {
      runCatching { host.startListening() }
      onDispose {
        runCatching { host.stopListening() }
      }
    }

    AndroidView(
      factory = { viewContext ->
        host.createView(viewContext, appWidgetId, providerInfo)
      },
      modifier = modifier.fillMaxSize(),
    )
  }
}

@Composable
private fun AndroidAppWidgetPlaceholder(
  value: String = "Not selected",
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
  ) {
    WidgetLabel("Android Widget")
    WidgetValue(value)
  }
}

private const val AppWidgetIdKey = "appWidgetId"
private const val FoldAmbientAppWidgetHostId = 6206
