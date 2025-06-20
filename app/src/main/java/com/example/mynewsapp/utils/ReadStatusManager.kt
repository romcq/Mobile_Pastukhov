package com.example.mynewsapp.utils

import android.content.Context
import android.content.SharedPreferences

object ReadStatusManager {
    private const val PREFS_NAME = "read_news"
    private const val READ_SET_KEY = "read_urls"

    fun markAsRead(context: Context, url: String) {
        val prefs = getPrefs(context)
        val set = prefs.getStringSet(READ_SET_KEY, mutableSetOf()) ?: mutableSetOf()
        set.add(url)
        prefs.edit().putStringSet(READ_SET_KEY, set).apply()
    }

    fun isRead(context: Context, url: String): Boolean {
        val prefs = getPrefs(context)
        return prefs.getStringSet(READ_SET_KEY, emptySet())?.contains(url) ?: false
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
