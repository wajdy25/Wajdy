package com.animecharacter.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "anime_character_prefs"
        
        // مفاتيح الإعدادات
        private const val KEY_SELECTED_CHARACTER = "selected_character"
        private const val KEY_CHARACTER_SIZE = "character_size"
        private const val KEY_CHARACTER_TRANSPARENCY = "character_transparency"
        private const val KEY_CHARACTER_POSITION_X = "character_position_x"
        private const val KEY_CHARACTER_POSITION_Y = "character_position_y"
        private const val KEY_PERSONALITY_MODE = "personality_mode"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_VOICE_LANGUAGE = "voice_language"
        private const val KEY_SELECTED_DIALECT = "selected_dialect"
        private const val KEY_LEARNED_DIALECTS = "learned_dialects"
        private const val KEY_LAST_KNOWN_LOCATION = "last_known_location"
        private const val KEY_SAVE_CONVERSATIONS = "save_conversations"
        private const val KEY_MICROPHONE_NOTIFICATIONS = "microphone_notifications"
        private const val KEY_DATA_ENCRYPTION = "data_encryption"
        private const val KEY_WELCOME_MESSAGE = "welcome_message"
        private const val KEY_FIRST_TIME = "first_time"
        private const val KEY_WAKE_WORD = "wake_word"
        private const val KEY_IS_WAKE_WORD_ENABLED = "is_wake_word_enabled"
        private const val KEY_USER_POINTS = "user_points"
        
        // القيم الافتراضية
        private const val DEFAULT_CHARACTER = "ساكورا"
        private const val DEFAULT_SIZE = 150
        private const val DEFAULT_TRANSPARENCY = 90
        private const val DEFAULT_POSITION_X = 100
        private const val DEFAULT_POSITION_Y = 200
        private const val DEFAULT_PERSONALITY = "ودود"
        private const val DEFAULT_SPEECH_RATE = 1.0f
        private const val DEFAULT_SPEECH_PITCH = 1.0f
        private const val DEFAULT_LANGUAGE = "ar-SA"
        private const val DEFAULT_WELCOME = "مرحباً! أنا شخصيتك الأنمي التفاعلية. كيف يمكنني مساعدتك اليوم؟"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // إعدادات الشخصية
    fun getSelectedCharacter(): String {
        return sharedPreferences.getString(KEY_SELECTED_CHARACTER, DEFAULT_CHARACTER) ?: DEFAULT_CHARACTER
    }
    
    fun setSelectedCharacter(character: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_CHARACTER, character).apply()
    }
    
    fun getCharacterSize(): Int {
        return sharedPreferences.getInt(KEY_CHARACTER_SIZE, DEFAULT_SIZE)
    }
    
    fun setCharacterSize(size: Int) {
        sharedPreferences.edit().putInt(KEY_CHARACTER_SIZE, size).apply()
    }
    
    fun getCharacterTransparency(): Int {
        return sharedPreferences.getInt(KEY_CHARACTER_TRANSPARENCY, DEFAULT_TRANSPARENCY)
    }
    
    fun setCharacterTransparency(transparency: Int) {
        sharedPreferences.edit().putInt(KEY_CHARACTER_TRANSPARENCY, transparency).apply()
    }
    
    fun getCharacterPositionX(): Int {
        return sharedPreferences.getInt(KEY_CHARACTER_POSITION_X, DEFAULT_POSITION_X)
    }
    
    fun getCharacterPositionY(): Int {
        return sharedPreferences.getInt(KEY_CHARACTER_POSITION_Y, DEFAULT_POSITION_Y)
    }
    
    fun saveCharacterPosition(x: Int, y: Int) {
        sharedPreferences.edit()
            .putInt(KEY_CHARACTER_POSITION_X, x)
            .putInt(KEY_CHARACTER_POSITION_Y, y)
            .apply()
    }
    
    // إعدادات الشخصية
    fun getPersonalityMode(): String {
        return sharedPreferences.getString(KEY_PERSONALITY_MODE, DEFAULT_PERSONALITY) ?: DEFAULT_PERSONALITY
    }
    
    fun setPersonalityMode(mode: String) {
        sharedPreferences.edit().putString(KEY_PERSONALITY_MODE, mode).apply()
    }
    
    // إعدادات الصوت
    fun getSpeechRate(): Float {
        return sharedPreferences.getFloat(KEY_SPEECH_RATE, DEFAULT_SPEECH_RATE)
    }
    
    fun setSpeechRate(rate: Float) {
        sharedPreferences.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
    }
    
    fun getSpeechPitch(): Float {
        return sharedPreferences.getFloat(KEY_SPEECH_PITCH, DEFAULT_SPEECH_PITCH)
    }
    
    fun setSpeechPitch(pitch: Float) {
        sharedPreferences.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()
    }
    
    fun getVoiceLanguage(): String {
        return sharedPreferences.getString(KEY_VOICE_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    fun setVoiceLanguage(language: String) {
        sharedPreferences.edit().putString(KEY_VOICE_LANGUAGE, language).apply()
    }
    
    // إعدادات الخصوصية
    fun getSaveConversations(): Boolean {
        return sharedPreferences.getBoolean(KEY_SAVE_CONVERSATIONS, true)
    }
    
    fun setSaveConversations(save: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SAVE_CONVERSATIONS, save).apply()
    }
    
    fun getMicrophoneNotifications(): Boolean {
        return sharedPreferences.getBoolean(KEY_MICROPHONE_NOTIFICATIONS, true)
    }
    
    fun setMicrophoneNotifications(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_MICROPHONE_NOTIFICATIONS, enable).apply()
    }
    
    fun getDataEncryption(): Boolean {
        return sharedPreferences.getBoolean(KEY_DATA_ENCRYPTION, true)
    }
    
    fun setDataEncryption(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DATA_ENCRYPTION, enable).apply()
    }
    
    // رسائل أخرى
    fun getWelcomeMessage(): String {
        return sharedPreferences.getString(KEY_WELCOME_MESSAGE, DEFAULT_WELCOME) ?: DEFAULT_WELCOME
    }
    
    fun setWelcomeMessage(message: String) {
        sharedPreferences.edit().putString(KEY_WELCOME_MESSAGE, message).apply()
    }
    
    fun isFirstTime(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_TIME, true)
    }
    
    fun setFirstTime(firstTime: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, firstTime).apply()
    }
    
    // دوال مساعدة
    fun getCharacterImage(characterName: String): String {
        return when (characterName) {
            "ساكورا" -> "sakura_image"
            "ناروتو" -> "naruto_image"
            "لوفي" -> "luffy_image"
            "غوكو" -> "goku_image"
            else -> "default_character_image"
        }
    }
    
    fun getAvailableCharacters(): List<String> {
        return listOf("ساكورا", "ناروتو", "لوفي", "غوكو")
    }
    
    fun getAvailablePersonalities(): List<String> {
        return listOf("ودود", "رسمي", "مرح", "حكيم", "نشيط")
    }
    
    fun getAvailableLanguages(): Map<String, String> {
        return mapOf(
            "ar-SA" to "العربية",
            "en-US" to "الإنجليزية",
            "ja-JP" to "اليابانية"
        )
    }
    
    // إعادة تعيين الإعدادات
    fun resetToDefaults() {
        sharedPreferences.edit().clear().apply()
    }
    
    // تصدير الإعدادات
    fun exportSettings(): Map<String, Any> {
        return sharedPreferences.all
    }
    
    // استيراد الإعدادات
    fun importSettings(settings: Map<String, Any>) {
        val editor = sharedPreferences.edit()
        settings.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        editor.apply()
    }
}



    // إعدادات اسم التفعيل
    fun getWakeWord(): String {
        return sharedPreferences.getString(KEY_WAKE_WORD, "") ?: ""
    }

    fun setWakeWord(wakeWord: String) {
        sharedPreferences.edit().putString(KEY_WAKE_WORD, wakeWord).apply()
    }

    fun isWakeWordEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_WAKE_WORD_ENABLED, false)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_WAKE_WORD_ENABLED, enabled).apply()
    }




    // إعدادات إخفاء الأيقونة أثناء الألعاب
    fun isHideOnGameEnabled(): Boolean {
        return sharedPreferences.getBoolean("hide_on_game_enabled", false)
    }

    fun setHideOnGameEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("hide_on_game_enabled", enabled).apply()
    }

    fun getGamePackageNames(): Set<String> {
        return sharedPreferences.getStringSet("game_package_names", setOf(
            "com.tencent.ig", // PUBG Mobile
            "com.garena.game.codm", // Call of Duty Mobile
            "com.dts.freefireth", // Free Fire
            "com.miHoYo.GenshinImpact", // Genshin Impact
            "com.supercell.clashroyale", // Clash Royale
            "com.supercell.clashofclans" // Clash of Clans
        )) ?: setOf()
    }

    fun setGamePackageNames(packageNames: Set<String>) {
        sharedPreferences.edit().putStringSet("game_package_names", packageNames).apply()
    }




    fun getSelectedDialect(): String {
        return sharedPreferences.getString(KEY_SELECTED_DIALECT, "") ?: ""
    }

    fun setSelectedDialect(dialect: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_DIALECT, dialect).apply()
    }

    fun getAvailableDialects(): Map<String, String> {
        return mapOf(
            "ar-LY" to "الليبية",
            "ar-EG" to "المصرية",
            "ar-SA" to "السعودية",
            "ar-AE" to "الإماراتية",
            "ar-QA" to "القطرية",
            "ar-KW" to "الكويتية",
            "ar-BH" to "البحرينية",
            "ar-OM" to "العمانية",
            "ar-YE" to "اليمنية",
            "ar-JO" to "الأردنية",
            "ar-SY" to "السورية",
            "ar-LB" to "اللبنانية",
            "ar-IQ" to "العراقية",
            "ar-MA" to "المغربية",
            "ar-DZ" to "الجزائرية",
            "ar-TN" to "التونسية",
            "ar-SD" to "السودانية",
            "ar-MR" to "الموريتانية",
            "ar-SO" to "الصومالية",
            "ar-KM" to "القمرية",
            "ar-DJ" to "الجيبوتية",
            "ar-ER" to "الإريترية",
            "ar-TD" to "التشادية",
            "ar-SS" to "جنوب السودان"
        )
    }




    // نقاط المستخدم
    fun getUserPoints(): Int {
        return sharedPreferences.getInt(KEY_USER_POINTS, 0)
    }

    fun setUserPoints(points: Int) {
        sharedPreferences.edit().putInt(KEY_USER_POINTS, points).apply()
    }




    // حالة المكافآت
    fun isRewardUnlocked(rewardId: String): Boolean {
        return sharedPreferences.getBoolean("reward_unlocked_" + rewardId, false)
    }

    fun setRewardUnlocked(rewardId: String, unlocked: Boolean) {
        sharedPreferences.edit().putBoolean("reward_unlocked_" + rewardId, unlocked).apply()
    }

    // خيارات التخصيص
    fun unlockCustomizationOption(optionId: String) {
        sharedPreferences.edit().putBoolean("customization_option_unlocked_" + optionId, true).apply()
    }

    fun isCustomizationOptionUnlocked(optionId: String): Boolean {
        return sharedPreferences.getBoolean("customization_option_unlocked_" + optionId, false)
    }

    // الميزات
    fun unlockFeature(featureId: String) {
        sharedPreferences.edit().putBoolean("feature_unlocked_" + featureId, true).apply()
    }

    fun isFeatureUnlocked(featureId: String): Boolean {
        return sharedPreferences.getBoolean("feature_unlocked_" + featureId, false)
    }




    // اللهجات المتعلمة
    fun getLearnedDialects(): Set<String> {
        return sharedPreferences.getStringSet(KEY_LEARNED_DIALECTS, setOf()) ?: setOf()
    }

    fun addLearnedDialect(dialect: String) {
        val currentDialects = getLearnedDialects().toMutableSet()
        currentDialects.add(dialect)
        sharedPreferences.edit().putStringSet(KEY_LEARNED_DIALECTS, currentDialects).apply()
    }




    // آخر موقع معروف
    fun getLastKnownLocation(): String? {
        return sharedPreferences.getString(KEY_LAST_KNOWN_LOCATION, null)
    }

    fun setLastKnownLocation(location: String) {
        sharedPreferences.edit().putString(KEY_LAST_KNOWN_LOCATION, location).apply()
    }


