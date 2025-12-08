package com.kasper0_0.smarthome

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

class NotificationSettingsModule(private val context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {

    companion object {
        private const val TAG = "NotificationSettingsModule"
        private const val PREFS_NAME = "notification_settings"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_VOLUME_PERCENTAGE = "volume_percentage"
        
        
        fun getVibrationPattern(context: Context): LongArray {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
            
            return if (vibrationEnabled) {
                longArrayOf(0, 300, 100, 300) // Стандартная вибрация
            } else {
                longArrayOf(0) // Нет вибрации
            }
        }

        fun getVibrationAmplitude(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
            return if (vibrationEnabled) 255 else 0
        }
        
        
        fun createVibrationEffect(context: Context): VibrationEffect? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
            
            if (!vibrationEnabled) return null
            
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect.createOneShot(500L, 255)
            } else {
                null
            }
        }
    }

    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private lateinit var prefs: SharedPreferences

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.i(TAG, "NotificationSettingsModule initialized")
    }

    override fun getName(): String = "NotificationSettingsModule"

    @ReactMethod
    fun getCurrentVolume(promise: Promise) {
        try {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            val volumePercentage = if (maxVolume > 0) (currentVolume * 100 / maxVolume) else 0
            
            val result: WritableMap = Arguments.createMap()
            result.putInt("currentVolume", currentVolume)
            result.putInt("maxVolume", maxVolume)
            result.putInt("volumePercentage", volumePercentage)
            
            Log.i(TAG, "Current notification volume: $currentVolume/$maxVolume ($volumePercentage%)")
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current volume", e)
            promise.reject("VOLUME_GET_ERROR", "Error getting current volume: ${e.message}")
        }
    }

    @ReactMethod
    fun setNotificationVolume(volumePercentage: Int, promise: Promise) {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            val targetVolume = (volumePercentage * maxVolume / 100).coerceIn(0, maxVolume)
            
            audioManager.setStreamVolume(
                AudioManager.STREAM_NOTIFICATION,
                targetVolume,
                0 // No flags
            )
            
            // Сохраняем настройку
            prefs.edit().putInt(KEY_VOLUME_PERCENTAGE, volumePercentage).apply()
            
            Log.i(TAG, "Notification volume set to $targetVolume/$maxVolume ($volumePercentage%)")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting notification volume", e)
            promise.reject("VOLUME_SET_ERROR", "Error setting notification volume: ${e.message}")
        }
    }

    @ReactMethod
    fun setVibrationEnabled(enabled: Boolean, promise: Promise) {
        try {
            // Сохраняем состояние включения/выключения вибрации
            prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
            
            Log.i(TAG, "Vibration enabled: $enabled")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting vibration enabled", e)
            promise.reject("VIBRATION_ENABLED_ERROR", "Error setting vibration enabled: ${e.message}")
        }
    }

    @ReactMethod
    fun getVibrationEnabled(promise: Promise) {
        try {
            val enabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true) // По умолчанию true
            promise.resolve(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting vibration enabled", e)
            promise.reject("VIBRATION_ENABLED_GET_ERROR", "Error getting vibration enabled: ${e.message}")
        }
    }

    @ReactMethod
    fun testVibration(pattern: ReadableMap, promise: Promise) {
        try {
            val duration = pattern.getInt("duration")
            val intensity = pattern.getInt("intensity")
            
            // Сохраняем состояние включения/выключения вибрации
            prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, intensity > 0).apply()
            
            // Создаем паттерн вибрации аналогично методу getVibrationPattern
            val vibrationPattern = if (intensity > 0) {
                longArrayOf(0, 300, 100, 300) // Стандартная вибрация
            } else {
                longArrayOf(0) // Нет вибрации
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Для Android 8+ используем VibrationEffect с паттерном
                val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1) // -1 означает не повторять
                vibrator.vibrate(vibrationEffect)
            } else {
                // Для старых версий используем простую вибрацию
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration.toLong())
            }
            
            Log.i(TAG, "Test vibration: enabled=${intensity > 0}, pattern=${vibrationPattern.contentToString()}")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error testing vibration", e)
            promise.reject("VIBRATION_ERROR", "Error testing vibration: ${e.message}")
        }
    }

    @ReactMethod
    fun testNotificationSound(promise: Promise) {
        try {
            // Создаем канал для тестирования с кастомным звуком
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                
                val testChannel = android.app.NotificationChannel(
                    "test-channel",
                    "Test Notifications",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Test channel for custom sound"
                    setShowBadge(true)
                    enableLights(true)
                    enableVibration(true)
                    
                    // Используем звук sound.mp3
                    val soundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/raw/sound")
                    setSound(soundUri, null)
                }
                
                notificationManager.createNotificationChannel(testChannel)
            }
            
            // Воспроизводим звук sound.mp3
            val soundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/raw/sound")
            
            val notification = androidx.core.app.NotificationCompat.Builder(context, "test-channel")
                .setContentTitle("Тест кастомного звука")
                .setContentText("Проверка звука sound.mp3")
                .setSmallIcon(R.drawable.ic_notify)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(soundUri)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(9998, notification)
            
            Log.i(TAG, "Test notification sound played with sound.mp3")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error testing notification sound", e)
            promise.reject("SOUND_ERROR", "Error testing notification sound: ${e.message}")
        }
    }

    @ReactMethod
    fun getVibrationCapabilities(promise: Promise) {
        try {
            val hasVibrator = vibrator.hasVibrator()
            val hasAmplitudeControl = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.hasAmplitudeControl()
            } else {
                false
            }
            
            val result: WritableMap = Arguments.createMap()
            result.putBoolean("hasVibrator", hasVibrator)
            result.putBoolean("hasAmplitudeControl", hasAmplitudeControl)
            
            Log.i(TAG, "Vibration capabilities: hasVibrator=$hasVibrator, hasAmplitudeControl=$hasAmplitudeControl")
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting vibration capabilities", e)
            promise.reject("CAPABILITIES_ERROR", "Error getting vibration capabilities: ${e.message}")
        }
    }

    @ReactMethod
    fun forceUpdateNotificationChannel(promise: Promise) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                
                // Получаем текущие настройки вибрации
                val vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
                
                // Создаем новый канал ESP с уникальным именем на основе настроек
                val channelId = "esp-channel-${if (vibrationEnabled) "vib" else "novib"}"
                
                try {
                    // Удаляем старый канал если он существует
                    notificationManager.deleteNotificationChannel("esp-channel")
                    notificationManager.deleteNotificationChannel("esp-channel-vib")
                    notificationManager.deleteNotificationChannel("esp-channel-novib")
                    
                    val soundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/raw/sound")
                    val channel = android.app.NotificationChannel(
                        channelId,
                        "ESP Notifications",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Channel for ESP device notifications"
                        setShowBadge(true)
                        enableLights(true)
                        enableVibration(vibrationEnabled) // Применяем настройку вибрации
                        
                        // Принудительно устанавливаем звук sound.mp3
                        val audioAttributes = android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                        setSound(soundUri, audioAttributes)
                        Log.d(TAG, "Created new ESP channel: $channelId, vibration: $vibrationEnabled, sound: $soundUri")
                    }
                    
                    notificationManager.createNotificationChannel(channel)
                    Log.i(TAG, "New ESP channel created successfully: $channelId")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not create new ESP channel: ${e.message}")
                }
                
                // Для канала сервиса не удаляем его, а только обновляем настройки если возможно
                try {
                    val serviceChannel = notificationManager.getNotificationChannel("esp-service-channel")
                    if (serviceChannel != null) {
                        // Канал существует, но мы не можем его изменить, так как он используется сервисом
                        Log.i(TAG, "Service channel exists but cannot be modified while service is running")
                    } else {
                        // Канал не существует, создаем его
                        val soundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/raw/sound")
                        val newServiceChannel = android.app.NotificationChannel(
                            "esp-service-channel",
                            "ESP Service Notifications",
                            android.app.NotificationManager.IMPORTANCE_HIGH
                        ).apply {
                            description = "Channel for ESP foreground service notifications"
                            setShowBadge(true)
                            enableLights(true)
                            enableVibration(true) // Сервис всегда с вибрацией
                            
                            // Принудительно устанавливаем звук sound.mp3
                            val audioAttributes = android.media.AudioAttributes.Builder()
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                .build()
                            setSound(soundUri, audioAttributes)
                        }
                        
                        notificationManager.createNotificationChannel(newServiceChannel)
                        Log.i(TAG, "Service channel created successfully with custom sound")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update service channel: ${e.message}")
                }
                
                Log.i(TAG, "Notification channels update completed. New channel: $channelId, vibration: $vibrationEnabled")
                promise.resolve(channelId) // Возвращаем ID нового канала
            } else {
                Log.w(TAG, "Notification channels not supported on this Android version")
                promise.resolve("esp-channel")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification channels", e)
            promise.reject("CHANNEL_UPDATE_ERROR", "Error updating notification channels: ${e.message}")
        }
    }

    @ReactMethod
    fun setNotificationChannelSettings(channelId: String, importance: Int, enableSound: Boolean, enableVibration: Boolean, promise: Promise) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                
                // Сначала удаляем существующий канал, если он есть
                notificationManager.deleteNotificationChannel(channelId)
                
                val channel = android.app.NotificationChannel(
                    channelId,
                    "ESP Notifications",
                    importance
                ).apply {
                    description = "Channel for ESP device notifications"
                    setShowBadge(true)
                    enableLights(true)
                    enableVibration(enableVibration)
                    
                    if (enableSound) {
                        // Используем звук sound.mp3 из assets
                        val soundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/raw/sound")
                        setSound(soundUri, null)
                    } else {
                        setSound(null, null)
                    }
                }
                
                notificationManager.createNotificationChannel(channel)
                Log.i(TAG, "Notification channel updated: sound=$enableSound, vibration=$enableVibration")
                promise.resolve(true)
            } else {
                Log.w(TAG, "Notification channels not supported on this Android version")
                promise.resolve(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting notification channel settings", e)
            promise.reject("CHANNEL_ERROR", "Error setting notification channel settings: ${e.message}")
        }
    }


}
