package com.kasper0_0.smarthome

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import java.text.SimpleDateFormat
import java.util.*

class DatabaseModule(private val context: ReactApplicationContext) :
    ReactContextBaseJavaModule(context)
{

    companion object
    {
        private const val TAG = "DatabaseModule"
        private const val DATABASE_NAME = "esp_history.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_HISTORY = "history"

        // Колонки таблицы
        private const val COLUMN_ID = "id"
        private const val COLUMN_DEVICE_ID = "device_id"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    private lateinit var databaseHelper: DatabaseHelper

    init
    {
        databaseHelper = DatabaseHelper(context)
        Log.i(TAG, "DatabaseModule initialized")
    }

    override fun getName(): String = "DatabaseModule"

    @ReactMethod
    fun initDB(promise: Promise)
    {
        try
        {
            databaseHelper.writableDatabase
            Log.i(TAG, "Database initialized successfully")
            promise.resolve(true)
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Failed to initialize database", e)
            promise.reject("DB_INIT_ERROR", "Failed to initialize database: ${e.message}")
        }
    }

    @ReactMethod
    fun insertHistory(
        deviceId: String, type: String, message: String, timestamp: String, promise: Promise
    )
    {
        try
        {
            val db = databaseHelper.writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_DEVICE_ID, deviceId)
                put(COLUMN_TYPE, type)
                put(COLUMN_MESSAGE, message)
                put(COLUMN_TIMESTAMP, timestamp)
            }

            val id = db.insert(TABLE_HISTORY, null, values)
            if (id != -1L)
            {
                Log.i(TAG, "History inserted successfully: $type - $message")
                promise.resolve(id)
            }
            else
            {
                Log.e(TAG, "Failed to insert history")
                promise.reject("INSERT_ERROR", "Failed to insert history")
            }
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error inserting history", e)
            promise.reject("INSERT_ERROR", "Error inserting history: ${e.message}")
        }
    }

    @ReactMethod
    fun getHistory(deviceId: String, promise: Promise)
    {
        try
        {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(TABLE_HISTORY, arrayOf(COLUMN_TYPE, COLUMN_MESSAGE, COLUMN_TIMESTAMP), "$COLUMN_DEVICE_ID = ?", arrayOf(deviceId), null, null, "$COLUMN_TIMESTAMP DESC")

            val result: WritableArray = Arguments.createArray()
            cursor.use {
                while (it.moveToNext())
                {
                    val item: WritableMap = Arguments.createMap()
                    item.putString("type", it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE)))
                    item.putString("message", it.getString(it.getColumnIndexOrThrow(COLUMN_MESSAGE)))
                    item.putString("timestamp", it.getString(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)))
                    result.pushMap(item)
                }
            }

            Log.i(TAG, "Retrieved ${result.size()} history items for device: $deviceId")
            promise.resolve(result)
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error getting history", e)
            promise.reject("GET_ERROR", "Error getting history: ${e.message}")
        }
    }

    @ReactMethod
    fun deleteHistory(deviceId: String, promise: Promise)
    {
        try
        {
            val db = databaseHelper.writableDatabase
            val deletedRows = db.delete(TABLE_HISTORY, "$COLUMN_DEVICE_ID = ?", arrayOf(deviceId))
            Log.i(TAG, "Deleted $deletedRows history items for device: $deviceId")
            promise.resolve(deletedRows)
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error deleting history", e)
            promise.reject("DELETE_ERROR", "Error deleting history: ${e.message}")
        }
    }

    @ReactMethod
    fun clearAllHistory(promise: Promise)
    {
        try
        {
            val db = databaseHelper.writableDatabase
            val deletedRows = db.delete(TABLE_HISTORY, null, null)
            Log.i(TAG, "Deleted all history items: $deletedRows")
            promise.resolve(deletedRows)
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error clearing all history", e)
            promise.reject("CLEAR_ERROR", "Error clearing all history: ${e.message}")
        }
    }

    // Внутренний класс для работы с базой данных
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
                Log.i(TAG, "Database table created successfully")
            }
            catch (e: Exception)
            {
                Log.e(TAG, "Error creating database table", e)
            }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
        {
            try
            {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
                onCreate(db)
                Log.i(TAG, "Database upgraded from version $oldVersion to $newVersion")
            }
            catch (e: Exception)
            {
                Log.e(TAG, "Error upgrading database", e)
            }
        }
    }
}
