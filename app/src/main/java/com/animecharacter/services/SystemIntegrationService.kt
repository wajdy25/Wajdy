package com.animecharacter.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.*

/**
 * خدمة تكامل النظام - تتيح للشخصية التفاعل مع وظائف النظام
 * بطريقة آمنة ومحدودة الصلاحيات
 */
class SystemIntegrationService(private val context: Context) {
    
    companion object {
        private const val TAG = "SystemIntegrationService"
    }
    
    // أنواع الأوامر المدعومة
    enum class SystemCommand {
        PLAY_MUSIC,
        PAUSE_MUSIC,
        STOP_MUSIC,
        NEXT_TRACK,
        PREVIOUS_TRACK,
        SET_ALARM,
        SET_TIMER,
        SET_REMINDER,
        OPEN_APP,
        ADJUST_VOLUME,
        TOGGLE_WIFI,
        TOGGLE_BLUETOOTH,
        OPEN_SETTINGS,
        TAKE_PHOTO,
        SEND_MESSAGE,
        MAKE_CALL,
        OPEN_BROWSER,
        SEARCH_WEB,
        GET_WEATHER,
        GET_TIME,
        GET_DATE,
        UNKNOWN
    }
    
    // نتيجة تنفيذ الأمر
    data class CommandResult(
        val success: Boolean,
        val message: String,
        val data: Any? = null
    )
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * معالجة أمر نصي من المستخدم
     */
    suspend fun processTextCommand(command: String): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                val systemCommand = parseCommand(command)
                executeCommand(systemCommand, command)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing command: $command", e)
                CommandResult(false, "حدث خطأ أثناء تنفيذ الأمر: ${e.message}")
            }
        }
    }
    
    /**
     * تحليل الأمر النصي وتحديد نوعه
     */
    private fun parseCommand(command: String): SystemCommand {
        val lowerCommand = command.lowercase().trim()
        
        return when {
            // أوامر الموسيقى
            lowerCommand.contains("شغل") && (lowerCommand.contains("موسيقى") || lowerCommand.contains("أغنية")) -> SystemCommand.PLAY_MUSIC
            lowerCommand.contains("أوقف") && lowerCommand.contains("موسيقى") -> SystemCommand.PAUSE_MUSIC
            lowerCommand.contains("التالي") || lowerCommand.contains("الأغنية التالية") -> SystemCommand.NEXT_TRACK
            lowerCommand.contains("السابق") || lowerCommand.contains("الأغنية السابقة") -> SystemCommand.PREVIOUS_TRACK
            
            // أوامر المنبه والتذكير
            lowerCommand.contains("منبه") || lowerCommand.contains("أيقظني") -> SystemCommand.SET_ALARM
            lowerCommand.contains("مؤقت") || lowerCommand.contains("عداد") -> SystemCommand.SET_TIMER
            lowerCommand.contains("ذكرني") || lowerCommand.contains("تذكير") -> SystemCommand.SET_REMINDER
            
            // أوامر التطبيقات
            lowerCommand.contains("افتح") && lowerCommand.contains("تطبيق") -> SystemCommand.OPEN_APP
            lowerCommand.contains("إعدادات") -> SystemCommand.OPEN_SETTINGS
            lowerCommand.contains("كاميرا") || lowerCommand.contains("صورة") -> SystemCommand.TAKE_PHOTO
            lowerCommand.contains("متصفح") || lowerCommand.contains("إنترنت") -> SystemCommand.OPEN_BROWSER
            
            // أوامر الصوت
            lowerCommand.contains("ارفع الصوت") || lowerCommand.contains("زود الصوت") -> SystemCommand.ADJUST_VOLUME
            lowerCommand.contains("اخفض الصوت") || lowerCommand.contains("قلل الصوت") -> SystemCommand.ADJUST_VOLUME
            
            // أوامر الاتصال
            lowerCommand.contains("واي فاي") || lowerCommand.contains("wifi") -> SystemCommand.TOGGLE_WIFI
            lowerCommand.contains("بلوتوث") || lowerCommand.contains("bluetooth") -> SystemCommand.TOGGLE_BLUETOOTH
            
            // أوامر المعلومات
            lowerCommand.contains("الوقت") || lowerCommand.contains("الساعة") -> SystemCommand.GET_TIME
            lowerCommand.contains("التاريخ") || lowerCommand.contains("اليوم") -> SystemCommand.GET_DATE
            lowerCommand.contains("الطقس") || lowerCommand.contains("الجو") -> SystemCommand.GET_WEATHER
            
            // أوامر البحث
            lowerCommand.contains("ابحث") || lowerCommand.contains("بحث") -> SystemCommand.SEARCH_WEB
            
            else -> SystemCommand.UNKNOWN
        }
    }
    
    /**
     * تنفيذ الأمر المحدد
     */
    private suspend fun executeCommand(systemCommand: SystemCommand, originalCommand: String): CommandResult {
        return when (systemCommand) {
            SystemCommand.PLAY_MUSIC -> playMusic()
            SystemCommand.PAUSE_MUSIC -> pauseMusic()
            SystemCommand.STOP_MUSIC -> stopMusic()
            SystemCommand.NEXT_TRACK -> nextTrack()
            SystemCommand.PREVIOUS_TRACK -> previousTrack()
            SystemCommand.SET_ALARM -> setAlarm(originalCommand)
            SystemCommand.SET_TIMER -> setTimer(originalCommand)
            SystemCommand.SET_REMINDER -> setReminder(originalCommand)
            SystemCommand.OPEN_APP -> openApp(originalCommand)
            SystemCommand.ADJUST_VOLUME -> adjustVolume(originalCommand)
            SystemCommand.TOGGLE_WIFI -> toggleWifi()
            SystemCommand.TOGGLE_BLUETOOTH -> toggleBluetooth()
            SystemCommand.OPEN_SETTINGS -> openSettings()
            SystemCommand.TAKE_PHOTO -> takePhoto()
            SystemCommand.OPEN_BROWSER -> openBrowser(originalCommand)
            SystemCommand.SEARCH_WEB -> searchWeb(originalCommand)
            SystemCommand.GET_TIME -> getCurrentTime()
            SystemCommand.GET_DATE -> getCurrentDate()
            SystemCommand.GET_WEATHER -> getWeather()
            SystemCommand.UNKNOWN -> CommandResult(false, "لم أتمكن من فهم الأمر. يرجى المحاولة مرة أخرى.")
            else -> CommandResult(false, "هذا الأمر غير مدعوم حالياً.")
        }
    }
    
    /**
     * تشغيل الموسيقى
     */
    private fun playMusic(): CommandResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult(true, "تم فتح مشغل الموسيقى")
        } catch (e: Exception) {
            // محاولة بديلة
            try {
                val intent = Intent("com.android.music.musicservicecommand")
                intent.putExtra("command", "play")
                context.sendBroadcast(intent)
                CommandResult(true, "تم إرسال أمر تشغيل الموسيقى")
            } catch (e2: Exception) {
                CommandResult(false, "لم أتمكن من تشغيل الموسيقى. تأكد من وجود مشغل موسيقى مثبت.")
            }
        }
    }
    
    /**
     * إيقاف الموسيقى مؤقتاً
     */
    private fun pauseMusic(): CommandResult {
        return try {
            val intent = Intent("com.android.music.musicservicecommand")
            intent.putExtra("command", "pause")
            context.sendBroadcast(intent)
            CommandResult(true, "تم إيقاف الموسيقى مؤقتاً")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من إيقاف الموسيقى")
        }
    }
    
    /**
     * إيقاف الموسيقى نهائياً
     */
    private fun stopMusic(): CommandResult {
        return try {
            val intent = Intent("com.android.music.musicservicecommand")
            intent.putExtra("command", "stop")
            context.sendBroadcast(intent)
            CommandResult(true, "تم إيقاف الموسيقى")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من إيقاف الموسيقى")
        }
    }
    
    /**
     * الانتقال للأغنية التالية
     */
    private fun nextTrack(): CommandResult {
        return try {
            val intent = Intent("com.android.music.musicservicecommand")
            intent.putExtra("command", "next")
            context.sendBroadcast(intent)
            CommandResult(true, "تم الانتقال للأغنية التالية")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من الانتقال للأغنية التالية")
        }
    }
    
    /**
     * الانتقال للأغنية السابقة
     */
    private fun previousTrack(): CommandResult {
        return try {
            val intent = Intent("com.android.music.musicservicecommand")
            intent.putExtra("command", "previous")
            context.sendBroadcast(intent)
            CommandResult(true, "تم الانتقال للأغنية السابقة")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من الانتقال للأغنية السابقة")
        }
    }
    
    /**
     * ضبط منبه
     */
    private fun setAlarm(command: String): CommandResult {
        return try {
            val time = extractTimeFromCommand(command)
            if (time != null) {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                intent.putExtra(AlarmClock.EXTRA_HOUR, time.first)
                intent.putExtra(AlarmClock.EXTRA_MINUTES, time.second)
                intent.putExtra(AlarmClock.EXTRA_MESSAGE, "منبه من الشخصية الأنمي")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                CommandResult(true, "تم ضبط المنبه على الساعة ${time.first}:${time.second}")
            } else {
                CommandResult(false, "لم أتمكن من فهم الوقت المطلوب. يرجى تحديد الوقت بوضوح.")
            }
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من ضبط المنبه: ${e.message}")
        }
    }
    
    /**
     * ضبط مؤقت
     */
    private fun setTimer(command: String): CommandResult {
        return try {
            val duration = extractDurationFromCommand(command)
            if (duration != null) {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER)
                intent.putExtra(AlarmClock.EXTRA_LENGTH, duration)
                intent.putExtra(AlarmClock.EXTRA_MESSAGE, "مؤقت من الشخصية الأنمي")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                CommandResult(true, "تم ضبط المؤقت لمدة ${duration / 60} دقيقة")
            } else {
                CommandResult(false, "لم أتمكن من فهم المدة المطلوبة. يرجى تحديد المدة بوضوح.")
            }
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من ضبط المؤقت: ${e.message}")
        }
    }
    
    /**
     * إنشاء تذكير
     */
    private fun setReminder(command: String): CommandResult {
        return try {
            // هذا يتطلب تكامل مع تطبيق التقويم أو التذكيرات
            val reminderText = extractReminderText(command)
            val time = extractTimeFromCommand(command)
            
            if (reminderText.isNotEmpty()) {
                // محاولة فتح تطبيق التذكيرات
                val intent = Intent(Intent.ACTION_INSERT)
                intent.data = Uri.parse("content://com.android.calendar/events")
                intent.putExtra("title", reminderText)
                intent.putExtra("description", "تذكير من الشخصية الأنمي")
                
                if (time != null) {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, time.first)
                    calendar.set(Calendar.MINUTE, time.second)
                    intent.putExtra("beginTime", calendar.timeInMillis)
                }
                
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                CommandResult(true, "تم إنشاء التذكير: $reminderText")
            } else {
                CommandResult(false, "لم أتمكن من فهم محتوى التذكير. يرجى تحديد ما تريد تذكيره.")
            }
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من إنشاء التذكير: ${e.message}")
        }
    }
    
    /**
     * فتح تطبيق
     */
    private fun openApp(command: String): CommandResult {
        return try {
            val appName = extractAppName(command)
            val packageName = getPackageNameForApp(appName)
            
            if (packageName != null) {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    CommandResult(true, "تم فتح تطبيق $appName")
                } else {
                    CommandResult(false, "لم أتمكن من فتح تطبيق $appName")
                }
            } else {
                CommandResult(false, "لم أتمكن من العثور على تطبيق $appName")
            }
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من فتح التطبيق: ${e.message}")
        }
    }
    
    /**
     * تعديل مستوى الصوت
     */
    private fun adjustVolume(command: String): CommandResult {
        return try {
            val isIncrease = command.contains("ارفع") || command.contains("زود")
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            
            val newVolume = if (isIncrease) {
                minOf(currentVolume + 2, maxVolume)
            } else {
                maxOf(currentVolume - 2, 0)
            }
            
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI)
            
            val action = if (isIncrease) "رفع" else "خفض"
            CommandResult(true, "تم $action مستوى الصوت")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من تعديل مستوى الصوت: ${e.message}")
        }
    }
    
    /**
     * تبديل حالة الواي فاي
     */
    private fun toggleWifi(): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult(true, "تم فتح إعدادات الواي فاي")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من الوصول لإعدادات الواي فاي: ${e.message}")
        }
    }
    
    /**
     * تبديل حالة البلوتوث
     */
    private fun toggleBluetooth(): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult(true, "تم فتح إعدادات البلوتوث")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من الوصول لإعدادات البلوتوث: ${e.message}")
        }
    }
    
    /**
     * فتح الإعدادات
     */
    private fun openSettings(): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult(true, "تم فتح الإعدادات")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من فتح الإعدادات: ${e.message}")
        }
    }
    
    /**
     * فتح الكاميرا لالتقاط صورة
     */
    private fun takePhoto(): CommandResult {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult(true, "تم فتح الكاميرا")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من فتح الكاميرا: ${e.message}")
        }
    }
    
    /**
     * فتح المتصفح
     */
    private fun openBrowser(command: String): CommandResult {
        return try {
            val url = extractUrlFromCommand(command) ?: "https://www.google.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            CommandResult(true, "تم فتح المتصفح")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من فتح المتصفح: ${e.message}")
        }
    }
    
    /**
     * البحث في الويب
     */
    private fun searchWeb(command: String): CommandResult {
        return try {
            val searchQuery = extractSearchQuery(command)
            if (searchQuery.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra("query", searchQuery)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                CommandResult(true, "تم البحث عن: $searchQuery")
            } else {
                CommandResult(false, "لم أتمكن من فهم ما تريد البحث عنه")
            }
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من إجراء البحث: ${e.message}")
        }
    }
    
    /**
     * الحصول على الوقت الحالي
     */
    private fun getCurrentTime(): CommandResult {
        return try {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val timeString = String.format("%02d:%02d", hour, minute)
            CommandResult(true, "الوقت الحالي هو $timeString")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من الحصول على الوقت: ${e.message}")
        }
    }
    
    /**
     * الحصول على التاريخ الحالي
     */
    private fun getCurrentDate(): CommandResult {
        return try {
            val calendar = Calendar.getInstance()
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val dateString = "$day/$month/$year"
            CommandResult(true, "التاريخ اليوم هو $dateString")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من الحصول على التاريخ: ${e.message}")
        }
    }
    
    /**
     * الحصول على معلومات الطقس
     */
    private fun getWeather(): CommandResult {
        return try {
            // هذا يتطلب API خارجي للطقس
            CommandResult(false, "ميزة الطقس تتطلب اتصال بالإنترنت وستكون متاحة في تحديث قادم")
        } catch (e: Exception) {
            CommandResult(false, "لم أتمكن من الحصول على معلومات الطقس: ${e.message}")
        }
    }
    
    // دوال مساعدة لاستخراج المعلومات من الأوامر
    
    private fun extractTimeFromCommand(command: String): Pair<Int, Int>? {
        // استخراج الوقت من الأمر (ساعة:دقيقة)
        val timeRegex = Regex("(\\d{1,2}):(\\d{2})")
        val match = timeRegex.find(command)
        return if (match != null) {
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            if (hour in 0..23 && minute in 0..59) {
                Pair(hour, minute)
            } else null
        } else {
            // محاولة استخراج الساعة فقط
            val hourRegex = Regex("الساعة (\\d{1,2})")
            val hourMatch = hourRegex.find(command)
            if (hourMatch != null) {
                val hour = hourMatch.groupValues[1].toInt()
                if (hour in 0..23) Pair(hour, 0) else null
            } else null
        }
    }
    
    private fun extractDurationFromCommand(command: String): Int? {
        // استخراج المدة بالثواني
        val minuteRegex = Regex("(\\d+)\\s*دقيقة")
        val minuteMatch = minuteRegex.find(command)
        if (minuteMatch != null) {
            return minuteMatch.groupValues[1].toInt() * 60
        }
        
        val secondRegex = Regex("(\\d+)\\s*ثانية")
        val secondMatch = secondRegex.find(command)
        if (secondMatch != null) {
            return secondMatch.groupValues[1].toInt()
        }
        
        return null
    }
    
    private fun extractReminderText(command: String): String {
        // استخراج نص التذكير
        val reminderRegex = Regex("ذكرني\\s+(.+)")
        val match = reminderRegex.find(command)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }
    
    private fun extractAppName(command: String): String {
        // استخراج اسم التطبيق
        val appRegex = Regex("افتح\\s+تطبيق\\s+(.+)")
        val match = appRegex.find(command)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }
    
    private fun extractUrlFromCommand(command: String): String? {
        // استخراج URL من الأمر
        val urlRegex = Regex("(https?://[^\\s]+)")
        val match = urlRegex.find(command)
        return match?.groupValues?.get(1)
    }
    
    private fun extractSearchQuery(command: String): String {
        // استخراج استعلام البحث
        val searchRegex = Regex("ابحث\\s+عن\\s+(.+)")
        val match = searchRegex.find(command)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }
    
    private fun getPackageNameForApp(appName: String): String? {
        // قاموس أسماء التطبيقات الشائعة
        val appPackages = mapOf(
            "واتساب" to "com.whatsapp",
            "فيسبوك" to "com.facebook.katana",
            "انستغرام" to "com.instagram.android",
            "تويتر" to "com.twitter.android",
            "يوتيوب" to "com.google.android.youtube",
            "جيميل" to "com.google.android.gm",
            "خرائط" to "com.google.android.apps.maps",
            "كاميرا" to "com.android.camera",
            "معرض" to "com.android.gallery3d",
            "متجر" to "com.android.vending",
            "كروم" to "com.android.chrome",
            "تقويم" to "com.android.calendar"
        )
        
        return appPackages[appName.lowercase()]
    }
    
    /**
     * التحقق من الصلاحيات المطلوبة
     */
    fun checkRequiredPermissions(): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        val requiredPermissions = listOf(
            android.Manifest.permission.SET_ALARM,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.INTERNET
        )
        
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        
        return missingPermissions
    }
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        // تنظيف أي موارد مستخدمة
        Log.d(TAG, "SystemIntegrationService cleaned up")
    }
}

