package com.hsfault.webline.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Home page one has 12 fixed slots (matching the design) plus a 5-slot dock.
 * A slot holds an app key, a folder token, or "" for empty.
 */
class LayoutStore(context: Context) {

    private val prefs = context.getSharedPreferences("webline_layout_v2", Context.MODE_PRIVATE)

    var slots by mutableStateOf(readFixed(KEY_SLOTS, SLOT_COUNT))
        private set
    var dock by mutableStateOf(readFixed(KEY_DOCK, DOCK_SIZE))
        private set
    var extras by mutableStateOf(readList(KEY_EXTRAS))
        private set
    var tools by mutableStateOf(readList(KEY_TOOLS))
        private set
    var social by mutableStateOf(readList(KEY_SOCIAL))
        private set

    val initialized: Boolean
        get() = prefs.getBoolean(KEY_INIT, false)

    fun folder(folder: Folder): List<String> = if (folder == Folder.TOOLS) tools else social

    fun seed(slots: List<String>, dock: List<String>, tools: List<String>, social: List<String>) {
        this.slots = List(SLOT_COUNT) { slots.getOrElse(it) { "" } }
        this.dock = List(DOCK_SIZE) { dock.getOrElse(it) { "" } }
        this.tools = tools.distinct()
        this.social = social.distinct()
        this.extras = emptyList()
        save()
        prefs.edit().putBoolean(KEY_INIT, true).apply()
    }

    fun setSlot(index: Int, value: String) {
        if (index !in slots.indices) return
        slots = slots.toMutableList().also { it[index] = value }
        save()
    }

    fun setDock(index: Int, value: String) {
        if (index !in dock.indices) return
        dock = dock.toMutableList().also { it[index] = value }
        save()
    }

    fun addExtra(key: String) {
        if (key !in extras) {
            extras = extras + key
            save()
        }
    }

    fun removeExtra(key: String) {
        extras = extras - key
        save()
    }

    fun addToFolder(folder: Folder, key: String) {
        when (folder) {
            Folder.TOOLS -> if (key !in tools) tools = tools + key
            Folder.SOCIAL -> if (key !in social) social = social + key
        }
        save()
    }

    fun removeFromFolder(folder: Folder, key: String) {
        when (folder) {
            Folder.TOOLS -> tools = tools - key
            Folder.SOCIAL -> social = social - key
        }
        save()
    }

    /** Clears slots pointing at uninstalled apps. Folder slots are kept. */
    fun prune(installed: Set<String>) {
        fun keep(v: String) = v.isEmpty() || v.startsWith("folder:") || v in installed
        val s = slots.map { if (keep(it)) it else "" }
        val d = dock.map { if (keep(it)) it else "" }
        val e = extras.filter { it in installed }
        val t = tools.filter { it in installed }
        val so = social.filter { it in installed }
        if (s != slots || d != dock || e != extras || t != tools || so != social) {
            slots = s; dock = d; extras = e; tools = t; social = so
            save()
        }
    }

    private fun save() {
        prefs.edit()
            .putString(KEY_SLOTS, slots.joinToString(SEP))
            .putString(KEY_DOCK, dock.joinToString(SEP))
            .putString(KEY_EXTRAS, extras.joinToString(SEP))
            .putString(KEY_TOOLS, tools.joinToString(SEP))
            .putString(KEY_SOCIAL, social.joinToString(SEP))
            .apply()
    }

    private fun readFixed(key: String, count: Int): List<String> {
        val parts = prefs.getString(key, null)?.split(SEP) ?: emptyList()
        return List(count) { parts.getOrElse(it) { "" } }
    }

    private fun readList(key: String): List<String> =
        prefs.getString(key, "").orEmpty().split(SEP).filter { it.isNotBlank() }

    companion object {
        const val SLOT_COUNT = 12
        const val DOCK_SIZE = 5
        private const val SEP = ";"
        private const val KEY_SLOTS = "slots"
        private const val KEY_DOCK = "dock"
        private const val KEY_EXTRAS = "extras"
        private const val KEY_TOOLS = "tools"
        private const val KEY_SOCIAL = "social"
        private const val KEY_INIT = "initialized"
    }
}