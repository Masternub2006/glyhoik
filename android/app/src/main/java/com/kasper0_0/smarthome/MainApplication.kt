package com.kasper0_0.smarthome

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import com.kasper0_0.smarthome.ForegroundServicePackage
import com.kasper0_0.smarthome.DatabasePackage
import com.kasper0_0.smarthome.NotificationSettingsPackage

import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.ReactHost
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader

import expo.modules.ApplicationLifecycleDispatcher
import expo.modules.ReactNativeHostWrapper

class MainApplication : Application(), ReactApplication
{

    companion object
    {
        private const val TAG = "MainApplication"
    }

    override val reactNativeHost: ReactNativeHost = ReactNativeHostWrapper(this, object :
        DefaultReactNativeHost(this)
    {
        override fun getPackages(): List<ReactPackage>
        {
            try
            {
                val packages = PackageList(this).packages.toMutableList()
                packages.add(ForegroundServicePackage())
                packages.add(DatabasePackage())
                packages.add(NotificationSettingsPackage())
                Log.i(TAG, "Packages loaded successfully: ${packages.size} packages")
                return packages
            }
            catch (e: Exception)
            {
                Log.e(TAG, "Failed to load packages", e) // Возвращаем базовые пакеты в случае ошибки
                return listOf(ForegroundServicePackage(), DatabasePackage(), NotificationSettingsPackage())
            }
        }

        override fun getJSMainModuleName(): String = ".expo/.virtual-metro-entry"

        override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

        override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
        override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
    })

    override val reactHost: ReactHost
        get() = ReactNativeHostWrapper.createReactHost(applicationContext, reactNativeHost)

    override fun onCreate()
    {
        super.onCreate()
        Log.i(TAG, "Application onCreate started")

        try
        { // Инициализация SoLoader для нативных библиотек
            SoLoader.init(this, OpenSourceMergedSoMapping)
            Log.i(TAG, "SoLoader initialized successfully")

            // Загрузка New Architecture если включена
            if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED)
            {
                load()
                Log.i(TAG, "New Architecture loaded successfully")
            }

            // Уведомление Expo о создании приложения
            ApplicationLifecycleDispatcher.onApplicationCreate(this)
            Log.i(TAG, "Application created successfully")

        }
        catch (e: Exception)
        {
            Log.e(TAG, "Failed to initialize application", e) // В продакшене можно добавить отправку crash report
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration)
    {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "Configuration changed")
        try
        {
            ApplicationLifecycleDispatcher.onConfigurationChanged(this, newConfig)
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Failed to handle configuration change", e)
        }
    }

    override fun onLowMemory()
    {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")
        try
        { // Очистка кэша и неиспользуемых ресурсов
            System.gc()
            Log.i(TAG, "Memory cleanup completed")
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Failed to perform memory cleanup", e)
        }
    }

    override fun onTrimMemory(level: Int)
    {
        super.onTrimMemory(level)
        Log.i(TAG, "Trim memory level: $level")

        when (level)
        {
            TRIM_MEMORY_RUNNING_CRITICAL, TRIM_MEMORY_RUNNING_LOW, TRIM_MEMORY_RUNNING_MODERATE ->
            { // Приложение активно, освобождаем неиспользуемые ресурсы
                try
                {
                    System.gc()
                    Log.i(TAG, "Memory trimmed for running app")
                }
                catch (e: Exception)
                {
                    Log.e(TAG, "Failed to trim memory for running app", e)
                }
            }

            TRIM_MEMORY_UI_HIDDEN ->
            { // UI скрыт, освобождаем UI ресурсы
                try
                {
                    Log.i(TAG, "UI resources trimmed")
                }
                catch (e: Exception)
                {
                    Log.e(TAG, "Failed to trim UI resources", e)
                }
            }
        }
    }

    override fun onTerminate()
    {
        Log.i(TAG, "Application terminating")
        try
        { // Очистка ресурсов при завершении работы
            super.onTerminate()
            Log.i(TAG, "Application terminated successfully")
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error during application termination", e)
        }
    }
}
