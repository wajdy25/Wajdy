package com.animecharacter.services

import android.content.Context
import android.graphics.*
import android.util.Log
import com.animecharacter.models.Character
import com.animecharacter.managers.EmotionalResponseManager
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.*
import java.util.concurrent.ConcurrentHashMap

/**
 * خدمة تحريك الشخصية المتقدمة
 * تدعم Lip Sync، تعابير الوجه، والحركات الديناميكية
 */
class CharacterAnimationService(private val context: Context) {
    
    companion object {
        private const val TAG = "CharacterAnimationService"
        private const val ANIMATION_FPS = 24
        private const val FRAME_DURATION = 1000L / ANIMATION_FPS
    }
    
    // أنواع الرسوم المتحركة
    enum class AnimationType {
        IDLE,           // حالة الخمول
        TALKING,        // أثناء الكلام
        LISTENING,      // أثناء الاستماع
        THINKING,       // أثناء التفكير
        HAPPY,          // سعيد
        SAD,            // حزين
        SURPRISED,      // متفاجئ
        ANGRY,          // غاضب
        EXCITED,        // متحمس
        CONFUSED,       // محتار
        WAVING,         // يلوح
        NODDING,        // يومئ بالرأس
        SHAKING_HEAD    // يهز رأسه
    }
    
    // نقاط الوجه للتحريك
    data class FacialLandmarks(
        val leftEye: PointF,
        val rightEye: PointF,
        val nose: PointF,
        val mouthCenter: PointF,
        val mouthLeft: PointF,
        val mouthRight: PointF,
        val leftEyebrow: PointF,
        val rightEyebrow: PointF,
        val chin: PointF
    )
    
    // حالة الرسوم المتحركة
    data class AnimationState(
        val currentAnimation: AnimationType,
        val progress: Float, // 0.0 to 1.0
        val facialLandmarks: FacialLandmarks,
        val blendShapes: Map<String, Float>, // للتعابير المختلطة
        val bodyPose: BodyPose,
        val lipSyncData: LipSyncData?
    )
    
    // وضعية الجسم
    data class BodyPose(
        val headRotation: Float,
        val headTilt: Float,
        val shoulderHeight: Float,
        val armPosition: Pair<Float, Float>, // left, right
        val bodyLean: Float
    )
    
    // بيانات مزامنة الشفاه
    data class LipSyncData(
        val currentViseme: String,
        val intensity: Float,
        val mouthOpenness: Float,
        val mouthWidth: Float,
        val tonguePosition: Float
    )
    
    // Visemes للغة العربية والإنجليزية
    private val visemeMap = mapOf(
        // أصوات العربية
        "ا" to LipSyncData("A", 0.8f, 0.7f, 0.5f, 0.3f),
        "ب" to LipSyncData("B", 0.9f, 0.1f, 0.3f, 0.2f),
        "ت" to LipSyncData("T", 0.7f, 0.3f, 0.4f, 0.8f),
        "ث" to LipSyncData("TH", 0.6f, 0.4f, 0.5f, 0.9f),
        "ج" to LipSyncData("J", 0.8f, 0.5f, 0.6f, 0.4f),
        "ح" to LipSyncData("H", 0.5f, 0.6f, 0.7f, 0.3f),
        "د" to LipSyncData("D", 0.7f, 0.3f, 0.4f, 0.7f),
        "ر" to LipSyncData("R", 0.6f, 0.4f, 0.5f, 0.8f),
        "س" to LipSyncData("S", 0.5f, 0.2f, 0.3f, 0.6f),
        "ش" to LipSyncData("SH", 0.6f, 0.3f, 0.2f, 0.5f),
        "ع" to LipSyncData("AA", 0.9f, 0.8f, 0.6f, 0.4f),
        "ف" to LipSyncData("F", 0.7f, 0.2f, 0.4f, 0.3f),
        "ق" to LipSyncData("Q", 0.8f, 0.5f, 0.5f, 0.2f),
        "ك" to LipSyncData("K", 0.6f, 0.3f, 0.4f, 0.1f),
        "ل" to LipSyncData("L", 0.7f, 0.4f, 0.5f, 0.9f),
        "م" to LipSyncData("M", 0.8f, 0.1f, 0.3f, 0.2f),
        "ن" to LipSyncData("N", 0.6f, 0.3f, 0.4f, 0.7f),
        "ه" to LipSyncData("H", 0.5f, 0.6f, 0.7f, 0.3f),
        "و" to LipSyncData("W", 0.9f, 0.3f, 0.2f, 0.4f),
        "ي" to LipSyncData("Y", 0.7f, 0.4f, 0.8f, 0.5f),
        
        // أصوات إنجليزية إضافية
        "A" to LipSyncData("A", 0.8f, 0.7f, 0.5f, 0.3f),
        "E" to LipSyncData("E", 0.6f, 0.5f, 0.7f, 0.4f),
        "I" to LipSyncData("I", 0.7f, 0.4f, 0.8f, 0.5f),
        "O" to LipSyncData("O", 0.9f, 0.6f, 0.3f, 0.3f),
        "U" to LipSyncData("U", 0.8f, 0.4f, 0.2f, 0.4f),
        
        // صمت
        "SILENCE" to LipSyncData("SILENCE", 0.0f, 0.1f, 0.5f, 0.2f)
    )
    
