package com.animecharacter.services

import android.content.Context
import android.util.Log
import com.animecharacter.managers.Live2DManager
import com.animecharacter.services.AIService
import com.animecharacter.services.Live2DAnimationEngine
import com.animecharacter.utils.EmotionAnalyzer
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * خدمة ربط Live2D بالذكاء الاصطناعي
 * تحلل ردود الذكاء الاصطناعي وتطبق التعبيرات والحركات المناسبة
 */
class Live2DAIIntegrationService(private val context: Context) {
    
    companion object {
        private const val TAG = "Live2DAIIntegration"
        
        // أنواع المشاعر
        const val EMOTION_HAPPY = "happy"
        const val EMOTION_SAD = "sad"
        const val EMOTION_EXCITED = "excited"
        const val EMOTION_SURPRISED = "surprised"
        const val EMOTION_ANGRY = "angry"
        const val EMOTION_THINKING = "thinking"
        const val EMOTION_LOVE = "love"
        const val EMOTION_NEUTRAL = "neutral"
        const val EMOTION_CONFUSED = "confused"
        const val EMOTION_SLEEPY = "sleepy"
        
        // أنواع الاستجابات
        const val RESPONSE_TYPE_GREETING = "greeting"
        const val RESPONSE_TYPE_QUESTION = "question"
        const val RESPONSE_TYPE_ANSWER = "answer"
        const val RESPONSE_TYPE_COMPLIMENT = "compliment"
        const val RESPONSE_TYPE_JOKE = "joke"
        const val RESPONSE_TYPE_STORY = "story"
        const val RESPONSE_TYPE_GOODBYE = "goodbye"
        
        // مستويات الطاقة
        const val ENERGY_LOW = "low"
        const val ENERGY_MEDIUM = "medium"
        const val ENERGY_HIGH = "high"
    }
    
    // الخدمات المرتبطة
    private var aiService: AIService? = null
    private var live2DManager: Live2DManager? = null
    private var animationEngine: Live2DAnimationEngine? = null
    private var emotionAnalyzer: EmotionAnalyzer? = null
    
    // حالة التكامل
    private val integrationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isInitialized = false
    private var isListening = false
    
    // تحليل المشاعر والسياق
    private val emotionHistory = mutableListOf<String>()
    private val responsePatterns = ConcurrentHashMap<String, EmotionResponse>()
    private var currentEmotion = EMOTION_NEUTRAL
    private var currentEnergyLevel = ENERGY_MEDIUM
    
    // إعدادات التفاعل
    private var emotionSensitivity = 0.7f
    private var responseDelay = 500L // تأخير قبل التفاعل
    private var expressionDuration = 3000L // مدة التعبير
    
    /**
     * استجابة عاطفية
     */
    data class EmotionResponse(
        val emotion: String,
        val expression: String,
        val motionType: String,
        val motionIndex: Int = -1,
        val energyLevel: String = ENERGY_MEDIUM,
        val priority: Int = 1
    )
    
    /**
     * تهيئة الخدمة
     */
    fun initialize(
        aiService: AIService,
        live2DManager: Live2DManager,
        animationEngine: Live2DAnimationEngine
    ): Boolean {
        return try {
            this.aiService = aiService
            this.live2DManager = live2DManager
            this.animationEngine = animationEngine
            this.emotionAnalyzer = EmotionAnalyzer(context)
            
            // تحميل أنماط الاستجابة
            loadResponsePatterns()
            
            // بدء الاستماع لردود الذكاء الاصطناعي
            startListening()
            
            isInitialized = true
            Log.d(TAG, "تم تهيئة خدمة ربط Live2D بالذكاء الاصطناعي")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تهيئة خدمة التكامل", e)
            false
        }
    }
    
