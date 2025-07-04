package com.animecharacter.utils

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * مُحسِّن الأداء لـ Live2D
 * يراقب ويحسن أداء التطبيق لضمان تشغيل سلس على جميع الأجهزة
 */
class Live2DPerformanceOptimizer(private val context: Context) {
    
    companion object {
        private const val TAG = "Live2DPerformanceOptimizer"
        
        // عتبات الأداء
        private const val TARGET_FPS = 30f
        private const val MIN_ACCEPTABLE_FPS = 20f
        private const val MAX_MEMORY_USAGE_MB = 100f
        private const val MAX_CPU_USAGE_PERCENT = 50f
        
        // فترات المراقبة
        private const val PERFORMANCE_CHECK_INTERVAL = 1000L // مللي ثانية
        private const val MEMORY_CHECK_INTERVAL = 5000L // مللي ثانية
        private const val OPTIMIZATION_COOLDOWN = 10000L // مللي ثانية
        
        // مستويات الجودة
        const val QUALITY_HIGH = "high"
        const val QUALITY_MEDIUM = "medium"
        const val QUALITY_LOW = "low"
        const val QUALITY_ULTRA_LOW = "ultra_low"
    }
    
    // مراقب الأداء
    private val performanceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false
    
    // إحصائيات الأداء
    private val frameTimeHistory = mutableListOf<Long>()
    private val memoryUsageHistory = mutableListOf<Float>()
    private val cpuUsageHistory = mutableListOf<Float>()
    
    // إعدادات الأداء الحالية
    private var currentQualityLevel = QUALITY_HIGH
    private var currentFPS = TARGET_FPS
    private var isOptimizationEnabled = true
    
    // معلومات الجهاز
    private var devicePerformanceLevel = "unknown"
    private var availableMemoryMB = 0f
    private var cpuCores = 0
    
    // إعدادات التحسين
    private val optimizationSettings = ConcurrentHashMap<String, Any>()
    
    // مؤقتات التحسين
    private var lastOptimizationTime = 0L
    private var lastMemoryCleanup = 0L
    
    init {
        analyzeDeviceCapabilities()
        initializeOptimizationSettings()
    }
    
    /**
     * تحليل قدرات الجهاز
     */
    private fun analyzeDeviceCapabilities() {
        try {
            // معلومات الذاكرة
            val runtime = Runtime.getRuntime()
            availableMemoryMB = (runtime.maxMemory() / (1024 * 1024)).toFloat()
            
            // معلومات المعالج
            cpuCores = runtime.availableProcessors()
            
            // تحديد مستوى أداء الجهاز
            devicePerformanceLevel = when {
                availableMemoryMB >= 4096 && cpuCores >= 8 -> "high"
                availableMemoryMB >= 2048 && cpuCores >= 4 -> "medium"
                availableMemoryMB >= 1024 && cpuCores >= 2 -> "low"
                else -> "ultra_low"
            }
            
            // تعديل الإعدادات بناءً على قدرات الجهاز
            adjustSettingsForDevice()
            
            Log.d(TAG, "تحليل الجهاز: ذاكرة=${availableMemoryMB}MB، معالج=${cpuCores} نواة، مستوى=$devicePerformanceLevel")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تحليل قدرات الجهاز", e)
            devicePerformanceLevel = "low" // افتراضي آمن
        }
    }
    
    /**
     * تعديل الإعدادات بناءً على الجهاز
     */
    private fun adjustSettingsForDevice() {
        when (devicePerformanceLevel) {
            "high" -> {
                currentQualityLevel = QUALITY_HIGH
                currentFPS = TARGET_FPS
            }
            "medium" -> {
                currentQualityLevel = QUALITY_MEDIUM
                currentFPS = 25f
            }
            "low" -> {
                currentQualityLevel = QUALITY_LOW
                currentFPS = 20f
            }
            "ultra_low" -> {
                currentQualityLevel = QUALITY_ULTRA_LOW
                currentFPS = 15f
            }
        }
    }
    
