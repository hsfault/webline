package com.hsfault.webline.data

import android.content.Context

/** Small user settings (the name shown on the greeting card). */
class UserPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("webline_user", Context.MODE_PRIVATE)

    var name: String
        get() = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME
        set(value) {
            prefs.edit().putString(KEY_NAME, value.trim()).apply()
        }

    companion object {
        const val DEFAULT_NAME = "Zeeshan"
        private const val KEY_NAME = "name"
    }
}