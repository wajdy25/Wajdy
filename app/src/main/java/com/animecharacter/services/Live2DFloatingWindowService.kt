package com.animecharacter.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat
import com.animecharacter.utils.PreferencesHelper
import kotlin.math.*

/**
 * خدمة النافذة العائمة المتقدمة لـ Live2D
 * تدعم السحب، تغيير الحجم، والتحكم الكامل في الموقع
 */
class Live2DFloatingWindowService : Service() {
    
    companion object {
        private const val TAG = "Live2DFloatingWindow"
        
        // أوامر الخدمة
        const val ACTION_START = "START_FLOATING_WINDOW"
        const val ACTION_STOP = "STOP_FLOATING_WINDOW"
        const val ACTION_UPDATE_SIZE = "UPDATE_SIZE"
        const val ACTION_UPDATE_POSITION = "UPDATE_POSITION"
        const val ACTION_UPDATE_ALPHA = "UPDATE_ALPHA"
        const val ACTION_SWITCH_CHARACTER = "SWITCH_CHARACTER"
        const val ACTION_CUSTOMIZE_CHARACTER = "CUSTOMIZE_CHARACTER"
        
        // المعاملات
        const val EXTRA_SCALE = "scale"
        const val EXTRA_X = "x"
        const val EXTRA_Y = "y"
        const val EXTRA_ALPHA = "alpha"
        const val EXTRA_CHARACTER_ID = "character_id"
        const val EXTRA_CUSTOMIZATION_OPTION_ID = "customization_option_id"
        const val EXTRA_CUSTOMIZATION_VALUE = "customization_value"
        
        // إعدادات افتراضية
        private const val DEFAULT_SCALE = 0.06f
        private const val MIN_SCALE = 0.03f
        private const val MAX_SCALE = 0.15f
        private const val DEFAULT_ALPHA = 1.0f
    }
    
    // مدير النوافذ والعرض
    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    
    // خدمة Live2D
    private var live2DService: Live2DService? = null
    
    // إعدادات النافذة
    private var currentScale = DEFAULT_SCALE
    private var currentAlpha = DEFAULT_ALPHA
    private var positionX = 100
    private var positionY = 100
    
    // إدارة اللمس والسحب
    private var isDragging = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialX = 0
    private var initialY = 0
    
    // كاشف الإيماءات
    private var gestureDetector: GestureDetectorCompat? = null
    
