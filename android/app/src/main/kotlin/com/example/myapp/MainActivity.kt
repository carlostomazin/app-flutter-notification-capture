package com.example.notif_reader

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.StatusBarNotification
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Canal de eventos: envia notificações para o Dart
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, "notifications.stream")
            .setStreamHandler(NotificationsStreamHandler)

        // Canal de métodos: comandos de controle
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "notifications.ctrl")
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "openSettings" -> {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        result.success(true)
                    }
                    "isEnabled" -> {
                        result.success(isListenerEnabled(this))
                    }
                    "getActive" -> {
                        val list = NotificationBridge.getActive()
                        result.success(list)
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun isListenerEnabled(context: Context): Boolean {
        val cn = ComponentName(context, NotifListenerService::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(cn.flattenToString(), ignoreCase = true)
    }
}

// StreamHandler singleton
object NotificationsStreamHandler : EventChannel.StreamHandler {
    @Volatile
    private var sink: EventChannel.EventSink? = null

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        sink = events
        NotificationBridge.sink = events
    }

    override fun onCancel(arguments: Any?) {
        sink = null
        NotificationBridge.sink = null
    }

    fun send(map: Map<String, Any?>) {
        sink?.success(map)
    }
}

// Ponte entre o service e o Flutter
object NotificationBridge {
    @Volatile
    var service: NotifListenerService? = null

    @Volatile
    var sink: EventChannel.EventSink? = null

    fun toMap(sbn: StatusBarNotification): Map<String, Any?> {
        val n = sbn.notification
        val extras = n.extras
        return mapOf(
            "package" to sbn.packageName,
            "title" to (extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""),
            "text" to (extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""),
            "subText" to (extras?.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""),
            "category" to (n.category ?: ""),
            "channelId" to (n.channelId ?: ""),
            "postTime" to sbn.postTime,
            "id" to sbn.id
        )
    }

    fun getActive(): List<Map<String, Any?>> {
        val s = service ?: return emptyList()
        return s.activeNotifications?.map { toMap(it) } ?: emptyList()
    }
}