    private var currentAnimationState = AnimationState(
        currentAnimation = AnimationType.IDLE,
        progress = 0f,
        facialLandmarks = getDefaultFacialLandmarks(),
        blendShapes = getDefaultBlendShapes(),
        bodyPose = getDefaultBodyPose(),
        lipSyncData = null
    )
    
    private val animationCoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isAnimating = false
    private val animationQueue = mutableListOf<AnimationType>()
    private val blendShapeTargets = ConcurrentHashMap<String, Float>()
    
    /**
     * بدء تشغيل خدمة الرسوم المتحركة
     */
    fun startAnimationService() {
        if (isAnimating) return
        
        isAnimating = true
        animationCoroutineScope.launch {
            animationLoop()
        }
        
        Log.d(TAG, "Animation service started")
    }
    
    /**
     * إيقاف خدمة الرسوم المتحركة
     */
    fun stopAnimationService() {
        isAnimating = false
        animationQueue.clear()
        Log.d(TAG, "Animation service stopped")
    }
    
    /**
     * تشغيل رسوم متحركة محددة
     */
    fun playAnimation(animationType: AnimationType, priority: Boolean = false) {
        if (priority) {
            animationQueue.add(0, animationType)
        } else {
            animationQueue.add(animationType)
        }
        
        Log.d(TAG, "Animation queued: $animationType")
    }
    
    /**
     * تطبيق مزامنة الشفاه للنص المنطوق
     */
    fun applyLipSync(text: String, audioData: ByteArray? = null) {
        animationCoroutineScope.launch {
            val visemeSequence = textToVisemes(text)
            
            for ((index, viseme) in visemeSequence.withIndex()) {
                val lipSyncData = visemeMap[viseme] ?: visemeMap["SILENCE"]!!
                
                currentAnimationState = currentAnimationState.copy(
                    lipSyncData = lipSyncData
                )
                
                // مدة كل viseme (تقريبية)
                val duration = calculateVisemeDuration(viseme, audioData)
                delay(duration)
            }
            
            // العودة للحالة الطبيعية
            currentAnimationState = currentAnimationState.copy(
                lipSyncData = visemeMap["SILENCE"]
            )
        }
    }
    
    /**
     * تطبيق تعبير عاطفي
     */
    fun applyEmotionalExpression(response: EmotionalResponseManager.EmotionalResponse) {
        animationCoroutineScope.launch {
            // تطبيق التغييرات المرئية
            applyVisualChanges(response.visualChanges)
            
            // تطبيق التغييرات السلوكية
            applyBehavioralChanges(response.behavioralChanges)
            
            // الحفاظ على التعبير لمدة محددة
            delay(response.duration)
            
            // العودة للحالة الطبيعية تدريجياً
            returnToNeutralExpression()
        }
    }
    
    /**
     * تخصيص مظهر الشخصية
     */
    fun customizeCharacterAppearance(
        character: Character,
        customizations: Map<String, Any>
    ) {
        // تطبيق تخصيصات الألوان
        customizations["hairColor"]?.let { color ->
            // تغيير لون الشعر
            updateCharacterColor("hair", color as Int)
        }
        
        customizations["eyeColor"]?.let { color ->
            // تغيير لون العيون
            updateCharacterColor("eyes", color as Int)
        }
        
        customizations["skinTone"]?.let { color ->
            // تغيير لون البشرة
            updateCharacterColor("skin", color as Int)
        }
        
        customizations["outfit"]?.let { outfit ->
            // تغيير الملابس
            updateCharacterOutfit(outfit as String)
        }
        
        customizations["accessories"]?.let { accessories ->
            // إضافة/إزالة الإكسسوارات
            updateCharacterAccessories(accessories as List<String>)
        }
        
        Log.d(TAG, "Character appearance customized")
    }
    
