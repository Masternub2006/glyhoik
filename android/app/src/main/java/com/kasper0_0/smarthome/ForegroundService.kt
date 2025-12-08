package com.kasper0_0.smarthome

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.io.IOException
import android.os.VibrationEffect
import android.media.AudioAttributes

class ForegroundService : Service()
{

    companion object
    {
        const val TAG = "ForegroundService"
        const val CHANNEL_ID = "esp-channel"
        const val SERVICE_CHANNEL_ID = "esp-service-channel"
        const val EXTRA_URL = "websocket_url"
        const val EXTRA_TITLE = "notification_title"
        const val EXTRA_TEXT = "notification_text"
        const val NOTIFICATION_ID = 1001

        const val ACTION_WEBSOCKET_STATUS = "com.kasper0_0.smarthome.WEBSOCKET_STATUS"
        const val ACTION_WEBSOCKET_MESSAGE = "com.kasper0_0.smarthome.WEBSOCKET_MESSAGE"
        const val ACTION_WEBSOCKET_ERROR = "com.kasper0_0.smarthome.WEBSOCKET_ERROR"
        const val EXTRA_STATUS = "status"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ERROR = "error"

        private const val DATABASE_NAME = "esp_history.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_HISTORY = "history"
        private const val COLUMN_ID = "id"
        const val COLUMN_DEVICE_ID = "device_id"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val DEVICE_ID = "ESP1337"

        private const val RECONNECT_DELAY_MS = 5000L
        private const val MAX_RECONNECT_ATTEMPTS = 10

        private const val ROUTER_URL = "https://www.kbkontur.ru/iot/search.php"
        private const val AP_IP = "192.168.4.1"
    }

    private var socketUrl: String? = null
    private var webSocket: WebSocket? = null
    private var isServiceRunning = false
    private var isConnected = false
    private var reconnectAttempts = 0
    private var currentNotificationChannelId: String = CHANNEL_ID
    private var isAppInForeground = true
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var reconnectHandler: Handler
    private var hasShownNoConnectionNotification = false // Флаг для показа уведомления только один раз
    private var hasShownMacAlert = false // Флаг для показа MAC только один раз при подключении
    private var hasShownConnectionErrorAlert = false // Флаг для показа ошибки подключения только один раз

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate()
    {
        super.onCreate()
        isServiceRunning = false
        isConnected = false
        reconnectAttempts = 0
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        databaseHelper = DatabaseHelper(this)
        reconnectHandler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        if (intent == null)
        {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == "UPDATE_NOTIFICATION")
        {
            updateServiceNotification()
            return START_STICKY
        }

        if (intent.action == "APP_FOREGROUND")
        {
            isAppInForeground = true
            return START_STICKY
        }

        if (intent.action == "APP_BACKGROUND")
        {
            isAppInForeground = false
            return START_STICKY
        }

        if (!checkPermissions())
        {
            sendEvent("WebSocketError", "Required permissions not granted")
            stopSelf()
            return START_NOT_STICKY
        }

        socketUrl = intent.getStringExtra(EXTRA_URL)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "ESP Service"
        val text = intent.getStringExtra(EXTRA_TEXT) ?: "WebSocket is running"

        if (socketUrl.isNullOrEmpty())
        {
            try
            {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, buildNotification(title, text))
                isServiceRunning = true
                detectConnectionAndConnect()
            }
            catch (e: Exception)
            {
                sendEvent("WebSocketError", "Failed to start service: ${e.message}")
                stopSelf()
                return START_NOT_STICKY
            }
        }
        else
        {
            try
            {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, buildNotification(title, text))
                isServiceRunning = true

                connectWebSocket(socketUrl!!)
            }
            catch (e: Exception)
            {
                sendEvent("WebSocketError", "Failed to start service: ${e.message}")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    override fun onDestroy()
    {
        super.onDestroy()
        isServiceRunning = false
        isConnected = false

        reconnectHandler.removeCallbacksAndMessages(null)

        try
        {
            webSocket?.let { ws ->
                ws.close(1000, "Service destroyed")
                webSocket = null
            }
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
        catch (e: Exception)
        {
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?)
    {
        super.onTaskRemoved(rootIntent)
    }

    private fun checkPermissions(): Boolean
    {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        else
        {
            true
        }
    }

    private fun buildNotification(title: String, text: String): Notification
    {
        val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/raw/sound")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            try
            {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val existingChannel = notificationManager.getNotificationChannel(SERVICE_CHANNEL_ID)
                if (existingChannel == null)
                {
                    val serviceChannel = NotificationChannel(SERVICE_CHANNEL_ID, "ESP Service Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Channel for ESP foreground service notifications"
                        setShowBadge(true)
                        enableLights(true)
                        enableVibration(true)

                        val audioAttributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                        setSound(soundUri, audioAttributes)
                    }

                    notificationManager.createNotificationChannel(serviceChannel)
                }
            }
            catch (e: Exception)
            {
            }
        }

        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notify)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)
            .setSound(soundUri)

        val notification = notificationBuilder.build()
        return notification
    }

