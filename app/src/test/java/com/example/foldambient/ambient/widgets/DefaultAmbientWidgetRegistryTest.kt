package com.example.foldambient.ambient.widgets

import com.example.foldambient.ambient.WidgetInstance
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class DefaultAmbientWidgetRegistryTest {
  @Test
  fun phoneWidgetTemplateIsNotExposedInV1Picker() {
    val registry = defaultAmbientWidgetRegistry()

    assertFalse(
      "Android AppWidget hosting is intentionally hidden until the bind/configure/delete flow is complete.",
      registry.templates.any { template -> template.widgetType == "android.appwidget" },
    )
  }

  @Test
  fun androidAppWidgetRendererRemainsAvailableForFutureWork() {
    val registry = defaultAmbientWidgetRegistry()

    assertNotNull(
      registry.widgetFor(
        WidgetInstance(
          id = "future-appwidget",
          widgetType = "android.appwidget",
        ),
      ),
    )
  }
}
