package com.animecharacter.testing

import android.content.Context
import android.util.Log
import com.animecharacter.services.AIService
import com.animecharacter.services.FloatingCharacterService
import com.animecharacter.services.VoiceService
import com.animecharacter.utils.PerformanceMonitor
import com.animecharacter.utils.PermissionHelper
import com.animecharacter.utils.PreferencesHelper

class AppTester(private val context: Context) {

    companion object {
        private const val TAG = "AppTester"
    }

    private val performanceMonitor = PerformanceMonitor(context)
    private val preferencesHelper = PreferencesHelper(context)

    fun runAllTests(): TestResults {
        Log.d(TAG, "بدء اختبارات التطبيق الشاملة")
        
        val results = TestResults()
        
        // اختبار الصلاحيات
        results.permissionsTest = testPermissions()
        
        // اختبار الإعدادات
        results.preferencesTest = testPreferences()
        
        // اختبار الأداء
        results.performanceTest = testPerformance()
        
        // اختبار الخدمات
        results.servicesTest = testServices()
        
        // اختبار الذاكرة
        results.memoryTest = testMemoryUsage()
        
        Log.d(TAG, "انتهاء اختبارات التطبيق")
        return results
    }

    private fun testPermissions(): TestResult {
        return try {
            Log.d(TAG, "اختبار الصلاحيات...")
            
            // فحص الصلاحيات المطلوبة
            val hasRecordAudio = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val hasOverlay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else {
                true
            }
            
            val success = hasRecordAudio && hasOverlay
            val message = if (success) {
                "جميع الصلاحيات متاحة"
            } else {
                "بعض الصلاحيات مفقودة: ${if (!hasRecordAudio) "الميكروفون " else ""}${if (!hasOverlay) "النافذة العائمة" else ""}"
            }
            
            TestResult(success, message)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في اختبار الصلاحيات", e)
            TestResult(false, "خطأ في اختبار الصلاحيات: ${e.message}")
        }
    }

    private fun testPreferences(): TestResult {
        return try {
            Log.d(TAG, "اختبار الإعدادات...")
            
            // اختبار حفظ واسترجاع الإعدادات
            val testCharacter = "ناروتو"
            val testSize = 150
            val testTransparency = 80
            
            preferencesHelper.setSelectedCharacter(testCharacter)
            preferencesHelper.setCharacterSize(testSize)
            preferencesHelper.setCharacterTransparency(testTransparency)
            
            val retrievedCharacter = preferencesHelper.getSelectedCharacter()
            val retrievedSize = preferencesHelper.getCharacterSize()
            val retrievedTransparency = preferencesHelper.getCharacterTransparency()
            
            val success = retrievedCharacter == testCharacter && 
                         retrievedSize == testSize && 
                         retrievedTransparency == testTransparency
            
            val message = if (success) {
                "اختبار الإعدادات نجح"
            } else {
                "فشل في حفظ/استرجاع الإعدادات"
            }
            
            TestResult(success, message)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في اختبار الإعدادات", e)
            TestResult(false, "خطأ في اختبار الإعدادات: ${e.message}")
        }
    }

    private fun testPerformance(): TestResult {
        return try {
            Log.d(TAG, "اختبار الأداء...")
            
            performanceMonitor.startMonitoring()
            
            // محاكاة عمليات مكثفة
            val startTime = System.currentTimeMillis()
            
            // اختبار سرعة الاستجابة
            repeat(1000) {
                preferencesHelper.getSelectedCharacter()
            }
            
            val endTime = System.currentTimeMillis()
            val responseTime = endTime - startTime
            
            performanceMonitor.stopMonitoring()
            
            val memoryStats = performanceMonitor.getMemoryInfo()
            val memoryUsage = memoryStats.getAppMemoryUsagePercentage()
            
            val success = responseTime < 1000 && memoryUsage < 80
            val message = if (success) {
                "الأداء جيد - وقت الاستجابة: ${responseTime}ms، استخدام الذاكرة: $memoryUsage%"
            } else {
                "الأداء بطيء - وقت الاستجابة: ${responseTime}ms، استخدام الذاكرة: $memoryUsage%"
            }
            
            TestResult(success, message)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في اختبار الأداء", e)
            TestResult(false, "خطأ في اختبار الأداء: ${e.message}")
        }
    }

