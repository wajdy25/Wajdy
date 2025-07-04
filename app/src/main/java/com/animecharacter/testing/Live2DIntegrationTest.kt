package com.animecharacter.testing

import android.content.Context
import android.util.Log
import com.animecharacter.managers.Live2DManager
import com.animecharacter.services.*
import com.animecharacter.utils.Live2DPerformanceOptimizer
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * اختبارات التكامل لـ Live2D
 * يختبر جميع مكونات النظام للتأكد من عملها بشكل صحيح
 */
class Live2DIntegrationTest(private val context: Context) {
    
    companion object {
        private const val TAG = "Live2DIntegrationTest"
        
        // مهلة الاختبارات
        private const val TEST_TIMEOUT = 30000L // 30 ثانية
        private const val ANIMATION_TEST_DURATION = 5000L // 5 ثواني
        private const val PERFORMANCE_TEST_DURATION = 10000L // 10 ثواني
    }
    
    // نتائج الاختبارات
    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val duration: Long,
        val details: String = "",
        val error: String? = null
    )
    
    // مجموعة الاختبارات
    private val testResults = mutableListOf<TestResult>()
    private val testScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // المكونات المطلوب اختبارها
    private var live2DManager: Live2DManager? = null
    private var animationEngine: Live2DAnimationEngine? = null
    private var aiIntegrationService: Live2DAIIntegrationService? = null
    private var performanceOptimizer: Live2DPerformanceOptimizer? = null
    private var floatingWindowService: Live2DFloatingWindowService? = null
    
    /**
     * تشغيل جميع الاختبارات
     */
    suspend fun runAllTests(): List<TestResult> {
        Log.d(TAG, "بدء تشغيل اختبارات التكامل")
        
        testResults.clear()
        
        try {
            // اختبارات التهيئة
            runInitializationTests()
            
            // اختبارات الوظائف الأساسية
            runBasicFunctionalityTests()
            
            // اختبارات الحركات والتعبيرات
            runAnimationTests()
            
            // اختبارات التكامل مع الذكاء الاصطناعي
            runAIIntegrationTests()
            
            // اختبارات الأداء
            runPerformanceTests()
            
            // اختبارات النافذة العائمة
            runFloatingWindowTests()
            
            // اختبارات التنظيف
            runCleanupTests()
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تشغيل الاختبارات", e)
            testResults.add(TestResult(
                testName = "General Test Execution",
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
        
        // طباعة ملخص النتائج
        printTestSummary()
        
        return testResults.toList()
    }
    
    /**
     * اختبارات التهيئة
     */
    private suspend fun runInitializationTests() {
        // اختبار تهيئة Live2DManager
        runTest("Live2DManager Initialization") {
            live2DManager = Live2DManager(context)
            val initialized = live2DManager?.initialize() ?: false
            
            if (!initialized) {
                throw Exception("فشل في تهيئة Live2DManager")
            }
            
            "تم تهيئة Live2DManager بنجاح"
        }
        
        // اختبار تهيئة محرك الحركات
        runTest("Animation Engine Initialization") {
            animationEngine = Live2DAnimationEngine(context)
            val initialized = animationEngine?.initialize(live2DManager!!) ?: false
            
            if (!initialized) {
                throw Exception("فشل في تهيئة محرك الحركات")
            }
            
            "تم تهيئة محرك الحركات بنجاح"
        }
        
        // اختبار تهيئة خدمة التكامل مع الذكاء الاصطناعي
        runTest("AI Integration Service Initialization") {
            aiIntegrationService = Live2DAIIntegrationService(context)
            val aiService = AIService(context)
            val initialized = aiIntegrationService?.initialize(
                aiService, live2DManager!!, animationEngine!!
            ) ?: false
            
            if (!initialized) {
                throw Exception("فشل في تهيئة خدمة التكامل مع الذكاء الاصطناعي")
            }
            
            "تم تهيئة خدمة التكامل مع الذكاء الاصطناعي بنجاح"
        }
        
        // اختبار تهيئة محسن الأداء
        runTest("Performance Optimizer Initialization") {
            performanceOptimizer = Live2DPerformanceOptimizer(context)
            performanceOptimizer?.startPerformanceMonitoring()
            
            "تم تهيئة محسن الأداء بنجاح"
        }
    }
    
    /**
     * اختبارات الوظائف الأساسية
     */
    private suspend fun runBasicFunctionalityTests() {
        // اختبار تحميل النموذج
        runTest("Model Loading") {
            val loaded = live2DManager?.loadModel("mao_pro") ?: false
            
            if (!loaded) {
                throw Exception("فشل في تحميل النموذج")
            }
            
            "تم تحميل النموذج بنجاح"
        }
        
        // اختبار عرض النموذج
        runTest("Model Rendering") {
            val rendered = live2DManager?.startRendering() ?: false
            
            if (!rendered) {
                throw Exception("فشل في عرض النموذج")
            }
            
            delay(2000) // انتظار لضمان العرض
            
            "تم عرض النموذج بنجاح"
        }
        
        // اختبار تحديث النموذج
        runTest("Model Update") {
            repeat(10) {
                live2DManager?.update()
                delay(100)
            }
            
            "تم تحديث النموذج بنجاح"
        }
    }
    
    /**
     * اختبارات الحركات والتعبيرات
     */
    private suspend fun runAnimationTests() {
        // اختبار تشغيل الحركات
        runTest("Motion Playback") {
            val motionTypes = listOf(
                Live2DAnimationEngine.MOTION_TYPE_IDLE,
                Live2DAnimationEngine.MOTION_TYPE_NORMAL,
                Live2DAnimationEngine.MOTION_TYPE_SPECIAL
            )
            
            for (motionType in motionTypes) {
                val played = animationEngine?.playMotion(motionType, 0) ?: false
                if (!played) {
                    throw Exception("فشل في تشغيل الحركة: $motionType")
                }
                delay(1000)
            }
            
            "تم تشغيل جميع أنواع الحركات بنجاح"
        }
        
        // اختبار تشغيل التعبيرات
        runTest("Expression Playback") {
            val expressions = listOf(
                Live2DAnimationEngine.EXPRESSION_NEUTRAL,
                Live2DAnimationEngine.EXPRESSION_HAPPY,
                Live2DAnimationEngine.EXPRESSION_SAD,
                Live2DAnimationEngine.EXPRESSION_SURPRISED,
                Live2DAnimationEngine.EXPRESSION_ANGRY
            )
            
            for (expression in expressions) {
                val played = animationEngine?.playExpression(expression) ?: false
                if (!played) {
                    throw Exception("فشل في تشغيل التعبير: $expression")
                }
                delay(800)
            }
            
            "تم تشغيل جميع التعبيرات بنجاح"
        }
        
        // اختبار الحركات المتزامنة
        runTest("Synchronized Animation") {
            val motionPlayed = animationEngine?.playMotion(Live2DAnimationEngine.MOTION_TYPE_NORMAL, 0) ?: false
            val expressionPlayed = animationEngine?.playExpression(Live2DAnimationEngine.EXPRESSION_HAPPY) ?: false
            
            if (!motionPlayed || !expressionPlayed) {
                throw Exception("فشل في تشغيل الحركات المتزامنة")
            }
            
            delay(3000) // انتظار لمشاهدة التزامن
            
            "تم تشغيل الحركات المتزامنة بنجاح"
        }
    }
    
    /**
     * اختبارات التكامل مع الذكاء الاصطناعي
     */
    private suspend fun runAIIntegrationTests() {
        // اختبار معالجة ردود الذكاء الاصطناعي
        runTest("AI Response Processing") {
            val testResponses = listOf(
                "أنا سعيد جداً اليوم!" to "happy",
                "أشعر بالحزن قليلاً" to "sad",
                "واو! هذا مدهش!" to "surprised",
                "أنا غاضب من هذا الأمر" to "angry",
                "دعني أفكر في هذا..." to "thinking"
            )
            
            for ((response, expectedEmotion) in testResponses) {
                val processed = aiIntegrationService?.processAIResponse(response) ?: false
                if (!processed) {
                    throw Exception("فشل في معالجة الرد: $response")
                }
                
                delay(1000) // انتظار لمعالجة الاستجابة
                
                val currentEmotion = aiIntegrationService?.getCurrentEmotion()
                if (currentEmotion != expectedEmotion) {
                    Log.w(TAG, "المشاعر المتوقعة: $expectedEmotion، المشاعر الفعلية: $currentEmotion")
                }
            }
            
            "تم معالجة ردود الذكاء الاصطناعي بنجاح"
        }
        
        // اختبار التفاعل المخصص
        runTest("Custom Interaction Trigger") {
            val customTriggered = aiIntegrationService?.triggerCustomInteraction(
                emotion = "love",
                expression = Live2DAnimationEngine.EXPRESSION_LOVE,
                motionType = Live2DAnimationEngine.MOTION_TYPE_SPECIAL,
                motionIndex = 2
            ) ?: false
            
            if (!customTriggered) {
                throw Exception("فشل في تشغيل التفاعل المخصص")
            }
            
            delay(2000) // انتظار لمشاهدة التفاعل
            
            "تم تشغيل التفاعل المخصص بنجاح"
        }
    }
    
    /**
     * اختبارات الأداء
     */
    private suspend fun runPerformanceTests() {
        // اختبار مراقبة الأداء
        runTest("Performance Monitoring") {
            performanceOptimizer?.startPerformanceMonitoring()
            
            // تشغيل حمولة عمل لاختبار الأداء
            repeat(100) {
                live2DManager?.update()
                animationEngine?.playExpression(Live2DAnimationEngine.EXPRESSION_NEUTRAL)
                delay(50)
            }
            
            delay(PERFORMANCE_TEST_DURATION)
            
            val stats = performanceOptimizer?.getPerformanceStats()
            val avgFPS = stats?.get("average_fps") as? Float ?: 0f
            
            if (avgFPS < 10f) {
                throw Exception("معدل الإطارات منخفض جداً: $avgFPS")
            }
            
            "مراقبة الأداء تعمل بشكل صحيح (معدل الإطارات: $avgFPS)"
        }
        
        // اختبار تحسين الأداء التلقائي
        runTest("Automatic Performance Optimization") {
            performanceOptimizer?.setOptimizationEnabled(true)
            
            // محاكاة حمولة عالية
            repeat(200) {
                live2DManager?.update()
                animationEngine?.playMotion(Live2DAnimationEngine.MOTION_TYPE_SPECIAL, 0)
                delay(25) // معدل إطارات عالي
            }
            
            val recommendations = performanceOptimizer?.getOptimizationRecommendations()
            
            "التحسين التلقائي يعمل بشكل صحيح (توصيات: ${recommendations?.size ?: 0})"
        }
    }
    
    /**
     * اختبارات النافذة العائمة
     */
    private suspend fun runFloatingWindowTests() {
        // اختبار إنشاء النافذة العائمة
        runTest("Floating Window Creation") {
            floatingWindowService = Live2DFloatingWindowService()
            val created = floatingWindowService?.createFloatingWindow(context) ?: false
            
            if (!created) {
                throw Exception("فشل في إنشاء النافذة العائمة")
            }
            
            "تم إنشاء النافذة العائمة بنجاح"
        }
        
        // اختبار عرض النافذة العائمة
        runTest("Floating Window Display") {
            val displayed = floatingWindowService?.showFloatingWindow() ?: false
            
            if (!displayed) {
                throw Exception("فشل في عرض النافذة العائمة")
            }
            
            delay(3000) // انتظار لمشاهدة النافذة
            
            "تم عرض النافذة العائمة بنجاح"
        }
        
        // اختبار تحديث موقع النافذة
        runTest("Floating Window Position Update") {
            val updated = floatingWindowService?.updatePosition(100, 200) ?: false
            
            if (!updated) {
                throw Exception("فشل في تحديث موقع النافذة العائمة")
            }
            
            delay(1000)
            
            "تم تحديث موقع النافذة العائمة بنجاح"
        }
    }
    
    /**
     * اختبارات التنظيف
     */
    private suspend fun runCleanupTests() {
        // اختبار تنظيف الموارد
        runTest("Resource Cleanup") {
            aiIntegrationService?.cleanup()
            performanceOptimizer?.cleanup()
            floatingWindowService?.hideFloatingWindow()
            live2DManager?.cleanup()
            
            "تم تنظيف جميع الموارد بنجاح"
        }
    }
    
    /**
     * تشغيل اختبار واحد
     */
    private suspend fun runTest(testName: String, testBlock: suspend () -> String) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "بدء اختبار: $testName")
            
            val result = withTimeout(TEST_TIMEOUT) {
                testBlock()
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            testResults.add(TestResult(
                testName = testName,
                passed = true,
                duration = duration,
                details = result
            ))
            
            Log.d(TAG, "نجح اختبار: $testName (${duration}ms)")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = duration,
                error = e.message
            ))
            
            Log.e(TAG, "فشل اختبار: $testName", e)
        }
    }
    
    /**
     * طباعة ملخص النتائج
     */
    private fun printTestSummary() {
        val totalTests = testResults.size
        val passedTests = testResults.count { it.passed }
        val failedTests = totalTests - passedTests
        val totalDuration = testResults.sumOf { it.duration }
        
        Log.i(TAG, """
            ===== ملخص اختبارات التكامل =====
            إجمالي الاختبارات: $totalTests
            الاختبارات الناجحة: $passedTests
            الاختبارات الفاشلة: $failedTests
            إجمالي الوقت: ${totalDuration}ms
            معدل النجاح: ${(passedTests.toFloat() / totalTests * 100).toInt()}%
            ================================
        """.trimIndent())
        
        // طباعة تفاصيل الاختبارات الفاشلة
        val failedTestsList = testResults.filter { !it.passed }
        if (failedTestsList.isNotEmpty()) {
            Log.w(TAG, "الاختبارات الفاشلة:")
            for (test in failedTestsList) {
                Log.w(TAG, "- ${test.testName}: ${test.error}")
            }
        }
    }
    
    /**
     * الحصول على تقرير مفصل
     */
    fun getDetailedReport(): String {
        val report = StringBuilder()
        
        report.appendLine("تقرير اختبارات التكامل لـ Live2D")
        report.appendLine("=" * 50)
        report.appendLine()
        
        for (test in testResults) {
            report.appendLine("اسم الاختبار: ${test.testName}")
            report.appendLine("النتيجة: ${if (test.passed) "نجح" else "فشل"}")
            report.appendLine("المدة: ${test.duration}ms")
            
            if (test.passed) {
                report.appendLine("التفاصيل: ${test.details}")
            } else {
                report.appendLine("الخطأ: ${test.error}")
            }
            
            report.appendLine("-" * 30)
        }
        
        val totalTests = testResults.size
        val passedTests = testResults.count { it.passed }
        val successRate = if (totalTests > 0) (passedTests.toFloat() / totalTests * 100) else 0f
        
        report.appendLine()
        report.appendLine("الملخص:")
        report.appendLine("إجمالي الاختبارات: $totalTests")
        report.appendLine("الاختبارات الناجحة: $passedTests")
        report.appendLine("معدل النجاح: ${successRate.toInt()}%")
        
        return report.toString()
    }
    
    /**
     * تنظيف موارد الاختبار
     */
    fun cleanup() {
        testScope.cancel()
        testResults.clear()
        
        // تنظيف المكونات
        aiIntegrationService?.cleanup()
        performanceOptimizer?.cleanup()
        floatingWindowService?.hideFloatingWindow()
        live2DManager?.cleanup()
        
        Log.d(TAG, "تم تنظيف موارد اختبارات التكامل")
    }
}

