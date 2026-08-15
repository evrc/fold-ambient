package com.example.foldambient.ambient

import android.content.Context

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

    return runCatching { AmbientPageDeckCodec.normalize(AmbientPageDeckCodec.decode(storedDeck)) }
      .getOrElse { DefaultAmbientPages.createDeck() }
      .also(::saveDeck)
  }

  fun saveDeck(deck: AmbientPageDeck) {
    preferences.edit().putString(KEY_DECK_JSON, AmbientPageDeckCodec.encode(deck)).apply()
  }
}

private const val KEY_DECK_JSON = "deck_json"
