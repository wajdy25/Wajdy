# دليل تثبيت تطبيق الشخصية الأنمي التفاعلية
# Installation Guide for Anime Character App

## خطوات التثبيت / Installation Steps

### 1. تحضير الجهاز / Prepare Device
- تفعيل وضع المطور / Enable Developer Mode
- تفعيل تثبيت التطبيقات من مصادر غير معروفة / Enable Unknown Sources
- تفعيل تصحيح USB (اختياري) / Enable USB Debugging (optional)

### 2. تثبيت التطبيق / Install the App

#### الطريقة الأولى: عبر ADB / Method 1: Via ADB
```bash
adb install AnimeCharacterApp-v1.0.0-debug.apk
```

#### الطريقة الثانية: التثبيت المباشر / Method 2: Direct Install
1. انسخ ملف APK إلى الهاتف / Copy APK file to phone
2. افتح مدير الملفات / Open file manager
3. انقر على ملف APK / Tap on APK file
4. اتبع التعليمات / Follow instructions

### 3. إعداد مفتاح OpenAI API / Setup OpenAI API Key
1. احصل على مفتاح من / Get a key from: https://platform.openai.com/api-keys
2. افتح التطبيق / Open the app
3. اذهب إلى الإعدادات / Go to Settings
4. أدخل المفتاح في حقل "OpenAI API Key" / Enter key in "OpenAI API Key" field
5. احفظ الإعدادات / Save settings

### 4. منح الصلاحيات / Grant Permissions

#### صلاحية الميكروفون / Microphone Permission
- ستظهر تلقائياً عند أول استخدام / Will appear automatically on first use
- أو اذهب إلى إعدادات الهاتف > التطبيقات > الشخصية الأنمي > الصلاحيات
- Or go to Phone Settings > Apps > Anime Character > Permissions

#### صلاحية النافذة العائمة / Overlay Permission
1. اذهب إلى إعدادات الهاتف / Go to phone settings
2. التطبيقات > الشخصية الأنمي / Apps > Anime Character
3. الصلاحيات > العرض فوق التطبيقات الأخرى / Permissions > Display over other apps
4. فعّل الخيار / Enable the option

### 5. اختيار الشخصية / Select Character
1. افتح التطبيق / Open the app
2. انقر على "اختيار الشخصية" / Tap "Select Character"
3. اختر من الشخصيات المتاحة / Choose from available characters:
   - ساكورا هارونو / Sakura Haruno
   - ناروتو أوزوماكي / Naruto Uzumaki
   - مونكي دي لوفي / Monkey D. Luffy
   - سون غوكو / Son Goku
4. خصص المظهر والحجم / Customize appearance and size

### 6. البدء / Start Using
1. اضغط "تشغيل الشخصية العائمة" / Tap "Start Floating Character"
2. ستظهر الشخصية على الشاشة الرئيسية / Character will appear on home screen
3. انقر على الشخصية للتفاعل / Tap character to interact
4. استخدم الميكروفون للتحدث / Use microphone to speak
5. استمتع بالمحادثة! / Enjoy chatting!

## استكشاف الأخطاء / Troubleshooting

### الشخصية لا تظهر / Character Not Appearing
- تحقق من صلاحية النافذة العائمة / Check overlay permission
- أعد تشغيل التطبيق / Restart the app
- تحقق من إعدادات النظام / Check system settings

### لا يعمل الصوت / Voice Not Working
- تحقق من صلاحية الميكروفون / Check microphone permission
- تحقق من اتصال الإنترنت / Check internet connection
- جرب إعادة تشغيل خدمة الصوت / Try restarting voice service

### بطء الاستجابة / Slow Response
- تحقق من سرعة الإنترنت / Check internet speed
- أغلق التطبيقات الأخرى / Close other apps
- قلل حجم الشخصية / Reduce character size

### مشاكل الذكاء الاصطناعي / AI Issues
- تحقق من صحة مفتاح OpenAI API / Verify OpenAI API key
- تحقق من رصيد الحساب / Check account balance
- جرب إعادة إدخال المفتاح / Try re-entering the key

## نصائح الاستخدام / Usage Tips

### توفير البطارية / Battery Saving
- قلل حجم الشخصية عند عدم الاستخدام / Reduce character size when not in use
- استخدم الوضع الليلي / Use dark mode
- أوقف الميزات الصوتية عند عدم الحاجة / Disable voice features when not needed

### تحسين الأداء / Performance Optimization
- أعد تشغيل التطبيق دورياً / Restart app periodically
- امسح ذاكرة التخزين المؤقت / Clear cache
- أغلق التطبيقات غير المستخدمة / Close unused apps

### الخصوصية / Privacy
- راجع إعدادات الخصوصية بانتظام / Review privacy settings regularly
- احذف المحادثات القديمة / Delete old conversations
- استخدم التشفير / Use encryption

## الدعم / Support
- اقرأ ملف README.md للمزيد من التفاصيل / Read README.md for more details
- تحقق من سجل الأخطاء في إعدادات التطبيق / Check error logs in app settings
- أعد تشغيل الهاتف في حالة المشاكل المستمرة / Restart phone for persistent issues
