package com.hsfault.webline.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.hsfault.webline.panel.NotificationStore

/**
 * Enabled in Notification access. Gives WEBLINE the playing song (media sessions)
 * and the notification list shown in the custom panel.
 */
class WeblineNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationStore.attach(this)
    }

    override fun onListenerDisconnected() {
        NotificationStore.detach()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        NotificationStore.refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        NotificationStore.refresh()
    }
}