    /**
     * تهيئة إعدادات التحسين
     */
    private fun initializeOptimizationSettings() {
        // إعدادات الجودة العالية
        optimizationSettings["${QUALITY_HIGH}_texture_resolution"] = 1.0f
        optimizationSettings["${QUALITY_HIGH}_animation_quality"] = 1.0f
        optimizationSettings["${QUALITY_HIGH}_physics_enabled"] = true
        optimizationSettings["${QUALITY_HIGH}_auto_blink"] = true
        optimizationSettings["${QUALITY_HIGH}_auto_expressions"] = true
        
        // إعدادات الجودة المتوسطة
        optimizationSettings["${QUALITY_MEDIUM}_texture_resolution"] = 0.8f
        optimizationSettings["${QUALITY_MEDIUM}_animation_quality"] = 0.8f
        optimizationSettings["${QUALITY_MEDIUM}_physics_enabled"] = true
        optimizationSettings["${QUALITY_MEDIUM}_auto_blink"] = true
        optimizationSettings["${QUALITY_MEDIUM}_auto_expressions"] = false
        
        // إعدادات الجودة المنخفضة
        optimizationSettings["${QUALITY_LOW}_texture_resolution"] = 0.6f
        optimizationSettings["${QUALITY_LOW}_animation_quality"] = 0.6f
        optimizationSettings["${QUALITY_LOW}_physics_enabled"] = false
        optimizationSettings["${QUALITY_LOW}_auto_blink"] = true
        optimizationSettings["${QUALITY_LOW}_auto_expressions"] = false
        
        // إعدادات الجودة المنخفضة جداً
        optimizationSettings["${QUALITY_ULTRA_LOW}_texture_resolution"] = 0.4f
        optimizationSettings["${QUALITY_ULTRA_LOW}_animation_quality"] = 0.4f
        optimizationSettings["${QUALITY_ULTRA_LOW}_physics_enabled"] = false
        optimizationSettings["${QUALITY_ULTRA_LOW}_auto_blink"] = false
        optimizationSettings["${QUALITY_ULTRA_LOW}_auto_expressions"] = false
        
        Log.d(TAG, "تم تهيئة إعدادات التحسين")
    }
    
    /**
     * بدء مراقبة الأداء
     */
    fun startPerformanceMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        
        // مراقبة معدل الإطارات
        performanceScope.launch {
            while (isMonitoring) {
                try {
                    monitorFrameRate()
                    delay(PERFORMANCE_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "خطأ في مراقبة معدل الإطارات", e)
                }
            }
        }
        
        // مراقبة استخدام الذاكرة
        performanceScope.launch {
            while (isMonitoring) {
                try {
                    monitorMemoryUsage()
                    delay(MEMORY_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "خطأ في مراقبة الذاكرة", e)
                }
            }
        }
        
