package com.animecharacter.models

data class Character(
    val id: String,
    val name: String,
    val description: String,
    val modelPath: String, // مسار ملف Live2D model3.json
    val previewImagePath: String, // مسار صورة معاينة الشخصية
    val defaultAnimations: Map<String, String>, // خريطة للرسوم المتحركة الافتراضية (مثل idle, happy, sad)
    val customizationOptions: List<CustomizationOption> = emptyList()
)

data class CustomizationOption(
    val id: String,
    val name: String,
    val type: CustomizationType,
    val values: List<String>, // مسارات الصور/الألوان/الأنماط
    val currentValue: String // القيمة الحالية المختارة
) {
    enum class CustomizationType {
        APPEARANCE, // مظهر الشخصية (ملابس، شعر، عيون)
        COLOR,      // لون (خلفية النافذة، تأثيرات)
        BACKGROUND, // خلفية النافذة العائمة
        EFFECT      // تأثيرات بصرية إضافية
    }
}


