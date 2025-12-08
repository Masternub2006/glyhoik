package com.kasper0_0.smarthome

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.app.NotificationChannel
import android.app.NotificationManager

class ForegroundServiceModule(private val context: ReactApplicationContext) :
    ReactContextBaseJavaModule(context) {

    companion object {
        private const val TAG = "ForegroundServiceModule"
        var reactContext: ReactApplicationContext? = null
    }

    private lateinit var localBroadcastManager: LocalBroadcastManager
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { handleBroadcast(it) }
        }
    }

    init {
        reactContext = context
        localBroadcastManager = LocalBroadcastManager.getInstance(context)
        setupBroadcastReceiver()
        Log.i(TAG, "ForegroundServiceModule initialized")
    }

    override fun getName(): String = "ForegroundServiceModule"

    @ReactMethod
    fun startService(title: String, text: String, url: String, promise: Promise) {
        try {
            Log.i(TAG, "Starting service with url: $url")
            
            // Валидация параметров
            if (title.isBlank() || text.isBlank() || url.isBlank()) {
                promise.reject("INVALID_PARAMETERS", "Title, text and URL cannot be empty")
                return
            }

            // Проверка разрешений
            if (!checkPermissions()) {
                promise.reject("PERMISSION_DENIED", "Required permissions not granted")
                return
            }

            // Проверка валидности URL
            if (!isValidWebSocketUrl(url)) {
                promise.reject("INVALID_URL", "Invalid WebSocket URL format")
                return
            }

            val intent = Intent(context, ForegroundService::class.java).apply {
                putExtra(ForegroundService.EXTRA_TITLE, title)
                putExtra(ForegroundService.EXTRA_TEXT, text)
                putExtra(ForegroundService.EXTRA_URL, url)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            // Принудительно обновляем канал уведомлений с кастомным звуком
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.deleteNotificationChannel("esp-channel")
                
                val channel = NotificationChannel(
                    "esp-channel",
                    "ESP Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for ESP device notifications"
                    setShowBadge(true)
                    enableLights(true)
                    enableVibration(true)
                    
                    // Всегда используем звук sound.mp3
                    val soundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/raw/sound")
                    setSound(soundUri, null)
                }
                
                notificationManager.createNotificationChannel(channel)
                Log.i(TAG, "Notification channel updated with custom sound")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update notification channel", e)
            }
            
            Log.i(TAG, "Service start request sent successfully")
            promise.resolve(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
            promise.reject("SERVICE_START_ERROR", "Failed to start service: ${e.message}")
        }
    }

    @ReactMethod
    fun isServiceRunning(promise: Promise) {
        try {
            // Просто возвращаем true, так как сервис должен работать постоянно
            // после запуска приложения
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check service status", e)
            promise.reject("SERVICE_CHECK_ERROR", "Failed to check service status: ${e.message}")
        }
    }

    @ReactMethod
    fun updateServiceNotification(promise: Promise) {
        try {
            val intent = Intent(context, ForegroundService::class.java)
            intent.action = "UPDATE_NOTIFICATION"
            context.startService(intent)
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update service notification", e)
            promise.reject("UPDATE_ERROR", "Failed to update service notification: ${e.message}")
        }
    }

    @ReactMethod
    fun checkPermissions(promise: Promise) {
        try {
            val hasPermissions = checkPermissions()
            promise.resolve(hasPermissions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check permissions", e)
            promise.reject("PERMISSION_CHECK_ERROR", "Failed to check permissions: ${e.message}")
        }
    }

    @ReactMethod
    fun requestNotificationPermission(promise: Promise) {
        try {
            Log.i(TAG, "Requesting notification permission...")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ - запрашиваем POST_NOTIFICATIONS
                val activity = currentActivity
                if (activity != null) {
                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) 
                        == PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "POST_NOTIFICATIONS permission already granted")
                        promise.resolve(true)
                    } else {
                        Log.i(TAG, "Requesting POST_NOTIFICATIONS permission")
                        // Здесь нужно будет обработать результат в MainActivity
                        promise.resolve(false)
                    }
                } else {
                    Log.w(TAG, "Activity is null, cannot request permission")
                    promise.resolve(false)
                }
            } else {
                // Android 12 и ниже - разрешения даются автоматически
                Log.i(TAG, "Android version < 13, permissions granted automatically")
                promise.resolve(true)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request notification permission", e)
            promise.reject("PERMISSION_REQUEST_ERROR", "Failed to request permission: ${e.message}")
        }
    }

    @ReactMethod
    fun testNotification(promise: Promise) {
        try {
            Log.i(TAG, "Testing notification...")
            
            // Проверяем разрешения
            if (!checkPermissions()) {
                Log.w(TAG, "Permissions not granted")
                promise.reject("PERMISSION_DENIED", "Required permissions not granted")
                return
            }

            showTestNotification(promise)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send test notification", e)
            promise.reject("NOTIFICATION_ERROR", "Failed to send notification: ${e.message}")
        }
    }

    private fun showTestNotification(promise: Promise) {
        try {
            // Создаем тестовое уведомление
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Создаем канал если его нет
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "esp-channel",
                    "ESP Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for ESP notifications"
                    setShowBadge(true)
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Создаем уведомление
            val notification = androidx.core.app.NotificationCompat.Builder(context, "esp-channel")
                .setContentTitle("Тестовое уведомление")
                .setContentText("Это тестовое уведомление!")
                .setSmallIcon(R.drawable.ic_notify)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(9999, notification)
            
            Log.i(TAG, "Test notification sent successfully")
            promise.resolve(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send test notification", e)
            promise.reject("NOTIFICATION_ERROR", "Failed to send notification: ${e.message}")
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            val foregroundServicePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
            
            notificationPermission && foregroundServicePermission
        } else {
            // Для Android < 13 проверяем только FOREGROUND_SERVICE
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isValidWebSocketUrl(url: String): Boolean {
        return try {
            url.startsWith("ws://") || url.startsWith("wss://")
        } catch (e: Exception) {
            false
        }
    }

    private fun setupBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(ForegroundService.ACTION_WEBSOCKET_STATUS)
            addAction(ForegroundService.ACTION_WEBSOCKET_MESSAGE)
            addAction(ForegroundService.ACTION_WEBSOCKET_ERROR)
            addAction("com.kasper0_0.smarthome.SHOW_ALERT")
        }
        localBroadcastManager.registerReceiver(broadcastReceiver, filter)
        Log.i(TAG, "Broadcast receiver registered")
    }

    private fun handleBroadcast(intent: Intent) {
        when (intent.action) {
            ForegroundService.ACTION_WEBSOCKET_STATUS -> {
                val status = intent.getBooleanExtra(ForegroundService.EXTRA_STATUS, false)
                sendEventToReactNative("WebSocketConnectionStatus", status)
            }
            ForegroundService.ACTION_WEBSOCKET_MESSAGE -> {
                val message = intent.getStringExtra(ForegroundService.EXTRA_MESSAGE) ?: ""
                sendEventToReactNative("WebSocketMessage", message)
            }
            ForegroundService.ACTION_WEBSOCKET_ERROR -> {
                val error = intent.getStringExtra(ForegroundService.EXTRA_ERROR) ?: ""
                sendEventToReactNative("WebSocketError", error)
            }
            "com.kasper0_0.smarthome.SHOW_ALERT" -> {
                val alertData = intent.getStringExtra("alert_data") ?: ""
                sendEventToReactNative("ShowAlert", alertData)
            }
        }
    }

    private fun sendEventToReactNative(eventName: String, data: Any) {
        try {
            reactContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                ?.emit(eventName, data)
            Log.d(TAG, "Event sent to React Native: $eventName with data: $data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send event to React Native: ${e.message}", e)
        }
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        try {
            localBroadcastManager.unregisterReceiver(broadcastReceiver)
            Log.i(TAG, "Broadcast receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister broadcast receiver", e)
        }
    }
}
