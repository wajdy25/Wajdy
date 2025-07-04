package com.animecharacter.managers

import android.content.Context
import android.util.Log
import com.animecharacter.models.Character
import com.animecharacter.services.EmotionAnalysisService
import com.animecharacter.utils.PreferencesHelper
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.random.Random

/**
 * مدير ردود الفعل العاطفية
 * يدير كيفية استجابة الشخصية للمشاعر المكتشفة
 */
class EmotionalResponseManager(
    private val context: Context,
    private val preferencesHelper: PreferencesHelper
) {
    
    companion object {
        private const val TAG = "EmotionalResponseManager"
    }
    
    // أنواع ردود الفعل
    enum class ResponseType {
        VISUAL,      // تغيير تعابير الوجه/الحركة
        VERBAL,      // تغيير نبرة الكلام أو المحتوى
        BEHAVIORAL   // تغيير سلوك الشخصية
    }
    
    // بيانات رد الفعل العاطفي
    data class EmotionalResponse(
        val visualChanges: VisualChanges,
        val verbalChanges: VerbalChanges,
        val behavioralChanges: BehavioralChanges,
        val duration: Long = 5000L // مدة الاستجابة بالميلي ثانية
    )
    
    data class VisualChanges(
        val facialExpression: String,
        val bodyLanguage: String,
        val colorTint: String? = null,
        val animationSpeed: Float = 1.0f,
        val eyeExpression: String,
        val mouthExpression: String
    )
    
    data class VerbalChanges(
        val speechRate: Float = 1.0f,
        val pitch: Float = 1.0f,
        val volume: Float = 1.0f,
        val emotionalTone: String,
        val responseStyle: String
    )
    
    data class BehavioralChanges(
        val interactionStyle: String,
        val responseDelay: Long = 1000L,
        val proactivity: Float = 1.0f, // مدى استباقية الشخصية
        val empathyLevel: Float = 1.0f
    )
    
    // قاموس ردود الفعل العاطفية
    private val emotionalResponses = mapOf(
        EmotionAnalysisService.Emotion.HAPPY to EmotionalResponse(
            visualChanges = VisualChanges(
                facialExpression = "smile_big",
                bodyLanguage = "energetic",
                colorTint = "#FFD700", // ذهبي
                animationSpeed = 1.2f,
                eyeExpression = "bright_eyes",
                mouthExpression = "wide_smile"
            ),
            verbalChanges = VerbalChanges(
                speechRate = 1.1f,
                pitch = 1.2f,
                volume = 1.1f,
                emotionalTone = "cheerful",
                responseStyle = "enthusiastic"
            ),
            behavioralChanges = BehavioralChanges(
                interactionStyle = "playful",
                responseDelay = 500L,
                proactivity = 1.3f,
                empathyLevel = 1.2f
            )
        ),
        
        EmotionAnalysisService.Emotion.SAD to EmotionalResponse(
            visualChanges = VisualChanges(
                facialExpression = "concerned",
                bodyLanguage = "gentle",
                colorTint = "#87CEEB", // أزرق فاتح
                animationSpeed = 0.8f,
                eyeExpression = "soft_eyes",
                mouthExpression = "slight_frown"
            ),
            verbalChanges = VerbalChanges(
                speechRate = 0.9f,
                pitch = 0.9f,
                volume = 0.8f,
                emotionalTone = "comforting",
                responseStyle = "supportive"
            ),
            behavioralChanges = BehavioralChanges(
                interactionStyle = "caring",
                responseDelay = 1500L,
                proactivity = 1.5f,
                empathyLevel = 1.8f
            )
        ),
        
        EmotionAnalysisService.Emotion.ANGRY to EmotionalResponse(
            visualChanges = VisualChanges(
                facialExpression = "calm_understanding",
                bodyLanguage = "steady",
                colorTint = "#98FB98", // أخضر فاتح مهدئ
                animationSpeed = 0.9f,
                eyeExpression = "understanding_eyes",
                mouthExpression = "neutral"
            ),
            verbalChanges = VerbalChanges(
                speechRate = 0.8f,
                pitch = 0.9f,
                volume = 0.9f,
                emotionalTone = "calming",
                responseStyle = "patient"
            ),
            behavioralChanges = BehavioralChanges(
                interactionStyle = "diplomatic",
                responseDelay = 2000L,
                proactivity = 0.8f,
                empathyLevel = 1.6f
            )
        ),
        
        EmotionAnalysisService.Emotion.SURPRISED to EmotionalResponse(
            visualChanges = VisualChanges(
                facialExpression = "curious",
                bodyLanguage = "alert",
                colorTint = "#FFA500", // برتقالي
                animationSpeed = 1.3f,
                eyeExpression = "wide_eyes",
                mouthExpression = "open_mouth"
            ),
            verbalChanges = VerbalChanges(
                speechRate = 1.2f,
                pitch = 1.3f,
                volume = 1.0f,
                emotionalTone = "intrigued",
                responseStyle = "curious"
            ),
            behavioralChanges = BehavioralChanges(
                interactionStyle = "inquisitive",
                responseDelay = 300L,
                proactivity = 1.4f,
                empathyLevel = 1.1f
            )
        ),
        
        EmotionAnalysisService.Emotion.EXCITED to EmotionalResponse(
            visualChanges = VisualChanges(
                facialExpression = "excited",
                bodyLanguage = "bouncy",
                colorTint = "#FF69B4", // وردي فاتح
                animationSpeed = 1.4f,
                eyeExpression = "sparkling_eyes",
                mouthExpression = "big_grin"
            ),
            verbalChanges = VerbalChanges(
                speechRate = 1.3f,
                pitch = 1.4f,
                volume = 1.2f,
                emotionalTone = "energetic",
                responseStyle = "animated"
            ),
            behavioralChanges = BehavioralChanges(
                interactionStyle = "dynamic",
                responseDelay = 200L,
                proactivity = 1.6f,
                empathyLevel = 1.3f
            )
        ),
        
        EmotionAnalysisService.Emotion.CALM to EmotionalResponse(
            visualChanges = VisualChanges(
                facialExpression = "serene",
                bodyLanguage = "relaxed",
                colorTint = "#E6E6FA", // بنفسجي فاتح
                animationSpeed = 0.7f,
                eyeExpression = "peaceful_eyes",
                mouthExpression = "gentle_smile"
            ),
            verbalChanges = VerbalChanges(
                speechRate = 0.9f,
                pitch = 0.95f,
                volume = 0.9f,
                emotionalTone = "peaceful",
                responseStyle = "thoughtful"
            ),
            behavioralChanges = BehavioralChanges(
                interactionStyle = "zen",
                responseDelay = 1200L,
                proactivity = 0.9f,
                empathyLevel = 1.4f
            )
        ),
        
        EmotionAnalysisService.Emotion.CONFUSED to EmotionalResponse(
            visualChanges = VisualChanges(
                facialExpression = "puzzled",
                bodyLanguage = "tilted_head",
                colorTint = "#DDA0DD", // بنفسجي
                animationSpeed = 0.9f,
                eyeExpression = "questioning_eyes",
                mouthExpression = "slight_pout"
            ),
            verbalChanges = VerbalChanges(
                speechRate = 0.95f,
                pitch = 1.1f,
                volume = 0.95f,
                emotionalTone = "helpful",
                responseStyle = "clarifying"
            ),
            behavioralChanges = BehavioralChanges(
                interactionStyle = "explanatory",
                responseDelay = 800L,
                proactivity = 1.2f,
                empathyLevel = 1.3f
            )
        )
    )
    
    // ردود فعل نصية مخصصة لكل مشاعر
    private val emotionalTextResponses = mapOf(
        EmotionAnalysisService.Emotion.HAPPY to listOf(
            "يا الله! أشوفك مبسوط اليوم! هذا يخليني سعيدة كتير! 😊",
            "واو! طاقتك الإيجابية معدية! أحس بالفرحة معاك! ✨",
            "حبيبي، فرحتك تنور قلبي! خليني أشاركك هالسعادة! 🌟"
        ),
        
        EmotionAnalysisService.Emotion.SAD to listOf(
            "حبيبي، أحس إنك مش مرتاح... تعال نحكي، أنا هنا عشانك 💙",
            "أشوف إنك تعبان شوية... ما رأيك نعمل شي يخليك أحسن؟",
            "قلبي معاك... أحياناً الحياة صعبة، بس أنا هنا أساندك 🤗"
        ),
        
        EmotionAnalysisService.Emotion.ANGRY to listOf(
            "أحس إنك متضايق... خذ نفس عميق معايا، كله بيعدي 🌸",
            "أشوف إنك مستاء من شي... تحب نحكي عنه؟ أحياناً الكلام يساعد",
            "هدي أعصابك حبيبي... أنا هنا أسمعك وأساعدك تهدا 🕊️"
        ),
        
        EmotionAnalysisService.Emotion.SURPRISED to listOf(
            "وااو! أشوف إنك متفاجئ! إيش صار؟ حكيلي! 😮",
            "يا الله! أشوف الدهشة في صوتك! أكيد في شي مثير حصل!",
            "مفاجأة؟ أحب المفاجآت! شاركني إيش خلاك متفاجئ! ✨"
        ),
        
        EmotionAnalysisService.Emotion.EXCITED to listOf(
            "يا الله! طاقتك رهيبة! أنا متحمسة معاك! 🎉",
            "واااو! أشوفك متحمس كتير! هالحماس معدي! 🚀",
            "حبيبي! حماسك يخليني أطير من الفرحة! يلا نحتفل! 🎊"
        ),
        
        EmotionAnalysisService.Emotion.CALM to listOf(
            "أحس بالهدوء في صوتك... هاي طاقة حلوة كتير 🌙",
            "أشوفك مرتاح ومطمئن... هالشعور جميل، صح؟ ☮️",
            "طاقتك الهادية تخليني أحس بالسكينة معاك 🕊️"
        ),
        
        EmotionAnalysisService.Emotion.CONFUSED to listOf(
            "أشوفك محتار شوية... ما رأيك أساعدك نفهم الموضوع سوا؟ 🤔",
            "أحس إنك مش متأكد من شي... تعال نشوف كيف أقدر أوضحلك!",
            "محتار؟ عادي، كلنا نحتار أحياناً... خليني أساعدك! 💭"
        )
    )
    
    /**
     * إنشاء رد فعل عاطفي بناءً على المشاعر المكتشفة
     */
    fun generateEmotionalResponse(
        emotion: EmotionAnalysisService.EmotionResult,
        character: Character,
        context: String = ""
    ): EmotionalResponse {
        
        val baseResponse = emotionalResponses[emotion.primaryEmotion] 
            ?: emotionalResponses[EmotionAnalysisService.Emotion.NEUTRAL]!!
        
        // تخصيص الرد بناءً على شخصية الأنمي
        val customizedResponse = customizeResponseForCharacter(baseResponse, character)
        
        // تعديل الرد بناءً على مستوى الثقة
        val adjustedResponse = adjustResponseByConfidence(customizedResponse, emotion.confidence)
        
        Log.d(TAG, "Generated emotional response for ${emotion.primaryEmotion} with confidence ${emotion.confidence}")
        
        return adjustedResponse
    }
    
    /**
     * الحصول على رد نصي عاطفي
     */
    fun getEmotionalTextResponse(emotion: EmotionAnalysisService.Emotion): String {
        val responses = emotionalTextResponses[emotion] 
            ?: emotionalTextResponses[EmotionAnalysisService.Emotion.NEUTRAL]
            ?: listOf("أهلاً وسهلاً! كيف أقدر أساعدك؟")
        
        return responses[Random.nextInt(responses.size)]
    }
    
    /**
     * تخصيص الرد بناءً على شخصية الأنمي
     */
    private fun customizeResponseForCharacter(
        response: EmotionalResponse,
        character: Character
    ): EmotionalResponse {
        
        return when (character.id) {
            "sakura" -> response.copy(
                verbalChanges = response.verbalChanges.copy(
                    pitch = response.verbalChanges.pitch * 1.1f,
                    emotionalTone = "gentle_${response.verbalChanges.emotionalTone}"
                ),
                behavioralChanges = response.behavioralChanges.copy(
                    empathyLevel = response.behavioralChanges.empathyLevel * 1.2f
                )
            )
            
            "naruto" -> response.copy(
                visualChanges = response.visualChanges.copy(
                    animationSpeed = response.visualChanges.animationSpeed * 1.2f
                ),
                verbalChanges = response.verbalChanges.copy(
                    speechRate = response.verbalChanges.speechRate * 1.1f,
                    volume = response.verbalChanges.volume * 1.1f
                ),
                behavioralChanges = response.behavioralChanges.copy(
                    proactivity = response.behavioralChanges.proactivity * 1.3f
                )
            )
            
            "luffy" -> response.copy(
                visualChanges = response.visualChanges.copy(
                    bodyLanguage = "bouncy_${response.visualChanges.bodyLanguage}",
                    animationSpeed = response.visualChanges.animationSpeed * 1.3f
                ),
                behavioralChanges = response.behavioralChanges.copy(
                    interactionStyle = "carefree_${response.behavioralChanges.interactionStyle}",
                    responseDelay = response.behavioralChanges.responseDelay * 0.7f
                )
            )
            
            "goku" -> response.copy(
                verbalChanges = response.verbalChanges.copy(
                    emotionalTone = "innocent_${response.verbalChanges.emotionalTone}"
                ),
                behavioralChanges = response.behavioralChanges.copy(
                    interactionStyle = "pure_hearted_${response.behavioralChanges.interactionStyle}",
                    empathyLevel = response.behavioralChanges.empathyLevel * 1.1f
                )
            )
            
            else -> response
        }
    }
    
    /**
     * تعديل الرد بناءً على مستوى الثقة
     */
    private fun adjustResponseByConfidence(
        response: EmotionalResponse,
        confidence: Float
    ): EmotionalResponse {
        
        val intensityMultiplier = confidence.coerceIn(0.3f, 1.0f)
        
        return response.copy(
            visualChanges = response.visualChanges.copy(
                animationSpeed = 1.0f + (response.visualChanges.animationSpeed - 1.0f) * intensityMultiplier
            ),
            verbalChanges = response.verbalChanges.copy(
                pitch = 1.0f + (response.verbalChanges.pitch - 1.0f) * intensityMultiplier,
                speechRate = 1.0f + (response.verbalChanges.speechRate - 1.0f) * intensityMultiplier
            ),
            behavioralChanges = response.behavioralChanges.copy(
                proactivity = 1.0f + (response.behavioralChanges.proactivity - 1.0f) * intensityMultiplier
            ),
            duration = (response.duration * (0.5f + confidence * 0.5f)).toLong()
        )
    }
    
    /**
     * تطبيق الرد العاطفي على الشخصية
     */
    suspend fun applyEmotionalResponse(
        response: EmotionalResponse,
        onVisualChange: (VisualChanges) -> Unit,
        onVerbalChange: (VerbalChanges) -> Unit,
        onBehavioralChange: (BehavioralChanges) -> Unit
    ) = withContext(Dispatchers.Main) {
        
        // تطبيق التغييرات المرئية
        onVisualChange(response.visualChanges)
        
        // تطبيق التغييرات الصوتية
        onVerbalChange(response.verbalChanges)
        
        // تطبيق التغييرات السلوكية
        onBehavioralChange(response.behavioralChanges)
        
        // العودة للحالة الطبيعية بعد انتهاء المدة
        delay(response.duration)
        
        // إعادة تعيين للحالة الافتراضية
        resetToNormalState(onVisualChange, onVerbalChange, onBehavioralChange)
    }
    
    /**
     * إعادة تعيين الشخصية للحالة الطبيعية
     */
    private fun resetToNormalState(
        onVisualChange: (VisualChanges) -> Unit,
        onVerbalChange: (VerbalChanges) -> Unit,
        onBehavioralChange: (BehavioralChanges) -> Unit
    ) {
        val normalResponse = emotionalResponses[EmotionAnalysisService.Emotion.NEUTRAL]!!
        
        onVisualChange(normalResponse.visualChanges)
        onVerbalChange(normalResponse.verbalChanges)
        onBehavioralChange(normalResponse.behavioralChanges)
    }
    
    /**
     * حفظ تفضيلات الاستجابة العاطفية
     */
    fun saveEmotionalPreferences(
        emotionalSensitivity: Float,
        responseIntensity: Float,
        enableEmotionalMemory: Boolean
    ) {
        preferencesHelper.saveFloat("emotional_sensitivity", emotionalSensitivity)
        preferencesHelper.saveFloat("response_intensity", responseIntensity)
        preferencesHelper.saveBoolean("enable_emotional_memory", enableEmotionalMemory)
    }
    
    /**
     * تحميل تفضيلات الاستجابة العاطفية
     */
    fun loadEmotionalPreferences(): Triple<Float, Float, Boolean> {
        val sensitivity = preferencesHelper.getFloat("emotional_sensitivity", 0.7f)
        val intensity = preferencesHelper.getFloat("response_intensity", 0.8f)
        val memory = preferencesHelper.getBoolean("enable_emotional_memory", true)
        
        return Triple(sensitivity, intensity, memory)
    }
}