    /**
     * الحصول على حالة الرسوم المتحركة الحالية
     */
    fun getCurrentAnimationState(): AnimationState {
        return currentAnimationState
    }
    
    /**
     * حلقة الرسوم المتحركة الرئيسية
     */
    private suspend fun animationLoop() {
        while (isAnimating) {
            // معالجة قائمة انتظار الرسوم المتحركة
            if (animationQueue.isNotEmpty()) {
                val nextAnimation = animationQueue.removeAt(0)
                executeAnimation(nextAnimation)
            }
            
            // تحديث الرسوم المتحركة الحالية
            updateCurrentAnimation()
            
            // تطبيق blend shapes
            applyBlendShapes()
            
            // انتظار الإطار التالي
            delay(FRAME_DURATION)
        }
    }
    
    /**
     * تنفيذ رسوم متحركة محددة
     */
    private suspend fun executeAnimation(animationType: AnimationType) {
        currentAnimationState = currentAnimationState.copy(
            currentAnimation = animationType,
            progress = 0f
        )
        
        val animationDuration = getAnimationDuration(animationType)
        val frameCount = (animationDuration / FRAME_DURATION).toInt()
        
        for (frame in 0..frameCount) {
            val progress = frame.toFloat() / frameCount
            
            currentAnimationState = currentAnimationState.copy(
                progress = progress,
                facialLandmarks = calculateFacialLandmarks(animationType, progress),
                bodyPose = calculateBodyPose(animationType, progress)
            )
            
            delay(FRAME_DURATION)
        }
        
        // العودة للحالة الخاملة
        if (animationType != AnimationType.IDLE) {
            currentAnimationState = currentAnimationState.copy(
                currentAnimation = AnimationType.IDLE,
                progress = 0f
            )
        }
    }
    
    /**
     * تحويل النص إلى تسلسل visemes
     */
    private fun textToVisemes(text: String): List<String> {
        val visemes = mutableListOf<String>()
        val cleanText = text.replace(Regex("[^\\p{L}\\s]"), "").lowercase()
        
        for (char in cleanText) {
            when {
                char.isWhitespace() -> visemes.add("SILENCE")
                char.toString() in visemeMap -> visemes.add(char.toString())
                else -> {
                    // محاولة تقريب الصوت
                    val approximateViseme = approximateViseme(char)
                    visemes.add(approximateViseme)
                }
            }
        }
        
        return visemes
    }
    
    /**
     * تقريب الصوت لأقرب viseme
     */
    private fun approximateViseme(char: Char): String {
        return when (char) {
            in 'a'..'z' -> {
                // تقريب بسيط للأحرف الإنجليزية
                when (char) {
                    'a', 'e', 'i', 'o', 'u' -> char.toString().uppercase()
                    'b', 'p', 'm' -> "ب"
                    't', 'd', 'n' -> "ت"
                    'f', 'v' -> "ف"
                    's', 'z' -> "س"
                    'r' -> "ر"
                    'l' -> "ل"
                    else -> "ا"
                }
            }
            else -> "SILENCE"
        }
    }
    
    /**
     * حساب مدة الـ viseme
     */
    private fun calculateVisemeDuration(viseme: String, audioData: ByteArray?): Long {
        // مدة افتراضية بناءً على نوع الصوت
        return when (viseme) {
            "SILENCE" -> 100L
            in listOf("ا", "ع", "A", "E", "I", "O", "U") -> 150L // أصوات العلة
            in listOf("ب", "م", "B", "M", "P") -> 120L // أصوات شفوية
            in listOf("ت", "د", "ن", "T", "D", "N") -> 100L // أصوات لسانية
            else -> 110L // أصوات أخرى
        }
    }
    
