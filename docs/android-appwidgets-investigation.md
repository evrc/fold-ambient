# Android AppWidget Hosting Investigation

Phase 9 investigated hosting standard Android AppWidgets inside Fold Ambient.

Official Android documentation describes the host flow around these platform APIs:

- `AppWidgetHost`;
- `AppWidgetManager`;
- `AppWidgetHostView`;
- Compose `AndroidView` interoperability for rendering platform views inside Compose.

Current implementation:

- adds a Fold Ambient widget type named `android.appwidget`;
- renders an `AppWidgetHostView` when a persisted `appWidgetId` is present;
- shows a placeholder when no Android widget has been selected yet;
- starts `AppWidgetHost` listening while the widget is composed and stops listening on disposal;
- does not request `BIND_APPWIDGET`;
- does not yet allocate, bind, configure, or delete Android app widget IDs.

v1 stabilization intentionally keeps this implementation out of the user-facing widget picker until the full allocation, binding, configuration, and cleanup flow is implemented.

Future picker/binding work must handle:

- allocating a persistent app widget ID with `AppWidgetHost.allocateAppWidgetId()`;
- binding the selected provider with `AppWidgetManager.bindAppWidgetIdIfAllowed()`;
- launching `AppWidgetManager.ACTION_APPWIDGET_BIND` when user approval is required;
- launching provider configuration activities when required;
- persisting the bound app widget ID in the owning `WidgetInstance`;
- calling `AppWidgetHost.deleteAppWidgetId()` when an Android widget instance is removed.

Do not treat the current placeholder as a finished user flow. It is only a safe foundation for validating host rendering and lifecycle behavior before a real phone-widget picker is implemented.