        Log.d(TAG, "تم بدء مراقبة الأداء")
    }
    
    /**
     * إيقاف مراقبة الأداء
     */
    fun stopPerformanceMonitoring() {
        isMonitoring = false
        Log.d(TAG, "تم إيقاف مراقبة الأداء")
    }
    
    /**
     * مراقبة معدل الإطارات
     */
    private fun monitorFrameRate() {
        val currentTime = System.currentTimeMillis()
        
        // حساب معدل الإطارات الحالي
        if (frameTimeHistory.isNotEmpty()) {
            val timeDiff = currentTime - frameTimeHistory.last()
            val currentFPS = if (timeDiff > 0) 1000f / timeDiff else 0f
            
            // إضافة إلى السجل
            frameTimeHistory.add(currentTime)
            
            // الاحتفاظ بآخر 30 قياس فقط
            if (frameTimeHistory.size > 30) {
                frameTimeHistory.removeAt(0)
            }
            
            // فحص الحاجة للتحسين
            if (currentFPS < MIN_ACCEPTABLE_FPS && isOptimizationEnabled) {
                triggerPerformanceOptimization("low_fps")
            }
        } else {
            frameTimeHistory.add(currentTime)
        }
    }
    
    /**
     * مراقبة استخدام الذاكرة
     */
    private fun monitorMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemoryMB = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toFloat()
        val memoryUsagePercent = (usedMemoryMB / availableMemoryMB) * 100f
        
        memoryUsageHistory.add(usedMemoryMB)
        
        // الاحتفاظ بآخر 10 قياسات فقط
        if (memoryUsageHistory.size > 10) {
            memoryUsageHistory.removeAt(0)
        }
        
        // فحص الحاجة لتنظيف الذاكرة
        if (usedMemoryMB > MAX_MEMORY_USAGE_MB || memoryUsagePercent > 80f) {
            triggerMemoryCleanup()
        }
        
        // فحص الحاجة للتحسين
        if (memoryUsagePercent > 90f && isOptimizationEnabled) {
            triggerPerformanceOptimization("high_memory_usage")
        }
    }
    
    /**
     * تشغيل تحسين الأداء
     */
    private fun triggerPerformanceOptimization(reason: String) {
        val currentTime = System.currentTimeMillis()
        
        // فحص فترة الانتظار
        if (currentTime - lastOptimizationTime < OPTIMIZATION_COOLDOWN) {
            return
        }
        
        lastOptimizationTime = currentTime
        
        when (reason) {
            "low_fps" -> optimizeForFrameRate()
            "high_memory_usage" -> optimizeForMemoryUsage()
            "high_cpu_usage" -> optimizeForCPUUsage()
        }
        
        Log.d(TAG, "تم تشغيل تحسين الأداء: $reason")
    }
    
    /**
     * تحسين معدل الإطارات
     */
    private fun optimizeForFrameRate() {
        when (currentQualityLevel) {
            QUALITY_HIGH -> {
                currentQualityLevel = QUALITY_MEDIUM
                currentFPS = 25f
            }
            QUALITY_MEDIUM -> {
                currentQualityLevel = QUALITY_LOW
                currentFPS = 20f
            }
            QUALITY_LOW -> {
                currentQualityLevel = QUALITY_ULTRA_LOW
                currentFPS = 15f
            }
        }
        
        applyQualitySettings()
        Log.d(TAG, "تم تحسين معدل الإطارات: مستوى الجودة = $currentQualityLevel")
    }
    
    /**
     * تحسين استخدام الذاكرة
     */
    private fun optimizeForMemoryUsage() {
        // تنظيف الذاكرة أولاً
        triggerMemoryCleanup()
        
        // تقليل جودة الصور
        val currentTextureRes = optimizationSettings["${currentQualityLevel}_texture_resolution"] as Float
        optimizationSettings["${currentQualityLevel}_texture_resolution"] = maxOf(0.3f, currentTextureRes - 0.1f)
        
        // تعطيل الميزات غير الضرورية
        optimizationSettings["${currentQualityLevel}_auto_expressions"] = false
        if (currentQualityLevel != QUALITY_ULTRA_LOW) {
            optimizationSettings["${currentQualityLevel}_physics_enabled"] = false
        }
        
        applyQualitySettings()
        Log.d(TAG, "تم تحسين استخدام الذاكرة")
    }
    
    /**
     * تحسين استخدام المعالج
     */
    private fun optimizeForCPUUsage() {
        // تقليل معدل الإطارات
        currentFPS = maxOf(10f, currentFPS - 5f)
        
        // تقليل جودة الحركة
        val currentAnimationQuality = optimizationSettings["${currentQualityLevel}_animation_quality"] as Float
        optimizationSettings["${currentQualityLevel}_animation_quality"] = maxOf(0.3f, currentAnimationQuality - 0.1f)
        
        // تعطيل الحركات التلقائية
        optimizationSettings["${currentQualityLevel}_auto_blink"] = false
        optimizationSettings["${currentQualityLevel}_auto_expressions"] = false
        
        applyQualitySettings()
        Log.d(TAG, "تم تحسين استخدام المعالج")
    }
    
    /**
     * تطبيق إعدادات الجودة
     */
    private fun applyQualitySettings() {
        // في التطبيق الحقيقي، نطبق الإعدادات على محرك Live2D
        // هنا نسجل الإعدادات فقط
        
        val textureRes = optimizationSettings["${currentQualityLevel}_texture_resolution"] as Float
        val animationQuality = optimizationSettings["${currentQualityLevel}_animation_quality"] as Float
        val physicsEnabled = optimizationSettings["${currentQualityLevel}_physics_enabled"] as Boolean
        val autoBlinkEnabled = optimizationSettings["${currentQualityLevel}_auto_blink"] as Boolean
        val autoExpressionsEnabled = optimizationSettings["${currentQualityLevel}_auto_expressions"] as Boolean
        
        Log.d(TAG, """
            إعدادات الجودة المطبقة:
            - مستوى الجودة: $currentQualityLevel
            - معدل الإطارات: $currentFPS
            - دقة الصور: $textureRes
            - جودة الحركة: $animationQuality
            - الفيزياء: $physicsEnabled
            - الرمش التلقائي: $autoBlinkEnabled
            - التعبيرات التلقائية: $autoExpressionsEnabled
        """.trimIndent())
    }
    
    /**
     * تنظيف الذاكرة
     */
    private fun triggerMemoryCleanup() {
        val currentTime = System.currentTimeMillis()
        
        // فحص فترة الانتظار
        if (currentTime - lastMemoryCleanup < 5000) {
            return
        }
        
        lastMemoryCleanup = currentTime
        
        try {
            // تشغيل جامع القمامة
            System.gc()
            
            // تنظيف السجلات القديمة
            if (frameTimeHistory.size > 10) {
                frameTimeHistory.subList(0, frameTimeHistory.size - 10).clear()
            }
            
            if (memoryUsageHistory.size > 5) {
                memoryUsageHistory.subList(0, memoryUsageHistory.size - 5).clear()
            }
            
            Log.d(TAG, "تم تنظيف الذاكرة")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تنظيف الذاكرة", e)
        }
    }
    
    /**
     * الحصول على إحصائيات الأداء
     */
    fun getPerformanceStats(): Map<String, Any> {
        val avgFPS = if (frameTimeHistory.size >= 2) {
            val totalTime = frameTimeHistory.last() - frameTimeHistory.first()
            val frameCount = frameTimeHistory.size - 1
            if (totalTime > 0) (frameCount * 1000f) / totalTime else 0f
        } else {
            0f
        }
        
        val avgMemoryUsage = if (memoryUsageHistory.isNotEmpty()) {
            memoryUsageHistory.average().toFloat()
        } else {
            0f
        }
        
        val runtime = Runtime.getRuntime()
        val currentMemoryUsage = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toFloat()
        
        return mapOf(
            "device_performance_level" to devicePerformanceLevel,
            "current_quality_level" to currentQualityLevel,
            "target_fps" to currentFPS,
            "average_fps" to avgFPS,
            "current_memory_usage_mb" to currentMemoryUsage,
            "average_memory_usage_mb" to avgMemoryUsage,
            "available_memory_mb" to availableMemoryMB,
            "cpu_cores" to cpuCores,
            "optimization_enabled" to isOptimizationEnabled
        )
    }
    
    /**
     * تعيين مستوى الجودة يدوياً
     */
    fun setQualityLevel(qualityLevel: String): Boolean {
        if (qualityLevel !in listOf(QUALITY_HIGH, QUALITY_MEDIUM, QUALITY_LOW, QUALITY_ULTRA_LOW)) {
            Log.w(TAG, "مستوى جودة غير صالح: $qualityLevel")
            return false
        }
        
        currentQualityLevel = qualityLevel
        applyQualitySettings()
        
        Log.d(TAG, "تم تعيين مستوى الجودة: $qualityLevel")
        return true
    }
    
    /**
     * تعيين معدل الإطارات المستهدف
     */
    fun setTargetFPS(fps: Float): Boolean {
        if (fps < 10f || fps > 60f) {
            Log.w(TAG, "معدل إطارات غير صالح: $fps")
            return false
        }
        
        currentFPS = fps
        Log.d(TAG, "تم تعيين معدل الإطارات المستهدف: $fps")
        return true
    }
    
    /**
     * تفعيل/إلغاء التحسين التلقائي
     */
    fun setOptimizationEnabled(enabled: Boolean) {
        isOptimizationEnabled = enabled
        Log.d(TAG, "التحسين التلقائي: ${if (enabled) "مفعل" else "معطل"}")
    }
    
    /**
     * إعادة تعيين إعدادات الأداء
     */
    fun resetPerformanceSettings() {
        adjustSettingsForDevice()
        initializeOptimizationSettings()
        applyQualitySettings()
        
        // مسح السجلات
        frameTimeHistory.clear()
        memoryUsageHistory.clear()
        cpuUsageHistory.clear()
        
        Log.d(TAG, "تم إعادة تعيين إعدادات الأداء")
    }
    
    /**
     * الحصول على توصيات التحسين
     */
    fun getOptimizationRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        val stats = getPerformanceStats()
        val avgFPS = stats["average_fps"] as Float
        val memoryUsage = stats["current_memory_usage_mb"] as Float
        val memoryPercent = (memoryUsage / availableMemoryMB) * 100f
        
        if (avgFPS < MIN_ACCEPTABLE_FPS) {
            recommendations.add("تقليل مستوى الجودة لتحسين معدل الإطارات")
        }
        
        if (memoryPercent > 80f) {
            recommendations.add("تقليل دقة الصور لتوفير الذاكرة")
        }
        
        if (currentQualityLevel == QUALITY_HIGH && devicePerformanceLevel in listOf("low", "ultra_low")) {
            recommendations.add("استخدام مستوى جودة أقل يناسب قدرات الجهاز")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("الأداء مُحسَّن بشكل جيد")
        }
        
        return recommendations
    }
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        stopPerformanceMonitoring()
        performanceScope.cancel()
        
        frameTimeHistory.clear()
        memoryUsageHistory.clear()
        cpuUsageHistory.clear()
        optimizationSettings.clear()
        
        Log.d(TAG, "تم تنظيف موارد محسن الأداء")
    }
}

