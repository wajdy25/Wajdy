package com.animecharacter.services

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.animecharacter.utils.PreferencesHelper
import kotlinx.coroutines.*

class AppMonitorService : Service() {

    private val TAG = "AppMonitorService"
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var activityManager: ActivityManager
    private lateinit var handler: Handler
    private lateinit var live2DFloatingWindowService: Live2DFloatingWindowService

    private val checkInterval: Long = 1000 // Check every 1 second
    private var isMonitoring = false
    private var lastForegroundApp: String? = null

    private val checkForegroundAppRunnable = object : Runnable {
        override fun run() {
            if (isMonitoring) {
                val foregroundApp = getForegroundAppPackageName()
                if (foregroundApp != null && foregroundApp != lastForegroundApp) {
                    Log.d(TAG, "Foreground app changed: $foregroundApp")
                    handleForegroundAppChange(foregroundApp)
                    lastForegroundApp = foregroundApp
                }
                handler.postDelayed(this, checkInterval)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferencesHelper = PreferencesHelper(this)
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        handler = Handler(Looper.getMainLooper())
        live2DFloatingWindowService = Live2DFloatingWindowService()
        Log.d(TAG, "AppMonitorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (preferencesHelper.isHideOnGameEnabled()) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        Log.d(TAG, "AppMonitorService destroyed")
    }

    private fun startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
            handler.post(checkForegroundAppRunnable)
            Log.d(TAG, "App monitoring started")
        }
    }

    private fun stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false
            handler.removeCallbacks(checkForegroundAppRunnable)
            Log.d(TAG, "App monitoring stopped")
        }
    }

    private fun getForegroundAppPackageName(): String? {
        // This method requires PACKAGE_USAGE_STATS permission for API 21+.
        // For simplicity, we'll use getRunningTasks which is deprecated but works for older APIs.
        // A more robust solution would involve UsageStatsManager for newer APIs.
        val tasks = activityManager.getRunningTasks(1)
        return if (tasks.isNotEmpty()) {
            tasks[0].topActivity?.packageName
        } else {
            null
        }
    }

    private fun handleForegroundAppChange(packageName: String) {
        val gamePackageNames = preferencesHelper.getGamePackageNames()
        if (gamePackageNames.contains(packageName)) {
            Log.d(TAG, "Game detected: $packageName. Hiding floating window.")
            live2DFloatingWindowService.hideWindowProgrammatically()
        } else {
            Log.d(TAG, "Non-game app detected: $packageName. Showing floating window.")
            live2DFloatingWindowService.showWindowProgrammatically()
        }
    }
}


