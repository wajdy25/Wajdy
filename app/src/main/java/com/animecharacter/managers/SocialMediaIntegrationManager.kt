package com.animecharacter.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.animecharacter.models.*
import com.animecharacter.services.*
import com.animecharacter.utils.PreferencesHelper
import kotlinx.coroutines.*

/**
 * مدير التكامل الرئيسي لميزات التفاعل مع وسائل التواصل الاجتماعي
 * يربط جميع الخدمات والمدراء معاً لتوفير تجربة متكاملة
 */
class SocialMediaIntegrationManager(private val context: Context) {

    companion object {
        private const val TAG = "SocialMediaIntegrationManager"
    }

    // الخدمات والمدراء
    private val preferencesHelper = PreferencesHelper(context)
    private val achievementManager = SocialMediaAchievementManager(context)
    private val visualEffectsService = VisualEffectsService(context)
    private val advancedAnimationService = AdvancedCharacterAnimationService(context)
    private val voiceInteractionService = AdvancedVoiceInteractionService(context)
    private val trendingTopicsService = TrendingTopicsService(context)    
    // إعدادات التكامل
    private var integrationSettings = SocialMediaSettings()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // مستقبل البث للأحداث
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { handleBroadcastIntent(it) }
        }
    }

    init {
        loadSettings()
        registerBroadcastReceivers()
        initializeServices()
    }

    /**
     * تحميل الإعدادات
     */
    private fun loadSettings() {
        integrationSettings = preferencesHelper.getSocialMediaSettings() ?: SocialMediaSettings()
        Log.d(TAG, "Loaded integration settings")
    }

    /**
     * تسجيل مستقبلات البث
     */
    private fun registerBroadcastReceivers() {
        val intentFilter = IntentFilter().apply {
            addAction("com.animecharacter.SOCIAL_MEDIA_INTERACTION")
            addAction("com.animecharacter.SOCIAL_MEDIA_SERVICE_STATUS")
            addAction("com.animecharacter.GOAL_COMPLETED")
            addAction("com.animecharacter.ANIMATION_EVENT")
            addAction("com.animecharacter.ANIMATION_UNLOCKED")
        }
        
        context.registerReceiver(broadcastReceiver, intentFilter)
        Log.d(TAG, "Broadcast receivers registered")
    }

    /**
     * تهيئة الخدمات
     */
    private fun initializeServices() {
        // تعيين العرض الأساسي للمؤثرات البصرية (سيتم تعيينه من النشاط الرئيسي)
        // visualEffectsService.setParentView(parentView)
        
        Log.d(TAG, "Services initialized")
    }

    /**
     * معالجة أحداث البث
     */
    private fun handleBroadcastIntent(intent: Intent) {
        when (intent.action) {
            "com.animecharacter.SOCIAL_MEDIA_INTERACTION" -> {
                handleSocialMediaInteraction(intent)
            }
            "com.animecharacter.SOCIAL_MEDIA_SERVICE_STATUS" -> {
                handleServiceStatusChange(intent)
            }
            "com.animecharacter.GOAL_COMPLETED" -> {
                handleGoalCompletion(intent)
            }
            "com.animecharacter.ANIMATION_EVENT" -> {
                handleAnimationEvent(intent)
            }
            "com.animecharacter.ANIMATION_UNLOCKED" -> {
                handleAnimationUnlock(intent)
            }
        }
    }

    /**
     * معالجة تفاعل وسائل التواصل الاجتماعي
     */
    private fun handleSocialMediaInteraction(intent: Intent) {
        if (!integrationSettings.isEnabled) return
        
        val interactionType = intent.getStringExtra("interaction_type")?.let {
            SocialMediaInteraction.Type.valueOf(it)
        } ?: return
        
        val count = intent.getIntExtra("interaction_count", 0)
        val platform = intent.getStringExtra("platform")?.let {
            SocialMediaPlatform.valueOf(it)
        } ?: SocialMediaPlatform.UNKNOWN
        
        val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
        
        val interaction = SocialMediaInteraction(
            type = interactionType,
            count = count,
            platform = platform,
            timestamp = timestamp,
            content = ""
        )
        
        Log.d(TAG, "Processing social media interaction: $interaction")
        
        scope.launch {
            processInteraction(interaction)
        }
    }

    /**
     * معالجة التفاعل
     */
    private suspend fun processInteraction(interaction: SocialMediaInteraction) {
        try {
            // معالجة التفاعل في مدير الإنجازات
            achievementManager.processNewInteraction(interaction)
            
            // تحديد مستوى الاحتفال
            val celebrationLevel = determineCelebrationLevel(interaction)
            
            if (celebrationLevel > 0) {
                // تشغيل الاحتفالات المختلفة بالتوازي
                val jobs = mutableListOf<Job>()
                
                // الاحتفال البصري
                if (integrationSettings.celebrationSettings.enableVisualEffects) {
                    jobs.add(launch {
                        triggerVisualCelebration(interaction, celebrationLevel)
                    })
                }
                
                // الاحتفال الصوتي
                if (integrationSettings.celebrationSettings.enableAudioCelebration) {
                    jobs.add(launch {
                        triggerAudioCelebration(interaction, celebrationLevel)
                    })
                }
                
                // الرسوم المتحركة
                jobs.add(launch {
                    triggerAnimationCelebration(interaction, celebrationLevel)
                })
                
                // انتظار انتهاء جميع الاحتفالات
                jobs.joinAll()
            }
            
            // تحديث الإحصائيات
            updateStatistics(interaction)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing interaction", e)
        }
    }

    /**
     * تحديد مستوى الاحتفال
     */
    private fun determineCelebrationLevel(interaction: SocialMediaInteraction): Int {
        val thresholds = integrationSettings.celebrationSettings.minimumThresholds
        val threshold = thresholds[interaction.type] ?: 1
        
        return when {
            interaction.count >= threshold * 100 -> 3 // احتفال كبير
            interaction.count >= threshold * 10 -> 2  // احتفال متوسط
            interaction.count >= threshold -> 1       // احتفال صغير
            else -> 0 // لا احتفال
        }
    }

    /**
     * تشغيل الاحتفال البصري
     */
    private suspend fun triggerVisualCelebration(interaction: SocialMediaInteraction, level: Int) {
        try {
            visualEffectsService.triggerInteractionEffect(interaction, level)
            Log.d(TAG, "Visual celebration triggered for ${interaction.type} level $level")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering visual celebration", e)
        }
    }

    /**
     * تشغيل الاحتفال الصوتي
     */
    private suspend fun triggerAudioCelebration(interaction: SocialMediaInteraction, level: Int) {
        try {
            voiceInteractionService.speakCelebrationMessage(interaction, level)
            Log.d(TAG, "Audio celebration triggered for ${interaction.type} level $level")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering audio celebration", e)
        }
    }

    /**
     * تشغيل احتفال الرسوم المتحركة
     */
    private suspend fun triggerAnimationCelebration(interaction: SocialMediaInteraction, level: Int) {
        try {
            advancedAnimationService.playAnimationForInteraction(interaction, level)
            Log.d(TAG, "Animation celebration triggered for ${interaction.type} level $level")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering animation celebration", e)
        }
    }

    /**
     * معالجة تغيير حالة الخدمة
     */
    private fun handleServiceStatusChange(intent: Intent) {
        val connected = intent.getBooleanExtra("connected", false)
        Log.d(TAG, "Social media service status changed: connected=$connected")
        
        if (connected) {
            // الخدمة متصلة
            scope.launch {
                voiceInteractionService.speakCustomMessage(
                    "تم تفعيل مراقبة وسائل التواصل الاجتماعي! سأحتفل معك بكل إنجاز جديد!"
                )
            }
        } else {
            // الخدمة منقطعة
            Log.w(TAG, "Social media monitoring service disconnected")
        }
    }

    /**
     * معالجة إنجاز الأهداف
     */
    private fun handleGoalCompletion(intent: Intent) {
        val goalId = intent.getStringExtra("goal_id") ?: return
        val goalTitle = intent.getStringExtra("goal_title") ?: return
        
        Log.d(TAG, "Goal completed: $goalTitle")
        
        scope.launch {
            // تشغيل احتفال خاص بإنجاز الأهداف
            val jobs = mutableListOf<Job>()
            
            // احتفال صوتي
            jobs.add(launch {
                val goal = SocialMediaGoal(
                    id = goalId,
                    title = goalTitle,
                    description = "",
                    platform = SocialMediaPlatform.UNKNOWN,
                    type = SocialMediaInteraction.Type.MILESTONE_REACHED,
                    targetValue = 0
                )
                voiceInteractionService.speakGoalCompletion(goal)
            })
            
            // احتفال بصري
            jobs.add(launch {
                val goalInteraction = SocialMediaInteraction(
                    type = SocialMediaInteraction.Type.MILESTONE_REACHED,
                    count = 1,
                    platform = SocialMediaPlatform.UNKNOWN,
                    timestamp = System.currentTimeMillis(),
                    content = "Goal completed: $goalTitle"
                )
                visualEffectsService.triggerInteractionEffect(goalInteraction, 3)
            })
            
            // رسوم متحركة خاصة
            jobs.add(launch {
                advancedAnimationService.playSpecificAnimation("goal_completion")
            })
            
            jobs.joinAll()
        }
    }

    /**
     * معالجة أحداث الرسوم المتحركة
     */
    private fun handleAnimationEvent(intent: Intent) {
        val event = intent.getStringExtra("event") ?: return
        val animationName = intent.getStringExtra("animation_name") ?: return
        
        Log.d(TAG, "Animation event: $event for $animationName")
        
        when (event) {
            "lottie_start", "webm_start", "gif_start" -> {
                // بداية الرسوم المتحركة
            }
            "lottie_end", "webm_end", "gif_end" -> {
                // نهاية الرسوم المتحركة
            }
            "animation_stopped" -> {
                // إيقاف الرسوم المتحركة
            }
        }
    }

    /**
     * معالجة فتح رسوم متحركة جديدة
     */
    private fun handleAnimationUnlock(intent: Intent) {
        val animationName = intent.getStringExtra("animation_name") ?: return
        
        Log.d(TAG, "New animation unlocked: $animationName")
        
        scope.launch {
            // إشعار المستخدم بالفتح الجديد
            voiceInteractionService.speakCustomMessage(
                "مبروك! فتحت رسوم متحركة جديدة: $animationName!"
            )
            
            delay(2000)
            
            // تشغيل احتفال صغير
            val unlockInteraction = SocialMediaInteraction(
                type = SocialMediaInteraction.Type.MILESTONE_REACHED,
                count = 1,
                platform = SocialMediaPlatform.UNKNOWN,
                timestamp = System.currentTimeMillis(),
                content = "Animation unlocked: $animationName"
            )
            
            visualEffectsService.triggerInteractionEffect(unlockInteraction, 2)
        }
    }

    /**
     * تحديث الإحصائيات
     */
    private fun updateStatistics(interaction: SocialMediaInteraction) {
        // تحديث إحصائيات الاستخدام
        preferencesHelper.incrementInteractionCount(interaction.type)
        preferencesHelper.updateLastInteractionTime(System.currentTimeMillis())
    }

    /**
     * تشغيل التقرير اليومي
     */
    fun triggerDailyReport() {
        if (!integrationSettings.notificationSettings.enableDailyReport) return
        
        scope.launch {
            try {
                val report = achievementManager.generateDailyReport()
                voiceInteractionService.speakDailyReport(report)
                
                Log.d(TAG, "Daily report triggered")
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering daily report", e)
            }
        }
    }

    /**
     * تشغيل رسالة تشجيع
     */
    fun triggerEncouragementMessage() {
        if (!integrationSettings.celebrationSettings.enableAudioCelebration) return
        
        scope.launch {
            voiceInteractionService.speakEncouragementMessage()
        }
    }

    /**
     * إنشاء هدف جديد
     */
    fun createGoal(
        title: String,
        description: String,
        platform: SocialMediaPlatform,
        type: SocialMediaInteraction.Type,
        targetValue: Int,
        deadline: Long? = null
    ): SocialMediaGoal {
        return achievementManager.createNewGoal(title, description, platform, type, targetValue, deadline)
    }

    /**
     * إضافة متابع مهم
     */
    fun addImportantFollower(
        name: String,
        username: String?,
        platform: SocialMediaPlatform,
        isCelebrity: Boolean = false,
        specialMessage: String? = null
    ): ImportantFollower {
        return achievementManager.addImportantFollower(name, username, platform, isCelebrity, specialMessage)
    }

    /**
     * تحديث الإعدادات
     */
    fun updateSettings(settings: SocialMediaSettings) {
        integrationSettings = settings
        preferencesHelper.saveSocialMediaSettings(settings)
        
        // تحديث إعدادات الصوت
        val voiceSettings = AdvancedVoiceInteractionService.VoiceSettings(
            isEnabled = settings.celebrationSettings.enableAudioCelebration,
            enableCelebrations = settings.celebrationSettings.enableAudioCelebration,
            enableDailyReports = settings.notificationSettings.enableDailyReport
        )
        voiceInteractionService.updateVoiceSettings(voiceSettings)
        
        Log.d(TAG, "Settings updated")
    }

    /**
     * الحصول على الإعدادات الحالية
     */
    fun getSettings(): SocialMediaSettings {
        return integrationSettings
    }

    /**
     * الحصول على الرسوم المتحركة المفتوحة
     */
    fun getUnlockedAnimations(): List<SpecialAnimation> {
        return advancedAnimationService.getUnlockedAnimations()
    }

    /**
     * الحصول على الرسوم المتحركة المقفلة
     */
    fun getLockedAnimations(): List<SpecialAnimation> {
        return advancedAnimationService.getLockedAnimations()
    }

    /**
     * تشغيل رسوم متحركة محددة
     */
    fun playAnimation(animationName: String) {
        advancedAnimationService.playSpecificAnimation(animationName)
    }

    /**
     * إيقاف جميع الاحتفالات
     */
    fun stopAllCelebrations() {
        advancedAnimationService.stopCurrentAnimation()
        visualEffectsService.stopAllEffects()
        voiceInteractionService.stopSpeaking()
    }

    /**
     * فحص حالة الخدمات
     */
    fun getServicesStatus(): Map<String, Boolean> {
        return mapOf(
            "voice_service" to !voiceInteractionService.isSpeaking(),
            "animation_service" to !advancedAnimationService.getCurrentAnimationInfo().second,
            "integration_enabled" to integrationSettings.isEnabled
        )
    }

    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering broadcast receiver", e)
        }
        
        scope.cancel()
        advancedAnimationService.cleanup()
        voiceInteractionService.cleanup()
        visualEffectsService.stopAllEffects()
        
        Log.d(TAG, "Cleanup completed")
    }
}



    /**
     * جلب المواضيع الرائجة
     */
    fun fetchTrendingTopics(geo: String = "US", time: String = "past_24_hours") {
        trendingTopicsService.getTrendingTopics(geo, time) {
            if (it != null) {
                Log.d(TAG, "Fetched ${it.size} trending topics.")
                // TODO: Add logic to process trending topics and make character interact
            } else {
                Log.e(TAG, "Failed to fetch trending topics.")
            }
        }
    }




    /**
     * معالجة المواضيع الرائجة وجعل الشخصية تتفاعل معها
     */
    private suspend fun processTrendingTopics(trends: List<TrendingTopicResponse.Trend>) {
        if (trends.isEmpty()) {
            Log.d(TAG, "No trending topics to process.")
            return
        }

        // اختيار ترند عشوائي للتفاعل معه
        val randomTrend = trends.random()
        val message = "مرحباً! هل تعلم أن \"${randomTrend.query}\" هو ترند اليوم؟ ما رأيك في ذلك؟"

        voiceInteractionService.speak(message)
        // يمكن إضافة رسوم متحركة أو مؤثرات بصرية هنا بناءً على الترند
        // advancedAnimationService.playSpecificAnimation("curiosity_animation")
        Log.d(TAG, "Character interacted with trending topic: ${randomTrend.query}")
    }




    /**
     * إعداد جدولة لجلب المواضيع الرائجة بشكل دوري
     */
    private fun setupTrendingTopicsScheduler() {
        // يمكن استخدام WorkManager لجدولة المهام الدورية
        // مثال: جلب الترندات كل ساعة
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                fetchTrendingTopics("US", "past_24_hours") // يمكن جعل geo و time قابلين للتخصيص
                delay(3600 * 1000) // جلب كل ساعة (3600 ثانية * 1000 ميلي ثانية)
            }
        }
        Log.d(TAG, "Trending topics scheduler setup completed")
    }


