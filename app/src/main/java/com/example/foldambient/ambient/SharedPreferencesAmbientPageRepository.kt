package com.example.foldambient.ambient

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SharedPreferencesAmbientPageRepository(
  context: Context,
) {
  private val preferences =
    context.getSharedPreferences("ambient_pages", Context.MODE_PRIVATE)

  fun loadDeck(): AmbientPageDeck {
    val storedDeck = preferences.getString(KEY_DECK_JSON, null)
    if (storedDeck.isNullOrBlank()) {
      return DefaultAmbientPages.createDeck().also(::saveDeck)
    }

    return runCatching { decodeDeck(storedDeck) }
      .getOrElse { DefaultAmbientPages.createDeck().also(::saveDeck) }
  }

  fun saveDeck(deck: AmbientPageDeck) {
    preferences.edit().putString(KEY_DECK_JSON, encodeDeck(deck)).apply()
  }

  private fun encodeDeck(deck: AmbientPageDeck): String =
    JSONObject()
      .put("selectedPageId", deck.selectedPageId)
      .put(
        "pages",
        JSONArray().also { pages ->
          deck.pages.forEach { page -> pages.put(encodePage(page)) }
        },
      )
      .toString()

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

  private fun decodeDeck(json: String): AmbientPageDeck {
    val source = JSONObject(json)
    val pages = source.getJSONArray("pages").mapObjects(::decodePage)
    return AmbientPageDeck(
      pages = pages,
      selectedPageId = source.getString("selectedPageId"),
    )
  }

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

private const val KEY_DECK_JSON = "deck_json"

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
  List(length()) { index -> transform(getJSONObject(index)) }

private fun JSONObject.toStringMap(): Map<String, String> =
  keys().asSequence().associateWith { key -> getString(key) }

private fun JSONObject.optLongOrNull(key: String): Long? =
  if (has(key)) getLong(key) else null
