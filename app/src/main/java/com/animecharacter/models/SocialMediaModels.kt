package com.animecharacter.models

/**
 * تعداد منصات التواصل الاجتماعي المدعومة
 */
enum class SocialMediaPlatform {
    FACEBOOK,
    INSTAGRAM,
    TWITTER,
    TIKTOK,
    SNAPCHAT,
    LINKEDIN,
    YOUTUBE,
    WHATSAPP,
    TELEGRAM,
    UNKNOWN
}

/**
 * نموذج بيانات التفاعل مع وسائل التواصل الاجتماعي
 */
data class SocialMediaInteraction(
    val type: Type,
    val count: Int,
    val platform: SocialMediaPlatform,
    val timestamp: Long,
    val content: String,
    val celebrationLevel: Int = 0
) {
    /**
     * أنواع التفاعل المختلفة
     */
    enum class Type {
        NEW_FOLLOWERS,    // متابعين جدد
        NEW_LIKES,        // إعجابات جديدة
        NEW_COMMENTS,     // تعليقات جديدة
        NEW_SHARES,       // مشاركات جديدة
        NEW_VIEWS,        // مشاهدات جديدة
        CELEBRITY_INTERACTION, // تفاعل من شخصية مشهورة
        TRENDING_MENTION,      // ذكر في ترند
        MILESTONE_REACHED      // وصول لهدف معين
    }
}

/**
 * نموذج بيانات الإنجازات اليومية
 */
data class DailyAchievement(
    val date: String, // تاريخ بصيغة YYYY-MM-DD
    val platform: SocialMediaPlatform,
    val totalFollowers: Int = 0,
    val newFollowers: Int = 0,
    val totalLikes: Int = 0,
    val newLikes: Int = 0,
    val totalComments: Int = 0,
    val newComments: Int = 0,
    val totalShares: Int = 0,
    val newShares: Int = 0,
    val totalViews: Int = 0,
    val newViews: Int = 0,
    val goalsReached: List<String> = emptyList(),
    val celebrationTriggered: Boolean = false
)

/**
 * نموذج بيانات الأهداف
 */
data class SocialMediaGoal(
    val id: String,
    val title: String,
    val description: String,
    val platform: SocialMediaPlatform,
    val type: SocialMediaInteraction.Type,
    val targetValue: Int,
    val currentValue: Int = 0,
    val deadline: Long? = null, // timestamp
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val rewardAnimation: String? = null,
    val rewardMessage: String? = null
)

/**
 * نموذج بيانات الشخصيات المشهورة أو المتابعين المهمين
 */
data class ImportantFollower(
    val id: String,
    val name: String,
    val username: String? = null,
    val platform: SocialMediaPlatform,
    val isVerified: Boolean = false,
    val isCelebrity: Boolean = false,
    val followerCount: Int? = null,
    val profileImageUrl: String? = null,
    val specialMessage: String? = null // رسالة خاصة عند التفاعل
)

/**
 * نموذج بيانات الترندات
 */
data class TrendingTopic(
    val id: String,
    val title: String,
    val hashtag: String? = null,
    val platform: SocialMediaPlatform,
    val popularity: Int, // مستوى الشعبية من 1-10
    val detectedAt: Long,
    val relatedToUser: Boolean = false, // هل الترند متعلق بالمستخدم
    val suggestedAction: String? = null // اقتراح للمستخدم
)

/**
 * نموذج بيانات المؤثرات البصرية
 */
data class VisualEffect(
    val type: String, // نوع المؤثر (hearts, stars, sparkles, etc.)
    val duration: Long = 3000, // مدة المؤثر بالميلي ثانية
    val intensity: Int = 1, // شدة المؤثر (1-3)
    val color: String? = null, // لون المؤثر
    val position: Position = Position.AROUND_CHARACTER
) {
    enum class Position {
        AROUND_CHARACTER,
        ABOVE_CHARACTER,
        BELOW_CHARACTER,
        FULL_SCREEN
    }
}

/**
 * نموذج بيانات الرسوم المتحركة الخاصة
 */
data class SpecialAnimation(
    val name: String,
    val filePath: String,
    val duration: Long,
    val triggerCondition: TriggerCondition,
    val unlockRequirement: UnlockRequirement? = null
) {
    data class TriggerCondition(
        val interactionType: SocialMediaInteraction.Type,
        val minimumCount: Int,
        val platform: SocialMediaPlatform? = null
    )
    
    data class UnlockRequirement(
        val totalInteractions: Int,
        val specificAchievements: List<String> = emptyList()
    )
}

/**
 * نموذج بيانات التقرير اليومي
 */
data class DailyReport(
    val date: String,
    val totalInteractions: Int,
    val platformBreakdown: Map<SocialMediaPlatform, DailyAchievement>,
    val goalsCompleted: List<SocialMediaGoal>,
    val newUnlocks: List<String>, // رسوم متحركة أو مؤثرات جديدة
    val topPerformingPlatform: SocialMediaPlatform?,
    val encouragementMessage: String,
    val suggestionsForTomorrow: List<String>
)

/**
 * نموذج بيانات إعدادات مراقبة وسائل التواصل الاجتماعي
 */
data class SocialMediaSettings(
    val isEnabled: Boolean = true,
    val monitoredPlatforms: Set<SocialMediaPlatform> = setOf(),
    val celebrationSettings: CelebrationSettings = CelebrationSettings(),
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val privacySettings: PrivacySettings = PrivacySettings()
) {
    data class CelebrationSettings(
        val enableVisualEffects: Boolean = true,
        val enableAudioCelebration: Boolean = true,
        val celebrationIntensity: Int = 2, // 1-3
        val minimumThresholds: Map<SocialMediaInteraction.Type, Int> = mapOf(
            SocialMediaInteraction.Type.NEW_FOLLOWERS to 1,
            SocialMediaInteraction.Type.NEW_LIKES to 10,
            SocialMediaInteraction.Type.NEW_COMMENTS to 1,
            SocialMediaInteraction.Type.NEW_SHARES to 1,
            SocialMediaInteraction.Type.NEW_VIEWS to 100
        )
    )
    
    data class NotificationSettings(
        val enableDailyReport: Boolean = true,
        val dailyReportTime: String = "20:00", // HH:mm
        val enableInstantCelebrations: Boolean = true,
        val enableGoalReminders: Boolean = true
    )
    
    data class PrivacySettings(
        val storeDataLocally: Boolean = true,
        val autoDeleteAfterDays: Int = 30,
        val shareAnonymousStats: Boolean = false
    )
}



/**
 * نموذج بيانات استجابة SearchApi لـ Google Trends Trending Now
 */
data class TrendingTopicResponse(
    val search_metadata: SearchMetadata,
    val search_parameters: SearchParameters,
    val trends: List<Trend>
) {
    data class SearchMetadata(
        val id: String,
        val status: String,
        val created_at: String,
        val request_time_taken: Double,
        val parsing_time_taken: Double,
        val total_time_taken: Double,
        val request_url: String,
        val html_url: String,
        val json_url: String
    )

    data class SearchParameters(
        val engine: String,
        val geo: String,
        val time: String,
        val hl: String
    )

    data class Trend(
        val position: Int,
        val query: String,
        val search_volume: Int,
        val percentage_increase: Int,
        val location: String,
        val categories: List<String>,
        val start_date: String,
        val is_active: Boolean,
        val keywords: List<String>,
        val news_token: String?
    )
}