    /**
     * تحميل أنماط الاستجابة
     */
    private fun loadResponsePatterns() {
        // تعريف أنماط الاستجابة العاطفية
        responsePatterns[EMOTION_HAPPY] = EmotionResponse(
            emotion = EMOTION_HAPPY,
            expression = Live2DAnimationEngine.EXPRESSION_HAPPY,
            motionType = Live2DAnimationEngine.MOTION_TYPE_SPECIAL,
            motionIndex = 0,
            energyLevel = ENERGY_HIGH
        )
        
        responsePatterns[EMOTION_SAD] = EmotionResponse(
            emotion = EMOTION_SAD,
            expression = Live2DAnimationEngine.EXPRESSION_SAD,
            motionType = Live2DAnimationEngine.MOTION_TYPE_IDLE,
            energyLevel = ENERGY_LOW
        )
        
        responsePatterns[EMOTION_EXCITED] = EmotionResponse(
            emotion = EMOTION_EXCITED,
            expression = Live2DAnimationEngine.EXPRESSION_HAPPY,
            motionType = Live2DAnimationEngine.MOTION_TYPE_SPECIAL,
            motionIndex = 1,
            energyLevel = ENERGY_HIGH,
            priority = 2
        )
        
        responsePatterns[EMOTION_SURPRISED] = EmotionResponse(
            emotion = EMOTION_SURPRISED,
            expression = Live2DAnimationEngine.EXPRESSION_SURPRISED,
            motionType = Live2DAnimationEngine.MOTION_TYPE_NORMAL,
            motionIndex = 0,
            energyLevel = ENERGY_MEDIUM
        )
        
        responsePatterns[EMOTION_ANGRY] = EmotionResponse(
            emotion = EMOTION_ANGRY,
            expression = Live2DAnimationEngine.EXPRESSION_ANGRY,
            motionType = Live2DAnimationEngine.MOTION_TYPE_NORMAL,
            motionIndex = 1,
            energyLevel = ENERGY_HIGH
        )
        
        responsePatterns[EMOTION_THINKING] = EmotionResponse(
            emotion = EMOTION_THINKING,
            expression = Live2DAnimationEngine.EXPRESSION_THINKING,
            motionType = Live2DAnimationEngine.MOTION_TYPE_IDLE,
            energyLevel = ENERGY_LOW
        )
        
        responsePatterns[EMOTION_LOVE] = EmotionResponse(
            emotion = EMOTION_LOVE,
            expression = Live2DAnimationEngine.EXPRESSION_LOVE,
            motionType = Live2DAnimationEngine.MOTION_TYPE_SPECIAL,
            motionIndex = 2,
            energyLevel = ENERGY_MEDIUM,
            priority = 2
        )
        
        responsePatterns[EMOTION_SLEEPY] = EmotionResponse(
            emotion = EMOTION_SLEEPY,
            expression = Live2DAnimationEngine.EXPRESSION_SLEEPY,
            motionType = Live2DAnimationEngine.MOTION_TYPE_IDLE,
            energyLevel = ENERGY_LOW
        )
        
        responsePatterns[EMOTION_CONFUSED] = EmotionResponse(
            emotion = EMOTION_CONFUSED,
            expression = Live2DAnimationEngine.EXPRESSION_THINKING,
            motionType = Live2DAnimationEngine.MOTION_TYPE_NORMAL,
            motionIndex = 2,
            energyLevel = ENERGY_MEDIUM
        )
        
        responsePatterns[EMOTION_NEUTRAL] = EmotionResponse(
            emotion = EMOTION_NEUTRAL,
            expression = Live2DAnimationEngine.EXPRESSION_NEUTRAL,
            motionType = Live2DAnimationEngine.MOTION_TYPE_IDLE,
            energyLevel = ENERGY_MEDIUM
        )
        
        Log.d(TAG, "تم تحميل ${responsePatterns.size} نمط استجابة")
    }
    
    /**
     * بدء الاستماع لردود الذكاء الاصطناعي
     */
    private fun startListening() {
        if (isListening) return
        
        isListening = true
        
        // في التطبيق الحقيقي، نحتاج لآلية callback من AIService
        // هنا نستخدم محاكاة للاستماع
        integrationScope.launch {
            while (isListening) {
                try {
                    // فحص دوري للردود الجديدة
                    checkForNewAIResponses()
                    delay(1000) // فحص كل ثانية
                } catch (e: Exception) {
                    Log.e(TAG, "خطأ في الاستماع للردود", e)
                }
            }
        }
        
        Log.d(TAG, "تم بدء الاستماع لردود الذكاء الاصطناعي")
    }
    