    // حالة الخدمة
    private var isWindowVisible = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "تم إنشاء خدمة النافذة العائمة")
        
        // تهيئة المكونات
        initializeComponents()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        hideFloatingWindow()
        cleanupResources()
        Log.d(TAG, "تم تدمير خدمة النافذة العائمة")
    }
    
    /**
     * تهيئة المكونات
     */
    private fun initializeComponents() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // تهيئة خدمة Live2D
        live2DService = Live2DService(this).apply {
            val activeCharacter = Live2DManager.getInstance(this@Live2DFloatingWindowService).getActiveCharacter()
            if (activeCharacter == null || !initialize(activeCharacter)) {
                Log.e(TAG, "فشل في تهيئة خدمة Live2D أو لا توجد شخصية نشطة")
                stopSelf()
                return
            }
        }
        
        // تحميل الإعدادات المحفوظة
        loadSettings()
        
        // إعداد كاشف الإيماءات
        setupGestureDetector()
        
        Log.d(TAG, "تم تهيئة المكونات بنجاح")
    }
    
    /**
     * معالجة الأوامر
     */
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START -> {
                showFloatingWindow()
            }
            ACTION_STOP -> {
                hideFloatingWindow()
                stopSelf()
            }
            ACTION_UPDATE_SIZE -> {
                val scale = intent.getFloatExtra(EXTRA_SCALE, currentScale)
                updateScale(scale)
            }
            ACTION_UPDATE_POSITION -> {
                val x = intent.getIntExtra(EXTRA_X, positionX)
                val y = intent.getIntExtra(EXTRA_Y, positionY)
                updatePosition(x, y)
            }
            ACTION_UPDATE_ALPHA -> {
                val alpha = intent.getFloatExtra(EXTRA_ALPHA, currentAlpha)
                updateAlpha(alpha)
            }
            ACTION_SWITCH_CHARACTER -> {
                val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID)
                if (characterId != null) {
                    live2DService?.loadCharacter(characterId)
                }
            }
            ACTION_CUSTOMIZE_CHARACTER -> {
                val optionId = intent.getStringExtra(EXTRA_CUSTOMIZATION_OPTION_ID)
                val value = intent.getStringExtra(EXTRA_CUSTOMIZATION_VALUE)
                if (optionId != null && value != null) {
                    live2DService?.customizeCharacter(optionId, value)
                }
            }
        }
    }
    
    /**
     * عرض النافذة العائمة
     */
    private fun showFloatingWindow() {
        if (isWindowVisible) return
        
        try {
            // إنشاء النافذة العائمة
            createFloatingWindow()
            
            // إضافة النافذة إلى مدير النوافذ
            windowManager?.addView(floatingView, layoutParams)
            
            // بدء خدمة Live2D
            live2DService?.startDisplay()
            
            isWindowVisible = true
            Log.d(TAG, "تم عرض النافذة العائمة")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في عرض النافذة العائمة", e)
        }
    }
    
    /**
     * إخفاء النافذة العائمة
     */
    private fun hideFloatingWindow() {
        if (!isWindowVisible) return
        
        try {
            // إيقاف خدمة Live2D
            live2DService?.stopDisplay()
            
            // إزالة النافذة من مدير النوافذ
            windowManager?.removeView(floatingView)
            
            isWindowVisible = false
            Log.d(TAG, "تم إخفاء النافذة العائمة")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في إخفاء النافذة العائمة", e)
        }
    }
    
    /**
     * إنشاء النافذة العائمة
     */
    private fun createFloatingWindow() {
        // إنشاء الحاوية الرئيسية
        floatingView = FrameLayout(this).apply {
            setBackgroundColor(0) // شفاف
        }
        
        // إعداد معاملات النافذة
        setupLayoutParams()
        
        // إعداد معالج اللمس
        setupTouchHandler()
        
        Log.d(TAG, "تم إنشاء النافذة العائمة")
    }
    
    /**
     * إعداد معاملات النافذة
     */
    private fun setupLayoutParams() {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        layoutParams = WindowManager.LayoutParams(
            calculateWindowWidth(),
            calculateWindowHeight(),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = positionX
            y = positionY
            alpha = currentAlpha
        }
    }
    
    /**
     * إعداد معالج اللمس
     */
    private fun setupTouchHandler() {
        floatingView?.setOnTouchListener { _, event ->
            gestureDetector?.onTouchEvent(event)
            handleTouchEvent(event)
            true
        }
    }
    
    /**
     * إعداد كاشف الإيماءات
     */
    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // تفاعل مع النقر المفرد
                live2DService?.let { service ->
                    // تشغيل تعبير عشوائي
                    val expressions = listOf("exp_01", "exp_02", "exp_03", "exp_04")
                    val randomExpression = expressions.random()
                    service.setExpression(randomExpression)
                }
                return true
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // تفاعل مع النقر المزدوج
                live2DService?.let { service ->
                    // تشغيل حركة خاصة
                    service.playMotion("Idle", false)
                }
                return true
            }
            
            override fun onLongPress(e: MotionEvent) {
                // تفاعل مع الضغط الطويل
                // يمكن إضافة قائمة خيارات هنا
                Log.d(TAG, "ضغط طويل على الشخصية")
            }
        })
    }
    
    /**
     * معالجة أحداث اللمس
     */
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialX = layoutParams?.x ?: 0
                initialY = layoutParams?.y ?: 0
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY
                
                // فحص ما إذا كان المستخدم يسحب
                if (!isDragging && (abs(deltaX) > 10 || abs(deltaY) > 10)) {
                    isDragging = true
                }
                
                if (isDragging) {
                    // تحديث موقع النافذة
                    val newX = initialX + deltaX.toInt()
                    val newY = initialY + deltaY.toInt()
                    
                    // التأكد من أن النافذة تبقى داخل الشاشة
                    val constrainedPosition = constrainToScreen(newX, newY)
                    updatePosition(constrainedPosition.first, constrainedPosition.second)
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    // حفظ الموقع الجديد
                    saveSettings()
                    isDragging = false
                }
                return true
            }
        }
        return false
    }
    
    /**
     * تحديث المقياس
     */
    private fun updateScale(newScale: Float) {
        val constrainedScale = newScale.coerceIn(MIN_SCALE, MAX_SCALE)
        if (currentScale == constrainedScale) return
        
        currentScale = constrainedScale
        
        // تحديث حجم النافذة
        layoutParams?.apply {
            width = calculateWindowWidth()
            height = calculateWindowHeight()
        }
        
        // تحديث خدمة Live2D
        live2DService?.setScale(currentScale)
        
        // تطبيق التغييرات
        if (isWindowVisible) {
            windowManager?.updateViewLayout(floatingView, layoutParams)
        }
        
        Log.d(TAG, "تم تحديث المقياس إلى: $currentScale")
    }
    
    /**
     * تحديث الموقع
     */
    private fun updatePosition(x: Int, y: Int) {
        positionX = x
        positionY = y
        
        layoutParams?.apply {
            this.x = x
            this.y = y
        }
        
        // تحديث خدمة Live2D
        live2DService?.setPosition(x.toFloat(), y.toFloat())
        
        // تطبيق التغييرات
        if (isWindowVisible) {
            windowManager?.updateViewLayout(floatingView, layoutParams)
        }
    }
    
    /**
     * تحديث الشفافية
     */
    private fun updateAlpha(newAlpha: Float) {
        val constrainedAlpha = newAlpha.coerceIn(0f, 1f)
        if (currentAlpha == constrainedAlpha) return
        
        currentAlpha = constrainedAlpha
        
        layoutParams?.alpha = currentAlpha
        
        // تحديث خدمة Live2D
        live2DService?.setAlpha(currentAlpha)
        
        // تطبيق التغييرات
        if (isWindowVisible) {
            windowManager?.updateViewLayout(floatingView, layoutParams)
        }
        
        Log.d(TAG, "تم تحديث الشفافية إلى: $currentAlpha")
    }
    
    /**
     * حساب عرض النافذة
     */
    private fun calculateWindowWidth(): Int {
        return WindowManager.LayoutParams.MATCH_PARENT
    }
    
    /**
     * حساب ارتفاع النافذة
     */
    private fun calculateWindowHeight(): Int {
        return WindowManager.LayoutParams.MATCH_PARENT
    }
    
    /**
     * تقييد الموقع داخل الشاشة
     */
    private fun constrainToScreen(x: Int, y: Int): Pair<Int, Int> {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val windowWidth = calculateWindowWidth()
        val windowHeight = calculateWindowHeight()
        
        val constrainedX = x.coerceIn(0, screenWidth - windowWidth)
        val constrainedY = y.coerceIn(0, screenHeight - windowHeight)
        
        return Pair(constrainedX, constrainedY)
    }
    
    /**
     * تحميل الإعدادات
     */
    private fun loadSettings() {
        val prefs = PreferencesHelper(this)
        
        currentScale = prefs.getFloat("live2d_scale", DEFAULT_SCALE)
        currentAlpha = prefs.getFloat("live2d_alpha", DEFAULT_ALPHA)
        positionX = prefs.getInt("live2d_position_x", 100)
        positionY = prefs.getInt("live2d_position_y", 100)
        
        Log.d(TAG, "تم تحميل الإعدادات: scale=$currentScale, alpha=$currentAlpha, pos=($positionX,$positionY)")
    }
    
    /**
     * حفظ الإعدادات
     */
    private fun saveSettings() {
        val prefs = PreferencesHelper(this)
        
        prefs.putFloat("live2d_scale", currentScale)
        prefs.putFloat("live2d_alpha", currentAlpha)
        prefs.putInt("live2d_position_x", positionX)
        prefs.putInt("live2d_position_y", positionY)
        
        Log.d(TAG, "تم حفظ الإعدادات")
    }
    
    /**
     * تنظيف الموارد
     */
    private fun cleanupResources() {
        live2DService?.cleanup()
        live2DService = null
        
        floatingView = null
        layoutParams = null
        windowManager = null
        gestureDetector = null
        
        Log.d(TAG, "تم تنظيف الموارد")
    }
    
    /**
     * الحصول على حالة النافذة
     */
    fun isWindowVisible(): Boolean = isWindowVisible
    
    /**
     * الحصول على الإعدادات الحالية
     */
    fun getCurrentSettings(): Map<String, Any> {
        return mapOf(
            "scale" to currentScale,
            "alpha" to currentAlpha,
            "x" to positionX,
            "y" to positionY,
            "visible" to isWindowVisible
        )
    }
}



    /**
     * إخفاء النافذة العائمة برمجياً
     */
    fun hideWindowProgrammatically() {
        if (isWindowVisible) {
            try {
                windowManager?.removeView(floatingView)
                isWindowVisible = false
                Log.d(TAG, "تم إخفاء النافذة العائمة برمجياً")
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في إخفاء النافذة العائمة برمجياً", e)
            }
        }
    }

    /**
     * إظهار النافذة العائمة برمجياً
     */
    fun showWindowProgrammatically() {
        if (!isWindowVisible && floatingView != null && layoutParams != null) {
            try {
                windowManager?.addView(floatingView, layoutParams)
                isWindowVisible = true
                Log.d(TAG, "تم إظهار النافذة العائمة برمجياً")
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في إظهار النافذة العائمة برمجياً", e)
            }
        }
    }


