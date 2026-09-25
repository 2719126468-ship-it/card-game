package com.example.hellocard.data

import android.content.Context

object DeckStorage {
    private const val PREF = "deck_prefs"
    private const val KEY_MAIN = "main_deck"
    private const val KEY_EXTRA = "extra_deck"

    fun saveMainDeck(context: Context, ids: List<String>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_MAIN, ids.joinToString(",")).apply()
    }

    fun loadMainDeck(context: Context): List<String> {
        val s = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_MAIN, "") ?: ""
        return if (s.isEmpty()) emptyList() else s.split(",").filter { it.isNotEmpty() }
    }

    fun saveExtraDeck(context: Context, ids: List<String>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_EXTRA, ids.joinToString(",")).apply()
    }

    fun loadExtraDeck(context: Context): List<String> {
        val s = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_EXTRA, "") ?: ""
        return if (s.isEmpty()) emptyList() else s.split(",").filter { it.isNotEmpty() }
    }
}
