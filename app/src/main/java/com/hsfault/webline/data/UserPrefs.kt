package com.hsfault.webline.data

import android.content.Context

/** Small user settings: greeting name and lock cover on/off. */
class UserPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("webline_user", Context.MODE_PRIVATE)

    var name: String
        get() = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME
        set(value) {
            prefs.edit().putString(KEY_NAME, value.trim()).apply()
        }

    var lockCoverEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LOCK, value).apply()
        }

    companion object {
        const val DEFAULT_NAME = "Zeeshan"
        private const val KEY_NAME = "name"
        private const val KEY_LOCK = "lock_cover"
    }
}