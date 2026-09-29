package com.hsfault.webline.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Saves which apps are pinned to the home pages and the dock. */
class LayoutStore(context: Context) {

    private val prefs = context.getSharedPreferences("webline_layout", Context.MODE_PRIVATE)

    var home by mutableStateOf(read(KEY_HOME))
        private set
    var dock by mutableStateOf(read(KEY_DOCK))
        private set

    val initialized: Boolean
        get() = prefs.getBoolean(KEY_INIT, false)

    fun seed(home: List<String>, dock: List<String>) {
        this.home = home
        this.dock = dock.take(DOCK_SIZE)
        prefs.edit()
            .putString(KEY_HOME, join(this.home))
            .putString(KEY_DOCK, join(this.dock))
            .putBoolean(KEY_INIT, true)
            .apply()
    }

    fun addToHome(key: String) {
        if (key !in home) {
            home = home + key
            save()
        }
    }

    fun removeFromHome(key: String) {
        home = home - key
        save()
    }

    fun addToDock(key: String) {
        if (key !in dock && dock.size < DOCK_SIZE) {
            dock = dock + key
            save()
        }
    }

    fun removeFromDock(key: String) {
        dock = dock - key
        save()
    }

    /** Drops pinned apps that were uninstalled. */
    fun prune(installed: Set<String>) {
        val h = home.filter { it in installed }
        val d = dock.filter { it in installed }
        if (h.size != home.size || d.size != dock.size) {
            home = h
            dock = d
            save()
        }
    }

    private fun save() {
        prefs.edit()
            .putString(KEY_HOME, join(home))
            .putString(KEY_DOCK, join(dock))
            .apply()
    }

    private fun read(key: String): List<String> =
        prefs.getString(key, "").orEmpty().split(SEP).filter { it.isNotBlank() }

    private fun join(list: List<String>): String = list.joinToString(SEP)

    companion object {
        const val DOCK_SIZE = 5
        private const val SEP = ";"
        private const val KEY_HOME = "home"
        private const val KEY_DOCK = "dock"
        private const val KEY_INIT = "initialized"
    }
}