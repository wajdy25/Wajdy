package com.animecharacter.services

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import com.animecharacter.models.Live2DModel
import com.animecharacter.utils.Live2DRenderer
import com.animecharacter.utils.PreferencesHelper
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * خدمة Live2D الأساسية لعرض الشخصية العائمة
 * تدعم العرض، الحركات، التعبيرات، والتفاعل مع المستخدم
 */
import com.animecharacter.models.Character
import com.animecharacter.models.CustomizationOption

class Live2DService(private val context: Context) {
    
    companion object {
        private const val TAG = "Live2DService"
        
        // إعدادات العرض
        private const val DEFAULT_SCALE = 1.0f // 100% من عرض الشاشة
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 1.0f
        
        // معدل الإطارات
        private const val TARGET_FPS = 30
        private const val FRAME_TIME_MS = 1000L / TARGET_FPS
    }
    
    // المتغيرات الأساسية
    private var live2DModel: Live2DModel? = null
    private var renderer: Live2DRenderer? = null
    private var surfaceView: SurfaceView? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var currentCharacter: Character? = null
    
    // إدارة الحالة
    private val isInitialized = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)
    private val renderScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // إعدادات العرض
    private var currentScale = DEFAULT_SCALE
    private var positionX = 100f
    private var positionY = 100f
    private var alpha = 1.0f
    
    // إدارة الحركات والتعبيرات
    private var currentExpression = ""
    private var currentMotion = ""
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * تهيئة خدمة Live2D
     */
    fun initialize(character: Character): Boolean {
        return try {
            Log.d(TAG, "بدء تهيئة خدمة Live2D...")
            currentCharacter = character
            
            // تحميل النموذج
            loadModel(character.modelPath)
            
            // إعداد العرض
            setupRenderer()
            
            // إعداد النافذة العائمة
            setupFloatingWindow()
            
            isInitialized.set(true)
            Log.d(TAG, "تم تهيئة خدمة Live2D بنجاح")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تهيئة خدمة Live2D", e)
            false
        }
    }
    
    /**
     * تحميل نموذج Live2D
     */
    private fun loadModel(modelPath: String) {
        try {
            live2DModel = Live2DModel.loadFromAssets(context, modelPath)
            
            if (live2DModel == null) {
                throw IOException("فشل في تحميل نموذج Live2D من: $modelPath")
            }
            
            Log.d(TAG, "تم تحميل النموذج بنجاح: ${live2DModel!!.getModelName()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تحميل النموذج", e)
            throw e
        }
    }
    
    /**
     * إعداد محرك العرض
     */
    private fun setupRenderer() {
        renderer = Live2DRenderer(context).apply {
            setModel(live2DModel!!)
            setScale(currentScale)
            setPosition(positionX, positionY)
            setAlpha(alpha)
        }
        
        Log.d(TAG, "تم إعداد محرك العرض")
    }
    
    /**
     * إعداد النافذة العائمة
     */
    private fun setupFloatingWindow() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // إعداد معاملات النافذة
        layoutParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            format = PixelFormat.TRANSLUCENT
            
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            x = positionX.toInt()
            y = positionY.toInt()
        }
        
        // إنشاء SurfaceView للعرض
        surfaceView = object : SurfaceView(context), SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "تم إنشاء السطح")
                startRendering()
            }
            
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d(TAG, "تم تغيير السطح: ${width}x${height}")
                renderer?.updateViewport(width, height)
            }
            
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "تم تدمير السطح")
                stopRendering()
            }
            
            override fun onTouchEvent(event: MotionEvent): Boolean {
                return handleTouch(event)
            }
        }.apply {
            holder.addCallback(this)
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        
        Log.d(TAG, "تم إعداد النافذة العائمة")
    }
    
    /**
     * بدء عرض الشخصية
     */
    fun startDisplay() {
        if (!isInitialized.get()) {
            Log.w(TAG, "الخدمة غير مهيأة")
            return
        }
        
        try {
            windowManager?.addView(surfaceView, layoutParams)
            isRunning.set(true)
            Log.d(TAG, "تم بدء عرض الشخصية")
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في بدء العرض", e)
        }
    }
    
    /**
     * إيقاف عرض الشخصية
     */
    fun stopDisplay() {
        if (!isRunning.get()) return
        
        try {
            stopRendering()
            windowManager?.removeView(surfaceView)
            isRunning.set(false)
            Log.d(TAG, "تم إيقاف عرض الشخصية")
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في إيقاف العرض", e)
        }
    }
    
    /**
     * بدء حلقة العرض
     */
    private fun startRendering() {
        if (isRunning.get()) return
        
        renderScope.launch {
            while (isRunning.get()) {
                val startTime = System.currentTimeMillis()
                
                try {
                    render()
                } catch (e: Exception) {
                    Log.e(TAG, "خطأ في العرض", e)
                }
                
                val elapsed = System.currentTimeMillis() - startTime
                val delay = maxOf(0, FRAME_TIME_MS - elapsed)
                
                if (delay > 0) {
                    delay(delay)
                }
            }
        }
    }
    
    /**
     * إيقاف حلقة العرض
     */
    private fun stopRendering() {
        isRunning.set(false)
    }
    
    /**
     * عرض الإطار
     */
    private fun render() {
        val holder = surfaceView?.holder ?: return
        val canvas = holder.lockCanvas() ?: return
        
        try {
            // مسح الخلفية
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
            
            // عرض النموذج
            renderer?.render(canvas)
            
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
    
    /**
     * تغيير التعبير
     */
    fun setExpression(expressionName: String) {
        if (currentExpression == expressionName) return
        
        currentExpression = expressionName
        live2DModel?.setExpression(expressionName)
        
        Log.d(TAG, "تم تغيير التعبير إلى: $expressionName")
    }
    
    /**
     * تشغيل حركة
     */
    fun playMotion(motionName: String, loop: Boolean = false) {
        if (currentMotion == motionName) return
        
        currentMotion = motionName
        live2DModel?.playMotion(motionName, loop)
        
        Log.d(TAG, "تم تشغيل الحركة: $motionName")
    }
    
    /**
     * تغيير حجم الشخصية
     */
    fun setScale(scale: Float) {
        val newScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        if (currentScale == newScale) return
        
        currentScale = newScale
        renderer?.setScale(newScale)
        
        // تحديث حجم النافذة
        layoutParams?.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
        
        if (isRunning.get()) {
            windowManager?.updateViewLayout(surfaceView, layoutParams)
        }
        
        Log.d(TAG, "تم تغيير الحجم إلى: $newScale")
    }
    
    /**
     * تغيير موقع الشخصية
     */
    fun setPosition(x: Float, y: Float) {
        positionX = x
        positionY = y
        
        renderer?.setPosition(x, y)
        
        layoutParams?.apply {
            this.x = x.toInt()
            this.y = y.toInt()
        }
        
        if (isRunning.get()) {
            windowManager?.updateViewLayout(surfaceView, layoutParams)
        }
        
        Log.d(TAG, "تم تغيير الموقع إلى: ($x, $y)")
    }
    
    /**
     * تغيير الشفافية
     */
    fun setAlpha(newAlpha: Float) {
        alpha = newAlpha.coerceIn(0f, 1f)
        renderer?.setAlpha(alpha)
        
        Log.d(TAG, "تم تغيير الشفافية إلى: $alpha")
    }
    
    /**
     * معالجة اللمس
     */
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var initialPositionX: Float = 0f
    private var initialPositionY: Float = 0f

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialPositionX = positionX
                initialPositionY = positionY
                live2DModel?.onTouch(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY
                setPosition(initialPositionX + deltaX, initialPositionY + deltaY)
                return true
            }
        }
        return false
    }
    
    /**
     * تحميل شخصية جديدة
     */
    fun loadCharacter(characterId: String) {
        // هنا يجب أن يكون لديك طريقة لجلب بيانات الشخصية من مكان ما (مثل قاعدة بيانات أو ملفات)
        // حالياً، سنستخدم الشخصية الافتراضية
        val character = currentCharacter // افترض أن currentCharacter هو الشخصية المطلوبة
        if (character != null) {
            try {
                // تنظيف النموذج القديم
                live2DModel?.dispose()
                
                // تحميل النموذج الجديد
                loadModel(character.modelPath)
                renderer?.setModel(live2DModel!!)
                
                // تطبيق الرسوم المتحركة الافتراضية
                character.defaultAnimations["idle"]?.let { playMotion(it, true) }
                
                Log.d(TAG, "تم تحميل الشخصية الجديدة: ${character.name}")
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في تحميل الشخصية: $characterId", e)
            }
        }
    }
    
    /**
     * تخصيص الشخصية
     */
    fun customizeCharacter(optionId: String, value: String) {
        val character = currentCharacter
        if (character != null) {
            val option = character.customizationOptions.find { it.id == optionId }
            if (option != null) {
                when (option.type) {
                    CustomizationOption.CustomizationType.BACKGROUND -> {
                        // TODO: تطبيق تغيير الخلفية
                        Log.d(TAG, "تغيير الخلفية إلى: $value")
                    }
                    CustomizationOption.CustomizationType.APPEARANCE -> {
                        // TODO: تطبيق تغيير المظهر (يتطلب دعم Live2D لتغيير الأجزاء)
                        Log.d(TAG, "تغيير المظهر إلى: $value")
                    }
                    CustomizationOption.CustomizationType.COLOR -> {
                        // TODO: تطبيق تغيير اللون
                        Log.d(TAG, "تغيير اللون إلى: $value")
                    }
                    CustomizationOption.CustomizationType.EFFECT -> {
                        // TODO: تطبيق تغيير التأثير
                        Log.d(TAG, "تغيير التأثير إلى: $value")
                    }
                }
            }
        }
    }
    

    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        stopDisplay()
        renderScope.cancel()
        live2DModel?.dispose()
        renderer?.dispose()
        
        Log.d(TAG, "تم تنظيف موارد Live2D")
    }
    
    /**
     * الحصول على حالة الخدمة
     */
    fun isRunning(): Boolean = isRunning.get()
    
    /**
     * الحصول على حالة التهيئة
     */
    fun isInitialized(): Boolean = isInitialized.get()
}
