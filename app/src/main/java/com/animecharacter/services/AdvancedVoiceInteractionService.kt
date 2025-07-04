package com.animecharacter.services

import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.location.LocationManager
import android.location.Geocoder
import java.util.Locale
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.animecharacter.utils.EmotionAnalyzer

class AdvancedVoiceInteractionService(private val context: Context, private val live2DAnimationEngine: Live2DAnimationEngine) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AdvancedVoiceInteractionService"
        private const val UTTERANCE_ID_PREFIX = "social_media_"
    }

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val preferencesHelper = PreferencesHelper(context)
    private val emotionAnalyzer = EmotionAnalyzer(context)    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // قوائم العبارات المحفزة
    private val celebrationMessages = mutableMapOf<String, List<String>>()
    private val encouragementMessages = mutableListOf<String>()
    private val dailyReportMessages = mutableListOf<String>()
    private val goalCompletionMessages = mutableListOf<String>()
    private val milestoneMessages = mutableMapOf<String, List<String>>()
    
    // إعدادات الصوت
    private var voiceSettings = VoiceSettings()
    private var isInitialized = false
    private var currentUtteranceId: String? = null
    private var isListening = false
    private lateinit var locationManager: LocationManager
    private lateinit var geocoder: Geocoder

    init {
        initializeTextToSpeech()
        initializeSpeechRecognizer()
        loadVoiceMessages()
        loadVoiceSettings()
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        geocoder = Geocoder(context, Locale.getDefault())
        updateLocationAndLearnDialect()
    }

    /**
     * تهيئة محرك النص إلى كلام
     */
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context, this)
    }

    /**
     * تهيئة محرك التعرف على الكلام
     */    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "onReadyForSpeech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Log.d(TAG, "onRmsChanged: $rmsdB")
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    Log.d(TAG, "onBufferReceived")
                }

                override fun onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech")
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
                        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
                        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
                        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
                        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
                        else -> "UNKNOWN_ERROR"
                    }
                    Log.e(TAG, "SpeechRecognizer onError: $errorMessage ($error)")
                    isListening = false
                    // Restart listening if not a client error (e.g., user stopped speaking)
                    if (error != SpeechRecognizer.ERROR_CLIENT) {
                        startListeningForWakeWord()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0]
                        Log.d(TAG, "onResults: $recognizedText")
                        val emotion = emotionAnalyzer.analyzeTextEmotion(recognizedText)
                        Log.d(TAG, "Recognized text emotion: $emotion")
                        live2DAnimationEngine.setEmotion(emotion)
                        handleRecognizedText(recognizedText)}
                    isListening = false
                    startListeningForWakeWord() // Continue listening
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partialText = matches[0]
                        Log.d(TAG, "onPartialResults: $partialText")
                        // Optional: handle partial results for faster wake word detection
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    Log.d(TAG, "onEvent: $eventType")
                }
            })
        } else {
            Log.e(TAG, "Speech recognition not available on this device.")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val selectedLanguage = preferencesHelper.getVoiceLanguage()
                val selectedDialect = preferencesHelper.getSelectedDialect()

                val locale = if (selectedDialect.isNotEmpty()) {
                    Locale(selectedLanguage.substringBefore("-"), selectedDialect.substringAfter("-"))
                } else {
                    Locale(selectedLanguage.substringBefore("-"), selectedLanguage.substringAfter("-"))
                }

                val result = tts.setLanguage(locale)
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Selected language/dialect not supported: $selectedLanguage/$selectedDialect, using default language")
                    tts.setLanguage(Locale.getDefault())
                }
                
                // تعيين معدل الكلام ونبرة الصوت
                tts.setSpeechRate(voiceSettings.speechRate)
                tts.setPitch(voiceSettings.pitch)
                
                // تعيين مستمع التقدم
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "Speech started: $utteranceId")
                        currentUtteranceId = utteranceId
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "Speech completed: $utteranceId")
                        currentUtteranceId = null
                    }
                    
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "Speech error: $utteranceId")
                        currentUtteranceId = null
                    }
                })
                
                isInitialized = true
                Log.d(TAG, "TextToSpeech initialized successfully")
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed")
        }
    }

    /**
     * تحميل الرسائل الصوتية
     */
    private fun loadVoiceMessages() {
        loadCelebrationMessages()
        loadEncouragementMessages()
        loadDailyReportMessages()
        loadGoalCompletionMessages()
        loadMilestoneMessages()
    }

    /**
     * تحميل رسائل الاحتفال
     */
    private fun loadCelebrationMessages() {
        // رسائل المتابعين الجدد
        celebrationMessages["new_followers_level_1"] = listOf(
            "عظيم! متابع جديد انضم إليك! استمر في التألق!",
            "رائع! شخص جديد أعجب بمحتواك!",
            "مبروك! عائلتك تكبر شيئاً فشيئاً!",
            "جميل! متابع جديد يقدر ما تقدمه!"
        )
        
        celebrationMessages["new_followers_level_2"] = listOf(
            "واو! عدة متابعين جدد! جمهورك يكبر بسرعة!",
            "ممتاز! المحتوى الخاص بك يجذب المزيد من الناس!",
            "رائع جداً! أصبحت أكثر شعبية!",
            "مذهل! الناس تحب ما تشاركه معهم!"
        )
        
        celebrationMessages["new_followers_level_3"] = listOf(
            "لا يُصدق! عدد كبير من المتابعين الجدد! أنت نجم بكل معنى الكلمة!",
            "تحطيم الأرقام القياسية! جمهورك ينمو بشكل مذهل!",
            "إنجاز خيالي! أصبحت مؤثراً حقيقياً!",
            "واو! هذا النمو السريع يدل على موهبتك الاستثنائية!"
        )
        
        // رسائل الإعجابات الجديدة
        celebrationMessages["new_likes_level_1"] = listOf(
            "جميل! إعجابات جديدة على منشورك!",
            "رائع! الناس تحب ما تشاركه!",
            "ممتاز! تفاعل إيجابي مع محتواك!",
            "عظيم! منشورك يلقى استحساناً!"
        )
        
        celebrationMessages["new_likes_level_2"] = listOf(
            "ممتاز! عدد رائع من الإعجابات الجديدة!",
            "مذهل! المحتوى الخاص بك يحقق تفاعلاً قوياً!",
            "رائع جداً! الناس تتفاعل بحماس مع منشوراتك!",
            "عظيم! هذا التفاعل يدل على جودة ما تقدمه!"
        )
        
        celebrationMessages["new_likes_level_3"] = listOf(
            "انفجار من الإعجابات! منشورك يحقق نجاحاً باهراً!",
            "تحطيم الأرقام القياسية! آلاف الإعجابات في وقت قصير!",
            "لا يُصدق! منشورك أصبح فيروسياً!",
            "إنجاز مذهل! أنت في القمة الآن!"
        )
        
        // رسائل التعليقات الجديدة
        celebrationMessages["new_comments_level_1"] = listOf(
            "رائع! شخص ما علق على منشورك!",
            "جميل! تفاعل مباشر مع جمهورك!",
            "ممتاز! الناس تريد التحدث معك!",
            "عظيم! محتواك يثير النقاش!"
        )
        
        celebrationMessages["new_comments_level_2"] = listOf(
            "مذهل! عدة تعليقات جديدة! الناس متحمسة للتفاعل معك!",
            "رائع جداً! منشورك يثير اهتمام الكثيرين!",
            "ممتاز! نقاش حيوي حول ما تشاركه!",
            "عظيم! أصبحت محور اهتمام الجمهور!"
        )
        
        celebrationMessages["new_comments_level_3"] = listOf(
            "الجميع يتحدث عنك! عشرات التعليقات على منشورك!",
            "إنجاز رائع! منشورك أصبح موضوع النقاش الأول!",
            "لا يُصدق! الناس لا تتوقف عن التعليق!",
            "مذهل! أصبحت ترند حقيقي!"
        )
        
        // رسائل المشاركات الجديدة
        celebrationMessages["new_shares_level_1"] = listOf(
            "رائع! شخص ما شارك منشورك!",
            "جميل! محتواك يستحق المشاركة!",
            "ممتاز! الناس تريد نشر ما تقوله!",
            "عظيم! منشورك ينتشر!"
        )
        
        celebrationMessages["new_shares_level_2"] = listOf(
            "مذهل! عدة مشاركات لمنشورك! الناس تحب نشر محتواك!",
            "رائع جداً! منشورك ينتشر بسرعة!",
            "ممتاز! أصبحت مصدر إلهام للآخرين!",
            "عظيم! محتواك يستحق الانتشار الواسع!"
        )
        
        celebrationMessages["new_shares_level_3"] = listOf(
            "منشورك ينتشر كالنار في الهشيم! مئات المشاركات!",
            "إنجاز خيالي! منشورك أصبح فيروسياً!",
            "لا يُصدق! الجميع يشارك ما كتبته!",
            "مذهل! أصبحت ظاهرة على وسائل التواصل!"
        )
        
        // رسائل المشاهدات الجديدة
        celebrationMessages["new_views_level_1"] = listOf(
            "جميل! مشاهدات جديدة على محتواك!",
            "رائع! الناس تشاهد ما تنشره!",
            "ممتاز! محتواك يجذب الانتباه!",
            "عظيم! المزيد من الناس تكتشفك!"
        )
        
        celebrationMessages["new_views_level_2"] = listOf(
            "مذهل! مئات المشاهدات الجديدة!",
            "رائع جداً! محتواك يحقق وصولاً واسعاً!",
            "ممتاز! الناس تتابع ما تقدمه بشغف!",
            "عظيم! أصبحت مشهوراً أكثر فأكثر!"
        )
        
        celebrationMessages["new_views_level_3"] = listOf(
            "فيديوك يحقق أرقاماً خيالية! آلاف المشاهدات!",
            "إنجاز مذهل! محتواك وصل لجمهور عريض!",
            "لا يُصدق! ملايين العيون تشاهد ما تقدمه!",
            "تحطيم الأرقام القياسية! أصبحت نجماً حقيقياً!"
        )
    }

    /**
     * تحميل رسائل التشجيع
     */
    private fun loadEncouragementMessages() {
        encouragementMessages.addAll(listOf(
            "استمر في العطاء! أنت على الطريق الصحيح!",
            "موهبتك تتألق أكثر كل يوم!",
            "الناس تحب ما تقدمه، لا تتوقف!",
            "أنت تصنع الفرق في حياة الآخرين!",
            "كل منشور جديد هو خطوة نحو النجاح!",
            "جمهورك يثق بك ويتطلع لما هو قادم!",
            "أنت مصدر إلهام للكثيرين!",
            "استمر في التطوير، النجاح قريب!",
            "كل تفاعل جديد يؤكد موهبتك!",
            "أنت تبني مجتمعاً رائعاً حولك!"
        ))
    }

    /**
     * تحميل رسائل التقرير اليومي
     */
    private fun loadDailyReportMessages() {
        dailyReportMessages.addAll(listOf(
            "إليك تقرير يومك على وسائل التواصل الاجتماعي:",
            "دعني أخبرك بإنجازاتك اليوم:",
            "هذا ملخص نشاطك اليوم:",
            "تقرير يومي عن تفاعلك مع الجمهور:",
            "إليك نظرة على أداءك اليوم:",
            "ملخص إنجازاتك في آخر 24 ساعة:",
            "تقرير شامل عن نشاطك اليوم:",
            "دعني أراجع معك إنجازات اليوم:"
        ))
    }

    /**
     * تحميل رسائل إنجاز الأهداف
     */
    private fun loadGoalCompletionMessages() {
        goalCompletionMessages.addAll(listOf(
            "مبروك! لقد حققت هدفاً جديداً!",
            "إنجاز رائع! وصلت لما كنت تسعى إليه!",
            "تهانينا! هدف آخر يتحقق بفضل جهدك!",
            "عظيم! لقد أنجزت ما خططت له!",
            "مذهل! هدف جديد في جعبة إنجازاتك!",
            "رائع! المثابرة أوصلتك لهدفك!",
            "ممتاز! خطوة أخرى نحو النجاح الكبير!",
            "مبروك! إنجاز يستحق الاحتفال!"
        ))
    }

    /**
     * تحميل رسائل المعالم
     */
    private fun loadMilestoneMessages() {
        milestoneMessages["followers_milestone"] = listOf(
            "معلم تاريخي! وصلت إلى {} متابع!",
            "إنجاز استثنائي! {} متابع يثقون بك!",
            "رقم مذهل! {} شخص يتابعون رحلتك!",
            "معلم جديد! {} متابع في رصيدك!"
        )
        
        milestoneMessages["likes_milestone"] = listOf(
            "رقم قياسي! {} إعجاب على محتواك!",
            "إنجاز رائع! {} إعجاب يؤكدون جودة ما تقدمه!",
            "معلم مذهل! {} إعجاب في رصيدك!",
            "تحطيم الأرقام! {} إعجاب على منشوراتك!"
        )
        
        milestoneMessages["views_milestone"] = listOf(
            "رقم خيالي! {} مشاهدة لمحتواك!",
            "إنجاز باهر! {} مشاهدة تؤكد شعبيتك!",
            "معلم استثنائي! {} مشاهدة في رصيدك!",
            "أرقام مذهلة! {} مشاهدة لما تقدمه!"
        )
    }

    /**
     * تحميل إعدادات الصوت
     */
    private fun loadVoiceSettings() {
        voiceSettings = preferencesHelper.getVoiceSettings() ?: VoiceSettings()
    }

    /**
     * تشغيل رسالة احتفال للتفاعل
     */
    fun speakCelebrationMessage(interaction: SocialMediaInteraction, celebrationLevel: Int) {
        if (!isInitialized || !voiceSettings.isEnabled) return
        
        val messageKey = "${interaction.type.name.lowercase()}_level_$celebrationLevel"
        val messages = celebrationMessages[messageKey] ?: return
     
(Content truncated due to size limit. Use line ranges to read in chunks)]
        val personalizedMessage = personalizeMessage(selectedMessage, interaction)
        
        speak(personalizedMessage, "celebration_${System.currentTimeMillis()}")
    }

    /**
     * تشغيل رسالة تشجيع
     */
    fun speakEncouragementMessage() {
        if (!isInitialized || !voiceSettings.isEnabled) return
        
        val message = encouragementMessages[Random.nextInt(encouragementMessages.size)]
        speak(message, "encouragement_${System.currentTimeMillis()}")
    }

    /**
     * تشغيل التقرير اليومي
     */
    fun speakDailyReport(report: DailyReport) {
        if (!isInitialized || !voiceSettings.isEnabled) return
        
        scope.launch {
            val intro = dailyReportMessages[Random.nextInt(dailyReportMessages.size)]
            speak(intro, "daily_report_intro")
            
            delay(2000) // انتظار انتهاء المقدمة
            
            val reportContent = buildDailyReportContent(report)
            speak(reportContent, "daily_report_content")
            
            delay(3000) // انتظار انتهاء المحتوى
            
            speak(report.encouragementMessage, "daily_report_encouragement")
        }
    }

    /**
     * تشغيل رسالة إنجاز هدف
     */
    fun speakGoalCompletion(goal: SocialMediaGoal) {
        if (!isInitialized || !voiceSettings.isEnabled) return
        
        val baseMessage = goalCompletionMessages[Random.nextInt(goalCompletionMessages.size)]
        val goalMessage = "لقد حققت هدف: ${goal.title}!"
        val fullMessage = "$baseMessage $goalMessage"
        
        speak(fullMessage, "goal_completion_${goal.id}")
    }

    /**
     * تشغيل رسالة معلم
     */
    fun speakMilestoneAchievement(type: SocialMediaInteraction.Type, milestone: Int, platform: SocialMediaPlatform) {
        if (!isInitialized || !voiceSettings.isEnabled) return
        
        val messageKey = when (type) {
            SocialMediaInteraction.Type.NEW_FOLLOWERS -> "followers_milestone"
            SocialMediaInteraction.Type.NEW_LIKES -> "likes_milestone"
            SocialMediaInteraction.Type.NEW_VIEWS -> "views_milestone"
            else -> "followers_milestone"
        }
        
        val messages = milestoneMessages[messageKey] ?: return
        val template = messages[Random.nextInt(messages.size)]
        val message = template.replace("{}", milestone.toString())
        
        speak(message, "milestone_${type.name}_$milestone")
    }

    /**
     * تشغيل رسالة مخصصة
     */
    fun speakCustomMessage(message: String, utteranceId: String? = null) {
        if (!isInitialized || !voiceSettings.isEnabled) return
        
        val id = utteranceId ?: "custom_${System.currentTimeMillis()}"
        speak(message, id)
    }

    /**
     * تشغيل الكلام
     */
    private fun speak(text: String, utteranceId: String) {
        textToSpeech?.let { tts ->
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            
            tts.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
            Log.d(TAG, "Speaking: $text")
        }
    }

    /**
     * تخصيص الرسالة بناءً على التفاعل
     */
    private fun personalizeMessage(message: String, interaction: SocialMediaInteraction): String {
        val platformName = getPlatformName(interaction.platform)
        val count = interaction.count
        
        return message
            .replace("{platform}", platformName)
            .replace("{count}", count.toString())
            .replace("{count_formatted}", formatNumber(count))
    }

    /**
     * بناء محتوى التقرير اليومي
     */
    private fun buildDailyReportContent(report: DailyReport): String {
        val content = StringBuilder()
        
        content.append("إجمالي التفاعلات اليوم: ${report.totalInteractions}. ")
        
        if (report.goalsCompleted.isNotEmpty()) {
            content.append("أنجزت ${report.goalsCompleted.size} هدف جديد. ")
        }
        
        if (report.topPerformingPlatform != null) {
            val platformName = getPlatformName(report.topPerformingPlatform)
            content.append("أفضل أداء كان على $platformName. ")
        }
        
        if (report.newUnlocks.isNotEmpty()) {
            content.append("فتحت ${report.newUnlocks.size} ميزة جديدة. ")
        }
        
        return content.toString()
    }

    /**
     * الحصول على اسم المنصة بالعربية
     */
    private fun getPlatformName(platform: SocialMediaPlatform): String {
        return when (platform) {
            SocialMediaPlatform.FACEBOOK -> "فيسبوك"
            SocialMediaPlatform.INSTAGRAM -> "إنستغرام"
            SocialMediaPlatform.TWITTER -> "تويتر"
            SocialMediaPlatform.TIKTOK -> "تيك توك"
            SocialMediaPlatform.YOUTUBE -> "يوتيوب"
            SocialMediaPlatform.SNAPCHAT -> "سناب شات"
            SocialMediaPlatform.LINKEDIN -> "لينكد إن"
            SocialMediaPlatform.WHATSAPP -> "واتساب"
            SocialMediaPlatform.TELEGRAM -> "تيليغرام"
            else -> "وسائل التواصل الاجتماعي"
        }
    }

    /**
     * تنسيق الأرقام
     */
    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000000 -> "${number / 1000000} مليون"
            number >= 1000 -> "${number / 1000} ألف"
            else -> number.toString()
        }
    }

    /**
     * إيقاف الكلام الحالي
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
        currentUtteranceId = null
    }

    /**
     * فحص إذا كان الكلام نشطاً
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true
    }

    /**
     * تحديث إعدادات الصوت
     */
    fun updateVoiceSettings(settings: VoiceSettings) {
        voiceSettings = settings
        preferencesHelper.saveVoiceSettings(settings)
        
        textToSpeech?.let { tts ->
            tts.setSpeechRate(settings.speechRate)
            tts.setPitch(settings.pitch)
        }
    }

    /**
     * الحصول على إعدادات الصوت الحالية
     */
    fun getVoiceSettings(): VoiceSettings {
        return voiceSettings
    }

    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        scope.cancel()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    /**
     * فئة إعدادات الصوت
     */
    data class VoiceSettings(
        val isEnabled: Boolean = true,
        val speechRate: Float = 1.0f,
        val pitch: Float = 1.0f,
        val volume: Float = 1.0f,
        val enableCelebrations: Boolean = true,
        val enableDailyReports: Boolean = true,
        val enableEncouragement: Boolean = true,
        val voiceGender: VoiceGender = VoiceGender.FEMALE,
        val personalityType: PersonalityType = PersonalityType.CHEERFUL
    )

    /**
     * تعداد جنس الصوت
     */
    enum class VoiceGender {
        MALE, FEMALE, NEUTRAL
    }

    /**
     * تعداد نوع الشخصية
     */
    enum class PersonalityType {
        CHEERFUL,    // مرحة
        CALM,        // هادئة
        ENERGETIC,   // نشيطة
        SUPPORTIVE,  // داعمة
        PROFESSIONAL // مهنية
    }
}



    /**
     * بدء الاستماع لكلمة التفعيل
     */
    fun startListeningForWakeWord() {
        if (!isListening && preferencesHelper.isWakeWordEnabled() && speechRecognizer != null) {
            val wakeWord = preferencesHelper.getWakeWord()
            if (wakeWord.isNotBlank()) {
                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ar"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(recognizerIntent)
                isListening = true
                Log.d(TAG, "Started listening for wake word: $wakeWord")
            } else {
                Log.w(TAG, "Wake word is not set. Not starting listener.")
            }
        } else if (isListening) {
            Log.d(TAG, "Already listening for wake word.")
        } else {
            Log.d(TAG, "Wake word feature is disabled or speech recognizer is null.")
        }
    }

    /**
     * إيقاف الاستماع لكلمة التفعيل
     */
    fun stopListeningForWakeWord() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d(TAG, "Stopped listening for wake word.")
        }
    }

    /**
     * معالجة النص المتعّرف عليه للتحقق من كلمة التفعيل
     */
    private fun handleRecognizedText(recognizedText: String) {
        val wakeWord = preferencesHelper.getWakeWord().trim().lowercase(Locale("ar"))
        val textLower = recognizedText.trim().lowercase(Locale("ar"))

        if (wakeWord.isNotBlank() && textLower.contains(wakeWord)) {
            Log.d(TAG, "Wake word '$wakeWord' detected in '$recognizedText'")
            // هنا يمكن تفعيل التفاعل الصوتي أو تشغيل الذكاء الاصطناعي
            // For now, let's just speak a confirmation message
            speakCustomMessage("نعم؟ كيف يمكنني المساعدة؟", "wake_word_response")
            // Optionally, stop listening temporarily to avoid re-triggering
            stopListeningForWakeWord()
            // Then, you might want to start a more general speech recognition session
            // or wait for a specific command.
        } else {
            Log.d(TAG, "Wake word not detected in '$recognizedText'")
        }
    }

    /**
     * إيقاف جميع عمليات TTS و SpeechRecognizer عند تدمير الخدمة
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
        scope.cancel()
        Log.d(TAG, "AdvancedVoiceInteractionService shutdown.")
    }





    /**
     * تحديث الموقع وتعلم اللهجة
     */
    private fun updateLocationAndLearnDialect() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation != null) {
            preferencesHelper.saveLastKnownLocation(lastKnownLocation.latitude, lastKnownLocation.longitude)
            learnDialectFromLocation(lastKnownLocation.latitude, lastKnownLocation.longitude)
        }
    }

    /**
     * تعلم اللهجة من الموقع
     */
    private fun learnDialectFromLocation(latitude: Double, longitude: Double) {
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val countryCode = addresses[0].countryCode
                val learnedDialects = preferencesHelper.getLearnedDialects().toMutableSet()
                if (learnedDialects.add(countryCode)) {
                    preferencesHelper.saveLearnedDialects(learnedDialects)
                    Log.d(TAG, "Learned new dialect: $countryCode")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error learning dialect from location", e)
        }
    }