    /**
     * فحص الردود الجديدة
     */
    private suspend fun checkForNewAIResponses() {
        // في التطبيق الحقيقي، نحصل على الردود من AIService
        // هنا نستخدم محاكاة
    }
    
    /**
     * معالجة رد الذكاء الاصطناعي
     */
    fun processAIResponse(response: String, context: String = ""): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "الخدمة غير مهيأة")
            return false
        }
        
        return try {
            integrationScope.launch {
                // تحليل المشاعر في الرد
                val detectedEmotion = analyzeResponseEmotion(response, context)
                
                // تحديد نوع الاستجابة
                val responseType = determineResponseType(response)
                
                // تحديد مستوى الطاقة
                val energyLevel = determineEnergyLevel(response, detectedEmotion)
                
                // تطبيق الاستجابة المناسبة
                applyEmotionalResponse(detectedEmotion, responseType, energyLevel)
                
                // تحديث السجل العاطفي
                updateEmotionHistory(detectedEmotion)
                
                Log.d(TAG, "تم معالجة رد الذكاء الاصطناعي: $detectedEmotion")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة رد الذكاء الاصطناعي", e)
            false
        }
    }
    
    /**
     * تحليل المشاعر في الرد
     */
    private fun analyzeResponseEmotion(response: String, context: String): String {
        return emotionAnalyzer?.analyzeText(response, context) ?: run {
            // تحليل مبسط بدون EmotionAnalyzer
            analyzeEmotionSimple(response)
        }
    }
    
    /**
     * تحليل المشاعر المبسط
     */
    private fun analyzeEmotionSimple(text: String): String {
        val lowerText = text.lowercase()
        
        return when {
            // مشاعر إيجابية
            lowerText.contains(Regex("(سعيد|مسرور|فرح|رائع|ممتاز|جميل|أحب|أعشق)")) -> EMOTION_HAPPY
            lowerText.contains(Regex("(متحمس|متشوق|مثير|رائع جداً|لا أصدق)")) -> EMOTION_EXCITED
            lowerText.contains(Regex("(أحبك|حبيبي|عزيزي|قلبي|حبي)")) -> EMOTION_LOVE
            
            // مشاعر سلبية
            lowerText.contains(Regex("(حزين|مكتئب|أسف|آسف|محبط|زعلان)")) -> EMOTION_SAD
            lowerText.contains(Regex("(غاضب|زعلان|منزعج|مستاء|غضبان)")) -> EMOTION_ANGRY
            
            // مشاعر الدهشة
            lowerText.contains(Regex("(مفاجأة|مدهش|لا أصدق|واو|يا إلهي)")) -> EMOTION_SURPRISED
            
            // مشاعر التفكير
            lowerText.contains(Regex("(أفكر|أتساءل|ربما|محتمل|لست متأكد|أعتقد)")) -> EMOTION_THINKING
            lowerText.contains(Regex("(محتار|مرتبك|لا أفهم|غريب|معقد)")) -> EMOTION_CONFUSED
            
            // مشاعر النعاس
            lowerText.contains(Regex("(نعسان|متعب|أريد النوم|مرهق)")) -> EMOTION_SLEEPY
            
            // افتراضي
            else -> EMOTION_NEUTRAL
        }
    }
    
    /**
     * تحديد نوع الاستجابة
     */
    private fun determineResponseType(response: String): String {
        val lowerText = response.lowercase()
        
        return when {
            lowerText.contains(Regex("(مرحبا|أهلا|السلام|صباح|مساء)")) -> RESPONSE_TYPE_GREETING
            lowerText.contains(Regex("(\\?|كيف|ماذا|متى|أين|لماذا|هل)")) -> RESPONSE_TYPE_QUESTION
            lowerText.contains(Regex("(جميل|رائع|ممتاز|أحسنت|مبدع)")) -> RESPONSE_TYPE_COMPLIMENT
            lowerText.contains(Regex("(هههه|هاها|مضحك|نكتة|طريف)")) -> RESPONSE_TYPE_JOKE
            lowerText.contains(Regex("(قصة|حكاية|كان يا ما كان|ذات مرة)")) -> RESPONSE_TYPE_STORY
            lowerText.contains(Regex("(وداعا|مع السلامة|إلى اللقاء|باي)")) -> RESPONSE_TYPE_GOODBYE
            else -> RESPONSE_TYPE_ANSWER
        }
    }
    
    /**
     * تحديد مستوى الطاقة
     */
    private fun determineEnergyLevel(response: String, emotion: String): String {
        val lowerText = response.lowercase()
        
        return when {
            // طاقة عالية
            emotion in listOf(EMOTION_EXCITED, EMOTION_HAPPY) -> ENERGY_HIGH
            lowerText.contains(Regex("(!{2,}|رائع جداً|مذهل|لا أصدق)")) -> ENERGY_HIGH
            
            // طاقة منخفضة
            emotion in listOf(EMOTION_SAD, EMOTION_SLEEPY, EMOTION_THINKING) -> ENERGY_LOW
            lowerText.contains(Regex("(متعب|نعسان|هادئ|بطيء)")) -> ENERGY_LOW
            
            // طاقة متوسطة
            else -> ENERGY_MEDIUM
        }
    }
    
    /**
     * تطبيق الاستجابة العاطفية
     */
    private suspend fun applyEmotionalResponse(
        emotion: String,
        responseType: String,
        energyLevel: String
    ) {
        val emotionResponse = responsePatterns[emotion] ?: responsePatterns[EMOTION_NEUTRAL]!!
        
        // تأخير قبل التفاعل لجعله طبيعياً أكثر
        delay(responseDelay)
        
        // تطبيق التعبير
        animationEngine?.playExpression(emotionResponse.expression)
        
        // تطبيق الحركة بناءً على مستوى الطاقة
        val motionType = when (energyLevel) {
            ENERGY_HIGH -> Live2DAnimationEngine.MOTION_TYPE_SPECIAL
            ENERGY_LOW -> Live2DAnimationEngine.MOTION_TYPE_IDLE
            else -> emotionResponse.motionType
        }
        
        animationEngine?.playMotion(motionType, emotionResponse.motionIndex)
        
        // تطبيق تأثيرات إضافية بناءً على نوع الاستجابة
        applyResponseTypeEffects(responseType, energyLevel)
        
        // تحديث الحالة الحالية
        currentEmotion = emotion
        currentEnergyLevel = energyLevel
        
        Log.d(TAG, "تم تطبيق الاستجابة العاطفية: $emotion ($responseType, $energyLevel)")
    }
    
    /**
     * تطبيق تأثيرات نوع الاستجابة
     */
    private suspend fun applyResponseTypeEffects(responseType: String, energyLevel: String) {
        when (responseType) {
            RESPONSE_TYPE_GREETING -> {
                // حركة ترحيب
                delay(1000)
                animationEngine?.playMotion(Live2DAnimationEngine.MOTION_TYPE_NORMAL, 0)
            }
            
            RESPONSE_TYPE_QUESTION -> {
                // تعبير فضول
                delay(500)
                animationEngine?.playExpression(Live2DAnimationEngine.EXPRESSION_THINKING)
            }
            
            RESPONSE_TYPE_COMPLIMENT -> {
                // تعبير خجل أو سعادة
                delay(800)
                animationEngine?.playExpression(Live2DAnimationEngine.EXPRESSION_HAPPY)
            }
            
            RESPONSE_TYPE_JOKE -> {
                // حركة ضحك
                delay(1200)
                animationEngine?.playMotion(Live2DAnimationEngine.MOTION_TYPE_SPECIAL, 0)
            }
            
            RESPONSE_TYPE_STORY -> {
                // حركات تعبيرية متنوعة
                delay(2000)
                animationEngine?.playMotion(Live2DAnimationEngine.MOTION_TYPE_NORMAL, 1)
            }
            
            RESPONSE_TYPE_GOODBYE -> {
                // حركة وداع
                delay(1000)
                animationEngine?.playMotion(Live2DAnimationEngine.MOTION_TYPE_SPECIAL, 2)
            }
        }
    }
    
    /**
     * تحديث السجل العاطفي
     */
    private fun updateEmotionHistory(emotion: String) {
        emotionHistory.add(emotion)
        
        // الاحتفاظ بآخر 10 مشاعر فقط
        if (emotionHistory.size > 10) {
            emotionHistory.removeAt(0)
        }
        
        // تحليل النمط العاطفي
        analyzeEmotionalPattern()
    }
    
    /**
     * تحليل النمط العاطفي
     */
    private fun analyzeEmotionalPattern() {
        if (emotionHistory.size < 3) return
        
        val recentEmotions = emotionHistory.takeLast(3)
        val dominantEmotion = recentEmotions.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        
        // تعديل الحساسية العاطفية بناءً على النمط
        when (dominantEmotion) {
            EMOTION_HAPPY, EMOTION_EXCITED -> {
                emotionSensitivity = 0.8f // حساسية أعلى للمشاعر الإيجابية
            }
            EMOTION_SAD, EMOTION_ANGRY -> {
                emotionSensitivity = 0.6f // حساسية أقل للمشاعر السلبية
            }
            else -> {
                emotionSensitivity = 0.7f // حساسية متوسطة
            }
        }
        
        Log.d(TAG, "تم تحليل النمط العاطفي: $dominantEmotion (حساسية: $emotionSensitivity)")
    }
    
    /**
     * تشغيل تفاعل مخصص
     */
    fun triggerCustomInteraction(
        emotion: String,
        expression: String? = null,
        motionType: String? = null,
        motionIndex: Int = -1
    ): Boolean {
        if (!isInitialized) return false
        
        return try {
            integrationScope.launch {
                val finalExpression = expression ?: responsePatterns[emotion]?.expression
                val finalMotionType = motionType ?: responsePatterns[emotion]?.motionType
                
                finalExpression?.let { animationEngine?.playExpression(it) }
                finalMotionType?.let { animationEngine?.playMotion(it, motionIndex) }
                
                updateEmotionHistory(emotion)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تشغيل التفاعل المخصص", e)
            false
        }
    }
    
    /**
     * تعيين إعدادات التفاعل
     */
    fun setInteractionSettings(
        sensitivity: Float = emotionSensitivity,
        delay: Long = responseDelay,
        duration: Long = expressionDuration
    ) {
        emotionSensitivity = sensitivity.coerceIn(0.1f, 1.0f)
        responseDelay = delay.coerceIn(0L, 2000L)
        expressionDuration = duration.coerceIn(1000L, 10000L)
        
        Log.d(TAG, "تم تحديث إعدادات التفاعل")
    }
    
    /**
     * الحصول على المشاعر الحالية
     */
    fun getCurrentEmotion(): String = currentEmotion
    
    /**
     * الحصول على مستوى الطاقة الحالي
     */
    fun getCurrentEnergyLevel(): String = currentEnergyLevel
    
    /**
     * الحصول على السجل العاطفي
     */
    fun getEmotionHistory(): List<String> = emotionHistory.toList()
    
    /**
     * إيقاف الاستماع
     */
    fun stopListening() {
        isListening = false
        Log.d(TAG, "تم إيقاف الاستماع لردود الذكاء الاصطناعي")
    }
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        stopListening()
        integrationScope.cancel()
        
        emotionHistory.clear()
        responsePatterns.clear()
        
        aiService = null
        live2DManager = null
        animationEngine = null
        emotionAnalyzer = null
        
        Log.d(TAG, "تم تنظيف موارد خدمة التكامل")
    }
}

