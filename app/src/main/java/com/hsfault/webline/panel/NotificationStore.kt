package com.hsfault.webline.panel

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class PanelNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
    val title: String,
    val text: String,
    val postTime: Long,
    val clearable: Boolean,
    val autoCancel: Boolean,
    val intent: PendingIntent?,
)

/** Live list of notifications, fed by the notification listener, read by the panel. */
object NotificationStore {

    var items by mutableStateOf<List<PanelNotification>>(emptyList())
        private set
    var connected by mutableStateOf(false)
        private set

    private var listener: NotificationListenerService? = null
    private val labels = HashMap<String, String>()
    private val icons = HashMap<String, ImageBitmap?>()

    fun attach(service: NotificationListenerService) {
        listener = service
        connected = true
        refresh()
    }

    fun detach() {
        listener = null
        connected = false
        items = emptyList()
    }

    fun refresh() {
        val service = listener ?: return
        val active = try {
            service.activeNotifications
        } catch (e: Exception) {
            null
        } ?: return
        items = active
            .filter { shouldShow(service, it) }
            .sortedByDescending { it.postTime }
            .map { toItem(service, it) }
    }

    fun dismiss(key: String) {
        try {
            listener?.cancelNotification(key)
        } catch (e: Exception) {
            // listener not connected
        }
    }

    fun clearAll() {
        try {
            listener?.cancelAllNotifications()
        } catch (e: Exception) {
            // listener not connected
        }
    }

    private fun shouldShow(context: Context, sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (sbn.packageName == context.packageName) return false
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return false
        // Media players already appear in the music card.
        if (n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return false
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)
        return !title.isNullOrBlank() || !text.isNullOrBlank()
    }

    private fun toItem(context: Context, sbn: StatusBarNotification): PanelNotification {
        val n = sbn.notification
        val extras = n.extras
        val pkg = sbn.packageName
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString().orEmpty()
        return PanelNotification(
            key = sbn.key,
            packageName = pkg,
            appName = label(context, pkg),
            icon = icon(context, sbn),
            title = title.ifBlank { text },
            text = if (title.isBlank()) "" else text,
            postTime = sbn.postTime,
            clearable = sbn.isClearable,
            autoCancel = (n.flags and Notification.FLAG_AUTO_CANCEL) != 0,
            intent = n.contentIntent,
        )
    }

    private fun label(context: Context, pkg: String): String = labels.getOrPut(pkg) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun icon(context: Context, sbn: StatusBarNotification): ImageBitmap? = icons.getOrPut(sbn.packageName) {
        try {
            context.packageManager.getApplicationIcon(sbn.packageName).toBitmap(96, 96).asImageBitmap()
        } catch (e: Exception) {
            try {
                sbn.notification.smallIcon?.loadDrawable(context)?.toBitmap(96, 96)?.asImageBitmap()
            } catch (inner: Exception) {
                null
            }
        }
    }
}