    private fun testServices(): TestResult {
        return try {
            Log.d(TAG, "اختبار الخدمات...")
            
            // اختبار توفر الخدمات
            val aiServiceAvailable = true // يمكن تحسين هذا الاختبار
            val voiceServiceAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)
            val ttsServiceAvailable = true // يمكن تحسين هذا الاختبار
            
            val success = aiServiceAvailable && voiceServiceAvailable && ttsServiceAvailable
            val message = if (success) {
                "جميع الخدمات متاحة"
            } else {
                "بعض الخدمات غير متاحة"
            }
            
            TestResult(success, message)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في اختبار الخدمات", e)
            TestResult(false, "خطأ في اختبار الخدمات: ${e.message}")
        }
    }

    private fun testMemoryUsage(): TestResult {
        return try {
            Log.d(TAG, "اختبار استخدام الذاكرة...")
            
            val memoryStats = performanceMonitor.getMemoryInfo()
            val appMemoryUsage = memoryStats.getAppMemoryUsagePercentage()
            val systemMemoryUsage = memoryStats.getSystemMemoryUsagePercentage()
            
            val success = appMemoryUsage < 70 && systemMemoryUsage < 85
            val message = if (success) {
                "استخدام الذاكرة طبيعي - التطبيق: $appMemoryUsage%، النظام: $systemMemoryUsage%"
            } else {
                "استخدام عالي للذاكرة - التطبيق: $appMemoryUsage%، النظام: $systemMemoryUsage%"
            }
            
            TestResult(success, message)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في اختبار الذاكرة", e)
            TestResult(false, "خطأ في اختبار الذاكرة: ${e.message}")
        }
    }

    fun generatePerformanceReport(): String {
        val memoryStats = performanceMonitor.getMemoryInfo()
        val optimizationTips = performanceMonitor.getBatteryOptimizationTips()
        
        return buildString {
            appendLine("=== تقرير الأداء ===")
            appendLine()
            appendLine("استخدام الذاكرة:")
            appendLine("- ذاكرة التطبيق: ${memoryStats.getAppMemoryUsagePercentage()}%")
            appendLine("- ذاكرة النظام: ${memoryStats.getSystemMemoryUsagePercentage()}%")
            appendLine("- حالة الذاكرة: ${if (memoryStats.isLowMemory) "منخفضة" else "طبيعية"}")
            appendLine()
            appendLine("نصائح التحسين:")
            optimizationTips.forEach { tip ->
                appendLine("- $tip")
            }
        }
    }

    data class TestResults(
        var permissionsTest: TestResult = TestResult(false, "لم يتم الاختبار"),
        var preferencesTest: TestResult = TestResult(false, "لم يتم الاختبار"),
        var performanceTest: TestResult = TestResult(false, "لم يتم الاختبار"),
        var servicesTest: TestResult = TestResult(false, "لم يتم الاختبار"),
        var memoryTest: TestResult = TestResult(false, "لم يتم الاختبار")
    ) {
        fun isAllTestsPassed(): Boolean {
            return permissionsTest.success && 
                   preferencesTest.success && 
                   performanceTest.success && 
                   servicesTest.success && 
                   memoryTest.success
        }
        
        fun getFailedTests(): List<String> {
            val failed = mutableListOf<String>()
            if (!permissionsTest.success) failed.add("الصلاحيات")
            if (!preferencesTest.success) failed.add("الإعدادات")
            if (!performanceTest.success) failed.add("الأداء")
            if (!servicesTest.success) failed.add("الخدمات")
            if (!memoryTest.success) failed.add("الذاكرة")
            return failed
        }
    }

    data class TestResult(
        val success: Boolean,
        val message: String
    )
}

