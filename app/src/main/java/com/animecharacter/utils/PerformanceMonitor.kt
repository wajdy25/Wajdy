package com.animecharacter.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import java.util.*

class PerformanceMonitor(private val context: Context) {

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MEMORY_CHECK_INTERVAL = 30000L // 30 ثانية
        private const val LOW_MEMORY_THRESHOLD = 50 * 1024 * 1024 // 50 MB
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var isMonitoring = false
    private var monitoringTimer: Timer? = null

    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringTimer = Timer()
        
        monitoringTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkMemoryUsage()
                checkCpuUsage()
            }
        }, 0, MEMORY_CHECK_INTERVAL)
        
        Log.d(TAG, "بدء مراقبة الأداء")
    }

    fun stopMonitoring() {
        isMonitoring = false
        monitoringTimer?.cancel()
        monitoringTimer = null
        Log.d(TAG, "إيقاف مراقبة الأداء")
    }

    private fun checkMemoryUsage() {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemory = memoryInfo.availMem
        val totalMemory = memoryInfo.totalMem
        val usedMemory = totalMemory - availableMemory
        val memoryPercentage = (usedMemory * 100 / totalMemory).toInt()
        
        Log.d(TAG, "استخدام الذاكرة: ${formatBytes(usedMemory)} / ${formatBytes(totalMemory)} ($memoryPercentage%)")
        
        if (availableMemory < LOW_MEMORY_THRESHOLD) {
            Log.w(TAG, "تحذير: ذاكرة منخفضة متاحة")
            triggerMemoryCleanup()
        }
        
        // فحص ذاكرة التطبيق
        val runtime = Runtime.getRuntime()
        val appUsedMemory = runtime.totalMemory() - runtime.freeMemory()
        val appMaxMemory = runtime.maxMemory()
        val appMemoryPercentage = (appUsedMemory * 100 / appMaxMemory).toInt()
        
        Log.d(TAG, "ذاكرة التطبيق: ${formatBytes(appUsedMemory)} / ${formatBytes(appMaxMemory)} ($appMemoryPercentage%)")
        
        if (appMemoryPercentage > 80) {
            Log.w(TAG, "تحذير: استخدام عالي لذاكرة التطبيق")
            System.gc() // اقتراح تنظيف الذاكرة
        }
    }

    private fun checkCpuUsage() {
        try {
            val cpuInfo = Debug.threadCpuTimeNanos()
            Log.d(TAG, "وقت المعالج للخيط: ${cpuInfo / 1000000} ms")
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في فحص استخدام المعالج", e)
        }
    }

    private fun triggerMemoryCleanup() {
        // تنظيف الذاكرة
        System.gc()
        Log.d(TAG, "تم تشغيل تنظيف الذاكرة")
    }

    fun getMemoryInfo(): MemoryStats {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        
        return MemoryStats(
            totalSystemMemory = memoryInfo.totalMem,
            availableSystemMemory = memoryInfo.availMem,
            usedSystemMemory = memoryInfo.totalMem - memoryInfo.availMem,
            totalAppMemory = runtime.maxMemory(),
            usedAppMemory = runtime.totalMemory() - runtime.freeMemory(),
            freeAppMemory = runtime.freeMemory(),
            isLowMemory = memoryInfo.lowMemory
        )
    }

    fun getBatteryOptimizationTips(): List<String> {
        val tips = mutableListOf<String>()
        
        val memoryStats = getMemoryInfo()
        
        if (memoryStats.getSystemMemoryUsagePercentage() > 80) {
            tips.add("استخدام الذاكرة مرتفع - قم بإغلاق التطبيقات غير المستخدمة")
        }
        
        if (memoryStats.getAppMemoryUsagePercentage() > 70) {
            tips.add("قم بتقليل حجم الشخصية أو شفافيتها لتوفير الذاكرة")
        }
        
        tips.add("استخدم الوضع الليلي لتوفير البطارية")
        tips.add("قلل من استخدام الميزات الصوتية عند عدم الحاجة")
        
        return tips
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }

    data class MemoryStats(
        val totalSystemMemory: Long,
        val availableSystemMemory: Long,
        val usedSystemMemory: Long,
        val totalAppMemory: Long,
        val usedAppMemory: Long,
        val freeAppMemory: Long,
        val isLowMemory: Boolean
    ) {
        fun getSystemMemoryUsagePercentage(): Int {
            return (usedSystemMemory * 100 / totalSystemMemory).toInt()
        }
        
        fun getAppMemoryUsagePercentage(): Int {
            return (usedAppMemory * 100 / totalAppMemory).toInt()
        }
    }
}

