package com.example.notif_reader

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationBridge.service = this
        // Opcional: enviar as ativas ao conectar
        activeNotifications?.forEach {
            NotificationsStreamHandler.send(NotificationBridge.toMap(it))
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationBridge.service = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        NotificationsStreamHandler.send(NotificationBridge.toMap(sbn))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Se quiser avisar remoções, envie com uma flag:
        // val map = NotificationBridge.toMap(sbn).toMutableMap()
        // map["removed"] = true
        // NotificationsStreamHandler.send(map)
    }
}
