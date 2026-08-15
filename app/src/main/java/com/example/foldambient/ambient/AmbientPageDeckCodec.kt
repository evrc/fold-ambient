package com.example.foldambient.ambient

import org.json.JSONArray
import org.json.JSONObject

internal object AmbientPageDeckCodec {
  const val SCHEMA_VERSION = 1

  fun encode(deck: AmbientPageDeck): String =
    JSONObject()
      .put("schemaVersion", SCHEMA_VERSION)
      .put("selectedPageId", deck.selectedPageId)
      .put(
        "pages",
        JSONArray().also { pages ->
          deck.pages.forEach { page -> pages.put(encodePage(page)) }
        },
      )
      .toString()

  fun decode(
    json: String,
    recoveryLogger: AmbientPageDeckRecoveryLogger = AmbientPageDeckRecoveryLogger.None,
  ): AmbientPageDeck {
    val source = JSONObject(json)
    val schemaVersion = source.optionalInt("schemaVersion")
    when {
      schemaVersion == null -> Unit
      schemaVersion > SCHEMA_VERSION ->
        recoveryLogger.log("unsupported page deck schema version: $schemaVersion, attempting best-effort recovery")
      schemaVersion < 1 ->
        recoveryLogger.log("unsupported page deck schema version: $schemaVersion, treating as legacy")
    }

    val pages = decodePages(source.optionalArray("pages"), recoveryLogger)
    val selectedPageId =
      source.optionalString("selectedPageId")
        ?: selectedPageIdFromIndex(
          pages = pages,
          selectedPageIndex = source.optionalInt("selectedPageIndex"),
        )

    return normalize(
      AmbientPageDeck(
        pages = pages,
        selectedPageId = selectedPageId.orEmpty(),
      ),
      recoveryLogger = recoveryLogger,
    )
  }

  fun normalize(
    deck: AmbientPageDeck,
    recoveryLogger: AmbientPageDeckRecoveryLogger = AmbientPageDeckRecoveryLogger.None,
  ): AmbientPageDeck {
    val defaultDeck = DefaultAmbientPages.createDeck()
    val normalizedPages =
      when {
        deck.pages.isEmpty() -> {
          recoveryLogger.log("empty page deck, using default pages")
          defaultDeck.pages
        }
        deck.pages.size == 1 -> deck.pages + defaultDeck.pages.drop(1)
        else -> deck.pages
      }
        .mapIndexed { index, page ->
          page.copy(widgets = page.widgets.resizedToSlotCount(page.layout.slotCount, pageIndex = index))
        }
        .withUniquePageIds(recoveryLogger)

    val normalizedSelectedPageId =
      if (normalizedPages.any { it.id == deck.selectedPageId }) {
        deck.selectedPageId
      } else {
        recoveryLogger.log("selected page is unavailable, using first page")
        normalizedPages.first().id
      }

    return deck.copy(
      pages = normalizedPages,
      selectedPageId = normalizedSelectedPageId,
    )
  }

  private fun encodePage(page: AmbientPage): JSONObject =
    JSONObject()
      .put("id", page.id)
      .put("title", page.title)
      .put("layout", page.layout.name)
      .put(
        "widgets",
        JSONArray().also { widgets ->
          page.widgets.forEach { widget -> widgets.put(encodeWidget(widget)) }
        },
      )

  private fun encodeWidget(widget: WidgetInstance): JSONObject =
    JSONObject()
      .put("id", widget.id)
      .put("widgetType", widget.widgetType)
      .put("configuration", JSONObject(widget.configuration.values))
      .put(
        "appearance",
        JSONObject().apply {
          widget.appearance.accentColor?.let { put("accentColor", it) }
        },
      )

  private fun decodePages(
    source: JSONArray?,
    recoveryLogger: AmbientPageDeckRecoveryLogger,
  ): List<AmbientPage> {
    if (source == null) {
      recoveryLogger.log("page deck has no page list, using default pages")
      return emptyList()
    }

    return List(source.length()) { index ->
      decodePage(
        source = source.optJSONObject(index),
        pageIndex = index,
        recoveryLogger = recoveryLogger,
      )
    }
  }