    /**
     * تطبيق التغييرات المرئية
     */
    private fun applyVisualChanges(visualChanges: EmotionalResponseManager.VisualChanges) {
        // تطبيق تعبير الوجه
        when (visualChanges.facialExpression) {
            "smile_big" -> {
                blendShapeTargets["mouthSmile"] = 0.9f
                blendShapeTargets["eyeSquint"] = 0.3f
            }
            "concerned" -> {
                blendShapeTargets["browDown"] = 0.6f
                blendShapeTargets["mouthFrown"] = 0.4f
            }
            "surprised" -> {
                blendShapeTargets["browUp"] = 0.8f
                blendShapeTargets["eyeWide"] = 0.7f
                blendShapeTargets["mouthOpen"] = 0.5f
            }
            "excited" -> {
                blendShapeTargets["mouthSmile"] = 1.0f
                blendShapeTargets["eyeWide"] = 0.6f
                blendShapeTargets["browUp"] = 0.4f
            }
        }
        
        // تطبيق تعبير العيون
        when (visualChanges.eyeExpression) {
            "bright_eyes" -> blendShapeTargets["eyeBrightness"] = 1.0f
            "soft_eyes" -> blendShapeTargets["eyeSoftness"] = 0.8f
            "wide_eyes" -> blendShapeTargets["eyeWide"] = 0.9f
            "sparkling_eyes" -> blendShapeTargets["eyeSparkle"] = 1.0f
        }
        
        // تطبيق تعبير الفم
        when (visualChanges.mouthExpression) {
            "wide_smile" -> blendShapeTargets["mouthSmile"] = 1.0f
            "slight_frown" -> blendShapeTargets["mouthFrown"] = 0.3f
            "open_mouth" -> blendShapeTargets["mouthOpen"] = 0.6f
            "big_grin" -> blendShapeTargets["mouthGrin"] = 0.9f
        }
    }
    
    /**
     * تطبيق التغييرات السلوكية
     */
    private fun applyBehavioralChanges(behavioralChanges: EmotionalResponseManager.BehavioralChanges) {
        // تطبيق نمط التفاعل
        when (behavioralChanges.interactionStyle) {
            "playful" -> playAnimation(AnimationType.WAVING)
            "caring" -> playAnimation(AnimationType.NODDING)
            "dynamic" -> playAnimation(AnimationType.EXCITED)
            "zen" -> playAnimation(AnimationType.IDLE)
        }
    }
    
    /**
     * العودة للتعبير المحايد
     */
    private suspend fun returnToNeutralExpression() {
        val neutralTargets = getDefaultBlendShapes()
        
        // تدريجياً إعادة تعيين جميع blend shapes
        for ((key, targetValue) in neutralTargets) {
            blendShapeTargets[key] = targetValue
        }
        
        // انتظار حتى اكتمال الانتقال
        delay(1000L)
    }
    
    /**
     * تحديث الرسوم المتحركة الحالية
     */
    private fun updateCurrentAnimation() {
        // تحديث تدريجي للرسوم المتحركة بناءً على النوع الحالي
        when (currentAnimationState.currentAnimation) {
            AnimationType.IDLE -> updateIdleAnimation()
            AnimationType.TALKING -> updateTalkingAnimation()
            AnimationType.LISTENING -> updateListeningAnimation()
            else -> { /* رسوم متحركة أخرى */ }
        }
    }
    
    /**
     * تحديث رسوم متحركة الخمول
     */
    private fun updateIdleAnimation() {
        val time = System.currentTimeMillis() / 1000.0
        
        // حركة تنفس بسيطة
        val breathingOffset = sin(time * 0.5) * 0.02f
        
        currentAnimationState = currentAnimationState.copy(
            bodyPose = currentAnimationState.bodyPose.copy(
                shoulderHeight = breathingOffset
            )
        )
        
        // رمش عشوائي
        if (Math.random() < 0.02) { // 2% احتمال في كل إطار
            blendShapeTargets["eyeBlink"] = 1.0f
            animationCoroutineScope.launch {
                delay(150L)
                blendShapeTargets["eyeBlink"] = 0.0f
            }
        }
    }
    
    /**
     * تحديث رسوم متحركة الكلام
     */
    private fun updateTalkingAnimation() {
        // حركة رأس بسيطة أثناء الكلام
        val time = System.currentTimeMillis() / 1000.0
        val headMovement = sin(time * 2.0) * 0.05f
        
        currentAnimationState = currentAnimationState.copy(
            bodyPose = currentAnimationState.bodyPose.copy(
                headRotation = headMovement.toFloat()
            )
        )
    }
    
    /**
     * تحديث رسوم متحركة الاستماع
     */
    private fun updateListeningAnimation() {
        // إمالة رأس بسيطة للإشارة للاستماع
        currentAnimationState = currentAnimationState.copy(
            bodyPose = currentAnimationState.bodyPose.copy(
                headTilt = 0.1f
            )
        )
    }
    
    /**
     * تطبيق blend shapes
     */
    private fun applyBlendShapes() {
        val currentBlendShapes = currentAnimationState.blendShapes.toMutableMap()
        
        // تحديث تدريجي لكل blend shape
        for ((key, targetValue) in blendShapeTargets) {
            val currentValue = currentBlendShapes[key] ?: 0f
            val difference = targetValue - currentValue
            val step = difference * 0.1f // سرعة الانتقال
            
            currentBlendShapes[key] = currentValue + step
        }
        
        currentAnimationState = currentAnimationState.copy(
            blendShapes = currentBlendShapes
        )
    }
    
