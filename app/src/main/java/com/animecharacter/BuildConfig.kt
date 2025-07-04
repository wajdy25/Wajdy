package com.animecharacter

object BuildConfig {
    const val DEBUG = true
    const val APPLICATION_ID = "com.animecharacter"
    const val BUILD_TYPE = "debug"
    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"
    
    // مفتاح OpenAI API - يجب تعيينه من المطور
    const val OPENAI_API_KEY = "YOUR_OPENAI_API_KEY_HERE"
    
    // إعدادات التطبيق
    const val MIN_SDK_VERSION = 21
    const val TARGET_SDK_VERSION = 34
    const val COMPILE_SDK_VERSION = 34
    
    // إعدادات الأداء
    const val ENABLE_PERFORMANCE_MONITORING = true
    const val ENABLE_CRASH_REPORTING = true
    const val ENABLE_ANALYTICS = false
    
    // إعدادات التطوير
    const val ENABLE_LOGGING = true
    const val ENABLE_DEBUG_MENU = true
}

