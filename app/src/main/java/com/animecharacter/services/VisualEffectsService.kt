package com.animecharacter.services

import android.animation.*
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.animecharacter.models.VisualEffect
import com.animecharacter.models.SocialMediaInteraction
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.*
import kotlin.random.Random

/**
 * خدمة المؤثرات البصرية والرسوم المتحركة للاحتفالات
 * تدير جميع المؤثرات البصرية التي تظهر عند التفاعل مع وسائل التواصل الاجتماعي
 */
class VisualEffectsService(private val context: Context) {

    companion object {
        private const val TAG = "VisualEffectsService"
        private const val EFFECT_DURATION_SHORT = 2000L
        private const val EFFECT_DURATION_MEDIUM = 3000L
        private const val EFFECT_DURATION_LONG = 5000L
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeEffects = mutableListOf<EffectInstance>()
    private var parentView: ViewGroup? = null

    /**
     * تعيين العرض الأساسي للمؤثرات
     */
    fun setParentView(view: ViewGroup) {
        parentView = view
    }

    /**
     * تشغيل مؤثر بصري بناءً على نوع التفاعل
     */
    fun triggerInteractionEffect(interaction: SocialMediaInteraction, celebrationLevel: Int) {
        val effects = getEffectsForInteraction(interaction, celebrationLevel)
        
        for (effect in effects) {
            playVisualEffect(effect)
        }
    }

    /**
     * تحديد المؤثرات المناسبة لنوع التفاعل
     */
    private fun getEffectsForInteraction(interaction: SocialMediaInteraction, level: Int): List<VisualEffect> {
        val effects = mutableListOf<VisualEffect>()
        
        when (interaction.type) {
            SocialMediaInteraction.Type.NEW_FOLLOWERS -> {
                effects.add(createHeartEffect(level))
                effects.add(createStarEffect(level))
                if (level >= 2) effects.add(createSparkleEffect(level))
            }
            SocialMediaInteraction.Type.NEW_LIKES -> {
                effects.add(createHeartEffect(level))
                effects.add(createSparkleEffect(level))
                if (level >= 3) effects.add(createFireworksEffect())
            }
            SocialMediaInteraction.Type.NEW_COMMENTS -> {
                effects.add(createSpeechBubbleEffect(level))
                effects.add(createStarEffect(level))
            }
            SocialMediaInteraction.Type.NEW_SHARES -> {
                effects.add(createArrowEffect(level))
                effects.add(createSparkleEffect(level))
                if (level >= 2) effects.add(createRippleEffect())
            }
            SocialMediaInteraction.Type.NEW_VIEWS -> {
                effects.add(createEyeEffect(level))
                effects.add(createNeonGlowEffect(level))
            }
            SocialMediaInteraction.Type.CELEBRITY_INTERACTION -> {
                effects.add(createStarShowerEffect())
                effects.add(createVipGlowEffect())
                effects.add(createGoldenRainEffect())
            }
            SocialMediaInteraction.Type.MILESTONE_REACHED -> {
                effects.add(createFireworksEffect())
                effects.add(createAchievementBadgeEffect())
                effects.add(createGoldenRainEffect())
            }
            else -> {
                effects.add(createSparkleEffect(1))
            }
        }
        
        return effects
    }

    /**
     * تشغيل مؤثر بصري
     */
    private fun playVisualEffect(effect: VisualEffect) {
        parentView?.let { parent ->
            scope.launch {
                when (effect.type) {
                    "hearts" -> playHeartEffect(parent, effect)
                    "stars" -> playStarEffect(parent, effect)
                    "sparkles" -> playSparkleEffect(parent, effect)
                    "fireworks" -> playFireworksEffect(parent, effect)
                    "speech_bubbles" -> playSpeechBubbleEffect(parent, effect)
                    "arrows" -> playArrowEffect(parent, effect)
                    "eyes" -> playEyeEffect(parent, effect)
                    "neon_glow" -> playNeonGlowEffect(parent, effect)
                    "star_shower" -> playStarShowerEffect(parent, effect)
                    "vip_glow" -> playVipGlowEffect(parent, effect)
                    "golden_rain" -> playGoldenRainEffect(parent, effect)
                    "achievement_badge" -> playAchievementBadgeEffect(parent, effect)
                    "ripple" -> playRippleEffect(parent, effect)
                    else -> playDefaultEffect(parent, effect)
                }
            }
        }
    }

    /**
     * مؤثر القلوب
     */
    private suspend fun playHeartEffect(parent: ViewGroup, effect: VisualEffect) {
        val heartCount = effect.intensity * 5
        
        repeat(heartCount) { index ->
            delay(index * 100L) // تأخير بين كل قلب
            
            val heartView = createHeartView()
            parent.addView(heartView)
            
            // تحديد موقع البداية
            val startX = parent.width * 0.5f + Random.nextFloat() * 200 - 100
            val startY = parent.height * 0.8f
            
            heartView.x = startX
            heartView.y = startY
            
            // رسوم متحركة للحركة
            val animatorSet = AnimatorSet()
            
            val moveY = ObjectAnimator.ofFloat(heartView, "y", startY, startY - 300)
            val moveX = ObjectAnimator.ofFloat(heartView, "x", startX, startX + Random.nextFloat() * 100 - 50)
            val scaleX = ObjectAnimator.ofFloat(heartView, "scaleX", 0f, 1f, 0.8f)
            val scaleY = ObjectAnimator.ofFloat(heartView, "scaleY", 0f, 1f, 0.8f)
            val alpha = ObjectAnimator.ofFloat(heartView, "alpha", 1f, 0f)
            
            moveY.duration = effect.duration
            moveX.duration = effect.duration
            scaleX.duration = 500
            scaleY.duration = 500
            alpha.duration = effect.duration
            alpha.startDelay = effect.duration / 2
            
            animatorSet.playTogether(moveY, moveX, scaleX, scaleY, alpha)
            animatorSet.interpolator = DecelerateInterpolator()
            
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parent.removeView(heartView)
                }
            })
            