    // دوال مساعدة للحصول على القيم الافتراضية
    private fun getDefaultFacialLandmarks(): FacialLandmarks {
        return FacialLandmarks(
            leftEye = PointF(0.3f, 0.4f),
            rightEye = PointF(0.7f, 0.4f),
            nose = PointF(0.5f, 0.5f),
            mouthCenter = PointF(0.5f, 0.7f),
            mouthLeft = PointF(0.4f, 0.7f),
            mouthRight = PointF(0.6f, 0.7f),
            leftEyebrow = PointF(0.3f, 0.3f),
            rightEyebrow = PointF(0.7f, 0.3f),
            chin = PointF(0.5f, 0.9f)
        )
    }
    
    private fun getDefaultBlendShapes(): Map<String, Float> {
        return mapOf(
            "mouthSmile" to 0.0f,
            "mouthFrown" to 0.0f,
            "mouthOpen" to 0.0f,
            "mouthGrin" to 0.0f,
            "eyeWide" to 0.0f,
            "eyeSquint" to 0.0f,
            "eyeBlink" to 0.0f,
            "eyeBrightness" to 0.5f,
            "eyeSoftness" to 0.5f,
            "eyeSparkle" to 0.0f,
            "browUp" to 0.0f,
            "browDown" to 0.0f
        )
    }
    
    private fun getDefaultBodyPose(): BodyPose {
        return BodyPose(
            headRotation = 0f,
            headTilt = 0f,
            shoulderHeight = 0f,
            armPosition = Pair(0f, 0f),
            bodyLean = 0f
        )
    }
    
    private fun getAnimationDuration(animationType: AnimationType): Long {
        return when (animationType) {
            AnimationType.WAVING -> 2000L
            AnimationType.NODDING -> 1500L
            AnimationType.SHAKING_HEAD -> 1500L
            AnimationType.EXCITED -> 3000L
            else -> 1000L
        }
    }
    
    private fun calculateFacialLandmarks(animationType: AnimationType, progress: Float): FacialLandmarks {
        val base = getDefaultFacialLandmarks()
        
        return when (animationType) {
            AnimationType.NODDING -> {
                val headMovement = sin(progress * PI * 2).toFloat() * 0.05f
                base.copy(
                    leftEye = PointF(base.leftEye.x, base.leftEye.y + headMovement),
                    rightEye = PointF(base.rightEye.x, base.rightEye.y + headMovement),
                    nose = PointF(base.nose.x, base.nose.y + headMovement),
                    mouthCenter = PointF(base.mouthCenter.x, base.mouthCenter.y + headMovement)
                )
            }
            AnimationType.SHAKING_HEAD -> {
                val headMovement = sin(progress * PI * 4).toFloat() * 0.03f
                base.copy(
                    leftEye = PointF(base.leftEye.x + headMovement, base.leftEye.y),
                    rightEye = PointF(base.rightEye.x + headMovement, base.rightEye.y),
                    nose = PointF(base.nose.x + headMovement, base.nose.y),
                    mouthCenter = PointF(base.mouthCenter.x + headMovement, base.mouthCenter.y)
                )
            }
            else -> base
        }
    }
    
    private fun calculateBodyPose(animationType: AnimationType, progress: Float): BodyPose {
        val base = getDefaultBodyPose()
        
        return when (animationType) {
            AnimationType.WAVING -> {
                val armMovement = sin(progress * PI * 3).toFloat() * 0.5f
                base.copy(armPosition = Pair(armMovement, base.armPosition.second))
            }
            AnimationType.EXCITED -> {
                val bounce = abs(sin(progress * PI * 4)).toFloat() * 0.1f
                base.copy(shoulderHeight = bounce)
            }
            else -> base
        }
    }
    
    private fun updateCharacterColor(part: String, color: Int) {
        // تطبيق لون جديد على جزء من الشخصية
        Log.d(TAG, "Updated $part color to $color")
    }
    
    private fun updateCharacterOutfit(outfit: String) {
        // تغيير ملابس الشخصية
        Log.d(TAG, "Updated character outfit to $outfit")
    }
    
    private fun updateCharacterAccessories(accessories: List<String>) {
        // تحديث إكسسوارات الشخصية
        Log.d(TAG, "Updated character accessories: $accessories")
    }
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        stopAnimationService()
        animationCoroutineScope.cancel()
    }
}

