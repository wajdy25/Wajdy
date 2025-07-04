package com.animecharacter.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import com.animecharacter.models.*
import com.animecharacter.services.CharacterAnimationService
import com.animecharacter.services.VoiceService
import com.animecharacter.services.AdvancedVoiceInteractionService
import com.animecharacter.utils.PreferencesHelper
import com.animecharacter.managers.RewardManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * مدير الإنجازات والأهداف لوسائل التواصل الاجتماعي
 * يتولى تتبع التقدم، إدارة الأهداف، وتشغيل الاحتفالات
 */
class SocialMediaAchievementManager(private val context: Context, private val advancedVoiceInteractionService: AdvancedVoiceInteractionService, private val rewardManager: RewardManager) {

    companion object {
        private const val TAG = "SocialMediaAchievementManager"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    private val preferencesHelper = PreferencesHelper(context)
    private val characterAnimationService = CharacterAnimationService(context)    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // قوائم الأهداف والإنجازات
    private val activeGoals = mutableListOf<SocialMediaGoal>()
    private val dailyAchievements = mutableMapOf<String, DailyAchievement>()
    private val importantFollowers = mutableListOf<ImportantFollower>()

    init {
        loadSavedData()
        setupDailyReportScheduler()
    }

    /**
     * تحميل البيانات المحفوظة
     */
    private fun loadSavedData() {
        // تحميل الأهداف النشطة
        activeGoals.addAll(preferencesHelper.getActiveGoals())
        
        // تحميل الإنجازات اليومية للأسبوع الماضي
        loadRecentDailyAchievements()
        
        // تحميل قائمة المتابعين المهمين
        importantFollowers.addAll(preferencesHelper.getImportantFollowers())
        
        Log.d(TAG, "Loaded ${activeGoals.size} active goals and ${dailyAchievements.size} daily achievements")
    }

    /**
     * تحميل الإنجازات اليومية الحديثة
     */
    private fun loadRecentDailyAchievements() {
        val calendar = Calendar.getInstance()
        for (i in 0..6) { // آخر 7 أيام
            val dateKey = dateFormat.format(calendar.time)
            val achievement = preferencesHelper.getDailyAchievement(dateKey)
            if (achievement != null) {
                dailyAchievements[dateKey] = achievement
            }
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
    }

    /**
     * معالجة تفاعل جديد من وسائل التواصل الاجتماعي
     */
    fun processNewInteraction(interaction: SocialMediaInteraction) {
        Log.d(TAG, "Processing new interaction: ${interaction.type} - ${interaction.count}")
        
        scope.launch {
            // تحديث الإنجازات اليومية
            updateDailyAchievements(interaction)
            
            // فحص الأهداف
            checkGoalProgress(interaction)
            
            // فحص الإنجازات الخاصة
            checkSpecialAchievements(interaction)
            
            // تحديث الإحصائيات
            updateStatistics(interaction)

            // إضافة نقاط للمستخدم
            rewardManager.addPoints(interaction.count)        }
    }

    /**
     * تحديث الإنجازات اليومية
     */
    private suspend fun updateDailyAchievements(interaction: SocialMediaInteraction) {
        val today = dateFormat.format(Date())
        val currentAchievement = dailyAchievements[today] ?: DailyAchievement(
            date = today,
            platform = interaction.platform
        )

        val updatedAchievement = when (interaction.type) {
            SocialMediaInteraction.Type.NEW_FOLLOWERS -> 
                currentAchievement.copy(newFollowers = currentAchievement.newFollowers + interaction.count)
            SocialMediaInteraction.Type.NEW_LIKES -> 
                currentAchievement.copy(newLikes = currentAchievement.newLikes + interaction.count)
            SocialMediaInteraction.Type.NEW_COMMENTS -> 
                currentAchievement.copy(newComments = currentAchievement.newComments + interaction.count)
            SocialMediaInteraction.Type.NEW_SHARES -> 
                currentAchievement.copy(newShares = currentAchievement.newShares + interaction.count)
            SocialMediaInteraction.Type.NEW_VIEWS -> 
                currentAchievement.copy(newViews = currentAchievement.newViews + interaction.count)
            else -> currentAchievement
        }

        dailyAchievements[today] = updatedAchievement
        preferencesHelper.saveDailyAchievement(today, updatedAchievement)
        
        Log.d(TAG, "Updated daily achievement for $today")
    }

    /**
     * فحص تقدم الأهداف
     */
    private suspend fun checkGoalProgress(interaction: SocialMediaInteraction) {
        val completedGoals = mutableListOf<SocialMediaGoal>()
        
        for (goal in activeGoals) {
            if (goal.isCompleted || goal.type != interaction.type) continue
            
            // تحديث القيمة الحالية للهدف
            val newCurrentValue = goal.currentValue + interaction.count
            val updatedGoal = goal.copy(currentValue = newCurrentValue)
            
            // فحص إذا تم إنجاز الهدف
            if (newCurrentValue >= goal.targetValue) {
                val completedGoal = updatedGoal.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis()
                )
                completedGoals.add(completedGoal)
                
                // تشغيل احتفال إنجاز الهدف
                triggerGoalCompletionCelebration(completedGoal)
                
                Log.d(TAG, "Goal completed: ${goal.title}")
            } else {
                // تحديث الهدف في القائمة
                val index = activeGoals.indexOf(goal)
                if (index >= 0) {
                    activeGoals[index] = updatedGoal
                }
            }
        }
        
        // إزالة الأهداف المكتملة من القائمة النشطة
        activeGoals.removeAll(completedGoals)
        
        // حفظ التحديثات
        preferencesHelper.saveActiveGoals(activeGoals)
        preferencesHelper.saveCompletedGoals(completedGoals)
    }

    /**
     * فحص الإنجازات الخاصة
     */
    private suspend fun checkSpecialAchievements(interaction: SocialMediaInteraction) {
        // فحص إنجازات المعالم (Milestones)
        checkMilestoneAchievements(interaction)
        
        // فحص تفاعل المشاهير
        checkCelebrityInteraction(interaction)
        
        // فحص الإنجازات المتتالية
        checkStreakAchievements(interaction)
    }

    /**
     * فحص إنجازات المعالم
     */
    private suspend fun checkMilestoneAchievements(interaction: SocialMediaInteraction) {
        val milestones = when (interaction.type) {
            SocialMediaInteraction.Type.NEW_FOLLOWERS -> listOf(10, 50, 100, 500, 1000, 5000, 10000)
            SocialMediaInteraction.Type.NEW_LIKES -> listOf(100, 500, 1000, 5000, 10000, 50000, 100000)
            SocialMediaInteraction.Type.NEW_VIEWS -> listOf(1000, 5000, 10000, 50000, 100000, 500000, 1000000)
            else -> emptyList()
        }
        
        val totalCount = getTotalCountForType(interaction.type, interaction.platform)
        
        for (milestone in milestones) {
            if (totalCount >= milestone && !isMilestoneReached(interaction.type, milestone, interaction.platform)) {
                // تسجيل إنجاز المعلم
                markMilestoneReached(interaction.type, milestone, interaction.platform)
                
                // تشغيل احتفال المعلم
                triggerMilestoneAchievement(interaction.type, milestone, interaction.platform)
                
                Log.d(TAG, "Milestone reached: ${interaction.type} - $milestone")
                break // إنجاز معلم واحد في المرة
            }
        }
    }

    /**
     * فحص تفاعل المشاهير
     */
    private suspend fun checkCelebrityInteraction(interaction: SocialMediaInteraction) {
        // هذه الميزة تتطلب تحليل أكثر تعقيداً لمحتوى الإشعار
        // لتحديد ما إذا كان التفاعل من شخصية مشهورة
        
        for (celebrity in importantFollowers.filter { it.isCelebrity && it.platform == interaction.platform }) {
            if (interaction.content.contains(celebrity.name, ignoreCase = true) ||
                (celebrity.username != null && interaction.content.contains(celebrity.username, ignoreCase = true))) {
                
                // تشغيل احتفال خاص للمشاهير
                triggerCelebrityInteractionCelebration(celebrity, interaction)
                
                Log.d(TAG, "Celebrity interaction detected: ${celebrity.name}")
                break
            }
        }
    }

    /**
     * فحص الإنجازات المتتالية
     */
    private suspend fun checkStreakAchievements(interaction: SocialMediaInteraction) {
        val today = dateFormat.format(Date())
        val streak = calculateCurrentStreak(interaction.platform)
        
        // إنجازات الإنتاجية المتتالية (7 أيام، 30 يوم، إلخ)
        val streakMilestones = listOf(7, 14, 30, 60, 100)
        
        for (milestone in streakMilestones) {
            if (streak >= milestone && !isStreakMilestoneReached(milestone, interaction.platform)) {
                markStreakMilestoneReached(milestone, interaction.platform)
                triggerStreakAchievement(milestone, interaction.platform)
                
                Log.d(TAG, "Streak milestone reached: $milestone days on ${interaction.platform}")
                break
            }
        }
    }

    /**
     * تشغيل احتفال إنجاز الهدف
     */
    private suspend fun triggerGoalCompletionCelebration(goal: SocialMediaGoal) {
        // رسالة صوتية
        val message = goal.rewardMessage ?: "مبروك! لقد أنجزت هدف: ${goal.title}!"
        advancedVoiceInteractionService.speak(message)
        val animation = goal.rewardAnimation ?: "goal_completion"
        characterAnimationService.playSpecialAnimation(animation)
        
        // مؤثرات بصرية
        val effects = listOf("fireworks", "golden_stars", "celebration_confetti")
        characterAnimationService.addVisualEffects(effects)
        
        // إشعار التطبيق الرئيسي
        broadcastGoalCompletion(goal)
    }

    /**
     * تشغيل احتفال المعلم
     */
    private suspend fun triggerMilestoneAchievement(type: SocialMediaInteraction.Type, milestone: Int, platform: SocialMediaPlatform) {
        val typeName = when (type) {
            SocialMediaInteraction.Type.NEW_FOLLOWERS -> "متابع"
            SocialMediaInteraction.Type.NEW_LIKES -> "إعجاب"
            SocialMediaInteraction.Type.NEW_VIEWS -> "مشاهدة"
            else -> "تفاعل"
        }
        
        val platformName = getPlatformName(platform)
        val message = "إنجاز رائع! وصلت إلى $milestone $typeName على $platformName!"
        
        advancedVoiceInteractionService.speak(message)
        characterAnimationService.addVisualEffects(listOf("golden_rain", "achievement_badge"))
    }

    /**
     * تشغيل احتفال تفاعل المشاهير
     */
    private suspend fun triggerCelebrityInteractionCelebration(celebrity: ImportantFollower, interaction: SocialMediaInteraction) {
        val message = celebrity.specialMessage ?: "واو! ${celebrity.name} تفاعل معك! هذا إنجاز كبير!"
        
        advancedVoiceInteractionService.speak(message)
        characterAnimationService.addVisualEffects(listOf("star_shower", "vip_glow"))
    }

    /**
     * تشغيل احتفال الإنجاز المتتالي
     */
    private suspend fun triggerStreakAchievement(days: Int, platform: SocialMediaPlatform) {
        val platformName = getPlatformName(platform)
        val message = "إنجاز مذهل! $days يوم متتالي من النشاط على $platformName!"
        
        advancedVoiceInteractionService.speak(message)
        characterAnimationService.addVisualEffects(listOf("fire_trail", "consistency_crown"))
    }

    /**
     * إنشاء هدف جديد
     */
    fun createNewGoal(
        title: String,
        description: String,
        platform: SocialMediaPlatform,
        type: SocialMediaInteraction.Type,
        targetValue: Int,
        deadline: Long? = null
    ): SocialMediaGoal {
        val goal = SocialMediaGoal(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            platform = platform,
            type = type,
            targetValue = targetValue,
            deadline = deadline
        )
        
        activeGoals.add(goal)
        preferencesHelper.saveActiveGoals(activeGoals)
        
        Log.d(TAG, "Created new goal: $title")
        return goal
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
        val follower = ImportantFollower(
            id = UUID.randomUUID().toString(),
            name = name,
            username = username,
            platform = platform,
            isCelebrity = isCelebrity,
            specialMessage = specialMessage
        )
        
        importantFollowers.add(follower)
        preferencesHelper.saveImportantFollowers(importantFollowers)
        
        Log.d(TAG, "Added important follower: $name")
        return follower
    }

    /**
     * إنشاء التقرير اليومي
     */
    fun generateDailyReport(date: String = dateFormat.format(Date())): DailyReport {
        val achievement = dailyAchievements[date] ?: DailyAchievement(date = date, platform = SocialMediaPlatform.UNKNOWN)
        
        val totalInteractions = achievement.newFollowers + achievement.newLikes + 
                               achievement.newComments + achievement.newShares + achievement.newViews
        
        val completedGoals = preferencesHelper.getCompletedGoalsForDate(date)
        val topPlatform = determineTopPerformingPlatform(date)
        val encouragement = generateEncouragementMessage(totalInteractions, completedGoals.size)
        val suggestions = generateSuggestionsForTomorrow(achievement)

        val reportText = "تقريرك اليومي: " +
                "لقد حققت ${totalInteractions} تفاعلًا إجماليًا اليوم. " +
                "أكملت ${completedGoals.size} هدفًا. " +
                "المنصة الأفضل أداءً كانت ${getPlatformName(topPlatform)}. " +
                "${encouragement} " +
                "${suggestions}"

        advancedVoiceInteractionService.speak(reportText)

        return DailyReport(
            date = date,
            totalInteractions = totalInteractions,
            platformBreakdown = mapOf(achievement.platform to achievement),
            goalsCompleted = completedGoals,
            newUnlocks = emptyList(), // سيتم تنفيذها لاحقاً
            topPerformingPlatform = topPlatform,
            encouragementMessage = encouragement,
            suggestionsForTomorrow = suggestions
        )    }

    /**
     * إعداد جدولة التقرير اليومي
     */
    private fun setupDailyReportScheduler() {
        // سيتم تنفيذ جدولة التقرير اليومي باستخدام WorkManager أو AlarmManager
        Log.d(TAG, "Daily report scheduler setup completed")
    }

    // دوال مساعدة
    private fun getTotalCountForType(type: SocialMediaInteraction.Type, platform: SocialMediaPlatform): Int {
        return preferencesHelper.getTotalCountForType(type, platform)
    }

    private fun isMilestoneReached(type: SocialMediaInteraction.Type, milestone: Int, platform: SocialMediaPlatform): Boolean {
        return preferencesHelper.isMilestoneReached(type, milestone, platform)
    }

    private fun markMilestoneReached(type: SocialMediaInteraction.Type, milestone: Int, platform: SocialMediaPlatform) {
        preferencesHelper.markMilestoneReached(type, milestone, platform)
    }

    private fun calculateCurrentStreak(platform: SocialMediaPlatform): Int {
        return preferencesHelper.getCurrentStreak(platform)
    }

    private fun isStreakMilestoneReached(days: Int, platform: SocialMediaPlatform): Boolean {
        return preferencesHelper.isStreakMilestoneReached(days, platform)
    }

    private fun markStreakMilestoneReached(days: Int, platform: SocialMediaPlatform) {
        preferencesHelper.markStreakMilestoneReached(days, platform)
    }

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

    private fun determineTopPerformingPlatform(date: String): SocialMediaPlatform? {
        // تحديد المنصة الأفضل أداءً لليوم
        return dailyAchievements[date]?.platform
    }

    private fun generateEncouragementMessage(totalInteractions: Int, completedGoals: Int): String {
        return when {
            totalInteractions > 100 -> "يوم رائع! تفاعل مذهل اليوم!"
            totalInteractions > 50 -> "أداء جيد! استمر في التقدم!"
            totalInteractions > 10 -> "بداية جيدة! يمكنك تحقيق المزيد!"
            else -> "كل يوم هو فرصة جديدة! لا تستسلم!"
        }
    }

    private fun generateSuggestionsForTomorrow(achievement: DailyAchievement): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (achievement.newFollowers < 5) {
            suggestions.add("حاول نشر محتوى جذاب لزيادة المتابعين")
        }
        
        if (achievement.newLikes < 20) {
            suggestions.add("استخدم هاشتاغات شائعة لزيادة الوصول")
        }
        
        if (achievement.newComments < 5) {
            suggestions.add("اطرح أسئلة لتشجيع التفاعل")
        }
        
        return suggestions
    }

    private fun updateStatistics(interaction: SocialMediaInteraction) {
        preferencesHelper.updateInteractionStatistics(interaction)
    }

    private fun broadcastGoalCompletion(goal: SocialMediaGoal) {
        val intent = Intent("com.animecharacter.GOAL_COMPLETED")
        intent.putExtra("goal_id", goal.id)
        intent.putExtra("goal_title", goal.title)
        context.sendBroadcast(intent)
    }
}

