package com.animecharacter.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.animecharacter.models.SocialMediaInteraction
import com.animecharacter.models.SocialMediaPlatform
import com.animecharacter.utils.PreferencesHelper
import java.util.regex.Pattern

/**
 * خدمة مراقبة إشعارات تطبيقات التواصل الاجتماعي
 * تستمع للإشعارات من تطبيقات مختلفة وتحلل محتواها لتحديد نوع التفاعل
 */
class SocialMediaNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "SocialMediaNotificationService"
        
        // قائمة تطبيقات التواصل الاجتماعي المدعومة
        private val SUPPORTED_PACKAGES = mapOf(
            "com.facebook.katana" to SocialMediaPlatform.FACEBOOK,
            "com.instagram.android" to SocialMediaPlatform.INSTAGRAM,
            "com.twitter.android" to SocialMediaPlatform.TWITTER,
            "com.zhiliaoapp.musically" to SocialMediaPlatform.TIKTOK,
            "com.snapchat.android" to SocialMediaPlatform.SNAPCHAT,
            "com.linkedin.android" to SocialMediaPlatform.LINKEDIN,
            "com.youtube.android" to SocialMediaPlatform.YOUTUBE,
            "com.whatsapp" to SocialMediaPlatform.WHATSAPP,
            "com.telegram.messenger" to SocialMediaPlatform.TELEGRAM
        )
        
        // أنماط التعرف على أنواع التفاعل المختلفة
        private val FOLLOWER_PATTERNS = listOf(
            Pattern.compile("(\\d+)\\s*(new\\s+)?(followers?|متابع|متابعين)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("followed\\s+you|تابعك|بدأ في متابعتك", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*people\\s+followed\\s+you", Pattern.CASE_INSENSITIVE)
        )
        
        private val LIKE_PATTERNS = listOf(
            Pattern.compile("(\\d+)\\s*(new\\s+)?(likes?|إعجاب|إعجابات)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("liked\\s+your|أعجب بـ|أعجبه منشورك", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*people\\s+liked", Pattern.CASE_INSENSITIVE)
        )
        
        private val COMMENT_PATTERNS = listOf(
            Pattern.compile("(\\d+)\\s*(new\\s+)?(comments?|تعليق|تعليقات)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("commented\\s+on|علق على|كتب تعليقاً", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*people\\s+commented", Pattern.CASE_INSENSITIVE)
        )
        
        private val SHARE_PATTERNS = listOf(
            Pattern.compile("(\\d+)\\s*(new\\s+)?(shares?|مشاركة|مشاركات)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("shared\\s+your|شارك منشورك|أعاد نشر", Pattern.CASE_INSENSITIVE)
        )
        
        private val VIEW_PATTERNS = listOf(
            Pattern.compile("(\\d+)\\s*(new\\s+)?(views?|مشاهدة|مشاهدات)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("people\\s+viewed|شاهد منشورك", Pattern.CASE_INSENSITIVE)
        )
    }

    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var characterAnimationService: CharacterAnimationService
    private lateinit var voiceService: VoiceService

    override fun onCreate() {
        super.onCreate()
        preferencesHelper = PreferencesHelper(this)
        characterAnimationService = CharacterAnimationService(this)
        voiceService = VoiceService(this)
        Log.d(TAG, "SocialMediaNotificationService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        
        // إشعار المستخدم أن الخدمة متصلة
        if (preferencesHelper.isSocialMediaMonitoringEnabled()) {
            broadcastServiceStatus(true)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
        broadcastServiceStatus(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        // التحقق من تمكين مراقبة وسائل التواصل الاجتماعي
        if (!preferencesHelper.isSocialMediaMonitoringEnabled()) {
            return
        }
        
        val packageName = sbn.packageName
        val platform = SUPPORTED_PACKAGES[packageName]
        
        if (platform != null) {
            Log.d(TAG, "Received notification from $platform: $packageName")
            analyzeNotification(sbn, platform)
        }
    }

    /**
     * تحليل محتوى الإشعار لتحديد نوع التفاعل
     */
    private fun analyzeNotification(sbn: StatusBarNotification, platform: SocialMediaPlatform) {
        val notification = sbn.notification
        val extras = notification.extras
        
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
        
        val fullContent = "$title $bigText".trim()
        
        Log.d(TAG, "Analyzing notification content: $fullContent")
        
        // تحليل نوع التفاعل
        val interaction = determineInteractionType(fullContent, platform)
        
        if (interaction != null) {
            Log.d(TAG, "Detected interaction: ${interaction.type} with count: ${interaction.count}")
            handleSocialMediaInteraction(interaction)
        }
    }

    /**
     * تحديد نوع التفاعل من محتوى الإشعار
     */
    private fun determineInteractionType(content: String, platform: SocialMediaPlatform): SocialMediaInteraction? {
        // البحث عن متابعين جدد
        for (pattern in FOLLOWER_PATTERNS) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val count = extractNumber(matcher) ?: 1
                return SocialMediaInteraction(
                    type = SocialMediaInteraction.Type.NEW_FOLLOWERS,
                    count = count,
                    platform = platform,
                    timestamp = System.currentTimeMillis(),
                    content = content
                )
            }
        }
        
        // البحث عن إعجابات جديدة
        for (pattern in LIKE_PATTERNS) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val count = extractNumber(matcher) ?: 1
                return SocialMediaInteraction(
                    type = SocialMediaInteraction.Type.NEW_LIKES,
                    count = count,
                    platform = platform,
                    timestamp = System.currentTimeMillis(),
                    content = content
                )
            }
        }
        
        // البحث عن تعليقات جديدة
        for (pattern in COMMENT_PATTERNS) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val count = extractNumber(matcher) ?: 1
                return SocialMediaInteraction(
                    type = SocialMediaInteraction.Type.NEW_COMMENTS,
                    count = count,
                    platform = platform,
                    timestamp = System.currentTimeMillis(),
                    content = content
                )
            }
        }
        
        // البحث عن مشاركات جديدة
        for (pattern in SHARE_PATTERNS) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val count = extractNumber(matcher) ?: 1
                return SocialMediaInteraction(
                    type = SocialMediaInteraction.Type.NEW_SHARES,
                    count = count,
                    platform = platform,
                    timestamp = System.currentTimeMillis(),
                    content = content
                )
            }
        }
        
        // البحث عن مشاهدات جديدة
        for (pattern in VIEW_PATTERNS) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val count = extractNumber(matcher) ?: 1
                return SocialMediaInteraction(
                    type = SocialMediaInteraction.Type.NEW_VIEWS,
                    count = count,
                    platform = platform,
                    timestamp = System.currentTimeMillis(),
                    content = content
                )
            }
        }
        
        return null
    }

    /**
     * استخراج الرقم من النتيجة المطابقة
     */
    private fun extractNumber(matcher: java.util.regex.Matcher): Int? {
        return try {
            for (i in 1..matcher.groupCount()) {
                val group = matcher.group(i)
                if (group != null && group.matches(Regex("\\d+"))) {
                    return group.toInt()
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * التعامل مع التفاعل المكتشف
     */
    private fun handleSocialMediaInteraction(interaction: SocialMediaInteraction) {
        // حفظ التفاعل في قاعدة البيانات المحلية
        saveSocialMediaInteraction(interaction)
        
        // تحديد مستوى الاحتفال بناءً على نوع وحجم التفاعل
        val celebrationLevel = determineCelebrationLevel(interaction)
        
        if (celebrationLevel > 0) {
            // تشغيل الاحتفال البصري
            triggerVisualCelebration(interaction, celebrationLevel)
            
            // تشغيل الاحتفال الصوتي
            triggerAudioCelebration(interaction, celebrationLevel)
            
            // إرسال إشعار للتطبيق الرئيسي
            broadcastSocialMediaInteraction(interaction)
        }
    }

    /**
     * تحديد مستوى الاحتفال بناءً على نوع وحجم التفاعل
     */
    private fun determineCelebrationLevel(interaction: SocialMediaInteraction): Int {
        return when (interaction.type) {
            SocialMediaInteraction.Type.NEW_FOLLOWERS -> {
                when {
                    interaction.count >= 100 -> 3 // احتفال كبير
                    interaction.count >= 10 -> 2  // احتفال متوسط
                    interaction.count >= 1 -> 1   // احتفال صغير
                    else -> 0
                }
            }
            SocialMediaInteraction.Type.NEW_LIKES -> {
                when {
                    interaction.count >= 1000 -> 3
                    interaction.count >= 100 -> 2
                    interaction.count >= 10 -> 1
                    else -> 0
                }
            }
            SocialMediaInteraction.Type.NEW_COMMENTS -> {
                when {
                    interaction.count >= 50 -> 3
                    interaction.count >= 10 -> 2
                    interaction.count >= 1 -> 1
                    else -> 0
                }
            }
            SocialMediaInteraction.Type.NEW_SHARES -> {
                when {
                    interaction.count >= 100 -> 3
                    interaction.count >= 10 -> 2
                    interaction.count >= 1 -> 1
                    else -> 0
                }
            }
            SocialMediaInteraction.Type.NEW_VIEWS -> {
                when {
                    interaction.count >= 10000 -> 3
                    interaction.count >= 1000 -> 2
                    interaction.count >= 100 -> 1
                    else -> 0
                }
            }
        }
    }

    /**
     * تشغيل الاحتفال البصري
     */
    private fun triggerVisualCelebration(interaction: SocialMediaInteraction, level: Int) {
        val animationType = when (level) {
            3 -> "celebration_big"
            2 -> "celebration_medium"
            1 -> "celebration_small"
            else -> return
        }
        
        characterAnimationService.playSpecialAnimation(animationType)
        
        // إضافة مؤثرات بصرية حول الشخصية
        val effects = when (interaction.type) {
            SocialMediaInteraction.Type.NEW_FOLLOWERS -> listOf("hearts", "stars")
            SocialMediaInteraction.Type.NEW_LIKES -> listOf("hearts", "sparkles")
            SocialMediaInteraction.Type.NEW_COMMENTS -> listOf("speech_bubbles", "stars")
            SocialMediaInteraction.Type.NEW_SHARES -> listOf("arrows", "sparkles")
            SocialMediaInteraction.Type.NEW_VIEWS -> listOf("eyes", "neon_glow")
        }
        
        characterAnimationService.addVisualEffects(effects)
    }

    /**
     * تشغيل الاحتفال الصوتي
     */
    private fun triggerAudioCelebration(interaction: SocialMediaInteraction, level: Int) {
        val message = generateCelebrationMessage(interaction, level)
        voiceService.speak(message)
    }

    /**
     * توليد رسالة الاحتفال
     */
    private fun generateCelebrationMessage(interaction: SocialMediaInteraction, level: Int): String {
        val platformName = when (interaction.platform) {
            SocialMediaPlatform.FACEBOOK -> "فيسبوك"
            SocialMediaPlatform.INSTAGRAM -> "إنستغرام"
            SocialMediaPlatform.TWITTER -> "تويتر"
            SocialMediaPlatform.TIKTOK -> "تيك توك"
            SocialMediaPlatform.YOUTUBE -> "يوتيوب"
            else -> "وسائل التواصل الاجتماعي"
        }
        
        return when (interaction.type) {
            SocialMediaInteraction.Type.NEW_FOLLOWERS -> {
                when (level) {
                    3 -> "واو! ${interaction.count} متابع جديد على $platformName! أنت نجم بكل معنى الكلمة!"
                    2 -> "رائع! ${interaction.count} متابع جديد! جمهورك يكبر بسرعة!"
                    1 -> "عظيم! متابع جديد على $platformName! استمر في التألق!"
                    else -> ""
                }
            }
            SocialMediaInteraction.Type.NEW_LIKES -> {
                when (level) {
                    3 -> "تحطيم الأرقام القياسية! ${interaction.count} إعجاب جديد! أنت في القمة!"
                    2 -> "ممتاز! ${interaction.count} إعجاب جديد! المحتوى الخاص بك رائع!"
                    1 -> "جميل! إعجابات جديدة على منشورك!"
                    else -> ""
                }
            }
            SocialMediaInteraction.Type.NEW_COMMENTS -> {
                when (level) {
                    3 -> "الجميع يتحدث عنك! ${interaction.count} تعليق جديد!"
                    2 -> "تفاعل رائع! ${interaction.count} تعليق جديد على منشورك!"
                    1 -> "شخص ما علق على منشورك! تفاعل جميل!"
                    else -> ""
                }
            }
            SocialMediaInteraction.Type.NEW_SHARES -> {
                when (level) {
                    3 -> "منشورك ينتشر كالنار في الهشيم! ${interaction.count} مشاركة!"
                    2 -> "رائع! ${interaction.count} شخص شارك منشورك!"
                    1 -> "شخص ما شارك منشورك! انتشار جميل!"
                    else -> ""
                }
            }
            SocialMediaInteraction.Type.NEW_VIEWS -> {
                when (level) {
                    3 -> "فيديوك يحقق أرقاماً خيالية! ${interaction.count} مشاهدة!"
                    2 -> "ممتاز! ${interaction.count} مشاهدة جديدة!"
                    1 -> "مشاهدات جديدة على المحتوى الخاص بك!"
                    else -> ""
                }
            }
        }
    }

    /**
     * حفظ التفاعل في قاعدة البيانات المحلية
     */
    private fun saveSocialMediaInteraction(interaction: SocialMediaInteraction) {
        // سيتم تنفيذ هذا مع قاعدة البيانات المحلية
        // يمكن استخدام SharedPreferences مؤقتاً أو Room Database
        val key = "social_interaction_${System.currentTimeMillis()}"
        preferencesHelper.saveSocialMediaInteraction(key, interaction)
    }

    /**
     * إرسال إشعار للتطبيق الرئيسي
     */
    private fun broadcastSocialMediaInteraction(interaction: SocialMediaInteraction) {
        val intent = Intent("com.animecharacter.SOCIAL_MEDIA_INTERACTION")
        intent.putExtra("interaction_type", interaction.type.name)
        intent.putExtra("interaction_count", interaction.count)
        intent.putExtra("platform", interaction.platform.name)
        intent.putExtra("timestamp", interaction.timestamp)
        sendBroadcast(intent)
    }

    /**
     * إرسال حالة الخدمة
     */
    private fun broadcastServiceStatus(connected: Boolean) {
        val intent = Intent("com.animecharacter.SOCIAL_MEDIA_SERVICE_STATUS")
        intent.putExtra("connected", connected)
        sendBroadcast(intent)
    }
}

