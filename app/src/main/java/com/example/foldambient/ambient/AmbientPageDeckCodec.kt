package com.example.foldambient.ambient

import org.json.JSONArray
import org.json.JSONObject

internal object AmbientPageDeckCodec {
  fun encode(deck: AmbientPageDeck): String =
    JSONObject()
      .put("selectedPageId", deck.selectedPageId)
      .put(
        "pages",
        JSONArray().also { pages ->
          deck.pages.forEach { page -> pages.put(encodePage(page)) }
        },
      )
      .toString()

  fun decode(json: String): AmbientPageDeck {
    val source = JSONObject(json)
    val pages = source.getJSONArray("pages").mapObjects(::decodePage)
    return AmbientPageDeck(
      pages = pages,
      selectedPageId = source.getString("selectedPageId"),
    )
  }

  fun normalize(deck: AmbientPageDeck): AmbientPageDeck {
    val defaultDeck = DefaultAmbientPages.createDeck()
    val normalizedPages =
      when {
        deck.pages.isEmpty() -> defaultDeck.pages
        deck.pages.size == 1 -> deck.pages + defaultDeck.pages.drop(1)
        else -> deck.pages
      }
    val normalizedSelectedPageId =
      if (normalizedPages.any { it.id == deck.selectedPageId }) deck.selectedPageId else normalizedPages.first().id

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

  private fun decodePage(source: JSONObject): AmbientPage =
    AmbientPage(
      id = source.getString("id"),
      title = source.getString("title"),
      layout = AmbientLayoutKind.valueOf(source.getString("layout")),
      widgets = source.getJSONArray("widgets").mapObjects(::decodeWidget),
    )

  private fun decodeWidget(source: JSONObject): WidgetInstance =
    WidgetInstance(
      id = source.getString("id"),
      widgetType = source.getString("widgetType"),
      configuration =
        WidgetConfiguration(
          values = source.getJSONObject("configuration").toStringMap(),
        ),
      appearance =
        WidgetAppearance(
          accentColor = source.optJSONObject("appearance")?.optLongOrNull("accentColor"),
        ),
    )
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
  List(length()) { index -> transform(getJSONObject(index)) }

private fun JSONObject.toStringMap(): Map<String, String> =
  keys().asSequence().associateWith { key -> getString(key) }

private fun JSONObject.optLongOrNull(key: String): Long? =
  if (has(key)) getLong(key) else null