  private fun decodePage(
    source: JSONObject?,
    pageIndex: Int,
    recoveryLogger: AmbientPageDeckRecoveryLogger,
  ): AmbientPage {
    if (source == null) {
      recoveryLogger.log("invalid page ${pageIndex + 1}, recovered with defaults")
      return fallbackPage(pageIndex)
    }

    val layout = decodeLayout(source.optionalString("layout"), pageIndex, recoveryLogger)
    val widgets =
      decodeWidgets(
        source = source.optionalArray("widgets"),
        slotCount = layout.slotCount,
        pageIndex = pageIndex,
        recoveryLogger = recoveryLogger,
      )

    return AmbientPage(
      id = source.optionalString("id") ?: "recovered-page-${pageIndex + 1}",
      title = source.optionalString("title") ?: "Page ${pageIndex + 1}",
      layout = layout,
      widgets = widgets,
    )
  }

  private fun decodeLayout(
    value: String?,
    pageIndex: Int,
    recoveryLogger: AmbientPageDeckRecoveryLogger,
  ): AmbientLayoutKind {
    if (value.isNullOrBlank()) {
      recoveryLogger.log("missing page layout on page ${pageIndex + 1}, using Duo")
      return AmbientLayoutKind.Duo
    }

    return AmbientLayoutKind.entries.firstOrNull { it.name == value }
      ?: AmbientLayoutKind.Duo.also {
        recoveryLogger.log("unsupported page layout: $value, using Duo")
      }
  }

  private fun decodeWidgets(
    source: JSONArray?,
    slotCount: Int,
    pageIndex: Int,
    recoveryLogger: AmbientPageDeckRecoveryLogger,
  ): List<WidgetInstance> {
    val decoded =
      if (source == null) {
        recoveryLogger.log("page ${pageIndex + 1} has no widget list, using fallback widgets")
        emptyList()
      } else {
        List(source.length()) { index ->
          decodeWidget(
            source = source.optJSONObject(index),
            pageIndex = pageIndex,
            slotIndex = index,
            recoveryLogger = recoveryLogger,
          )
        }
      }

    return decoded.resizedToSlotCount(slotCount, pageIndex)
  }

  private fun decodeWidget(
    source: JSONObject?,
    pageIndex: Int,
    slotIndex: Int,
    recoveryLogger: AmbientPageDeckRecoveryLogger,
  ): WidgetInstance {
    if (source == null) {
      recoveryLogger.log("invalid widget ${slotIndex + 1} on page ${pageIndex + 1}, using fallback")
      return fallbackWidget(pageIndex = pageIndex, slotIndex = slotIndex)
    }

    val id = source.optionalString("id") ?: "recovered-widget-${pageIndex + 1}-${slotIndex + 1}"
    val widgetType = source.optionalString("widgetType")
    if (widgetType == null || widgetType !in SupportedWidgetTypes) {
      recoveryLogger.log("unsupported widget type: ${widgetType ?: "<missing>"}, using fallback")
      return fallbackWidget(pageIndex = pageIndex, slotIndex = slotIndex, id = id)
    }

    return WidgetInstance(
      id = id,
      widgetType = widgetType,
      configuration = decodeWidgetConfiguration(source, pageIndex, slotIndex, recoveryLogger),
      appearance = decodeWidgetAppearance(source.optionalObject("appearance")),
    )
  }

  private fun decodeWidgetConfiguration(
    source: JSONObject,
    pageIndex: Int,
    slotIndex: Int,
    recoveryLogger: AmbientPageDeckRecoveryLogger,
  ): WidgetConfiguration {
    val configuration = source.optionalObject("configuration")
    if (configuration == null) {
      if (source.has("configuration")) {
        recoveryLogger.log("invalid widget configuration on page ${pageIndex + 1}, slot ${slotIndex + 1}, using defaults")
      }
      return WidgetConfiguration()
    }

    return WidgetConfiguration(values = configuration.toStringMap())
  }

