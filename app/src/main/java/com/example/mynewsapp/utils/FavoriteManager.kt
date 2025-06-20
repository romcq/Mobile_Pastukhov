package com.example.mynewsapp.utils

import android.content.Context
import android.content.SharedPreferences

object FavoriteManager {
    private const val PREFS_NAME = "favorites_prefs"
    private const val FAVORITES_KEY = "favorites_set"

    fun isFavorite(context: Context, url: String): Boolean {
        return getFavorites(context).contains(url)
    }

    fun toggleFavorite(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = getFavorites(context).toMutableSet()
        if (set.contains(url)) {
            set.remove(url)
        } else {
            set.add(url)
        }
        prefs.edit().putStringSet(FAVORITES_KEY, set).apply()
    }

    fun getFavorites(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(FAVORITES_KEY, emptySet()) ?: emptySet()
    }
}
