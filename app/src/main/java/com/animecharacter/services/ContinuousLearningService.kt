package com.animecharacter.services

import android.content.Context
import android.util.Log
import com.animecharacter.models.Message
import com.animecharacter.utils.PreferencesHelper
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.random.Random

/**
 * خدمة التعلم المستمر والتخصيص الشخصي
 * تحلل سلوك المستخدم وتحسن التفاعل بناءً على التعلم
 */
class ContinuousLearningService(
    private val context: Context,
    private val preferencesHelper: PreferencesHelper
) {
    
    companion object {
        private const val TAG = "ContinuousLearningService"
        private const val MAX_CONVERSATION_HISTORY = 1000
        private const val LEARNING_UPDATE_INTERVAL = 24 * 60 * 60 * 1000L // 24 ساعة
        private const val MIN_INTERACTIONS_FOR_LEARNING = 10
    }
    
    // أنواع أنماط التعلم
    enum class LearningPattern {
        CONVERSATION_STYLE,     // نمط المحادثة
        TOPIC_PREFERENCES,      // تفضيلات المواضيع
        INTERACTION_TIMING,     // توقيت التفاعل
        EMOTIONAL_RESPONSES,    // الاستجابات العاطفية
        COMMAND_USAGE,          // استخدام الأوامر
        LANGUAGE_STYLE          // نمط اللغة
    }
    
    // بيانات التعلم
    data class LearningData(
        val userId: String,
        val conversationHistory: MutableList<ConversationEntry>,
        val topicPreferences: MutableMap<String, Float>,
        val interactionPatterns: MutableMap<String, Any>,
        val emotionalProfile: EmotionalProfile,
        val commandUsage: MutableMap<String, Int>,
        val languagePreferences: LanguagePreferences,
        val lastUpdated: Long
    )
    
    // إدخال محادثة
    data class ConversationEntry(
        val timestamp: Long,
        val userMessage: String,
        val characterResponse: String,
        val userEmotion: String?,
        val responseRating: Float?, // تقييم المستخدم للرد (0-1)
        val topicTags: List<String>,
        val interactionDuration: Long
    )
    
    // الملف الشخصي العاطفي
    data class EmotionalProfile(
        val dominantEmotions: MutableMap<String, Float>,
        val emotionalTriggers: MutableMap<String, List<String>>,
        val preferredResponseStyle: String,
        val emotionalSensitivity: Float
    )
    
    // تفضيلات اللغة
    data class LanguagePreferences(
        val preferredLanguage: String,
        val formalityLevel: Float, // 0 = غير رسمي، 1 = رسمي جداً
        val complexityLevel: Float, // 0 = بسيط، 1 = معقد
        val humorAppreciation: Float,
        val dialectPreference: String?
    )
    
    // اقتراح ذكي
    data class SmartSuggestion(
        val type: SuggestionType,
        val content: String,
        val relevanceScore: Float,
        val timing: Long,
        val context: Map<String, Any>
    )
    
    enum class SuggestionType {
        TOPIC_SUGGESTION,       // اقتراح موضوع
        REMINDER,              // تذكير
        ACTIVITY_SUGGESTION,   // اقتراح نشاط
        MOOD_BOOSTER,         // رفع المعنويات
        LEARNING_TIP,         // نصيحة تعليمية
        PERSONALIZATION_TIP   // نصيحة تخصيص
    }
    
    private var learningData: LearningData
    private val learningCoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isLearningActive = false
    
    init {
        learningData = loadLearningData()
    }
    
    /**
     * بدء خدمة التعلم المستمر
     */
    fun startLearningService() {
        if (isLearningActive) return
        
        isLearningActive = true
        learningCoroutineScope.launch {
            continuousLearningLoop()
        }
        
        Log.d(TAG, "Continuous learning service started")
    }
    
    /**
     * إيقاف خدمة التعلم المستمر
     */
    fun stopLearningService() {
        isLearningActive = false
        saveLearningData()
        Log.d(TAG, "Continuous learning service stopped")
    }
    
    /**
     * تسجيل محادثة جديدة للتعلم
     */
    fun recordConversation(
        userMessage: String,
        characterResponse: String,
        userEmotion: String? = null,
        topicTags: List<String> = emptyList(),
        interactionDuration: Long = 0L
    ) {
        val entry = ConversationEntry(
            timestamp = System.currentTimeMillis(),
            userMessage = userMessage,
            characterResponse = characterResponse,
            userEmotion = userEmotion,
            responseRating = null,
            topicTags = topicTags,
            interactionDuration = interactionDuration
        )
        
        learningData.conversationHistory.add(entry)
        
        // الحفاظ على حد أقصى للتاريخ
        if (learningData.conversationHistory.size > MAX_CONVERSATION_HISTORY) {
            learningData.conversationHistory.removeAt(0)
        }
        
        // تحديث فوري للتعلم
        learningCoroutineScope.launch {
            updateLearningPatterns(entry)
        }
    }
    
    /**
     * تقييم رد الشخصية
     */
    fun rateResponse(rating: Float) {
        if (learningData.conversationHistory.isNotEmpty()) {
            val lastEntry = learningData.conversationHistory.last()
            val updatedEntry = lastEntry.copy(responseRating = rating)
            learningData.conversationHistory[learningData.conversationHistory.size - 1] = updatedEntry
            
            // تحديث التعلم بناءً على التقييم
            learningCoroutineScope.launch {
                updateBasedOnRating(updatedEntry)
            }
        }
    }
    
    /**
     * الحصول على اقتراحات ذكية
     */
    suspend fun getSmartSuggestions(): List<SmartSuggestion> {
        return withContext(Dispatchers.IO) {
            val suggestions = mutableListOf<SmartSuggestion>()
            
            // اقتراحات المواضيع
            suggestions.addAll(generateTopicSuggestions())
            
            // تذكيرات ذكية
            suggestions.addAll(generateSmartReminders())
            
            // اقتراحات الأنشطة
            suggestions.addAll(generateActivitySuggestions())
            
            // رفع المعنويات
            suggestions.addAll(generateMoodBoosters())
            
            // نصائح التعلم
            suggestions.addAll(generateLearningTips())
            
            // ترتيب حسب الصلة
            suggestions.sortedByDescending { it.relevanceScore }
        }
    }
    
    /**
     * تخصيص رد الشخصية بناءً على التعلم
     */
    fun personalizeResponse(baseResponse: String, context: Map<String, Any> = emptyMap()): String {
        var personalizedResponse = baseResponse
        
        // تطبيق نمط اللغة المفضل
        personalizedResponse = applyLanguageStyle(personalizedResponse)
        
        // تطبيق المستوى العاطفي المناسب
        personalizedResponse = applyEmotionalTone(personalizedResponse, context)
        
        // إضافة عناصر شخصية
        personalizedResponse = addPersonalElements(personalizedResponse)
        
        return personalizedResponse
    }
    
    /**
     * الحصول على تفضيلات المستخدم المتعلمة
     */
    fun getLearnedPreferences(): Map<String, Any> {
        return mapOf(
            "topicPreferences" to learningData.topicPreferences,
            "emotionalProfile" to learningData.emotionalProfile,
            "languagePreferences" to learningData.languagePreferences,
            "interactionPatterns" to learningData.interactionPatterns,
            "commandUsage" to learningData.commandUsage
        )
    }
    
    /**
     * تحديث تفضيل موضوع
     */
    fun updateTopicPreference(topic: String, preference: Float) {
        learningData.topicPreferences[topic] = preference.coerceIn(0f, 1f)
        saveLearningData()
    }
    
    /**
     * حلقة التعلم المستمر
     */
    private suspend fun continuousLearningLoop() {
        while (isLearningActive) {
            try {
                // تحديث أنماط التعلم
                if (learningData.conversationHistory.size >= MIN_INTERACTIONS_FOR_LEARNING) {
                    updateAllLearningPatterns()
                }
                
                // تنظيف البيانات القديمة
                cleanupOldData()
                
                // حفظ البيانات
                saveLearningData()
                
                // انتظار حتى التحديث التالي
                delay(LEARNING_UPDATE_INTERVAL)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in continuous learning loop", e)
                delay(60000L) // انتظار دقيقة قبل المحاولة مرة أخرى
            }
        }
    }
    
    /**
     * تحديث أنماط التعلم بناءً على محادثة جديدة
     */
    private suspend fun updateLearningPatterns(entry: ConversationEntry) {
        // تحديث تفضيلات المواضيع
        updateTopicPreferences(entry)
        
        // تحديث الملف الشخصي العاطفي
        updateEmotionalProfile(entry)
        
        // تحديث تفضيلات اللغة
        updateLanguagePreferences(entry)
        
        // تحديث أنماط التفاعل
        updateInteractionPatterns(entry)
    }
    
    /**
     * تحديث تفضيلات المواضيع
     */
    private fun updateTopicPreferences(entry: ConversationEntry) {
        for (topic in entry.topicTags) {
            val currentPreference = learningData.topicPreferences[topic] ?: 0.5f
            val rating = entry.responseRating ?: 0.7f // افتراضي إيجابي
            val newPreference = (currentPreference * 0.8f + rating * 0.2f).coerceIn(0f, 1f)
            learningData.topicPreferences[topic] = newPreference
        }
    }
    
    /**
     * تحديث الملف الشخصي العاطفي
     */
    private fun updateEmotionalProfile(entry: ConversationEntry) {
        entry.userEmotion?.let { emotion ->
            val currentLevel = learningData.emotionalProfile.dominantEmotions[emotion] ?: 0f
            learningData.emotionalProfile.dominantEmotions[emotion] = 
                (currentLevel * 0.9f + 0.1f).coerceIn(0f, 1f)
            
            // تحديث المحفزات العاطفية
            val triggers = learningData.emotionalProfile.emotionalTriggers[emotion] ?: mutableListOf()
            val keywords = extractKeywords(entry.userMessage)
            for (keyword in keywords) {
                if (keyword !in triggers) {
                    triggers.add(keyword)
                }
            }
            learningData.emotionalProfile.emotionalTriggers[emotion] = triggers.takeLast(10)
        }
    }
    
    /**
     * تحديث تفضيلات اللغة
     */
    private fun updateLanguagePreferences(entry: ConversationEntry) {
        // تحليل مستوى الرسمية
        val formalityScore = analyzeFormalityLevel(entry.userMessage)
        learningData.languagePreferences.formalityLevel = 
            (learningData.languagePreferences.formalityLevel * 0.9f + formalityScore * 0.1f)
        
        // تحليل مستوى التعقيد
        val complexityScore = analyzeComplexityLevel(entry.userMessage)
        learningData.languagePreferences.complexityLevel = 
            (learningData.languagePreferences.complexityLevel * 0.9f + complexityScore * 0.1f)
        
        // تحليل تقدير الفكاهة
        val humorScore = analyzeHumorAppreciation(entry.userMessage, entry.characterResponse)
        learningData.languagePreferences.humorAppreciation = 
            (learningData.languagePreferences.humorAppreciation * 0.9f + humorScore * 0.1f)
    }
    
    /**
     * تحديث أنماط التفاعل
     */
    private fun updateInteractionPatterns(entry: ConversationEntry) {
        val hour = Calendar.getInstance().apply { timeInMillis = entry.timestamp }.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().apply { timeInMillis = entry.timestamp }.get(Calendar.DAY_OF_WEEK)
        
        // تحديث أنماط التوقيت
        val hourKey = "hour_$hour"
        val currentHourCount = learningData.interactionPatterns[hourKey] as? Int ?: 0
        learningData.interactionPatterns[hourKey] = currentHourCount + 1
        
        val dayKey = "day_$dayOfWeek"
        val currentDayCount = learningData.interactionPatterns[dayKey] as? Int ?: 0
        learningData.interactionPatterns[dayKey] = currentDayCount + 1
        
        // تحديث متوسط مدة التفاعل
        val avgDurationKey = "avg_interaction_duration"
        val currentAvgDuration = learningData.interactionPatterns[avgDurationKey] as? Long ?: 0L
        val newAvgDuration = if (currentAvgDuration == 0L) {
            entry.interactionDuration
        } else {
            (currentAvgDuration * 0.9 + entry.interactionDuration * 0.1).toLong()
        }
        learningData.interactionPatterns[avgDurationKey] = newAvgDuration
    }
    
    /**
     * تحديث التعلم بناءً على التقييم
     */
    private suspend fun updateBasedOnRating(entry: ConversationEntry) {
        val rating = entry.responseRating ?: return
        
        if (rating >= 0.8f) {
            // رد ممتاز - تعزيز الأنماط المستخدمة
            reinforcePositivePatterns(entry)
        } else if (rating <= 0.3f) {
            // رد ضعيف - تجنب الأنماط المستخدمة
            adjustNegativePatterns(entry)
        }
    }
    
    /**
     * تعزيز الأنماط الإيجابية
     */
    private fun reinforcePositivePatterns(entry: ConversationEntry) {
        // تعزيز تفضيلات المواضيع
        for (topic in entry.topicTags) {
            val currentPreference = learningData.topicPreferences[topic] ?: 0.5f
            learningData.topicPreferences[topic] = (currentPreference + 0.1f).coerceIn(0f, 1f)
        }
        
        // تعزيز النمط العاطفي
        entry.userEmotion?.let { emotion ->
            val currentLevel = learningData.emotionalProfile.dominantEmotions[emotion] ?: 0f
            learningData.emotionalProfile.dominantEmotions[emotion] = 
                (currentLevel + 0.05f).coerceIn(0f, 1f)
        }
    }
    
    /**
     * تعديل الأنماط السلبية
     */
    private fun adjustNegativePatterns(entry: ConversationEntry) {
        // تقليل تفضيلات المواضيع
        for (topic in entry.topicTags) {
            val currentPreference = learningData.topicPreferences[topic] ?: 0.5f
            learningData.topicPreferences[topic] = (currentPreference - 0.1f).coerceIn(0f, 1f)
        }
    }
    
    /**
     * توليد اقتراحات المواضيع
     */
    private fun generateTopicSuggestions(): List<SmartSuggestion> {
        val suggestions = mutableListOf<SmartSuggestion>()
        
        // المواضيع المفضلة
        val topTopics = learningData.topicPreferences
            .filter { it.value > 0.7f }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
        
        for ((topic, preference) in topTopics) {
            suggestions.add(
                SmartSuggestion(
                    type = SuggestionType.TOPIC_SUGGESTION,
                    content = "هل تريد أن نتحدث عن $topic؟",
                    relevanceScore = preference,
                    timing = System.currentTimeMillis(),
                    context = mapOf("topic" to topic)
                )
            )
        }
        
        return suggestions
    }
    
    /**
     * توليد تذكيرات ذكية
     */
    private fun generateSmartReminders(): List<SmartSuggestion> {
        val suggestions = mutableListOf<SmartSuggestion>()
        
        // تذكيرات بناءً على أنماط التفاعل
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val usualInteractionHours = learningData.interactionPatterns
            .filter { it.key.startsWith("hour_") && it.value as Int > 5 }
            .map { it.key.substring(5).toInt() }
        
        if (currentHour in usualInteractionHours) {
            suggestions.add(
                SmartSuggestion(
                    type = SuggestionType.REMINDER,
                    content = "هذا وقت محادثتنا المعتاد! كيف حالك اليوم؟",
                    relevanceScore = 0.8f,
                    timing = System.currentTimeMillis(),
                    context = mapOf("hour" to currentHour)
                )
            )
        }
        
        return suggestions
    }
    
    /**
     * توليد اقتراحات الأنشطة
     */
    private fun generateActivitySuggestions(): List<SmartSuggestion> {
        val suggestions = mutableListOf<SmartSuggestion>()
        
        // اقتراحات بناءً على الحالة المزاجية
        val dominantEmotion = learningData.emotionalProfile.dominantEmotions
            .maxByOrNull { it.value }?.key
        
        when (dominantEmotion) {
            "happy" -> {
                suggestions.add(
                    SmartSuggestion(
                        type = SuggestionType.ACTIVITY_SUGGESTION,
                        content = "أراك في مزاج رائع! هل تريد أن نلعب لعبة أو نتحدث عن شيء ممتع؟",
                        relevanceScore = 0.7f,
                        timing = System.currentTimeMillis(),
                        context = mapOf("emotion" to "happy")
                    )
                )
            }
            "sad" -> {
                suggestions.add(
                    SmartSuggestion(
                        type = SuggestionType.MOOD_BOOSTER,
                        content = "أشعر أنك تحتاج لبعض التشجيع. هل تريد أن أحكي لك نكتة أو قصة مفرحة؟",
                        relevanceScore = 0.9f,
                        timing = System.currentTimeMillis(),
                        context = mapOf("emotion" to "sad")
                    )
                )
            }
        }
        
        return suggestions
    }
    
    /**
     * توليد معززات المزاج
     */
    private fun generateMoodBoosters(): List<SmartSuggestion> {
        val suggestions = mutableListOf<SmartSuggestion>()
        
        // معززات عامة
        val moodBoosters = listOf(
            "تذكر أنك شخص رائع ومميز!",
            "كل يوم جديد هو فرصة للبداية من جديد",
            "أنا هنا دائماً لدعمك ومساعدتك",
            "ابتسامتك تضيء يومي!"
        )
        
        suggestions.add(
            SmartSuggestion(
                type = SuggestionType.MOOD_BOOSTER,
                content = moodBoosters.random(),
                relevanceScore = 0.6f,
                timing = System.currentTimeMillis(),
                context = emptyMap()
            )
        )
        
        return suggestions
    }
    
    /**
     * توليد نصائح التعلم
     */
    private fun generateLearningTips(): List<SmartSuggestion> {
        val suggestions = mutableListOf<SmartSuggestion>()
        
        // نصائح تخصيص
        if (learningData.conversationHistory.size > 50) {
            suggestions.add(
                SmartSuggestion(
                    type = SuggestionType.PERSONALIZATION_TIP,
                    content = "لقد تعلمت الكثير عن تفضيلاتك! يمكنك تخصيص شخصيتي أكثر من الإعدادات",
                    relevanceScore = 0.5f,
                    timing = System.currentTimeMillis(),
                    context = mapOf("conversations_count" to learningData.conversationHistory.size)
                )
            )
        }
        
        return suggestions
    }
    
    /**
     * تطبيق نمط اللغة
     */
    private fun applyLanguageStyle(response: String): String {
        var styledResponse = response
        
        // تطبيق مستوى الرسمية
        if (learningData.languagePreferences.formalityLevel > 0.7f) {
            styledResponse = makeFormal(styledResponse)
        } else if (learningData.languagePreferences.formalityLevel < 0.3f) {
            styledResponse = makeInformal(styledResponse)
        }
        
        // تطبيق مستوى التعقيد
        if (learningData.languagePreferences.complexityLevel < 0.3f) {
            styledResponse = simplifyLanguage(styledResponse)
        }
        
        return styledResponse
    }
    
    /**
     * تطبيق النبرة العاطفية
     */
    private fun applyEmotionalTone(response: String, context: Map<String, Any>): String {
        val userEmotion = context["userEmotion"] as? String
        val preferredStyle = learningData.emotionalProfile.preferredResponseStyle
        
        return when (userEmotion) {
            "sad" -> addComfortingTone(response)
            "happy" -> addEnthusiasticTone(response)
            "angry" -> addCalmingTone(response)
            "excited" -> addMatchingExcitement(response)
            else -> response
        }
    }
    
    /**
     * إضافة عناصر شخصية
     */
    private fun addPersonalElements(response: String): String {
        var personalizedResponse = response
        
        // إضافة اسم المستخدم إذا كان متاحاً
        val userName = preferencesHelper.getUserName()
        if (userName.isNotEmpty() && Random.nextFloat() < 0.3f) {
            personalizedResponse = "$userName، $personalizedResponse"
        }
        
        // إضافة مراجع للمحادثات السابقة
        if (learningData.conversationHistory.isNotEmpty() && Random.nextFloat() < 0.2f) {
            val recentTopics = learningData.conversationHistory
                .takeLast(10)
                .flatMap { it.topicTags }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key
            
            if (recentTopics != null) {
                personalizedResponse += " بالمناسبة، كيف الأمور مع $recentTopics؟"
            }
        }
        
        return personalizedResponse
    }
    
    // دوال مساعدة لتحليل النص
    
    private fun extractKeywords(text: String): List<String> {
        return text.split("\\s+".toRegex())
            .filter { it.length > 3 }
            .map { it.lowercase() }
            .distinct()
            .take(5)
    }
    
    private fun analyzeFormalityLevel(text: String): Float {
        val formalWords = listOf("حضرتك", "سيادتك", "المحترم", "تفضل", "يرجى")
        val informalWords = listOf("هاي", "أهلين", "شلونك", "كيفك", "يلا")
        
        val formalCount = formalWords.count { text.contains(it, ignoreCase = true) }
        val informalCount = informalWords.count { text.contains(it, ignoreCase = true) }
        
        return when {
            formalCount > informalCount -> 0.8f
            informalCount > formalCount -> 0.2f
            else -> 0.5f
        }
    }
    
    private fun analyzeComplexityLevel(text: String): Float {
        val wordCount = text.split("\\s+".toRegex()).size
        val avgWordLength = text.replace("\\s+".toRegex(), "").length.toFloat() / wordCount
        
        return when {
            wordCount > 20 && avgWordLength > 6 -> 0.8f
            wordCount < 10 && avgWordLength < 4 -> 0.2f
            else -> 0.5f
        }
    }
    
    private fun analyzeHumorAppreciation(userMessage: String, characterResponse: String): Float {
        val humorIndicators = listOf("😂", "😄", "😆", "هههه", "ههههه", "لول", "مضحك", "نكتة")
        val humorCount = humorIndicators.count { 
            userMessage.contains(it, ignoreCase = true) || characterResponse.contains(it, ignoreCase = true)
        }
        
        return (humorCount.toFloat() / 3f).coerceIn(0f, 1f)
    }
    
    // دوال تنسيق النص
    
    private fun makeFormal(text: String): String {
        return text.replace("أهلاً", "أهلاً وسهلاً")
            .replace("شكراً", "أشكرك جزيل الشكر")
            .replace("ممتاز", "ممتاز جداً")
    }
    
    private fun makeInformal(text: String): String {
        return text.replace("أهلاً وسهلاً", "أهلاً")
            .replace("أشكرك جزيل الشكر", "شكراً")
            .replace("ممتاز جداً", "ممتاز")
    }
    
    private fun simplifyLanguage(text: String): String {
        return text.replace("استثنائي", "رائع")
            .replace("متميز", "جيد")
            .replace("بالتأكيد", "أكيد")
    }
    
    private fun addComfortingTone(text: String): String {
        return "لا تقلق، $text أنا هنا لدعمك."
    }
    
    private fun addEnthusiasticTone(text: String): String {
        return "$text! هذا رائع! 😊"
    }
    
    private fun addCalmingTone(text: String): String {
        return "تنفس بعمق... $text كل شيء سيكون بخير."
    }
    
    private fun addMatchingExcitement(text: String): String {
        return "$text! أنا متحمسة مثلك! 🎉"
    }
    
    // إدارة البيانات
    
    private fun loadLearningData(): LearningData {
        return try {
            val jsonString = preferencesHelper.getLearningData()
            if (jsonString.isNotEmpty()) {
                parseLearningDataFromJson(jsonString)
            } else {
                createDefaultLearningData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading learning data", e)
            createDefaultLearningData()
        }
    }
    
    private fun saveLearningData() {
        try {
            val jsonString = learningDataToJson(learningData)
            preferencesHelper.saveLearningData(jsonString)
            learningData.lastUpdated = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving learning data", e)
        }
    }
    
    private fun createDefaultLearningData(): LearningData {
        return LearningData(
            userId = UUID.randomUUID().toString(),
            conversationHistory = mutableListOf(),
            topicPreferences = mutableMapOf(),
            interactionPatterns = mutableMapOf(),
            emotionalProfile = EmotionalProfile(
                dominantEmotions = mutableMapOf(),
                emotionalTriggers = mutableMapOf(),
                preferredResponseStyle = "balanced",
                emotionalSensitivity = 0.5f
            ),
            commandUsage = mutableMapOf(),
            languagePreferences = LanguagePreferences(
                preferredLanguage = "ar",
                formalityLevel = 0.5f,
                complexityLevel = 0.5f,
                humorAppreciation = 0.5f,
                dialectPreference = null
            ),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun updateAllLearningPatterns() {
        // تحديث شامل لجميع أنماط التعلم
        val recentEntries = learningData.conversationHistory.takeLast(100)
        
        for (entry in recentEntries) {
            updateLearningPatterns(entry)
        }
    }
    
    private fun cleanupOldData() {
        val oneMonthAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        learningData.conversationHistory.removeAll { it.timestamp < oneMonthAgo }
    }
    
    private fun parseLearningDataFromJson(jsonString: String): LearningData {
        // تحليل JSON وإرجاع LearningData
        // هذا مبسط - في التطبيق الحقيقي نحتاج تحليل JSON كامل
        return createDefaultLearningData()
    }
    
    private fun learningDataToJson(data: LearningData): String {
        // تحويل LearningData إلى JSON
        // هذا مبسط - في التطبيق الحقيقي نحتاج تحويل JSON كامل
        return "{}"
    }
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        stopLearningService()
        saveLearningData()
        learningCoroutineScope.cancel()
    }
}