            animatorSet.start()
        }
    }

    /**
     * مؤثر النجوم
     */
    private suspend fun playStarEffect(parent: ViewGroup, effect: VisualEffect) {
        val starCount = effect.intensity * 8
        
        repeat(starCount) { index ->
            delay(index * 80L)
            
            val starView = createStarView()
            parent.addView(starView)
            
            // موقع عشوائي حول الشخصية
            val centerX = parent.width * 0.5f
            val centerY = parent.height * 0.5f
            val radius = 150f + Random.nextFloat() * 100
            val angle = Random.nextFloat() * 2 * PI
            
            val startX = centerX + cos(angle).toFloat() * radius
            val startY = centerY + sin(angle).toFloat() * radius
            
            starView.x = startX
            starView.y = startY
            
            // رسوم متحركة دوارة مع تلاشي
            val animatorSet = AnimatorSet()
            
            val rotation = ObjectAnimator.ofFloat(starView, "rotation", 0f, 360f)
            val scaleX = ObjectAnimator.ofFloat(starView, "scaleX", 0f, 1.2f, 0f)
            val scaleY = ObjectAnimator.ofFloat(starView, "scaleY", 0f, 1.2f, 0f)
            val alpha = ObjectAnimator.ofFloat(starView, "alpha", 0f, 1f, 0f)
            
            rotation.duration = effect.duration
            scaleX.duration = effect.duration
            scaleY.duration = effect.duration
            alpha.duration = effect.duration
            
            rotation.repeatCount = 2
            
            animatorSet.playTogether(rotation, scaleX, scaleY, alpha)
            animatorSet.interpolator = AccelerateDecelerateInterpolator()
            
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parent.removeView(starView)
                }
            })
            
            animatorSet.start()
        }
    }

    /**
     * مؤثر الشرارات
     */
    private suspend fun playSparkleEffect(parent: ViewGroup, effect: VisualEffect) {
        val sparkleCount = effect.intensity * 15
        
        repeat(sparkleCount) { index ->
            delay(index * 50L)
            
            val sparkleView = createSparkleView()
            parent.addView(sparkleView)
            
            // موقع عشوائي
            val x = Random.nextFloat() * parent.width
            val y = Random.nextFloat() * parent.height
            
            sparkleView.x = x
            sparkleView.y = y
            
            // رسوم متحركة سريعة
            val animatorSet = AnimatorSet()
            
            val scaleX = ObjectAnimator.ofFloat(sparkleView, "scaleX", 0f, 1f, 0f)
            val scaleY = ObjectAnimator.ofFloat(sparkleView, "scaleY", 0f, 1f, 0f)
            val alpha = ObjectAnimator.ofFloat(sparkleView, "alpha", 0f, 1f, 0f)
            val rotation = ObjectAnimator.ofFloat(sparkleView, "rotation", 0f, 180f)
            
            val duration = 800L + Random.nextLong(400)
            scaleX.duration = duration
            scaleY.duration = duration
            alpha.duration = duration
            rotation.duration = duration
            
            animatorSet.playTogether(scaleX, scaleY, alpha, rotation)
            animatorSet.interpolator = OvershootInterpolator()
            
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parent.removeView(sparkleView)
                }
            })
            
            animatorSet.start()
        }
    }

    /**
     * مؤثر الألعاب النارية
     */
    private suspend fun playFireworksEffect(parent: ViewGroup, effect: VisualEffect) {
        val fireworkCount = 3
        
        repeat(fireworkCount) { index ->
            delay(index * 800L)
            
            // نقطة انطلاق الألعاب النارية
            val centerX = parent.width * 0.5f + Random.nextFloat() * 200 - 100
            val centerY = parent.height * 0.3f + Random.nextFloat() * 100
            
            // إنشاء عدة جسيمات للانفجار
            repeat(12) { particleIndex ->
                val particleView = createFireworkParticleView()
                parent.addView(particleView)
                
                val angle = (particleIndex * 30).toDouble() * PI / 180
                val distance = 100f + Random.nextFloat() * 50
                
                val endX = centerX + cos(angle).toFloat() * distance
                val endY = centerY + sin(angle).toFloat() * distance
                
                particleView.x = centerX
                particleView.y = centerY
                
                val animatorSet = AnimatorSet()
                
                val moveX = ObjectAnimator.ofFloat(particleView, "x", centerX, endX)
                val moveY = ObjectAnimator.ofFloat(particleView, "y", centerY, endY)
                val alpha = ObjectAnimator.ofFloat(particleView, "alpha", 1f, 0f)
                val scale = ObjectAnimator.ofFloat(particleView, "scaleX", 1f, 0f)
                val scaleY = ObjectAnimator.ofFloat(particleView, "scaleY", 1f, 0f)
                
                moveX.duration = 1500
                moveY.duration = 1500
                alpha.duration = 1500
                scale.duration = 1500
                scaleY.duration = 1500
                
                alpha.startDelay = 500
                
                animatorSet.playTogether(moveX, moveY, alpha, scale, scaleY)
                animatorSet.interpolator = DecelerateInterpolator()
                
                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        parent.removeView(particleView)
                    }
                })
                
                animatorSet.start()
            }
        }
    }

    // دوال إنشاء العروض المختلفة
    private fun createHeartView(): ImageView {
        val imageView = ImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(60, 60)
        
        // إنشاء شكل قلب
        val drawable = ContextCompat.getDrawable(context, android.R.drawable.btn_star_big_on)
        drawable?.setTint(Color.parseColor("#FF69B4")) // لون وردي
        imageView.setImageDrawable(drawable)
        
        return imageView
    }

    private fun createStarView(): ImageView {
        val imageView = ImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(40, 40)
        
        val drawable = ContextCompat.getDrawable(context, android.R.drawable.btn_star_big_on)
        drawable?.setTint(Color.parseColor("#FFD700")) // لون ذهبي
        imageView.setImageDrawable(drawable)
        
        return imageView
    }

    private fun createSparkleView(): ImageView {
        val imageView = ImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(20, 20)
        
        val drawable = ContextCompat.getDrawable(context, android.R.drawable.btn_star_big_on)
        drawable?.setTint(Color.parseColor("#FFFFFF")) // لون أبيض
        imageView.setImageDrawable(drawable)
        
        return imageView
    }

    private fun createFireworkParticleView(): ImageView {
        val imageView = ImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(15, 15)
        
        val colors = arrayOf("#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF")
        val color = colors[Random.nextInt(colors.size)]
        
        val drawable = ContextCompat.getDrawable(context, android.R.drawable.btn_star_big_on)
        drawable?.setTint(Color.parseColor(color))
        imageView.setImageDrawable(drawable)
        
        return imageView
    }

    // دوال إنشاء المؤثرات المختلفة
    private fun createHeartEffect(level: Int): VisualEffect {
        return VisualEffect(
            type = "hearts",
            duration = when (level) {
                3 -> EFFECT_DURATION_LONG
                2 -> EFFECT_DURATION_MEDIUM
                else -> EFFECT_DURATION_SHORT
            },
            intensity = level,
            color = "#FF69B4",
            position = VisualEffect.Position.AROUND_CHARACTER
        )
    }

    private fun createStarEffect(level: Int): VisualEffect {
        return VisualEffect(
            type = "stars",
            duration = EFFECT_DURATION_MEDIUM,
            intensity = level,
            color = "#FFD700",
            position = VisualEffect.Position.AROUND_CHARACTER
        )
    }

    private fun createSparkleEffect(level: Int): VisualEffect {
        return VisualEffect(
            type = "sparkles",
            duration = EFFECT_DURATION_SHORT,
            intensity = level,
            color = "#FFFFFF",
            position = VisualEffect.Position.FULL_SCREEN
        )
    }

    private fun createFireworksEffect(): VisualEffect {
        return VisualEffect(
            type = "fireworks",
            duration = EFFECT_DURATION_LONG,
            intensity = 3,
            position = VisualEffect.Position.ABOVE_CHARACTER
        )
    }

    private fun createSpeechBubbleEffect(level: Int): VisualEffect {
        return VisualEffect(
            type = "speech_bubbles",
            duration = EFFECT_DURATION_MEDIUM,
            intensity = level,
            position = VisualEffect.Position.AROUND_CHARACTER
        )
    }

    private fun createArrowEffect(level: Int): VisualEffect {
        return VisualEffect(
            type = "arrows",
            duration = EFFECT_DURATION_MEDIUM,
            intensity = level,
            position = VisualEffect.Position.AROUND_CHARACTER
        )
    }

    private fun createEyeEffect(level: Int): VisualEffect {
        return VisualEffect(
            type = "eyes",
            duration = EFFECT_DURATION_SHORT,
            intensity = level,
            position = VisualEffect.Position.AROUND_CHARACTER
        )
    }

    private fun createNeonGlowEffect(level: Int): VisualEffect {
        return VisualEffect(
            type = "neon_glow",
            duration = EFFECT_DURATION_LONG,
            intensity = level,
            color = "#00FFFF",
            position = VisualEffect.Position.AROUND_CHARACTER
        )
    }

    private fun createStarShowerEffect(): VisualEffect {
        return VisualEffect(
            type = "star_shower",
            duration = EFFECT_DURATION_LONG,
            intensity = 3,
            color = "#FFD700",
            position = VisualEffect.Position.FULL_SCREEN
        )
    }

    private fun createVipGlowEffect(): VisualEffect {
        return VisualEffect(
            type = "vip_glow",
            duration = EFFECT_DURATION_LONG,
            intensity = 3,
            color = "#9400D3",
            position = VisualEffect.Position.AROUND_CHARACTER
        )
    }

    private fun createGoldenRainEffect(): VisualEffect {
        return VisualEffect(
            type = "golden_rain",
            duration = EFFECT_DURATION_LONG,
            intensity = 3,
            color = "#FFD700",
            position = VisualEffect.Position.FULL_SCREEN
        )
    }

    private fun createAchievementBadgeEffect(): VisualEffect {
        return VisualEffect(
            type = "achievement_badge",
            duration = EFFECT_DURATION_MEDIUM,
            intensity = 2,
            position = VisualEffect.Position.ABOVE_CHARACTER
        )
    }

    private fun createRippleEffect(): VisualEffect {
        return VisualEffect(
            type = "ripple",
            duration = EFFECT_DURATION_MEDIUM,
            intensity = 2,
            position = VisualEffect.Position.AROUND_CHARACTER
        )
    }

    // تنفيذ المؤثرات المتبقية (مبسطة)
    private suspend fun playSpeechBubbleEffect(parent: ViewGroup, effect: VisualEffect) {
        // تنفيذ مبسط لفقاعات الكلام
        playDefaultEffect(parent, effect)
    }

    private suspend fun playArrowEffect(parent: ViewGroup, effect: VisualEffect) {
        // تنفيذ مبسط للأسهم
        playDefaultEffect(parent, effect)
    }

    private suspend fun playEyeEffect(parent: ViewGroup, effect: VisualEffect) {
        // تنفيذ مبسط للعيون
        playDefaultEffect(parent, effect)
    }

    private suspend fun playNeonGlowEffect(parent: ViewGroup, effect: VisualEffect) {
        // تنفيذ مبسط للتوهج النيوني
        playDefaultEffect(parent, effect)
    }

    private suspend fun playStarShowerEffect(parent: ViewGroup, effect: VisualEffect) {
        // تنفيذ مبسط لمطر النجوم
        playStarEffect(parent, effect)
    }

    private suspend fun playVipGlowEffect(parent: ViewGroup, effect: VisualEffect) {
        // تنفيذ مبسط للتوهج الـ VIP
        playDefaultEffect(parent, effect)
    }

    private suspend fun playGoldenRainEffect(parent: ViewGroup, effect: VisualEffect) {
        // تنفيذ مبسط للمطر الذهبي
        playSparkleEffect(parent, effect)
    }

    private suspend fun playAchievementBadgeEffect(parent: ViewGroup, effect: VisualEffect) {
        // تنفيذ مبسط لشارة الإنجاز
        playDefaultEffect(parent, effect)
    }

    private suspend fun playRippleEffect(parent: ViewGroup, effect: VisualEffect) {
        // تنفيذ مبسط للموجات
        playDefaultEffect(parent, effect)
    }

    private suspend fun playDefaultEffect(parent: ViewGroup, effect: VisualEffect) {
        // مؤثر افتراضي بسيط
        playSparkleEffect(parent, effect)
    }

    /**
     * إيقاف جميع المؤثرات النشطة
     */
    fun stopAllEffects() {
        scope.coroutineContext.cancelChildren()
        activeEffects.clear()
    }

    /**
     * فئة داخلية لتتبع المؤثرات النشطة
     */
    private data class EffectInstance(
        val id: String,
        val effect: VisualEffect,
        val startTime: Long,
        val views: List<View>
    )
}

