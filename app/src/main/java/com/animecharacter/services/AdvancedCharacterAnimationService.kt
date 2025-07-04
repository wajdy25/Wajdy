package com.animecharacter.services

import android.content.Context
import android.util.Log
import com.animecharacter.models.*
import com.animecharacter.utils.PreferencesHelper
import kotlinx.coroutines.*
import java.io.File

/**
 * مدير الرسوم المتحركة المتقدم للشخصية الأنمي
 * يدير الرسوم المتحركة الخاصة بالاحتفالات والتفاعلات
 */
class AdvancedCharacterAnimationService(private val context: Context) {

    companion object {
        private const val TAG = "AdvancedCharacterAnimationService"
        
        // مسارات ملفات الرسوم المتحركة
        private const val ANIMATIONS_DIR = "animations"
        private const val LOTTIE_DIR = "$ANIMATIONS_DIR/lottie"
        private const val WEBM_DIR = "$ANIMATIONS_DIR/webm"
        private const val GIF_DIR = "$ANIMATIONS_DIR/gif"
    }

    private val preferencesHelper = PreferencesHelper(context)
    private val visualEffectsService = VisualEffectsService(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // قوائم الرسوم المتحركة المتاحة
    private val availableAnimations = mutableMapOf<String, SpecialAnimation>()
    private val unlockedAnimations = mutableSetOf<String>()
    private var currentAnimation: String? = null
    private var isAnimationPlaying = false

    init {
        loadAvailableAnimations()
        loadUnlockedAnimations()
    }

    /**
     * تحميل الرسوم المتحركة المتاحة
     */
    private fun loadAvailableAnimations() {
        // رسوم متحركة للمتابعين الجدد
        availableAnimations["new_followers_small"] = SpecialAnimation(
            name = "new_followers_small",
            filePath = "$LOTTIE_DIR/followers_celebration_small.json",
            duration = 2000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_FOLLOWERS,
                minimumCount = 1
            )
        )
        
        availableAnimations["new_followers_medium"] = SpecialAnimation(
            name = "new_followers_medium",
            filePath = "$LOTTIE_DIR/followers_celebration_medium.json",
            duration = 3000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_FOLLOWERS,
                minimumCount = 10
            )
        )
        