    private fun createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            try
            {
                val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/raw/sound")

                val channel = NotificationChannel(CHANNEL_ID, "ESP Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Channel for ESP WebSocket service and notifications"
                    setShowBadge(true)
                    enableLights(true)
                    enableVibration(true)

                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                }

                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
            catch (e: Exception)
            {
            }
        }
    }

    private fun connectWebSocket(url: String)
    {
        try
        {
            val request = Request.Builder()
                .url(url)
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener()
            {
                override fun onOpen(ws: WebSocket, response: Response)
                {
                    isConnected = true
                    reconnectAttempts = 0
                    hasShownNoConnectionNotification = false // Сбрасываем флаг при успешном подключении
                    hasShownMacAlert = false // Сбрасываем флаг MAC при новом подключении
                    hasShownConnectionErrorAlert = false // Сбрасываем флаг ошибки при новом подключении
                    sendEvent("WebSocketConnectionStatus", true)
                    updateServiceNotification()
                }

                override fun onMessage(ws: WebSocket, text: String)
                {
                    sendEvent("WebSocketMessage", text)
                    saveMessageToDatabase(text)
                    showNotificationFromESP(text)
                }

                override fun onMessage(ws: WebSocket, bytes: ByteString)
                {
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String)
                {
                    handleConnectionLost()
                    ws.close(1000, null)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?)
                {
                    handleConnectionLost()
                    sendEvent("WebSocketError", t.message ?: "Unknown WebSocket error")
                }
            })
        }
        catch (e: Exception)
        {
            handleConnectionLost()
            sendEvent("WebSocketError", "Failed to create connection: ${e.message}")
        }
    }

    private fun handleConnectionLost()
    {
        isConnected = false
        sendEvent("WebSocketConnectionStatus", false)

        // Показываем уведомление только если еще не показывали
        if (!hasShownNoConnectionNotification)
        {
            updateServiceNotification()
            hasShownNoConnectionNotification = true
        }

        // Показываем alert с ошибкой только один раз
        if (!hasShownConnectionErrorAlert)
        {
            sendEvent("ShowAlert", "Ошибка подключения|Потеряно соединение с ESP. Попытка переподключения...")
            hasShownConnectionErrorAlert = true
        }

        scheduleReconnect()
    }

    private fun scheduleReconnect()
    {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && isServiceRunning)
        {
            reconnectAttempts++

            reconnectHandler.postDelayed({
                if (isServiceRunning && !isConnected)
                {
                    detectConnectionAndConnect()
                }
            }, RECONNECT_DELAY_MS)
        }
        else if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS)
        { // Показываем уведомление только при достижении максимума попыток
            updateServiceNotification()
        }
    }

    private fun detectConnectionAndConnect()
    {
        try
        {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(ROUTER_URL)
                .build()

            client.newCall(request)
                .enqueue(object : Callback
                {
                    override fun onFailure(call: Call, e: IOException)
                    {
                        connectToAP()
                    }

                    override fun onResponse(call: Call, response: Response)
                    {
                        try
                        {
                            val responseBody = response.body?.string()
                            if (responseBody != null && responseBody.contains("local_ip"))
                            {
                                val ipMatch = Regex("\"local_ip\"\\s*:\\s*\"([^\"]+)\"").find(responseBody)
                                val ip = ipMatch?.groupValues?.get(1)
                                if (ip != null)
                                {
                                    val url = "ws://$ip/ws"
                                    connectWebSocket(url)
                                    return
                                }
                            }
                            connectToAP()
                        }
                        catch (e: Exception)
                        {
                            connectToAP()
                        }
                        finally
                        {
                            response.close()
                        }
                    }
                })
        }
        catch (e: Exception)
        {
            connectToAP()
        }
    }

    private fun connectToAP()
    {
        val url = "ws://$AP_IP/ws"
        connectWebSocket(url)
    }

    private fun updateNotification(title: String, text: String)
    {
        if (isServiceRunning)
        {
            try
            {
                val notification = buildNotification(title, text)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            catch (e: Exception)
            {
            }
        }
    }

    private fun updateServiceNotification()
    {
        if (isServiceRunning)
        {
            try
            {
                val title = "Умный дом"
                val text = if (isConnected)
                {
                    "Умный дом работает в фоновом режиме"
                }
                else
                {
                    "Подключитесь к хабу для получения уведомлений"
                }

                val notification = buildNotification(title, text)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            catch (e: Exception)
            {
            }
        }
    }

    private fun showNotificationFromESP(message: String)
    {
        try
        {
            if (message.contains("\"mac\"") && message.contains("\"rssi\"") && message.contains("\"gpio\""))
            {
                val macMatch = Regex("\"mac\"\\s*:\\s*\"([^\"]+)\"").find(message)
                val rssiMatch = Regex("\"rssi\"\\s*:\\s*([^,}]+)").find(message)
                val gpioMatch = Regex("\"gpio\"\\s*:\\s*\\{([^}]+)\\}").find(message)

                val mac = macMatch?.groupValues?.get(1) ?: "Unknown"
                val rssi = rssiMatch?.groupValues?.get(1)
                    ?.trim() ?: "Unknown"
                val gpioData = gpioMatch?.groupValues?.get(1) ?: "{}"

                // Показываем MAC только один раз при подключении, не сохраняем в историю
                if (isConnected && !hasShownMacAlert && isAppInForeground)
                {
                    val alertTitle = "Данные устройства"
                    val alertMessage = "MAC: $mac\nRSSI: $rssi\nGPIO: $gpioData"
                    sendEvent("ShowAlert", "$alertTitle|$alertMessage")
                    hasShownMacAlert = true
                }

                // НЕ показываем уведомление для MAC и НЕ сохраняем в базу
            }
            else
            {
                val messageData = parseMessageData(message)
                val notificationTitle = messageData?.type?.let { type ->
                    when (type)
                    {
                        "doorbell" -> "Дверной звонок"
                        "babycry" -> "Ребёнок плачет"
                        "intercom" -> "Домофон"
                        "smoke" -> "Датчик дыма"
                        "gas" -> "Утечка газа"
                        "phone" -> "Телефон"
                        "batterylow" -> "Разряжена батарея"
                        "gpio" -> "Изменение GPIO"
                        else -> "Сообщение от ESP"
                    }
                } ?: "Сообщение от ESP"

                val notificationText = messageData?.message ?: message

                if (isAppInForeground)
                {
                    showNotification(notificationTitle, notificationText)
                }
            }
        }
        catch (e: Exception)
        {
        }
    }

    private fun showNotification(title: String, text: String)
    {
        try
        {
            val notificationId = System.currentTimeMillis()
                .toInt()
            val channelId = currentNotificationChannelId

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                try
                {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val existingChannel = notificationManager.getNotificationChannel(channelId)

                    if (existingChannel == null)
                    {
                        val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/raw/sound")
                        val channel = NotificationChannel(channelId, "ESP Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                            description = "Channel for ESP WebSocket service and notifications"
                            setShowBadge(true)
                            enableLights(true)
                            enableVibration(true)

                            val audioAttributes = AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .build()
                            setSound(soundUri, audioAttributes)
                        }

                        notificationManager.createNotificationChannel(channel)
                    }
                }
                catch (e: Exception)
                {
                }
            }

            val notificationBuilder = androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notify)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

            val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/raw/sound")
            notificationBuilder.setSound(soundUri)

            try
            {
                val vibrationPattern = NotificationSettingsModule.getVibrationPattern(this)

                if (vibrationPattern.size > 1)
                {
                    notificationBuilder.setVibrate(vibrationPattern)
                }
            }
            catch (e: Exception)
            {
                notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
            }

            val notification = notificationBuilder.build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        }
        catch (e: Exception)
        {
        }
    }

    private fun sendEvent(eventName: String, data: Any)
    {
        try
        {
            val intent = Intent()
            when (eventName)
            {
                "WebSocketConnectionStatus" ->
                {
                    intent.action = ACTION_WEBSOCKET_STATUS
                    intent.putExtra(EXTRA_STATUS, data as Boolean)
                }

                "WebSocketMessage" ->
                {
                    intent.action = ACTION_WEBSOCKET_MESSAGE
                    intent.putExtra(EXTRA_MESSAGE, data as String)
                }

                "WebSocketError" ->
                {
                    intent.action = ACTION_WEBSOCKET_ERROR
                    intent.putExtra(EXTRA_ERROR, data as String)
                }

                "ShowAlert" ->
                {
                    intent.action = "com.kasper0_0.smarthome.SHOW_ALERT"
                    intent.putExtra("alert_data", data as String)
                }
            }

            if (intent.action != null)
            {
                localBroadcastManager.sendBroadcast(intent)
            }
        }
        catch (e: Exception)
        {
        }
    }

    private fun saveMessageToDatabase(messageText: String)
    {
        try
        { // Не сохраняем MAC сообщения в базу
            if (messageText.contains("\"mac\"") && messageText.contains("\"rssi\"") && messageText.contains("\"gpio\""))
            {
                return
            }

            val messageData = parseMessageData(messageText)
            if (messageData != null)
            {
                val db = databaseHelper.writableDatabase
                val values = ContentValues().apply {
                    put(COLUMN_DEVICE_ID, DEVICE_ID)
                    put(COLUMN_TYPE, messageData.type)
                    put(COLUMN_MESSAGE, messageData.message)
                    put(COLUMN_TIMESTAMP, messageData.timestamp)
                }

                val id = db.insert(TABLE_HISTORY, null, values)
            }
        }
        catch (e: Exception)
        {
        }
    }

    private fun parseMessageData(messageText: String): MessageData?
    {
        return try
        {
            if (messageText.contains("\"mac\"") && messageText.contains("\"rssi\"") && messageText.contains("\"gpio\""))
            {
                val macMatch = Regex("\"mac\"\\s*:\\s*\"([^\"]+)\"").find(messageText)
                val rssiMatch = Regex("\"rssi\"\\s*:\\s*([^,}]+)").find(messageText)
                val gpioMatch = Regex("\"gpio\"\\s*:\\s*\\{([^}]+)\\}").find(messageText)

                val mac = macMatch?.groupValues?.get(1) ?: "Unknown"
                val rssi = rssiMatch?.groupValues?.get(1)
                    ?.trim() ?: "Unknown"
                val gpioData = gpioMatch?.groupValues?.get(1) ?: "{}"

                val message = "MAC: $mac, RSSI: $rssi, GPIO: $gpioData"
                MessageData("device_data", message)
            }
            else when
            {
                messageText.contains("\"alarm\"") ->
                {
                    when
                    {
                        messageText.contains("\"doorbell\"") -> MessageData("doorbell", "Кто-то позвонил в дверь")
                        messageText.contains("\"babycry\"") -> MessageData("babycry", "Ребёнок плачет")
                        messageText.contains("\"intercom\"") -> MessageData("intercom", "Звонит домофон")
                        messageText.contains("\"smoke\"") -> MessageData("smoke", "Сработал датчик дыма")
                        messageText.contains("\"gas\"") -> MessageData("gas", "Сработал датчик утечки бытового газа")
                        messageText.contains("\"phone\"") -> MessageData("phone", "Звонит телефон")
                        messageText.contains("\"test\"") -> MessageData("test", "Проверка работы системы")
                        else ->
                        {
                            val alarmMatch = Regex("\"alarm\":\\s*\\{[^}]*\"([^\"]+)\"[^}]*\\}").find(messageText)
                            if (alarmMatch != null)
                            {
                                val alarmType = alarmMatch.groupValues[1]
                                MessageData(alarmType, "Сработала сигнализация: $alarmType")
                            }
                            else
                            {
                                MessageData("alarm", "Сработала сигнализация")
                            }
                        }
                    }
                }

                messageText.contains("\"batterylow\"") ->
                {
                    when
                    {
                        messageText.contains("\"doorbell\"") -> MessageData("batterylow", "Разряжена батарея датчика дверного звонка")
                        messageText.contains("\"babycry\"") -> MessageData("batterylow", "Разряжена батарея радионяни")
                        messageText.contains("\"intercom\"") -> MessageData("batterylow", "Разряжена батарея домофона")
                        messageText.contains("\"smoke\"") -> MessageData("batterylow", "Разряжена батарея датчика дыма")
                        messageText.contains("\"gas\"") -> MessageData("batterylow", "Разряжена батарея датчика утечки газа")
                        messageText.contains("\"phone\"") -> MessageData("batterylow", "Разряжена батарея датчика телефона")
                        else ->
                        {
                            val batteryMatch = Regex("\"batterylow\":\\s*\\{[^}]*\"([^\"]+)\"[^}]*\\}").find(messageText)
                            if (batteryMatch != null)
                            {
                                val batteryType = batteryMatch.groupValues[1]
                                MessageData("batterylow", "Разряжен датчик: $batteryType")
                            }
                            else
                            {
                                MessageData("batterylow", "Разряжен один из датчиков")
                            }
                        }
                    }
                }

                messageText.contains("\"gpio\"") ->
                {
                    val gpioMatch = Regex("\"gpio\":\\s*\\{[^}]*\"([^\"]+)\"\\s*:\\s*([^,}]+)[^}]*\\}").find(messageText)
                    if (gpioMatch != null)
                    {
                        val gpioPin = gpioMatch.groupValues[1]
                        val gpioValue = gpioMatch.groupValues[2].trim()
                        MessageData("gpio", "GPIO $gpioPin → $gpioValue")
                    }
                    else
                    {
                        MessageData("gpio", "Изменение состояния GPIO")
                    }
                }

                else ->
                {
                    MessageData("unknown", messageText)
                }
            }
        }
        catch (e: Exception)
        {
            null
        }
    }

    private data class MessageData(
        val type: String, val message: String, val timestamp: String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
    )

    private inner class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION)
    {

        override fun onCreate(db: SQLiteDatabase)
        {
            val createTable = """
                CREATE TABLE $TABLE_HISTORY (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_DEVICE_ID TEXT NOT NULL,
                    $COLUMN_TYPE TEXT NOT NULL,
                    $COLUMN_MESSAGE TEXT NOT NULL,
                    $COLUMN_TIMESTAMP TEXT NOT NULL
                )
            """.trimIndent()

            try
            {
                db.execSQL(createTable)
            }
            catch (e: Exception)
            {
            }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
        {
            try
            {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
                onCreate(db)
            }
            catch (e: Exception)
            {
            }
        }
    }
}
