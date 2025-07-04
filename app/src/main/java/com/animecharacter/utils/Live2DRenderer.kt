package com.animecharacter.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import com.animecharacter.models.Live2DModel
import kotlin.math.*

/**
 * محرك عرض Live2D لرسم النموذج على Canvas
 */
class Live2DRenderer(private val context: Context) {
    
    companion object {
        private const val TAG = "Live2DRenderer"
    }
    
    // النموذج والإعدادات
    private var model: Live2DModel? = null
    private var scale = 1.0f
    private var positionX = 0f
    private var positionY = 0f
    private var alpha = 1.0f
    
    // إعدادات العرض
    private var viewportWidth = 0
    private var viewportHeight = 0
    
    // أدوات الرسم
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    private val matrix = Matrix()
    private val tempRectF = RectF()
    
    // بيانات الصور المحملة
    private val textureBitmaps = mutableMapOf<String, Bitmap>()
    
    // حالة الرسم
    private var isInitialized = false
    private var lastUpdateTime = 0L
    
    /**
     * تعيين النموذج
     */
    fun setModel(newModel: Live2DModel) {
        model = newModel
        loadTextures()
        isInitialized = true
        Log.d(TAG, "تم تعيين النموذج: ${newModel.getModelName()}")
    }
    
    /**
     * تحميل الصور
     */
    private fun loadTextures() {
        val model = this.model ?: return
        
        textureBitmaps.clear()
        
        val textureData = model.getTextureData()
        for ((path, data) in textureData) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) {
                    textureBitmaps[path] = bitmap
                    Log.d(TAG, "تم تحميل الصورة: $path (${bitmap.width}x${bitmap.height})")
                } else {
                    Log.w(TAG, "فشل في فك تشفير الصورة: $path")
                }
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في تحميل الصورة: $path", e)
            }
        }
    }
    
    /**
     * تحديث منطقة العرض
     */
    fun updateViewport(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        Log.d(TAG, "تم تحديث منطقة العرض: ${width}x${height}")
    }
    
    /**
     * تعيين المقياس
     */
    fun setScale(newScale: Float) {
        scale = newScale
        Log.d(TAG, "تم تعيين المقياس: $scale")
    }
    
    /**
     * تعيين الموقع
     */
    fun setPosition(x: Float, y: Float) {
        positionX = x
        positionY = y
        Log.d(TAG, "تم تعيين الموقع: ($x, $y)")
    }
    
    /**
     * تعيين الشفافية
     */
    fun setAlpha(newAlpha: Float) {
        alpha = newAlpha.coerceIn(0f, 1f)
        Log.d(TAG, "تم تعيين الشفافية: $alpha")
    }
    
    /**
     * عرض النموذج
     */
    fun render(canvas: Canvas) {
        if (!isInitialized || model == null) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val deltaTime = if (lastUpdateTime > 0) {
            (currentTime - lastUpdateTime) / 1000f
        } else {
            0f
        }
        lastUpdateTime = currentTime
        
        // تحديث النموذج
        model?.update(deltaTime)
        
        // حفظ حالة Canvas
        canvas.save()
        
        try {
            // تطبيق التحويلات
            setupTransform(canvas)
            
            // تطبيق الشفافية
            paint.alpha = (alpha * 255).toInt()
            
            // رسم النموذج
            drawModel(canvas)
            
        } finally {
            // استعادة حالة Canvas
            canvas.restore()
        }
    }
    
    /**
     * إعداد التحويلات
     */
    private fun setupTransform(canvas: Canvas) {
        // انتقال إلى الموقع المحدد
        canvas.translate(positionX, positionY)
        
        // تطبيق المقياس
        canvas.scale(scale, scale)
        
        // توسيط النموذج
        if (viewportWidth > 0 && viewportHeight > 0) {
            canvas.translate(viewportWidth / 2f, viewportHeight / 2f)
        }
    }
    
    /**
     * رسم النموذج
     */
    private fun drawModel(canvas: Canvas) {
        // رسم مبسط للنموذج
        // في التطبيق الحقيقي، نحتاج لمحرك عرض Live2D متقدم
        
        if (textureBitmaps.isNotEmpty()) {
            // رسم الصورة الأولى كتمثيل مبسط
            val firstTexture = textureBitmaps.values.first()
            
            // حساب الحجم المناسب
            val modelWidth = firstTexture.width.toFloat()
            val modelHeight = firstTexture.height.toFloat()
            
            // رسم الصورة
            tempRectF.set(
                -modelWidth / 2f,
                -modelHeight / 2f,
                modelWidth / 2f,
                modelHeight / 2f
            )
            
            canvas.drawBitmap(firstTexture, null, tempRectF, paint)        } else {
            // رسم شكل بديل إذا لم تتوفر الصور
            drawPlaceholder(canvas)
        }
    }
    

    /**
     * رسم شكل بديل
     */
    private fun drawPlaceholder(canvas: Canvas) {
        val placeholderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.MAGENTA
            alpha = (alpha * 200).toInt()
        }
        
        // رسم دائرة كشكل بديل
        canvas.drawCircle(0f, 0f, 50f, placeholderPaint)
        
        // رسم نص
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText("Live2D", 0f, 5f, textPaint)
    }
    
    /**
     * الحصول على حدود النموذج
     */
    fun getModelBounds(): RectF {
        if (textureBitmaps.isNotEmpty()) {
            val firstTexture = textureBitmaps.values.first()
            val width = firstTexture.width * scale
            val height = firstTexture.height * scale
            
            return RectF(
                positionX - width / 2f,
                positionY - height / 2f,
                positionX + width / 2f,
                positionY + height / 2f
            )
        }
        
        return RectF(positionX - 50f, positionY - 50f, positionX + 50f, positionY + 50f)
    }
    
    /**
     * فحص ما إذا كانت النقطة داخل النموذج
     */
    fun isPointInModel(x: Float, y: Float): Boolean {
        val bounds = getModelBounds()
        return bounds.contains(x, y)
    }
    
    /**
     * تنظيف الموارد
     */
    fun dispose() {
        for (bitmap in textureBitmaps.values) {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        textureBitmaps.clear()
        
        model = null
        isInitialized = false
        
        Log.d(TAG, "تم تنظيف موارد محرك العرض")
    }
    
    /**
     * الحصول على حالة التهيئة
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * الحصول على النموذج الحالي
     */
    fun getModel(): Live2DModel? = model
}