  private fun decodeWidgetAppearance(source: JSONObject?): WidgetAppearance =
    WidgetAppearance(
      accentColor = source?.optionalLong("accentColor"),
    )

  private fun selectedPageIdFromIndex(
    pages: List<AmbientPage>,
    selectedPageIndex: Int?,
  ): String? {
    if (selectedPageIndex == null || pages.isEmpty()) return null
    return pages[selectedPageIndex.coerceIn(0, pages.lastIndex)].id
  }

  private fun fallbackPage(pageIndex: Int): AmbientPage {
    val defaultPage = DefaultAmbientPages.createDeck().pages.getOrNull(pageIndex)
    return defaultPage
      ?: AmbientPage(
        id = "recovered-page-${pageIndex + 1}",
        title = "Page ${pageIndex + 1}",
        layout = AmbientLayoutKind.Duo,
        widgets = List(AmbientLayoutKind.Duo.slotCount) { slotIndex -> fallbackWidget(pageIndex, slotIndex) },
      )
  }

  private fun fallbackWidget(
    pageIndex: Int,
    slotIndex: Int,
    id: String = "recovered-widget-${pageIndex + 1}-${slotIndex + 1}",
  ): WidgetInstance =
    WidgetInstance(
      id = id,
      widgetType = FallbackWidgetType,
      configuration =
        WidgetConfiguration(
          values =
            mapOf(
              "label" to "Widget",
              "value" to "Ready",
            ),
        ),
    )

  private fun List<WidgetInstance>.resizedToSlotCount(
    slotCount: Int,
    pageIndex: Int,
  ): List<WidgetInstance> =
    List(slotCount) { slotIndex ->
      getOrNull(slotIndex) ?: fallbackWidget(pageIndex = pageIndex, slotIndex = slotIndex)
    }

  private fun List<AmbientPage>.withUniquePageIds(recoveryLogger: AmbientPageDeckRecoveryLogger): List<AmbientPage> {
    val seen = mutableSetOf<String>()
    return mapIndexed { index, page ->
      if (page.id.isNotBlank() && seen.add(page.id)) {
        page
      } else {
        val recoveredId = generateSequence("recovered-page-${index + 1}") { "$it-copy" }
          .first { it !in seen }
        seen += recoveredId
        recoveryLogger.log("duplicate or blank page id on page ${index + 1}, using recovered id")
        page.copy(id = recoveredId)
      }
    }
  }

  private val SupportedWidgetTypes =
    setOf(
      "android.appwidget",
      "battery.status",
      "clock.analog",
      "clock.digital",
      "date.today",
      "dummy.text",
      "empty",
      "lyrics.current",
      "media.playback",
      "weather.current",
    )
}

internal fun interface AmbientPageDeckRecoveryLogger {
  fun log(message: String)

  companion object {
    val None = AmbientPageDeckRecoveryLogger {}
  }
}

private fun JSONObject.toStringMap(): Map<String, String> =
  keys().asSequence().mapNotNull { key ->
    val value = opt(key)
    when (value) {
      is String -> key to value
      is Number -> key to value.toString()
      is Boolean -> key to value.toString()
      else -> null
    }
  }.toMap()

private fun JSONObject.optionalArray(key: String): JSONArray? =
  opt(key) as? JSONArray

private fun JSONObject.optionalObject(key: String): JSONObject? =
  opt(key) as? JSONObject

private fun JSONObject.optionalString(key: String): String? =
  (opt(key) as? String)?.takeIf { it.isNotBlank() }

private fun JSONObject.optionalInt(key: String): Int? {
  val value = opt(key)
  return when (value) {
    is Number -> value.toInt()
    is String -> value.toIntOrNull()
    else -> null
  }
}

private fun JSONObject.optionalLong(key: String): Long? {
  val value = opt(key)
  return when (value) {
    is Number -> value.toLong()
    is String -> value.toLongOrNull()
    else -> null
  }
}

private const val FallbackWidgetType = "dummy.text"
