package com.animecharacter.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: Activity) {

    companion object {
        const val REQUEST_RECORD_AUDIO = 1001
        const val REQUEST_OVERLAY_PERMISSION = 1002
        const val REQUEST_NOTIFICATION_PERMISSION = 1003
        const val REQUEST_STORAGE_PERMISSION = 1004
    }

    /**
     * فحص صلاحية الميكروفون
     */
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * طلب صلاحية الميكروفون
     */
    fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO
        )
    }

    /**
     * فحص صلاحية النافذة العائمة
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(activity)
        } else {
            true
        }
    }

    /**
     * طلب صلاحية النافذة العائمة
     */
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        }
    }

    /**
     * فحص صلاحية الإشعارات (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * طلب صلاحية الإشعارات
     */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    /**
     * فحص صلاحية قراءة الملفات
     */
    fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * طلب صلاحية قراءة الملفات
     */
    fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_STORAGE_PERMISSION
        )
    }

    /**
     * فحص جميع الصلاحيات المطلوبة
     */
    fun hasAllRequiredPermissions(): Boolean {
        return hasRecordAudioPermission() && 
               hasOverlayPermission() && 
               hasNotificationPermission()
    }

    /**
     * طلب جميع الصلاحيات المطلوبة
     */
    fun requestAllRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!hasRecordAudioPermission()) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (!hasStoragePermission()) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                REQUEST_RECORD_AUDIO
            )
        }

        // طلب صلاحية النافذة العائمة بشكل منفصل
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
        }
    }

    /**
     * فحص ما إذا كان المستخدم رفض الصلاحية نهائياً
     */
    fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * فتح إعدادات التطبيق
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }

    /**
     * معالجة نتيجة طلب الصلاحيات
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: (String) -> Unit
    ) {
        when (requestCode) {
            REQUEST_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    onDenied("صلاحية الميكروفون مطلوبة للتفاعل الصوتي")
                }
            }
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    onDenied("صلاحية الإشعارات مطلوبة لتنبيهك عند تشغيل الميكروفون")
                }
            }
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    onDenied("صلاحية قراءة الملفات مطلوبة لإضافة شخصيات مخصصة")
                }
            }
        }
    }

    /**
     * معالجة نتيجة طلب صلاحية النافذة العائمة
     */
    fun handleOverlayPermissionResult(
        requestCode: Int,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (hasOverlayPermission()) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }

    /**
     * الحصول على رسالة توضيحية للصلاحية
     */
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> 
                "يحتاج التطبيق لصلاحية الميكروفون للتفاعل الصوتي مع الشخصية"
            Manifest.permission.POST_NOTIFICATIONS -> 
                "يحتاج التطبيق لصلاحية الإشعارات لتنبيهك عند تشغيل الميكروفون"
            Manifest.permission.READ_EXTERNAL_STORAGE -> 
                "يحتاج التطبيق لصلاحية قراءة الملفات لإضافة شخصيات مخصصة"
            else -> "هذه الصلاحية مطلوبة لتشغيل التطبيق بشكل صحيح"
        }
    }

    /**
     * فحص ما إذا كانت الصلاحية مهمة أم لا
     */
    fun isPermissionCritical(permission: String): Boolean {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> true
            else -> false
        }
    }
}

