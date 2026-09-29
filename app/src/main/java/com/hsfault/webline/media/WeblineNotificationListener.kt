package com.hsfault.webline.media

import android.service.notification.NotificationListenerService

/**
 * Turning this on in Notification access lets WEBLINE read what's playing
 * (and, in the next drop, show notifications in the custom panel).
 */
class WeblineNotificationListener : NotificationListenerService()