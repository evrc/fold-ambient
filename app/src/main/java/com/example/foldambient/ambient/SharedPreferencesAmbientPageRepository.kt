package com.example.foldambient.ambient

import android.content.Context
import android.util.Log

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

    val recoveryLogger = AmbientPageDeckRecoveryLogger { message -> Log.w(LogTag, message) }
    return runCatching { AmbientPageDeckCodec.decode(storedDeck, recoveryLogger) }
      .getOrElse { error ->
        Log.w(LogTag, "stored page deck is unreadable, using default deck", error)
        DefaultAmbientPages.createDeck()
      }
      .also(::saveDeck)
  }

  fun saveDeck(deck: AmbientPageDeck) {
    preferences.edit().putString(KEY_DECK_JSON, AmbientPageDeckCodec.encode(deck)).apply()
  }
}

private const val KEY_DECK_JSON = "deck_json"
private const val LogTag = "FoldAmbientPersistence"
