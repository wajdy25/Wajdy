package com.animecharacter.services

import android.content.Context
import android.util.Log
import com.animecharacter.models.Live2DModel
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*
import kotlin.random.Random

/**
 * محرك الحركات والتعبيرات الديناميكية لـ Live2D
 * يدير تشغيل الحركات، التعبيرات، والانتقالات السلسة
 */
class Live2DAnimationEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "Live2DAnimationEngine"
        
        // أنواع الحركات
        const val MOTION_TYPE_IDLE = "idle"
        const val MOTION_TYPE_NORMAL = "normal"
        const val MOTION_TYPE_SPECIAL = "special"
        
        // أنواع التعبيرات
        const val EXPRESSION_HAPPY = "exp_01"
        const val EXPRESSION_SAD = "exp_02"
        const val EXPRESSION_SURPRISED = "exp_03"
        const val EXPRESSION_ANGRY = "exp_04"
        const val EXPRESSION_THINKING = "exp_05"
        const val EXPRESSION_LOVE = "exp_06"
        const val EXPRESSION_SLEEPY = "exp_07"
        const val EXPRESSION_NEUTRAL = "exp_08"
        
        // معدلات التحديث
        private const val ANIMATION_UPDATE_RATE = 60f // FPS
        private const val EXPRESSION_BLEND_SPEED = 2.0f
        private const val MOTION_BLEND_SPEED = 1.5f
        
        // أوقات الحركات التلقائية
        private const val AUTO_BLINK_INTERVAL = 3000L // 3 ثوانٍ
        private const val AUTO_IDLE_INTERVAL = 10000L // 10 ثوانٍ
        private const val AUTO_EXPRESSION_INTERVAL = 15000L // 15 ثانية
    }
    
    // النموذج والحالة
    private var model: Live2DModel? = null
    private val animationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // إدارة الحركات
    private val activeMotions = ConcurrentHashMap<String, MotionState>()
    private var currentMainMotion: MotionState? = null
    private var isMotionPlaying = false
    
    // إدارة التعبيرات
    private val activeExpressions = ConcurrentHashMap<String, ExpressionState>()
    private var currentExpression: ExpressionState? = null
    private var targetExpression: ExpressionState? = null
    
    // الحركات التلقائية
    private var autoBlinkEnabled = true
    private var autoIdleEnabled = true
    private var autoExpressionEnabled = false
    
    // مؤقتات الحركات التلقائية
    private var lastBlinkTime = 0L
    private var lastIdleTime = 0L
    private var lastExpressionTime = 0L
    
    // معاملات النموذج
    private val modelParameters = ConcurrentHashMap<String, Float>()
    private val targetParameters = ConcurrentHashMap<String, Float>()
    
    // حالة المحرك
    private var isInitialized = false
    private var isRunning = false
    
    /**
     * حالة الحركة
     */
    data class MotionState(
        val name: String,
        val data: JSONObject,
        var currentTime: Float = 0f,
        var weight: Float = 0f,
        var targetWeight: Float = 0f,
        var isLooping: Boolean = false,
        var isFinished: Boolean = false
    )
    
    /**
     * حالة التعبير
     */
    data class ExpressionState(
        val name: String,
        val data: JSONObject,
        var weight: Float = 0f,
        var targetWeight: Float = 0f,
        val parameters: MutableMap<String, Float> = mutableMapOf()
    )
    
    /**
     * تهيئة المحرك
     */
    fun initialize(live2DModel: Live2DModel): Boolean {
        return try {
            model = live2DModel
            
            // تحميل بيانات الحركات والتعبيرات
            loadAnimationData()
            
            // بدء حلقة التحديث
            startUpdateLoop()
            
            isInitialized = true
            Log.d(TAG, "تم تهيئة محرك الحركات بنجاح")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تهيئة محرك الحركات", e)
            false
        }
    }
    
    /**
     * تحميل بيانات الحركات والتعبيرات
     */
    private fun loadAnimationData() {
        // تحميل التعبيرات المتاحة
        val expressions = model?.getAvailableExpressions() ?: emptyList()
        for (expressionName in expressions) {
            // في التطبيق الحقيقي، نحتاج لتحميل بيانات التعبير من الملفات
            val expressionData = createMockExpressionData(expressionName)
            val expressionState = ExpressionState(expressionName, expressionData)
            activeExpressions[expressionName] = expressionState
        }
        
        Log.d(TAG, "تم تحميل ${activeExpressions.size} تعبير")
    }
    
    /**
     * إنشاء بيانات تعبير وهمية (للاختبار)
     */
    private fun createMockExpressionData(expressionName: String): JSONObject {
        return JSONObject().apply {
            put("Type", "Live2D Expression")
            put("Parameters", org.json.JSONArray().apply {
                // معاملات وهمية للتعبير
                when (expressionName) {
                    EXPRESSION_HAPPY -> {
                        put(createParameterObject("ParamEyeLOpen", 1.0f))
                        put(createParameterObject("ParamEyeROpen", 1.0f))
                        put(createParameterObject("ParamMouthForm", 1.0f))
                    }
                    EXPRESSION_SAD -> {
                        put(createParameterObject("ParamEyeLOpen", 0.3f))
                        put(createParameterObject("ParamEyeROpen", 0.3f))
                        put(createParameterObject("ParamMouthForm", -0.8f))
                    }
                    EXPRESSION_SURPRISED -> {
                        put(createParameterObject("ParamEyeLOpen", 1.5f))
                        put(createParameterObject("ParamEyeROpen", 1.5f))
                        put(createParameterObject("ParamMouthOpenY", 0.8f))
                    }
                    else -> {
                        put(createParameterObject("ParamEyeLOpen", 1.0f))
                        put(createParameterObject("ParamEyeROpen", 1.0f))
                    }
                }
            })
        }
    }
    
    /**
     * إنشاء كائن معامل
     */
    private fun createParameterObject(id: String, value: Float): JSONObject {
        return JSONObject().apply {
            put("Id", id)
            put("Value", value)
        }
    }
    
    /**
     * بدء حلقة التحديث
     */
    private fun startUpdateLoop() {
        if (isRunning) return
        
        isRunning = true
        animationScope.launch {
            var lastTime = System.currentTimeMillis()
            
            while (isRunning) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastTime) / 1000f
                lastTime = currentTime
                
                try {
                    updateAnimations(deltaTime)
                    updateAutoAnimations(currentTime)
                } catch (e: Exception) {
                    Log.e(TAG, "خطأ في تحديث الحركات", e)
                }
                
                delay((1000f / ANIMATION_UPDATE_RATE).toLong())
            }
        }
        
        Log.d(TAG, "تم بدء حلقة تحديث الحركات")
    }
    
    /**
     * تحديث الحركات
     */
    private fun updateAnimations(deltaTime: Float) {
        // تحديث الحركات النشطة
        updateMotions(deltaTime)
        
        // تحديث التعبيرات
        updateExpressions(deltaTime)
        
        // تحديث معاملات النموذج
        updateModelParameters(deltaTime)
    }
    
    /**
     * تحديث الحركات
     */
    private fun updateMotions(deltaTime: Float) {
        val motionsToRemove = mutableListOf<String>()
        
        for ((name, motion) in activeMotions) {
            // تحديث وقت الحركة
            motion.currentTime += deltaTime
            
            // تحديث الوزن
            val weightDiff = motion.targetWeight - motion.weight
            if (abs(weightDiff) > 0.01f) {
                motion.weight += weightDiff * MOTION_BLEND_SPEED * deltaTime
            } else {
                motion.weight = motion.targetWeight
            }
            
            // فحص انتهاء الحركة
            if (!motion.isLooping && motion.currentTime >= getMotionDuration(motion)) {
                motion.isFinished = true
                motion.targetWeight = 0f
            }
            
            // إزالة الحركات المنتهية
            if (motion.isFinished && motion.weight <= 0.01f) {
                motionsToRemove.add(name)
            }
        }
        
        // إزالة الحركات المنتهية
        for (name in motionsToRemove) {
            activeMotions.remove(name)
            Log.d(TAG, "تم إنهاء الحركة: $name")
        }
    }
    
    /**
     * تحديث التعبيرات
     */
    private fun updateExpressions(deltaTime: Float) {
        // تحديث التعبير الحالي
        currentExpression?.let { current ->
            val weightDiff = current.targetWeight - current.weight
            if (abs(weightDiff) > 0.01f) {
                current.weight += weightDiff * EXPRESSION_BLEND_SPEED * deltaTime
            } else {
                current.weight = current.targetWeight
            }
        }
        
        // تحديث التعبير المستهدف
        targetExpression?.let { target ->
            val weightDiff = target.targetWeight - target.weight
            if (abs(weightDiff) > 0.01f) {
                target.weight += weightDiff * EXPRESSION_BLEND_SPEED * deltaTime
            } else {
                target.weight = target.targetWeight
                
                // إذا وصل التعبير المستهدف للوزن الكامل، اجعله التعبير الحالي
                if (target.weight >= 0.99f) {
                    currentExpression?.targetWeight = 0f
                    currentExpression = target
                    targetExpression = null
                }
            }
        }
    }
    
    /**
     * تحديث معاملات النموذج
     */
    private fun updateModelParameters(deltaTime: Float) {
        // تطبيق تأثيرات التعبيرات
        applyExpressionEffects()
        
        // تطبيق تأثيرات الحركات
        applyMotionEffects()
        
        // تحديث المعاملات بسلاسة
        for ((paramId, targetValue) in targetParameters) {
            val currentValue = modelParameters[paramId] ?: 0f
            val diff = targetValue - currentValue
            
            if (abs(diff) > 0.001f) {
                val newValue = currentValue + diff * 5f * deltaTime
                modelParameters[paramId] = newValue
            } else {
                modelParameters[paramId] = targetValue
            }
        }
    }
    
    /**
     * تطبيق تأثيرات التعبيرات
     */
    private fun applyExpressionEffects() {
        // مسح المعاملات المستهدفة
        targetParameters.clear()
        
        // تطبيق التعبير الحالي
        currentExpression?.let { expression ->
            applyExpressionParameters(expression)
        }
        
        // تطبيق التعبير المستهدف
        targetExpression?.let { expression ->
            applyExpressionParameters(expression)
        }
    }
    
    /**
     * تطبيق معاملات التعبير
     */
    private fun applyExpressionParameters(expression: ExpressionState) {
        try {
            val parameters = expression.data.getJSONArray("Parameters")
            
            for (i in 0 until parameters.length()) {
                val param = parameters.getJSONObject(i)
                val id = param.getString("Id")
                val value = param.getDouble("Value").toFloat()
                val weightedValue = value * expression.weight
                
                // دمج مع القيم الموجودة
                val existingValue = targetParameters[id] ?: 0f
                targetParameters[id] = existingValue + weightedValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "خطأ في تطبيق معاملات التعبير: ${expression.name}", e)
        }
    }
    
    /**
     * تطبيق تأثيرات الحركات
     */
    private fun applyMotionEffects() {
        // تطبيق الحركات النشطة
        for ((_, motion) in activeMotions) {
            if (motion.weight > 0.01f) {
                applyMotionParameters(motion)
            }
        }
    }
    
    /**
     * تطبيق معاملات الحركة
     */
    private fun applyMotionParameters(motion: MotionState) {
        // تطبيق مبسط للحركة
        // في التطبيق الحقيقي، نحتاج لمعالجة معقدة للحركات
        
        val timeNormalized = motion.currentTime / getMotionDuration(motion)
        val motionValue = sin(timeNormalized * PI * 2).toFloat() * motion.weight
        
        // تطبيق على معاملات الحركة الأساسية
        val baseParams = listOf("ParamAngleX", "ParamAngleY", "ParamBodyAngleX")
        for (paramId in baseParams) {
            val existingValue = targetParameters[paramId] ?: 0f
            targetParameters[paramId] = existingValue + motionValue * 0.1f
        }
    }
    
    /**
     * تحديث الحركات التلقائية
     */
    private fun updateAutoAnimations(currentTime: Long) {
        // رمش العين التلقائي
        if (autoBlinkEnabled && currentTime - lastBlinkTime > AUTO_BLINK_INTERVAL) {
            performAutoBlink()
            lastBlinkTime = currentTime + Random.nextLong(-1000, 1000) // تنويع التوقيت
        }
        
        // الحركة الخاملة التلقائية
        if (autoIdleEnabled && currentTime - lastIdleTime > AUTO_IDLE_INTERVAL) {
            if (!isMotionPlaying) {
                playIdleMotion()
            }
            lastIdleTime = currentTime
        }
        
        // التعبير التلقائي
        if (autoExpressionEnabled && currentTime - lastExpressionTime > AUTO_EXPRESSION_INTERVAL) {
            playRandomExpression()
            lastExpressionTime = currentTime
        }
    }
    
    /**
     * تشغيل تعبير
     */
    fun playExpression(expressionName: String, fadeTime: Float = 1.0f): Boolean {
        val expression = activeExpressions[expressionName]
        if (expression == null) {
            Log.w(TAG, "التعبير غير موجود: $expressionName")
            return false
        }
        
        // إعداد التعبير المستهدف
        targetExpression = expression
        expression.targetWeight = 1.0f
        
        Log.d(TAG, "تم تشغيل التعبير: $expressionName")
        return true
    }
    
    /**
     * تشغيل حركة
     */
    fun playMotion(motionType: String, motionIndex: Int = -1, loop: Boolean = false): Boolean {
        val motionName = when (motionType) {
            MOTION_TYPE_IDLE -> "mtn_01"
            MOTION_TYPE_NORMAL -> {
                val normalMotions = listOf("mtn_02", "mtn_03", "mtn_04")
                if (motionIndex >= 0 && motionIndex < normalMotions.size) {
                    normalMotions[motionIndex]
                } else {
                    normalMotions.random()
                }
            }
            MOTION_TYPE_SPECIAL -> {
                val specialMotions = listOf("special_01", "special_02", "special_03")
                if (motionIndex >= 0 && motionIndex < specialMotions.size) {
                    specialMotions[motionIndex]
                } else {
                    specialMotions.random()
                }
            }
            else -> {
                Log.w(TAG, "نوع الحركة غير معروف: $motionType")
                return false
            }
        }
        
        // إنشاء حالة الحركة
        val motionData = createMockMotionData(motionName)
        val motionState = MotionState(
            name = motionName,
            data = motionData,
            isLooping = loop,
            targetWeight = 1.0f
        )
        
        // إيقاف الحركة الرئيسية السابقة
        currentMainMotion?.targetWeight = 0f
        
        // تشغيل الحركة الجديدة
        activeMotions[motionName] = motionState
        currentMainMotion = motionState
        isMotionPlaying = true
        
        Log.d(TAG, "تم تشغيل الحركة: $motionName (loop: $loop)")
        return true
    }
    
    /**
     * إنشاء بيانات حركة وهمية
     */
    private fun createMockMotionData(motionName: String): JSONObject {
        return JSONObject().apply {
            put("Type", "Live2D Motion")
            put("Duration", when {
                motionName.startsWith("special") -> 5.0f
                motionName.startsWith("mtn") -> 3.0f
                else -> 2.0f
            })
        }
    }
    
    /**
     * الحصول على مدة الحركة
     */
    private fun getMotionDuration(motion: MotionState): Float {
        return try {
            motion.data.getDouble("Duration").toFloat()
        } catch (e: Exception) {
            3.0f // مدة افتراضية
        }
    }
    
    /**
     * تشغيل رمش العين التلقائي
     */
    private fun performAutoBlink() {
        // تطبيق رمش سريع
        targetParameters["ParamEyeLOpen"] = 0f
        targetParameters["ParamEyeROpen"] = 0f
        
        // إعادة فتح العينين بعد فترة قصيرة
        animationScope.launch {
            delay(150)
            targetParameters["ParamEyeLOpen"] = 1f
            targetParameters["ParamEyeROpen"] = 1f
        }
        
        Log.d(TAG, "تم تنفيذ رمش العين التلقائي")
    }
    
    /**
     * تشغيل الحركة الخاملة
     */
    private fun playIdleMotion() {
        playMotion(MOTION_TYPE_IDLE, loop = false)
    }
    
    /**
     * تشغيل تعبير عشوائي
     */
    private fun playRandomExpression() {
        val expressions = activeExpressions.keys.toList()
        if (expressions.isNotEmpty()) {
            val randomExpression = expressions.random()
            playExpression(randomExpression)
        }
    }
    
    /**
     * إيقاف جميع الحركات
     */
    fun stopAllMotions() {
        for ((_, motion) in activeMotions) {
            motion.targetWeight = 0f
        }
        currentMainMotion = null
        isMotionPlaying = false
        
        Log.d(TAG, "تم إيقاف جميع الحركات")
    }
    
    /**
     * إيقاف التعبير الحالي
     */
    fun stopCurrentExpression() {
        currentExpression?.targetWeight = 0f
        targetExpression?.targetWeight = 0f
        
        Log.d(TAG, "تم إيقاف التعبير الحالي")
    }
    
    /**
     * تفعيل/إلغاء الحركات التلقائية
     */
    fun setAutoBlinkEnabled(enabled: Boolean) {
        autoBlinkEnabled = enabled
        Log.d(TAG, "رمش العين التلقائي: ${if (enabled) "مفعل" else "معطل"}")
    }
    
    fun setAutoIdleEnabled(enabled: Boolean) {
        autoIdleEnabled = enabled
        Log.d(TAG, "الحركة الخاملة التلقائية: ${if (enabled) "مفعلة" else "معطلة"}")
    }
    
    fun setAutoExpressionEnabled(enabled: Boolean) {
        autoExpressionEnabled = enabled
        Log.d(TAG, "التعبيرات التلقائية: ${if (enabled) "مفعلة" else "معطلة"}")
    }
    
    /**
     * الحصول على معاملات النموذج الحالية
     */
    fun getCurrentParameters(): Map<String, Float> {
        return modelParameters.toMap()
    }
    
    /**
     * الحصول على التعبيرات المتاحة
     */
    fun getAvailableExpressions(): List<String> {
        return activeExpressions.keys.toList()
    }
    
    /**
     * فحص حالة تشغيل الحركة
     */
    fun isMotionPlaying(): Boolean = isMotionPlaying
    
    /**
     * الحصول على التعبير الحالي
     */
    fun getCurrentExpression(): String? = currentExpression?.name
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        isRunning = false
        animationScope.cancel()
        
        activeMotions.clear()
        activeExpressions.clear()
        modelParameters.clear()
        targetParameters.clear()
        
        currentMainMotion = null
        currentExpression = null
        targetExpression = null
        
        Log.d(TAG, "تم تنظيف موارد محرك الحركات")
    }
}



    /**
     * تعيين التعبير بناءً على المشاعر
     */
    fun setEmotion(emotion: String) {
        when (emotion) {
            "happy" -> playExpression(EXPRESSION_HAPPY)
            "sad" -> playExpression(EXPRESSION_SAD)
            "surprised" -> playExpression(EXPRESSION_SURPRISED)
            "angry" -> playExpression(EXPRESSION_ANGRY)
            "neutral" -> playExpression(EXPRESSION_NEUTRAL)
            else -> Log.w(TAG, "مشاعر غير معروفة: $emotion")
        }
    }




    // قائمة الحركات والتعبيرات المفتوحة
    private val unlockedAnimations = mutableSetOf<String>()
    private val unlockedExpressions = mutableSetOf<String>()

    /**
     * فتح حركة جديدة
     */
    fun unlockAnimation(animationId: String) {
        unlockedAnimations.add(animationId)
        Log.d(TAG, "Animation unlocked: $animationId")
    }

    /**
     * فتح تعبير جديد
     */
    fun unlockExpression(expressionId: String) {
        unlockedExpressions.add(expressionId)
        Log.d(TAG, "Expression unlocked: $expressionId")
    }

    /**
     * التحقق مما إذا كانت الحركة مفتوحة
     */
    fun isAnimationUnlocked(animationId: String): Boolean {
        return unlockedAnimations.contains(animationId)
    }

    /**
     * التحقق مما إذا كان التعبير مفتوحاً
     */
    fun isExpressionUnlocked(expressionId: String): Boolean {
        return unlockedExpressions.contains(expressionId)
    }