        availableAnimations["new_followers_big"] = SpecialAnimation(
            name = "new_followers_big",
            filePath = "$LOTTIE_DIR/followers_celebration_big.json",
            duration = 5000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_FOLLOWERS,
                minimumCount = 100
            ),
            unlockRequirement = SpecialAnimation.UnlockRequirement(
                totalInteractions = 500
            )
        )
        
        // رسوم متحركة للإعجابات
        availableAnimations["likes_celebration"] = SpecialAnimation(
            name = "likes_celebration",
            filePath = "$LOTTIE_DIR/likes_celebration.json",
            duration = 2500,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_LIKES,
                minimumCount = 10
            )
        )
        
        availableAnimations["likes_explosion"] = SpecialAnimation(
            name = "likes_explosion",
            filePath = "$LOTTIE_DIR/likes_explosion.json",
            duration = 4000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_LIKES,
                minimumCount = 1000
            ),
            unlockRequirement = SpecialAnimation.UnlockRequirement(
                totalInteractions = 1000
            )
        )
        
        // رسوم متحركة للتعليقات
        availableAnimations["comments_chat"] = SpecialAnimation(
            name = "comments_chat",
            filePath = "$LOTTIE_DIR/comments_chat.json",
            duration = 3000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_COMMENTS,
                minimumCount = 1
            )
        )
        
        // رسوم متحركة للمشاركات
        availableAnimations["shares_viral"] = SpecialAnimation(
            name = "shares_viral",
            filePath = "$LOTTIE_DIR/shares_viral.json",
            duration = 3500,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_SHARES,
                minimumCount = 1
            )
        )
        
        // رسوم متحركة للمشاهدات
        availableAnimations["views_trending"] = SpecialAnimation(
            name = "views_trending",
            filePath = "$LOTTIE_DIR/views_trending.json",
            duration = 3000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_VIEWS,
                minimumCount = 100
            )
        )
        
        // رسوم متحركة خاصة للمشاهير
        availableAnimations["celebrity_interaction"] = SpecialAnimation(
            name = "celebrity_interaction",
            filePath = "$LOTTIE_DIR/celebrity_interaction.json",
            duration = 6000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.CELEBRITY_INTERACTION,
                minimumCount = 1
            ),
            unlockRequirement = SpecialAnimation.UnlockRequirement(
                totalInteractions = 100,
                specificAchievements = listOf("first_celebrity_interaction")
            )
        )
        
        // رسوم متحركة للمعالم
        availableAnimations["milestone_achievement"] = SpecialAnimation(
            name = "milestone_achievement",
            filePath = "$LOTTIE_DIR/milestone_achievement.json",
            duration = 5000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.MILESTONE_REACHED,
                minimumCount = 1
            )
        )
        
        // رسوم متحركة للأهداف
        availableAnimations["goal_completion"] = SpecialAnimation(
            name = "goal_completion",
            filePath = "$LOTTIE_DIR/goal_completion.json",
            duration = 4000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.MILESTONE_REACHED,
                minimumCount = 1
            )
        )
        
        // رسوم متحركة للإنجازات المتتالية
        availableAnimations["streak_fire"] = SpecialAnimation(
            name = "streak_fire",
            filePath = "$LOTTIE_DIR/streak_fire.json",
            duration = 4500,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.MILESTONE_REACHED,
                minimumCount = 1
            ),
            unlockRequirement = SpecialAnimation.UnlockRequirement(
                totalInteractions = 200,
                specificAchievements = listOf("7_day_streak")
            )
        )
        
        // رسوم متحركة خاصة بالمنصات
        availableAnimations["instagram_special"] = SpecialAnimation(
            name = "instagram_special",
            filePath = "$LOTTIE_DIR/instagram_special.json",
            duration = 3500,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_LIKES,
                minimumCount = 50,
                platform = SocialMediaPlatform.INSTAGRAM
            )
        )
        
        availableAnimations["tiktok_viral"] = SpecialAnimation(
            name = "tiktok_viral",
            filePath = "$LOTTIE_DIR/tiktok_viral.json",
            duration = 4000,
            triggerCondition = SpecialAnimation.TriggerCondition(
                interactionType = SocialMediaInteraction.Type.NEW_VIEWS,
                minimumCount = 10000,
                platform = SocialMediaPlatform.TIKTOK
            ),
            unlockRequirement = SpecialAnimation.UnlockRequirement(
                totalInteractions = 1000
            )
        )
        
        Log.d(TAG, "Loaded ${availableAnimations.size} available animations")
    }

    /**
     * تحميل الرسوم المتحركة المفتوحة
     */
    private fun loadUnlockedAnimations() {
        unlockedAnimations.addAll(preferencesHelper.getUnlockedAnimations())
        
        // فتح الرسوم المتحركة الأساسية افتراضياً
        val basicAnimations = listOf(
            "new_followers_small",
            "new_followers_medium", 
            "likes_celebration",
            "comments_chat",
            "shares_viral",
            "views_trending",
            "milestone_achievement",
            "goal_completion"
        )
        
        unlockedAnimations.addAll(basicAnimations)
        preferencesHelper.saveUnlockedAnimations(unlockedAnimations.toList())
        
        Log.d(TAG, "Loaded ${unlockedAnimations.size} unlocked animations")
    }

    /**
     * تشغيل رسوم متحركة بناءً على التفاعل
     */
    fun playAnimationForInteraction(interaction: SocialMediaInteraction, celebrationLevel: Int) {
        scope.launch {
            val animation = selectBestAnimation(interaction, celebrationLevel)
            
            if (animation != null) {
                playAnimation(animation)
                
                // تشغيل المؤثرات البصرية المصاحبة
                visualEffectsService.triggerInteractionEffect(interaction, celebrationLevel)
                
                // فحص إمكانية فتح رسوم متحركة جديدة
                checkForNewUnlocks(interaction)
            } else {
                Log.w(TAG, "No suitable animation found for interaction: ${interaction.type}")
            }
        }
    }

    /**
     * اختيار أفضل رسوم متحركة للتفاعل
     */
    private fun selectBestAnimation(interaction: SocialMediaInteraction, level: Int): SpecialAnimation? {
        val suitableAnimations = availableAnimations.values.filter { animation ->
            // فحص إذا كانت الرسوم المتحركة مفتوحة
            if (!isAnimationUnlocked(animation.name)) return@filter false
            
            // فحص نوع التفاعل
            if (animation.triggerCondition.interactionType != interaction.type) return@filter false
            
            // فحص المنصة (إذا كانت محددة)
            if (animation.triggerCondition.platform != null && 
                animation.triggerCondition.platform != interaction.platform) return@filter false
            
            // فحص الحد الأدنى للعدد
            if (interaction.count < animation.triggerCondition.minimumCount) return@filter false
            
            true
        }
        
        // اختيار الرسوم المتحركة الأنسب بناءً على مستوى الاحتفال
        return when (level) {
            3 -> suitableAnimations.maxByOrNull { it.duration } // الأطول للاحتفال الكبير
            2 -> suitableAnimations.find { it.duration in 3000..4000 } ?: suitableAnimations.firstOrNull()
            1 -> suitableAnimations.minByOrNull { it.duration } // الأقصر للاحتفال الصغير
            else -> null
        }
    }

    /**
     * تشغيل رسوم متحركة محددة
     */
    private suspend fun playAnimation(animation: SpecialAnimation) {
        if (isAnimationPlaying) {
            Log.d(TAG, "Animation already playing, queuing: ${animation.name}")
            delay(1000) // انتظار قصير
            if (isAnimationPlaying) return // إذا كانت لا تزال تعمل، تجاهل
        }
        
        isAnimationPlaying = true
        currentAnimation = animation.name
        
        Log.d(TAG, "Playing animation: ${animation.name}")
        
        try {
            // تشغيل الرسوم المتحركة (سيتم تنفيذها مع مكتبة Lottie أو WebM)
            playAnimationFile(animation)
            
            // انتظار انتهاء الرسوم المتحركة
            delay(animation.duration)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing animation: ${animation.name}", e)
        } finally {
            isAnimationPlaying = false
            currentAnimation = null
        }
    }

    /**
     * تشغيل ملف الرسوم المتحركة
     */
    private suspend fun playAnimationFile(animation: SpecialAnimation) {
        val file = File(context.filesDir, animation.filePath)
        
        when {
            animation.filePath.endsWith(".json") -> {
                // تشغيل Lottie animation
                playLottieAnimation(animation)
            }
            animation.filePath.endsWith(".webm") -> {
                // تشغيل WebM video
                playWebMAnimation(animation)
            }
            animation.filePath.endsWith(".gif") -> {
                // تشغيل GIF animation
                playGifAnimation(animation)
            }
            else -> {
                Log.w(TAG, "Unsupported animation format: ${animation.filePath}")
            }
        }
    }

    /**
     * تشغيل رسوم متحركة Lottie
     */
    private suspend fun playLottieAnimation(animation: SpecialAnimation) {
        // سيتم تنفيذ هذا مع مكتبة Lottie
        Log.d(TAG, "Playing Lottie animation: ${animation.name}")
        
        // محاكاة تشغيل الرسوم المتحركة
        broadcastAnimationEvent("lottie_start", animation.name)
        delay(animation.duration)
        broadcastAnimationEvent("lottie_end", animation.name)
    }

    /**
     * تشغيل رسوم متحركة WebM
     */
    private suspend fun playWebMAnimation(animation: SpecialAnimation) {
        Log.d(TAG, "Playing WebM animation: ${animation.name}")
        
        broadcastAnimationEvent("webm_start", animation.name)
        delay(animation.duration)
        broadcastAnimationEvent("webm_end", animation.name)
    }

    /**
     * تشغيل رسوم متحركة GIF
     */
    private suspend fun playGifAnimation(animation: SpecialAnimation) {
        Log.d(TAG, "Playing GIF animation: ${animation.name}")
        
        broadcastAnimationEvent("gif_start", animation.name)
        delay(animation.duration)
        broadcastAnimationEvent("gif_end", animation.name)
    }

    /**
     * فحص إمكانية فتح رسوم متحركة جديدة
     */
    private fun checkForNewUnlocks(interaction: SocialMediaInteraction) {
        val totalInteractions = preferencesHelper.getTotalInteractionCount()
        val completedAchievements = preferencesHelper.getCompletedAchievements()
        
        for (animation in availableAnimations.values) {
            if (isAnimationUnlocked(animation.name)) continue
            
            val requirement = animation.unlockRequirement ?: continue
            
            // فحص إجمالي التفاعلات
            if (totalInteractions < requirement.totalInteractions) continue
            
            // فحص الإنجازات المحددة
            if (requirement.specificAchievements.isNotEmpty()) {
                val hasAllAchievements = requirement.specificAchievements.all { achievement ->
                    completedAchievements.contains(achievement)
                }
                if (!hasAllAchievements) continue
            }
            
            // فتح الرسوم المتحركة
            unlockAnimation(animation.name)
        }
    }

    /**
     * فتح رسوم متحركة جديدة
     */
    private fun unlockAnimation(animationName: String) {
        if (unlockedAnimations.add(animationName)) {
            preferencesHelper.saveUnlockedAnimations(unlockedAnimations.toList())
            
            Log.d(TAG, "Unlocked new animation: $animationName")
            
            // إشعار المستخدم بالفتح الجديد
            broadcastAnimationUnlock(animationName)
            
            // تشغيل احتفال صغير للفتح
            scope.launch {
                delay(500)
                playUnlockCelebration(animationName)
            }
        }
    }

    /**
     * تشغيل احتفال فتح رسوم متحركة جديدة
     */
    private suspend fun playUnlockCelebration(animationName: String) {
        val unlockEffect = VisualEffect(
            type = "unlock_celebration",
            duration = 2000,
            intensity = 2,
            color = "#FFD700",
            position = VisualEffect.Position.FULL_SCREEN
        )
        
        // تشغيل مؤثرات بصرية للفتح
        visualEffectsService.playVisualEffect(unlockEffect)
    }

    /**
     * فحص إذا كانت الرسوم المتحركة مفتوحة
     */
    private fun isAnimationUnlocked(animationName: String): Boolean {
        return unlockedAnimations.contains(animationName)
    }

    /**
     * الحصول على قائمة الرسوم المتحركة المفتوحة
     */
    fun getUnlockedAnimations(): List<SpecialAnimation> {
        return availableAnimations.values.filter { isAnimationUnlocked(it.name) }
    }

    /**
     * الحصول على قائمة الرسوم المتحركة المقفلة
     */
    fun getLockedAnimations(): List<SpecialAnimation> {
        return availableAnimations.values.filter { !isAnimationUnlocked(it.name) }
    }

    /**
     * تشغيل رسوم متحركة محددة يدوياً
     */
    fun playSpecificAnimation(animationName: String) {
        val animation = availableAnimations[animationName]
        if (animation != null && isAnimationUnlocked(animationName)) {
            scope.launch {
                playAnimation(animation)
            }
        } else {
            Log.w(TAG, "Animation not found or locked: $animationName")
        }
    }

    /**
     * إيقاف الرسوم المتحركة الحالية
     */
    fun stopCurrentAnimation() {
        if (isAnimationPlaying && currentAnimation != null) {
            Log.d(TAG, "Stopping current animation: $currentAnimation")
            scope.coroutineContext.cancelChildren()
            isAnimationPlaying = false
            currentAnimation = null
            
            broadcastAnimationEvent("animation_stopped", currentAnimation ?: "unknown")
        }
    }

    /**
     * إرسال أحداث الرسوم المتحركة
     */
    private fun broadcastAnimationEvent(event: String, animationName: String) {
        val intent = android.content.Intent("com.animecharacter.ANIMATION_EVENT")
        intent.putExtra("event", event)
        intent.putExtra("animation_name", animationName)
        context.sendBroadcast(intent)
    }

    /**
     * إرسال إشعار فتح رسوم متحركة جديدة
     */
    private fun broadcastAnimationUnlock(animationName: String) {
        val intent = android.content.Intent("com.animecharacter.ANIMATION_UNLOCKED")
        intent.putExtra("animation_name", animationName)
        context.sendBroadcast(intent)
    }

    /**
     * الحصول على معلومات الرسوم المتحركة الحالية
     */
    fun getCurrentAnimationInfo(): Pair<String?, Boolean> {
        return Pair(currentAnimation, isAnimationPlaying)
    }

    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        scope.cancel()
        visualEffectsService.stopAllEffects()
    }
}

