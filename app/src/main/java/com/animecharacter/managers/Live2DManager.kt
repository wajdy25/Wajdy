package com.animecharacter.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.animecharacter.models.Character
import com.animecharacter.models.CustomizationOption
import com.animecharacter.models.CustomizationOption.CustomizationType
import com.animecharacter.services.Live2DFloatingWindowService
import com.animecharacter.utils.PreferencesHelper
import kotlinx.coroutines.*

class Live2DManager(private val context: Context) {

    companion object {
        private const val TAG = "Live2DManager"
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001

        @Volatile
        private var INSTANCE: Live2DManager? = null

        fun getInstance(context: Context): Live2DManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Live2DManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // الحالة والإعدادات
    private var isInitialized = false
    private var isCharacterVisible = false
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // قوائم الشخصيات وإعدادات التخصيص
    private val availableCharacters = mutableListOf<Character>()
    private var activeCharacter: Character? = null

    // إعدادات الشخصية
    private var characterScale = 0.06f
    private var characterAlpha = 1.0f
    private var characterX = 100
    private var characterY = 100
    
    // قوائم التعبيرات والحركات المتاحة
    private val availableExpressions = listOf(
        "exp_01", "exp_02", "exp_03", "exp_04",
        "exp_05", "exp_06", "exp_07", "exp_08"
    )
    
    private val availableMotions = mapOf(
        "idle" to listOf("mtn_01"),
        "normal" to listOf("mtn_02", "mtn_03", "mtn_04"),
        "special" to listOf("special_01", "special_02", "special_03")
    )
    
    /**
     * تهيئة المدير
     */
    fun initialize(): Boolean {
        return try {
            loadAvailableCharacters()
            loadSettings()
            isInitialized = true
            Log.d(TAG, "تم تهيئة مدير Live2D بنجاح")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تهيئة مدير Live2D", e)
            false
        }
    }
    
    /**
     * بدء عرض الشخصية
     */
    fun startCharacter(): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "المدير غير مهيأ")
            return false
        }
        
        if (!checkOverlayPermission()) {
            Log.w(TAG, "لا توجد صلاحية العرض فوق التطبيقات الأخرى")
            return false
        }
        
        return try {
            val intent = Intent(context, Live2DFloatingWindowService::class.java).apply {
                action = Live2DFloatingWindowService.ACTION_START
            }
            
            context.startService(intent)
            isCharacterVisible = true
            
            Log.d(TAG, "تم بدء عرض الشخصية")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في بدء عرض الشخصية", e)
            false
        }
    }
    
    /**
     * إيقاف عرض الشخصية
     */
    fun stopCharacter(): Boolean {
        if (!isCharacterVisible) return true
        
        return try {
            val intent = Intent(context, Live2DFloatingWindowService::class.java).apply {
                action = Live2DFloatingWindowService.ACTION_STOP
            }
            
            context.startService(intent)
            isCharacterVisible = false
            
            Log.d(TAG, "تم إيقاف عرض الشخصية")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في إيقاف عرض الشخصية", e)
            false
        }
    }
    
    /**
     * تغيير حجم الشخصية
     */
    fun setCharacterScale(scale: Float): Boolean {
        val constrainedScale = scale.coerceIn(0.03f, 0.15f)
        if (characterScale == constrainedScale) return true
        
        characterScale = constrainedScale
        
        return try {
            val intent = Intent(context, Live2DFloatingWindowService::class.java).apply {
                action = Live2DFloatingWindowService.ACTION_UPDATE_SIZE
                putExtra(Live2DFloatingWindowService.EXTRA_SCALE, characterScale)
            }
            
            context.startService(intent)
            saveSettings()
            
            Log.d(TAG, "تم تغيير حجم الشخصية إلى: $characterScale")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تغيير حجم الشخصية", e)
            false
        }
    }
    
    /**
     * تغيير موقع الشخصية
     */
    fun setCharacterPosition(x: Int, y: Int): Boolean {
        characterX = x
        characterY = y
        
        return try {
            val intent = Intent(context, Live2DFloatingWindowService::class.java).apply {
                action = Live2DFloatingWindowService.ACTION_UPDATE_POSITION
                putExtra(Live2DFloatingWindowService.EXTRA_X, x)
                putExtra(Live2DFloatingWindowService.EXTRA_Y, y)
            }
            
            context.startService(intent)
            saveSettings()
            
            Log.d(TAG, "تم تغيير موقع الشخصية إلى: ($x, $y)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تغيير موقع الشخصية", e)
            false
        }
    }
    
    /**
     * تغيير شفافية الشخصية
     */
    fun setCharacterAlpha(alpha: Float): Boolean {
        val constrainedAlpha = alpha.coerceIn(0f, 1f)
        if (characterAlpha == constrainedAlpha) return true
        
        characterAlpha = constrainedAlpha
        
        return try {
            val intent = Intent(context, Live2DFloatingWindowService::class.java).apply {
                action = Live2DFloatingWindowService.ACTION_UPDATE_ALPHA
                putExtra(Live2DFloatingWindowService.EXTRA_ALPHA, characterAlpha)
            }
            
            context.startService(intent)
            saveSettings()
            
            Log.d(TAG, "تم تغيير شفافية الشخصية إلى: $characterAlpha")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تغيير شفافية الشخصية", e)
            false
        }
    }
    
    /**
     * تغيير تعبير الشخصية
     */
    fun setExpression(expressionName: String): Boolean {
        if (!availableExpressions.contains(expressionName)) {
            Log.w(TAG, "التعبير غير متاح: $expressionName")
            return false
        }
        
        // إرسال أمر تغيير التعبير عبر broadcast أو آلية أخرى
        // هذا مبسط - في التطبيق الحقيقي نحتاج لآلية تواصل مع الخدمة
        
        Log.d(TAG, "تم تغيير التعبير إلى: $expressionName")
        return true
    }
    
    /**
     * تشغيل حركة
     */
    fun playMotion(motionGroup: String, motionIndex: Int = -1): Boolean {
        if (!availableMotions.containsKey(motionGroup)) {
            Log.w(TAG, "مجموعة الحركات غير متاحة: $motionGroup")
            return false
        }
        
        val motions = availableMotions[motionGroup]!!
        val selectedMotion = if (motionIndex >= 0 && motionIndex < motions.size) {
            motions[motionIndex]
        } else {
            motions.random()
        }
        
        // إرسال أمر تشغيل الحركة
        Log.d(TAG, "تم تشغيل الحركة: $motionGroup -> $selectedMotion")
        return true
    }
    
    /**
     * تشغيل تعبير عشوائي
     */
    fun playRandomExpression(): Boolean {
        val randomExpression = availableExpressions.random()
        return setExpression(randomExpression)
    }
    
    /**
     * تشغيل حركة عشوائية
     */
    fun playRandomMotion(): Boolean {
        val randomGroup = availableMotions.keys.random()
        return playMotion(randomGroup)
    }
    
    /**
     * تشغيل تفاعل بناءً على المشاعر
     */
    fun playEmotionBasedInteraction(emotion: String): Boolean {
        return when (emotion.lowercase()) {
            "happy", "joy", "excited" -> {
                setExpression("exp_01") // تعبير سعيد
                playMotion("special", 0) // حركة احتفال
            }
            "sad", "disappointed" -> {
                setExpression("exp_02") // تعبير حزين
                playMotion("idle") // حركة هادئة
            }
            "surprised", "amazed" -> {
                setExpression("exp_03") // تعبير مندهش
                playMotion("normal", 0) // حركة مفاجأة
            }
            "angry", "frustrated" -> {
                setExpression("exp_04") // تعبير غاضب
                playMotion("normal", 1) // حركة انزعاج
            }
            "thinking", "confused" -> {
                setExpression("exp_05") // تعبير تفكير
                playMotion("idle") // حركة تأمل
            }
            "love", "affection" -> {
                setExpression("exp_06") // تعبير حب
                playMotion("special", 1) // حركة رومانسية
            }
            else -> {
                playRandomExpression()
                playRandomMotion()
            }
        }
    }
    
    /**
     * فحص صلاحية العرض فوق التطبيقات الأخرى
     */
    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * طلب صلاحية العرض فوق التطبيقات الأخرى
     */
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
        }
    }
    
    /**
     * تحميل الإعدادات
     */
    private fun loadSettings() {
        val prefs = PreferencesHelper(context)
        
        characterScale = prefs.getFloat("live2d_scale", 0.06f)
        characterAlpha = prefs.getFloat("live2d_alpha", 1.0f)
        characterX = prefs.getInt("live2d_position_x", 100)
        characterY = prefs.getInt("live2d_position_y", 100)
        
        Log.d(TAG, "تم تحميل الإعدادات")
    }
    
    /**
     * حفظ الإعدادات
     */
    private fun saveSettings() {
        val prefs = PreferencesHelper(context)
        
        prefs.putFloat("live2d_scale", characterScale)
        prefs.putFloat("live2d_alpha", characterAlpha)
        prefs.putInt("live2d_position_x", characterX)
        prefs.putInt("live2d_position_y", characterY)
        
        Log.d(TAG, "تم حفظ الإعدادات")
    }
    
    /**
     * الحصول على الإعدادات الحالية
     */
    fun getCurrentSettings(): Map<String, Any> {
        return mapOf(
            "scale" to characterScale,
            "alpha" to characterAlpha,
            "x" to characterX,
            "y" to characterY,
            "visible" to isCharacterVisible,
            "initialized" to isInitialized
        )
    }
    
    /**
     * الحصول على التعبيرات المتاحة
     */
    fun getAvailableExpressions(): List<String> = availableExpressions
    
    /**
     * الحصول على الحركات المتاحة
     */
    fun getAvailableMotions(): Map<String, List<String>> = availableMotions
    
    /**
     * فحص حالة الشخصية
     */
    fun isCharacterVisible(): Boolean = isCharacterVisible
    
    /**
     * فحص حالة التهيئة
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        stopCharacter()
        managerScope.cancel()
        
        Log.d(TAG, "تم تنظيف موارد المدير")
    }
}



    /**
     * تحميل الشخصيات المتاحة (بيانات وهمية حالياً)
     */
    private fun loadAvailableCharacters() {
        // مثال على شخصية افتراضية
        val defaultCharacter = Character(
            id = "mao_pro",
            name = "ماو برو",
            description = "شخصية أنمي افتراضية لطيفة وذكية.",
            modelPath = "live2d/mao_pro/mao_pro.model3.json",
            previewImagePath = "live2d/mao_pro/mao_pro_preview.png",
            defaultAnimations = mapOf(
                "idle" to "mtn_01",
                "happy" to "mtn_02",
                "sad" to "mtn_03",
                "angry" to "mtn_04"
            ),
            customizationOptions = listOf(
                CustomizationOption(
                    id = "background",
                    name = "الخلفية",
                    type = CustomizationType.BACKGROUND,
                    values = listOf("default_background", "city_background", "nature_background"),
                    currentValue = "default_background"
                )
            )
        )
        availableCharacters.add(defaultCharacter)
        activeCharacter = defaultCharacter // تعيين الشخصية الافتراضية كنشطة
        Log.d(TAG, "تم تحميل ${availableCharacters.size} شخصية متاحة.")
    }




    /**
     * تبديل الشخصية النشطة
     */
    fun switchCharacter(characterId: String): Boolean {
        val characterToSwitch = availableCharacters.find { it.id == characterId }
        return if (characterToSwitch != null) {
            activeCharacter = characterToSwitch
            val intent = Intent(context, Live2DFloatingWindowService::class.java).apply {
                action = Live2DFloatingWindowService.ACTION_SWITCH_CHARACTER
                putExtra(Live2DFloatingWindowService.EXTRA_CHARACTER_ID, characterId)
            }
            context.startService(intent)
            Log.d(TAG, "تم تبديل الشخصية إلى: ${characterToSwitch.name}")
            true
        } else {
            Log.w(TAG, "الشخصية ذات المعرف $characterId غير موجودة.")
            false
        }
    }




    /**
     * تخصيص خيار معين للشخصية النشطة
     */
    fun customizeCharacter(optionId: String, value: String): Boolean {
        val character = activeCharacter ?: return false
        val option = character.customizationOptions.find { it.id == optionId }

        return if (option != null && option.values.contains(value)) {
            val updatedOptions = character.customizationOptions.map { 
                if (it.id == optionId) it.copy(currentValue = value) else it 
            }
            activeCharacter = character.copy(customizationOptions = updatedOptions)
            val intent = Intent(context, Live2DFloatingWindowService::class.java).apply {
                action = Live2DFloatingWindowService.ACTION_CUSTOMIZE_CHARACTER
                putExtra(Live2DFloatingWindowService.EXTRA_CUSTOMIZATION_OPTION_ID, optionId)
                putExtra(Live2DFloatingWindowService.EXTRA_CUSTOMIZATION_VALUE, value)
            }
            context.startService(intent)
            Log.d(TAG, "تم تخصيص ${option.name} لـ ${character.name} بالقيمة: $value")
            true
        } else {
            Log.w(TAG, "خيار التخصيص $optionId أو القيمة $value غير صالحة.")
            false
        }
    }

    /**
     * الحصول على قائمة الشخصيات المتاحة
     */
    fun getAvailableCharacters(): List<Character> {
        return availableCharacters
    }

    /**
     * الحصول على الشخصية النشطة حالياً
     */
    fun getActiveCharacter(): Character? {
        return activeCharacter
    }


