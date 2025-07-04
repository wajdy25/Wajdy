# 👨‍💻 دليل المطور: ميزات التفاعل مع وسائل التواصل الاجتماعي

## 📚 فهرس المحتويات

1. [نظرة عامة على البنية](#نظرة-عامة-على-البنية)
2. [إعداد البيئة التطويرية](#إعداد-البيئة-التطويرية)
3. [الخدمات الأساسية](#الخدمات-الأساسية)
4. [نماذج البيانات](#نماذج-البيانات)
5. [التكامل والاختبار](#التكامل-والاختبار)
6. [إضافة ميزات جديدة](#إضافة-ميزات-جديدة)
7. [استكشاف الأخطاء](#استكشاف-الأخطاء)

---

## 🏗️ نظرة عامة على البنية

### الهيكل العام للمشروع

```
app/src/main/java/com/animecharacter/
├── services/
│   ├── SocialMediaNotificationService.kt
│   ├── VisualEffectsService.kt
│   ├── AdvancedCharacterAnimationService.kt
│   └── AdvancedVoiceInteractionService.kt
├── managers/
│   ├── SocialMediaAchievementManager.kt
│   └── SocialMediaIntegrationManager.kt
├── models/
│   └── SocialMediaModels.kt
├── testing/
│   └── SocialMediaFeaturesTest.kt
└── utils/
    └── PreferencesHelper.kt (محدث)
```

### تدفق البيانات

```
إشعار وسائل التواصل الاجتماعي
    ↓
SocialMediaNotificationService (تحليل)
    ↓
SocialMediaIntegrationManager (تنسيق)
    ↓
┌─────────────────┬─────────────────┬─────────────────┐
│  Visual Effects │   Animations    │  Voice Messages │
│     Service     │     Service     │     Service     │
└─────────────────┴─────────────────┴─────────────────┘
    ↓
SocialMediaAchievementManager (تتبع الإنجازات)
```

---

## ⚙️ إعداد البيئة التطويرية

### المتطلبات الأساسية

```kotlin
// في build.gradle (Module: app)
dependencies {
    // مكتبات جديدة للميزات
    implementation 'com.airbnb.android:lottie:5.2.0'
    implementation 'androidx.room:room-runtime:2.4.3'
    implementation 'androidx.room:room-ktx:2.4.3'
    kapt 'androidx.room:room-compiler:2.4.3'
    
    // Coroutines للمعالجة غير المتزامنة
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
    
    // ViewModel و LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
}
```

### الأذونات المطلوبة

```xml
<!-- في AndroidManifest.xml -->
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

### تسجيل الخدمات

```xml
<!-- خدمة مراقبة الإشعارات -->
<service
    android:name=".services.SocialMediaNotificationService"
    android:label="@string/social_media_notification_service_label"
    android:exported="false"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

---

## 🔧 الخدمات الأساسية

### 1. SocialMediaNotificationService

**الغرض:** مراقبة وتحليل إشعارات وسائل التواصل الاجتماعي

```kotlin
class SocialMediaNotificationService : NotificationListenerService() {
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // تحليل الإشعار
        val platform = SUPPORTED_PACKAGES[sbn.packageName]
        if (platform != null) {
            analyzeNotification(sbn, platform)
        }
    }
    
    private fun analyzeNotification(sbn: StatusBarNotification, platform: SocialMediaPlatform) {
        // استخراج النص والعنوان
        val notification = sbn.notification
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        
        // تحديد نوع التفاعل
        val interaction = determineInteractionType("$title $text", platform)
        
        // إرسال النتيجة
        if (interaction != null) {
            handleSocialMediaInteraction(interaction)
        }
    }
}
```

**إضافة منصة جديدة:**

```kotlin
// في SUPPORTED_PACKAGES
private val SUPPORTED_PACKAGES = mapOf(
    "com.newplatform.android" to SocialMediaPlatform.NEW_PLATFORM,
    // ... المنصات الأخرى
)

// إضافة أنماط تحليل جديدة
private val NEW_PLATFORM_PATTERNS = listOf(
    Pattern.compile("(\\d+)\\s*new\\s+connections", Pattern.CASE_INSENSITIVE),
    // ... أنماط أخرى
)
```

### 2. VisualEffectsService

**الغرض:** إدارة المؤثرات البصرية والاحتفالات

```kotlin
class VisualEffectsService(private val context: Context) {
    
    fun triggerInteractionEffect(interaction: SocialMediaInteraction, level: Int) {
        val effects = getEffectsForInteraction(interaction, level)
        effects.forEach { playVisualEffect(it) }
    }
    
    private fun playHeartEffect(parent: ViewGroup, effect: VisualEffect) {
        // إنشاء وتحريك القلوب
        repeat(effect.intensity * 5) { index ->
            val heartView = createHeartView()
            parent.addView(heartView)
            animateHeart(heartView, effect.duration)
        }
    }
}
```

**إضافة مؤثر جديد:**

```kotlin
// إنشاء مؤثر جديد
private fun createCustomEffect(level: Int): VisualEffect {
    return VisualEffect(
        type = "custom_effect",
        duration = 3000L,
        intensity = level,
        color = "#FF5722",
        position = VisualEffect.Position.AROUND_CHARACTER
    )
}

// تنفيذ المؤثر
private suspend fun playCustomEffect(parent: ViewGroup, effect: VisualEffect) {
    // منطق المؤثر المخصص
}
```

### 3. AdvancedVoiceInteractionService

**الغرض:** إدارة التفاعل الصوتي والرسائل المحفزة

```kotlin
class AdvancedVoiceInteractionService(private val context: Context) : TextToSpeech.OnInitListener {
    
    fun speakCelebrationMessage(interaction: SocialMediaInteraction, level: Int) {
        val messageKey = "${interaction.type.name.lowercase()}_level_$level"
        val messages = celebrationMessages[messageKey] ?: return
        
        val selectedMessage = messages[Random.nextInt(messages.size)]
        val personalizedMessage = personalizeMessage(selectedMessage, interaction)
        
        speak(personalizedMessage)
    }
}
```

**إضافة رسائل جديدة:**

```kotlin
// في loadCelebrationMessages()
celebrationMessages["new_platform_level_1"] = listOf(
    "رائع! تفاعل جديد على المنصة الجديدة!",
    "ممتاز! المحتوى الخاص بك يلقى استحساناً!",
    // ... رسائل أخرى
)
```

---

## 📊 نماذج البيانات

### SocialMediaInteraction

```kotlin
data class SocialMediaInteraction(
    val type: Type,
    val count: Int,
    val platform: SocialMediaPlatform,
    val timestamp: Long,
    val content: String,
    val celebrationLevel: Int = 0
) {
    enum class Type {
        NEW_FOLLOWERS,
        NEW_LIKES,
        NEW_COMMENTS,
        NEW_SHARES,
        NEW_VIEWS,
        CELEBRITY_INTERACTION,
        TRENDING_MENTION,
        MILESTONE_REACHED
    }
}
```

### SocialMediaGoal

```kotlin
data class SocialMediaGoal(
    val id: String,
    val title: String,
    val description: String,
    val platform: SocialMediaPlatform,
    val type: SocialMediaInteraction.Type,
    val targetValue: Int,
    val currentValue: Int = 0,
    val deadline: Long? = null,
    val isCompleted: Boolean = false,
    val rewardAnimation: String? = null,
    val rewardMessage: String? = null
)
```

### إضافة نموذج بيانات جديد

```kotlin
data class CustomFeature(
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val settings: Map<String, Any> = emptyMap()
)

// إضافة إلى SocialMediaSettings
data class SocialMediaSettings(
    // ... الحقول الموجودة
    val customFeatures: List<CustomFeature> = emptyList()
)
```

---

## 🧪 التكامل والاختبار

### تشغيل الاختبارات

```kotlin
// في النشاط الرئيسي أو نشاط الاختبار
class TestActivity : AppCompatActivity() {
    
    private lateinit var featuresTest: SocialMediaFeaturesTest
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        featuresTest = SocialMediaFeaturesTest(this)
        
        // تشغيل الاختبارات
        lifecycleScope.launch {
            val results = featuresTest.runAllTests()
            displayTestResults(results)
        }
    }
    
    private fun displayTestResults(results: List<SocialMediaFeaturesTest.TestResult>) {
        val report = featuresTest.generateTestReport()
        Log.d("TestResults", report)
        
        // عرض النتائج في واجهة المستخدم
        showTestDialog(report)
    }
}
```

### اختبار مكون محدد

```kotlin
// اختبار خدمة المؤثرات البصرية
private suspend fun testVisualEffectsOnly() {
    val visualEffectsService = VisualEffectsService(context)
    
    val testInteraction = SocialMediaInteraction(
        type = SocialMediaInteraction.Type.NEW_FOLLOWERS,
        count = 10,
        platform = SocialMediaPlatform.INSTAGRAM,
        timestamp = System.currentTimeMillis(),
        content = "Test interaction"
    )
    
    visualEffectsService.triggerInteractionEffect(testInteraction, 2)
}
```

### محاكاة الإشعارات للاختبار

```kotlin
// إرسال إشعار تجريبي
private fun simulateNotification() {
    val intent = Intent("com.animecharacter.SOCIAL_MEDIA_INTERACTION")
    intent.putExtra("interaction_type", "NEW_FOLLOWERS")
    intent.putExtra("interaction_count", 5)
    intent.putExtra("platform", "INSTAGRAM")
    intent.putExtra("timestamp", System.currentTimeMillis())
    
    sendBroadcast(intent)
}
```

---

## ➕ إضافة ميزات جديدة

### إضافة نوع تفاعل جديد

1. **تحديث التعداد:**
```kotlin
enum class Type {
    // ... الأنواع الموجودة
    NEW_CUSTOM_INTERACTION // نوع جديد
}
```

2. **إضافة أنماط التحليل:**
```kotlin
private val CUSTOM_PATTERNS = listOf(
    Pattern.compile("(\\d+)\\s*custom\\s+interactions", Pattern.CASE_INSENSITIVE)
)
```

3. **تحديث منطق التحليل:**
```kotlin
private fun determineInteractionType(content: String, platform: SocialMediaPlatform): SocialMediaInteraction? {
    // ... المنطق الموجود
    
    // فحص النوع الجديد
    for (pattern in CUSTOM_PATTERNS) {
        val matcher = pattern.matcher(content)
        if (matcher.find()) {
            return SocialMediaInteraction(
                type = SocialMediaInteraction.Type.NEW_CUSTOM_INTERACTION,
                count = extractNumber(matcher) ?: 1,
                platform = platform,
                timestamp = System.currentTimeMillis(),
                content = content
            )
        }
    }
    
    return null
}
```

### إضافة رسوم متحركة جديدة

1. **إضافة ملف الرسوم المتحركة:**
```
app/src/main/assets/animations/lottie/custom_animation.json
```

2. **تسجيل الرسوم المتحركة:**
```kotlin
// في loadAvailableAnimations()
availableAnimations["custom_animation"] = SpecialAnimation(
    name = "custom_animation",
    filePath = "$LOTTIE_DIR/custom_animation.json",
    duration = 3000,
    triggerCondition = SpecialAnimation.TriggerCondition(
        interactionType = SocialMediaInteraction.Type.NEW_CUSTOM_INTERACTION,
        minimumCount = 1
    )
)
```

### إضافة مؤثر بصري جديد

```kotlin
// في getEffectsForInteraction()
SocialMediaInteraction.Type.NEW_CUSTOM_INTERACTION -> {
    effects.add(createCustomVisualEffect(level))
}

private fun createCustomVisualEffect(level: Int): VisualEffect {
    return VisualEffect(
        type = "custom_visual",
        duration = 2500L,
        intensity = level,
        color = "#9C27B0",
        position = VisualEffect.Position.FULL_SCREEN
    )
}
```

---

## 🐛 استكشاف الأخطاء

### مشاكل شائعة وحلولها

#### 1. خدمة الإشعارات لا تعمل

**المشكلة:** `SocialMediaNotificationService` لا تستقبل إشعارات

**الحلول:**
```kotlin
// فحص إذن الوصول للإشعارات
private fun checkNotificationAccess(): Boolean {
    val enabledListeners = Settings.Secure.getString(
        contentResolver,
        "enabled_notification_listeners"
    )
    return enabledListeners?.contains(packageName) == true
}

// توجيه المستخدم لتفعيل الإذن
private fun requestNotificationAccess() {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    startActivity(intent)
}
```

#### 2. المؤثرات البصرية لا تظهر

**المشكلة:** `VisualEffectsService` لا يعرض المؤثرات

**الحل:**
```kotlin
// التأكد من تعيين العرض الأساسي
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        visualEffectsService.setParentView(rootView)
    }
}
```

#### 3. التفاعل الصوتي لا يعمل

**المشكلة:** `AdvancedVoiceInteractionService` صامت

**الحل:**
```kotlin
// فحص حالة TTS
override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
        // تعيين اللغة
        val result = textToSpeech?.setLanguage(Locale("ar"))
        if (result == TextToSpeech.LANG_MISSING_DATA) {
            // تحميل بيانات اللغة العربية
            val installIntent = Intent()
            installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            startActivity(installIntent)
        }
    }
}
```

### تسجيل الأخطاء (Logging)

```kotlin
// استخدام تسجيل مفصل للتطوير
class SocialMediaNotificationService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "SocialMediaNotificationService"
        private const val DEBUG = BuildConfig.DEBUG
    }
    
    private fun logDebug(message: String) {
        if (DEBUG) {
            Log.d(TAG, message)
        }
    }
    
    private fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
```

### أدوات التطوير

```kotlin
// إضافة أدوات تطوير للاختبار السريع
class DeveloperTools {
    
    companion object {
        fun simulateFollowersIncrease(context: Context, count: Int) {
            val intent = Intent("com.animecharacter.SOCIAL_MEDIA_INTERACTION")
            intent.putExtra("interaction_type", "NEW_FOLLOWERS")
            intent.putExtra("interaction_count", count)
            intent.putExtra("platform", "INSTAGRAM")
            intent.putExtra("timestamp", System.currentTimeMillis())
            context.sendBroadcast(intent)
        }
        
        fun testAllAnimations(animationService: AdvancedCharacterAnimationService) {
            val animations = animationService.getUnlockedAnimations()
            animations.forEach { animation ->
                animationService.playSpecificAnimation(animation.name)
            }
        }
    }
}
```

---

## 📋 قائمة المراجعة للتطوير

### قبل إضافة ميزة جديدة
- [ ] تحديد المتطلبات بوضوح
- [ ] تصميم نموذج البيانات
- [ ] إنشاء اختبارات الوحدة
- [ ] تحديث الوثائق

### قبل النشر
- [ ] تشغيل جميع الاختبارات
- [ ] فحص الأداء والذاكرة
- [ ] اختبار على أجهزة مختلفة
- [ ] مراجعة الأذونات المطلوبة
- [ ] تحديث دليل المستخدم

### بعد النشر
- [ ] مراقبة التقارير والأخطاء
- [ ] جمع ملاحظات المستخدمين
- [ ] تحليل الاستخدام
- [ ] التخطيط للتحديثات القادمة

---

*هذا الدليل يوفر أساساً قوياً لتطوير وصيانة ميزات التفاعل مع وسائل التواصل الاجتماعي. للمزيد من المساعدة، راجع الكود المصدري والتعليقات المفصلة.*



## 🗣️ ميزة اسم التفعيل الصوتي المخصص

تسمح هذه الميزة للمستخدم بتحديد كلمة تفعيل مخصصة للشخصية، بحيث تستجيب الشخصية للأوامر الصوتية فقط عند سماع هذه الكلمة.

### المكونات الرئيسية:

1.  **`PreferencesHelper.kt`**: تم إضافة دوال لحفظ واسترجاع كلمة التفعيل وحالة تفعيل الميزة.
    ```kotlin
    // إعدادات اسم التفعيل
    fun getWakeWord(): String
    fun setWakeWord(wakeWord: String)
    fun isWakeWordEnabled(): Boolean
    fun setWakeWordEnabled(enabled: Boolean)
    ```

2.  **`AdvancedVoiceInteractionService.kt`**: تم تعديل هذه الخدمة لتشمل:
    *   تهيئة `SpeechRecognizer` للاستماع المستمر.
    *   دوال `startListeningForWakeWord()` و `stopListeningForWakeWord()` للتحكم في عملية الاستماع.
    *   منطق `handleRecognizedText()` لمقارنة النص المتعّرف عليه بكلمة التفعيل المحددة في الإعدادات.
    *   إضافة إذن `RECORD_AUDIO` و `BIND_RECOGNITION_SERVICE` في `AndroidManifest.xml`.

3.  **`SettingsActivity.kt` و `preferences.xml`**: تم إضافة عناصر واجهة المستخدم للسماح للمستخدم بتفعيل/تعطيل الميزة وتحديد كلمة التفعيل.
    *   `SwitchPreferenceCompat` لـ `is_wake_word_enabled`.
    *   `EditTextPreference` لـ `wake_word`.

### كيفية الاختبار:

*   **اختبار الوحدة:** تم إنشاء `WakeWordFeatureTest.kt` لاختبار منطق تفعيل/تعطيل كلمة التفعيل وبدء/إيقاف الاستماع.
*   **الاختبار اليدوي:**
    1.  افتح إعدادات التطبيق وانتقل إلى قسم الصوت.
    2.  قم بتفعيل "تفعيل كلمة التفعيل" وأدخل كلمة تفعيل (مثال: "مُزنة").
    3.  ارجع إلى الشاشة الرئيسية للتطبيق.
    4.  قل كلمة التفعيل التي حددتها. يجب أن تستجيب الشخصية برسالة تأكيد.
    5.  جرب قول كلمات أخرى لا تتضمن كلمة التفعيل. يجب ألا تستجيب الشخصية.





## 🎮 ميزة إخفاء الأيقونة العائمة أثناء الألعاب

تسمح هذه الميزة بإخفاء الأيقونة العائمة للشخصية تلقائياً عند تشغيل الألعاب المحددة من قبل المستخدم، وإعادة إظهارها عند الخروج من اللعبة.

### المكونات الرئيسية:

1.  **`PreferencesHelper.kt`**: تم إضافة دوال لحفظ واسترجاع حالة تفعيل الميزة وقائمة حزم الألعاب.
    ```kotlin
    // إعدادات إخفاء الأيقونة أثناء الألعاب
    fun isHideOnGameEnabled(): Boolean
    fun setHideOnGameEnabled(enabled: Boolean)
    fun getGamePackageNames(): Set<String>
    fun setGamePackageNames(packageNames: Set<String>)
    ```

2.  **`Live2DFloatingWindowService.kt`**: تم إضافة دوال `hideWindowProgrammatically()` و `showWindowProgrammatically()` للتحكم في رؤية النافذة العائمة من خدمات أخرى.

3.  **`AppMonitorService.kt`**: خدمة جديدة مسؤولة عن:
    *   مراقبة التطبيق النشط في المقدمة باستخدام `ActivityManager`.
    *   التحقق مما إذا كان التطبيق النشط هو أحد الألعاب المحددة في الإعدادات.
    *   إخفاء أو إظهار النافذة العائمة بناءً على نتيجة التحقق.

4.  **`AndroidManifest.xml`**: تم إضافة إذن `PACKAGE_USAGE_STATS` (يتطلب إذن خاص من المستخدم) وتسجيل `AppMonitorService`.

5.  **`SettingsActivity.kt` و `preferences.xml`**: تم إضافة عناصر واجهة المستخدم للسماح للمستخدم بتفعيل/تعطيل الميزة وتحديد قائمة الألعاب.
    *   `SwitchPreferenceCompat` لـ `hide_on_game_enabled`.
    *   `MultiSelectListPreference` لـ `game_package_names` لعرض قائمة بالتطبيقات المثبتة واختيار الألعاب منها.

### كيفية الاختبار:

*   **اختبار الوحدة:** تم إنشاء `GameDetectionFeatureTest.kt` لاختبار منطق اكتشاف الألعاب وإخفاء/إظهار النافذة.
*   **الاختبار اليدوي:**
    1.  افتح إعدادات التطبيق وانتقل إلى قسم "إخفاء الأيقونة أثناء الألعاب".
    2.  قم بتفعيل "إخفاء الأيقونة تلقائياً" وحدد بعض الألعاب المثبتة على جهازك.
    3.  تأكد من أن الشخصية العائمة مرئية.
    4.  افتح إحدى الألعاب التي حددتها. يجب أن تختفي الأيقونة العائمة.
    5.  اخرج من اللعبة أو انتقل إلى تطبيق آخر. يجب أن تظهر الأيقونة العائمة مرة أخرى.





## 🧠 نظام تحليل المشاعر والحالة المزاجية

### الوصف
يقوم هذا النظام بتحليل مشاعر المستخدم بناءً على نصوص المحادثة مع الشخصية ونبرة الصوت عبر الأوامر الصوتية. يتم تعديل تعبيرات الشخصية وحركاتها تلقائيًا بناءً على الحالة المزاجية المكتشفة (فرح، حزن، حماس، إلخ).

### المكونات الرئيسية
- `EmotionAnalyzer.kt`: يقوم بتحليل النص واستخراج المشاعر.
- `AdvancedVoiceInteractionService.kt`: يدمج تحليل المشاعر من الأوامر الصوتية.
- `Live2DAnimationEngine.kt`: يقوم بتعديل تعبيرات وحركات الشخصية بناءً على المشاعر المكتشفة.

### كيفية الاستخدام
1. **تحليل المشاعر النصية:** عند إرسال نص إلى الشخصية، يتم تمرير النص إلى `EmotionAnalyzer.analyzeTextEmotion()`.
2. **تحليل المشاعر الصوتية:** عند استخدام الأوامر الصوتية، يتم تحليل النص المحول من الصوت بواسطة `EmotionAnalyzer`.
3. **تطبيق التعبيرات:** بناءً على المشاعر المكتشفة، يتم استدعاء `Live2DAnimationEngine.setEmotion()` لتغيير تعبيرات الشخصية.

### أمثلة على المشاعر المدعومة
- سعيد (happy)
- حزين (sad)
- متفاجئ (surprised)
- غاضب (angry)
- يفكر (thinking)
- حب (love)
- نعسان (sleepy)
- محايد (neutral)

### ملاحظات
- يمكن توسيع `EmotionAnalyzer` ليشمل نماذج تحليل مشاعر أكثر تعقيدًا أو تكاملًا مع خدمات خارجية.
- يجب توفير أصول Live2D المناسبة لكل تعبير لضمان عرض صحيح للشخصية.




## 📊 التقرير الصوتي اليومي للإنجازات

### الوصف
توفر هذه الميزة تقريرًا صوتيًا شاملاً للمستخدم نهاية كل يوم، يلخص إنجازاته على وسائل التواصل الاجتماعي، مثل عدد المتابعين الجدد، التفاعلات، وعبارات تحفيزية.

### المكونات الرئيسية
- `DailyReport.kt`: نموذج بيانات لتمثيل التقرير اليومي.
- `SocialMediaAchievementManager.kt`: يحتوي على منطق إنشاء التقرير اليومي وتوليد الرسالة الصوتية.
- `AdvancedVoiceInteractionService.kt`: يستخدم لتحويل نص التقرير إلى صوت.

### كيفية الاستخدام
1. **توليد التقرير:** يتم استدعاء الدالة `generateDailyReport()` في `SocialMediaAchievementManager` لتجميع البيانات اليومية.
2. **تحويل إلى صوت:** يتم تمرير نص التقرير الملخص إلى `advancedVoiceInteractionService.speak()` ليتم قراءته للمستخدم.
3. **الجدولة:** يمكن جدولة هذه الميزة لتشغيلها تلقائيًا في نهاية كل يوم باستخدام `WorkManager` أو `AlarmManager` في Android.

### محتوى التقرير
- إجمالي التفاعلات اليومية.
- عدد الأهداف المكتملة.
- المنصة الأفضل أداءً.
- رسالة تشجيعية بناءً على الأداء.
- اقتراحات لليوم التالي.

### ملاحظات
- يمكن تخصيص محتوى التقرير والرسائل التحفيزية بشكل أكبر بناءً على تفضيلات المستخدم أو أهدافه.
- يجب التأكد من أن خدمة الصوت (TTS) تعمل بشكل صحيح وأن اللغة المطلوبة مدعومة.




## 📈 التفاعل مع الترندات والمواضيع الرائجة

### الوصف
تتيح هذه الميزة للشخصية التفاعل مع المواضيع الرائجة والترندات على وسائل التواصل الاجتماعي، مما يجعل التفاعل أكثر حداثة وواقعية.

### المكونات الرئيسية
- `TrendingTopicsService.kt`: مسؤول عن جلب المواضيع الرائجة من مصادر خارجية (مثل Google Trends API).
- `SocialMediaIntegrationManager.kt`: يدمج `TrendingTopicsService` ويقوم بجدولة جلب المواضيع ومعالجتها.

### كيفية الاستخدام
1. **جلب المواضيع:** يتم استدعاء `TrendingTopicsService.fetchTrendingTopics()` لجلب قائمة بالمواضيع الرائجة.
2. **المعالجة والتفاعل:** يقوم `SocialMediaIntegrationManager` بمعالجة هذه المواضيع، ويمكن للشخصية استخدامها في المحادثات أو التعليقات التلقائية.
3. **الجدولة:** يتم جدولة عملية جلب المواضيع الرائجة بشكل دوري (مثلاً كل ساعة أو كل بضع ساعات) لضمان تحديث البيانات.

### ملاحظات
- يتطلب هذا التكامل استخدام واجهة برمجة تطبيقات (API) خارجية لجلب بيانات الترندات، وقد يتطلب مفتاح API واشتراكًا.
- يجب تصميم منطق التفاعل بعناية لضمان أن تكون استجابات الشخصية ذات صلة وطبيعية.




## 🎭 تعدد الشخصيات وإمكانية التخصيص

### الوصف
تسمح هذه الميزة للمستخدمين باختيار شخصيات أنمي متعددة والتخصيصات المختلفة لكل شخصية، بما في ذلك تغيير مظهر الشخصية وتعديل ألوان أو خلفيات النافذة العائمة.

### المكونات الرئيسية
- `Character.kt`: نموذج بيانات يمثل الشخصية وخصائصها.
- `Live2DManager.kt`: يدير تحميل وتبديل الشخصيات، وتطبيق خيارات التخصيص.
- `Live2DFloatingWindowService.kt`: يتلقى أوامر التخصيص ويطبقها على النافذة العائمة.
- `Live2DService.kt`: يتم تعديله لقبول كائن `Character` لتحميل النموذج الصحيح.

### كيفية الاستخدام
1. **إدارة الشخصيات:** يقوم `Live2DManager` بتحميل قائمة الشخصيات المتاحة من الأصول أو قاعدة البيانات.
2. **تبديل الشخصيات:** يمكن للمستخدمين اختيار شخصية من القائمة، ويقوم `Live2DManager.switchCharacter()` بتحميل النموذج الجديد وإرسال إشارة إلى `Live2DFloatingWindowService` لتحديث العرض.
3. **تخصيص المظهر:** يمكن للمستخدمين تغيير مظهر الشخصية (مثل الملابس، تسريحة الشعر) أو ألوان وخلفيات النافذة العائمة. يتم تطبيق هذه التخصيصات عبر `Live2DManager.customizeCharacter()` أو `Live2DManager.customizeFloatingWindow()`.

### ملاحظات
- تتطلب هذه الميزة توفير أصول Live2D لكل شخصية وخيارات تخصيصها.
- يجب تصميم واجهة مستخدم مناسبة في إعدادات التطبيق للسماح للمستخدمين باختيار الشخصيات وتخصيصها.




## 🗣️ دعم اللغات واللهجات المتعددة

### الوصف
تتيح هذه الميزة للمستخدمين تحديد لغة أو لهجة مفضلة للشخصية، مما يوفر تجربة تفاعلية أكثر تخصيصًا وملاءمة للثقافات المختلفة، خاصة دعم اللهجة الليبية.

### المكونات الرئيسية
- `PreferencesHelper.kt`: لحفظ واسترجاع اللغة واللهجة المختارة من قبل المستخدم.
- `AdvancedVoiceInteractionService.kt`: يستخدم لإعداد محرك تحويل النص إلى كلام (TextToSpeech) باللغة واللهجة المحددة.
- واجهة المستخدم في `SettingsActivity.kt` و `preferences.xml`: للسماح للمستخدم باختيار اللغة واللهجة.

### كيفية الاستخدام
1. **اختيار اللغة واللهجة:** يقوم المستخدم باختيار اللغة واللهجة المفضلة من خلال إعدادات التطبيق. يتم حفظ هذا الاختيار في `PreferencesHelper`.
2. **تطبيق اللغة على TTS:** عند تهيئة `AdvancedVoiceInteractionService`، يتم استرجاع اللغة واللهجة المختارة من `PreferencesHelper` وتطبيقها على إعدادات `TextToSpeech`.
3. **الردود المخصصة:** يمكن تطوير منطق إضافي في الخدمات المختلفة (مثل `AdvancedVoiceInteractionService` أو `SocialMediaAchievementManager`) لتقديم ردود مخصصة باللهجة المختارة.

### ملاحظات
- يعتمد دعم اللهجات بشكل كبير على توفرها في محرك TextToSpeech الخاص بالجهاز. قد تحتاج بعض اللهجات إلى حزم بيانات صوتية إضافية.
- لتقديم ردود مخصصة باللهجة، قد يتطلب الأمر قاعدة بيانات من العبارات باللهجات المختلفة أو استخدام نماذج لغوية متقدمة.




## 🏆 نظام المكافآت والتقدم التدريجي

### الوصف
يقدم هذا النظام نقاطًا ومكافآت للمستخدمين بناءً على تفاعلهم مع التطبيق والشخصية. تتيح هذه المكافآت فتح ميزات جديدة، حركات خاصة للشخصية، أو خيارات تخصيص إضافية، مما يحفز المستخدمين على الاستمرار في التفاعل.

### المكونات الرئيسية
- `Reward.kt`: نموذج بيانات يمثل المكافأة وأنواعها (حركة، تعبير، فتح شخصية، تخصيص، ميزة).
- `RewardManager.kt`: يدير إضافة النقاط، التحقق من شروط فتح المكافآت، وتفعيل المكافآت المفتوحة.
- `PreferencesHelper.kt`: لحفظ واسترجاع نقاط المستخدم وحالة المكافآت المفتوحة.
- `Live2DAnimationEngine.kt`: لتفعيل الحركات والتعبيرات الجديدة المفتوحة.
- `Live2DManager.kt`: لفتح شخصيات جديدة.

### كيفية الاستخدام
1. **إضافة النقاط:** يتم استدعاء `RewardManager.addPoints()` عند قيام المستخدم بتفاعلات معينة (مثل التفاعل مع إشعارات وسائل التواصل الاجتماعي، أو إكمال مهام).
2. **فتح المكافآت:** يقوم `RewardManager` تلقائيًا بالتحقق من المكافآت التي يمكن فتحها بناءً على نقاط المستخدم أو شروط أخرى.
3. **تفعيل المكافآت:** عند فتح مكافأة، يقوم `RewardManager` بتفعيلها عن طريق استدعاء الدوال المناسبة في `Live2DAnimationEngine` (للحركات والتعبيرات) أو `Live2DManager` (لفتح الشخصيات) أو تحديث `PreferencesHelper` (لخيارات التخصيص والميزات).

### أنواع المكافآت
- **حركات (Animations):** تفتح حركات جديدة للشخصية.
- **تعبيرات (Expressions):** تفتح تعبيرات وجه جديدة للشخصية.
- **فتح شخصيات (Character Unlock):** تتيح للمستخدم الوصول إلى شخصيات أنمي جديدة.
- **خيارات تخصيص (Customization Options):** تفتح خيارات جديدة لتخصيص مظهر الشخصية أو النافذة العائمة.
- **ميزات (Feature Unlock):** تفتح ميزات جديدة داخل التطبيق.

### ملاحظات
- يمكن توسيع قائمة المكافآت وشروط فتحها لتشمل سيناريوهات تفاعل أكثر تعقيدًا.
- يجب تصميم واجهة مستخدم لعرض نقاط المستخدم والمكافآت المتاحة والمفتوحة.




## 🧍 عرض الشخصية بجسم كامل

تم تعديل التطبيق لعرض الشخصية الأنمي بجسم كامل على شاشة الهاتف، بدون نافذة محددة أو خلفية شفافة واضحة، لتظهر كملصق ثلاثي الأبعاد يتحرك بحرية ويُعرض فوق التطبيقات أو على الشاشة الرئيسية.

### المتطلبات والخطوات:

1.  **تصميم نموذج Live2D بالحجم الكامل:**
    *   يجب أن تحتوي ملفات Live2D (مثل `.moc3`, `.model3.json`, `.texture` وغيرها) على الشخصية كاملة الجسم. تأكد أن النموذج المستخدم هو "كامل الجسم" وليس فقط الوجه أو نصف الجسم.

2.  **تعديل النافذة العائمة لتكون شفافة بالكامل:**
    *   تم استخدام `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` مع إعدادات شفافية تامة.
    *   تمت إزالة الخلفية من النافذة باستخدام `PixelFormat.TRANSLUCENT` وضبط `setBackgroundDrawable(null)`.

3.  **إظهار الشخصية بدون إطار أو حدود:**
    *   تم تعطيل أي حواف أو خطوط ظاهرة.
    *   تم التأكد من أن الـ Layout الخاص بالنافذة لا يحتوي على أي إطارات أو خلفيات غير مرغوبة.

4.  **تحريك الشخصية بحرية مثل ملصق:**
    *   تم السماح بتحريك الشخصية بحرية من خلال اللمس أو السحب على الشاشة.

### الملفات المتأثرة:

*   `app/src/main/java/com/animecharacter/services/Live2DFloatingWindowService.kt`
*   `app/src/main/java/com/animecharacter/services/Live2DService.kt`
*   `app/src/main/java/com/animecharacter/utils/Live2DRenderer.kt`


