package com.animecharacter.models

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * نموذج Live2D يمثل الشخصية والبيانات المرتبطة بها
 */
class Live2DModel private constructor(
    private val context: Context,
    private val modelData: JSONObject,
    private val basePath: String
) {
    
    companion object {
        private const val TAG = "Live2DModel"
        
        /**
         * تحميل نموذج من مجلد الأصول
         */
        fun loadFromAssets(context: Context, modelPath: String): Live2DModel? {
            return try {
                val inputStream = context.assets.open(modelPath)
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val modelData = JSONObject(jsonString)
                
                val basePath = modelPath.substringBeforeLast("/")
                
                Live2DModel(context, modelData, basePath).apply {
                    initialize()
                }
            } catch (e: Exception) {
                Log.e(TAG, "فشل في تحميل النموذج: $modelPath", e)
                null
            }
        }
    }
    
    // بيانات النموذج
    private var mocData: ByteArray? = null
    private var textureData: MutableMap<String, ByteArray> = ConcurrentHashMap()
    
    // التعبيرات والحركات
    private val expressions = mutableMapOf<String, JSONObject>()
    private val motions = mutableMapOf<String, MutableList<JSONObject>>()
    
    // الحالة الحالية
    private var currentExpression: String? = null
    private var currentMotion: String? = null
    private var isPlaying = false
    
    // إعدادات الفيزياء والوضعية
    private var physicsData: JSONObject? = null
    private var poseData: JSONObject? = null
    private var displayInfo: JSONObject? = null
    
    // معاملات النموذج
    private val parameters = mutableMapOf<String, Float>()
    private val parts = mutableMapOf<String, Float>()
    
    /**
     * تهيئة النموذج
     */
    private fun initialize() {
        try {
            loadMocData()
            loadTextures()
            loadExpressions()
            loadMotions()
            loadPhysics()
            loadPose()
            loadDisplayInfo()
            
            Log.d(TAG, "تم تهيئة النموذج بنجاح")
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تهيئة النموذج", e)
            throw e
        }
    }
    
    /**
     * تحميل بيانات MOC
     */
    private fun loadMocData() {
        val fileReferences = modelData.getJSONObject("FileReferences")
        val mocFile = fileReferences.getString("Moc")
        val mocPath = "$basePath/$mocFile"
        
        mocData = loadAssetFile(mocPath)
        Log.d(TAG, "تم تحميل بيانات MOC: $mocPath")
    }
    
    /**
     * تحميل الصور
     */
    private fun loadTextures() {
        val fileReferences = modelData.getJSONObject("FileReferences")
        val textures = fileReferences.getJSONArray("Textures")
        
        for (i in 0 until textures.length()) {
            val texturePath = textures.getString(i)
            val fullPath = "$basePath/$texturePath"
            
            try {
                val textureBytes = loadAssetFile(fullPath)
                textureData[texturePath] = textureBytes
                Log.d(TAG, "تم تحميل الصورة: $fullPath")
            } catch (e: Exception) {
                Log.w(TAG, "فشل في تحميل الصورة: $fullPath", e)
            }
        }
    }
    
    /**
     * تحميل التعبيرات
     */
    private fun loadExpressions() {
        val fileReferences = modelData.getJSONObject("FileReferences")
        if (!fileReferences.has("Expressions")) return
        
        val expressionArray = fileReferences.getJSONArray("Expressions")
        
        for (i in 0 until expressionArray.length()) {
            val expObj = expressionArray.getJSONObject(i)
            val name = expObj.getString("Name")
            val file = expObj.getString("File")
            val fullPath = "$basePath/$file"
            
            try {
                val expData = loadAssetJson(fullPath)
                expressions[name] = expData
                Log.d(TAG, "تم تحميل التعبير: $name")
            } catch (e: Exception) {
                Log.w(TAG, "فشل في تحميل التعبير: $name", e)
            }
        }
    }
    
    /**
     * تحميل الحركات
     */
    private fun loadMotions() {
        val fileReferences = modelData.getJSONObject("FileReferences")
        if (!fileReferences.has("Motions")) return
        
        val motionObj = fileReferences.getJSONObject("Motions")
        val keys = motionObj.keys()
        
        while (keys.hasNext()) {
            val groupName = keys.next()
            val motionArray = motionObj.getJSONArray(groupName)
            val motionList = mutableListOf<JSONObject>()
            
            for (i in 0 until motionArray.length()) {
                val motionItem = motionArray.getJSONObject(i)
                val file = motionItem.getString("File")
                val fullPath = "$basePath/$file"
                
                try {
                    val motionData = loadAssetJson(fullPath)
                    motionList.add(motionData)
                    Log.d(TAG, "تم تحميل الحركة: $groupName[$i]")
                } catch (e: Exception) {
                    Log.w(TAG, "فشل في تحميل الحركة: $groupName[$i]", e)
                }
            }
            
            if (motionList.isNotEmpty()) {
                motions[groupName] = motionList
            }
        }
    }
    
    /**
     * تحميل إعدادات الفيزياء
     */
    private fun loadPhysics() {
        val fileReferences = modelData.getJSONObject("FileReferences")
        if (!fileReferences.has("Physics")) return
        
        val physicsFile = fileReferences.getString("Physics")
        val fullPath = "$basePath/$physicsFile"
        
        try {
            physicsData = loadAssetJson(fullPath)
            Log.d(TAG, "تم تحميل إعدادات الفيزياء")
        } catch (e: Exception) {
            Log.w(TAG, "فشل في تحميل إعدادات الفيزياء", e)
        }
    }
    
    /**
     * تحميل إعدادات الوضعية
     */
    private fun loadPose() {
        val fileReferences = modelData.getJSONObject("FileReferences")
        if (!fileReferences.has("Pose")) return
        
        val poseFile = fileReferences.getString("Pose")
        val fullPath = "$basePath/$poseFile"
        
        try {
            poseData = loadAssetJson(fullPath)
            Log.d(TAG, "تم تحميل إعدادات الوضعية")
        } catch (e: Exception) {
            Log.w(TAG, "فشل في تحميل إعدادات الوضعية", e)
        }
    }
    
    /**
     * تحميل معلومات العرض
     */
    private fun loadDisplayInfo() {
        val fileReferences = modelData.getJSONObject("FileReferences")
        if (!fileReferences.has("DisplayInfo")) return
        
        val displayFile = fileReferences.getString("DisplayInfo")
        val fullPath = "$basePath/$displayFile"
        
        try {
            displayInfo = loadAssetJson(fullPath)
            Log.d(TAG, "تم تحميل معلومات العرض")
        } catch (e: Exception) {
            Log.w(TAG, "فشل في تحميل معلومات العرض", e)
        }
    }
    
    /**
     * تحميل ملف من الأصول
     */
    private fun loadAssetFile(path: String): ByteArray {
        return context.assets.open(path).use { it.readBytes() }
    }
    
    /**
     * تحميل ملف JSON من الأصول
     */
    private fun loadAssetJson(path: String): JSONObject {
        val jsonString = context.assets.open(path).bufferedReader().use { it.readText() }
        return JSONObject(jsonString)
    }
    
    /**
     * تعيين تعبير
     */
    fun setExpression(expressionName: String) {
        if (!expressions.containsKey(expressionName)) {
            Log.w(TAG, "التعبير غير موجود: $expressionName")
            return
        }
        
        currentExpression = expressionName
        val expData = expressions[expressionName]!!
        
        // تطبيق معاملات التعبير
        if (expData.has("Parameters")) {
            val params = expData.getJSONArray("Parameters")
            for (i in 0 until params.length()) {
                val param = params.getJSONObject(i)
                val id = param.getString("Id")
                val value = param.getDouble("Value").toFloat()
                
                parameters[id] = value
            }
        }
        
        Log.d(TAG, "تم تعيين التعبير: $expressionName")
    }
    
    /**
     * تشغيل حركة
     */
    fun playMotion(motionGroup: String, loop: Boolean = false) {
        if (!motions.containsKey(motionGroup)) {
            Log.w(TAG, "مجموعة الحركات غير موجودة: $motionGroup")
            return
        }
        
        val motionList = motions[motionGroup]!!
        if (motionList.isEmpty()) return
        
        // اختيار حركة عشوائية من المجموعة
        val randomMotion = motionList.random()
        currentMotion = motionGroup
        isPlaying = true
        
        // تطبيق الحركة (هذا مبسط - في التطبيق الحقيقي نحتاج لمحرك حركة متقدم)
        Log.d(TAG, "تم تشغيل الحركة: $motionGroup")
    }
    
    /**
     * إيقاف الحركة
     */
    fun stopMotion() {
        isPlaying = false
        currentMotion = null
        Log.d(TAG, "تم إيقاف الحركة")
    }
    
    /**
     * معالجة اللمس
     */
    fun onTouch(x: Float, y: Float) {
        // تحديد منطقة اللمس
        val hitAreas = getHitAreas()
        
        for (area in hitAreas) {
            if (isPointInArea(x, y, area)) {
                // تشغيل تفاعل مناسب
                when (area.getString("Id")) {
                    "HitAreaHead" -> {
                        // تشغيل تعبير سعيد أو حركة رأس
                        setExpression("exp_01")
                        playMotion("Idle")
                    }
                    "HitAreaBody" -> {
                        // تشغيل حركة جسم
                        playMotion("")
                    }
                }
                break
            }
        }
        
        Log.d(TAG, "تم التفاعل مع اللمس في: ($x, $y)")
    }
    
    /**
     * الحصول على مناطق اللمس
     */
    private fun getHitAreas(): List<JSONObject> {
        val areas = mutableListOf<JSONObject>()
        
        if (modelData.has("HitAreas")) {
            val hitAreas = modelData.getJSONArray("HitAreas")
            for (i in 0 until hitAreas.length()) {
                areas.add(hitAreas.getJSONObject(i))
            }
        }
        
        return areas
    }
    
    /**
     * فحص ما إذا كانت النقطة داخل المنطقة
     */
    private fun isPointInArea(x: Float, y: Float, area: JSONObject): Boolean {
        // تطبيق مبسط - في التطبيق الحقيقي نحتاج لحساب دقيق للمناطق
        return x >= 0 && y >= 0 && x <= 1 && y <= 1
    }
    
    /**
     * تحديث النموذج
     */
    fun update(deltaTime: Float) {
        // تحديث الحركات والتعبيرات
        if (isPlaying && currentMotion != null) {
            // تحديث الحركة الحالية
            updateMotion(deltaTime)
        }
        
        // تحديث الفيزياء
        updatePhysics(deltaTime)
        
        // تحديث الوضعية
        updatePose(deltaTime)
    }
    
    /**
     * تحديث الحركة
     */
    private fun updateMotion(deltaTime: Float) {
        // تطبيق مبسط لتحديث الحركة
        // في التطبيق الحقيقي نحتاج لمحرك حركة متقدم
    }
    
    /**
     * تحديث الفيزياء
     */
    private fun updatePhysics(deltaTime: Float) {
        // تطبيق مبسط لتحديث الفيزياء
        // في التطبيق الحقيقي نحتاج لمحاكاة فيزيائية
    }
    
    /**
     * تحديث الوضعية
     */
    private fun updatePose(deltaTime: Float) {
        // تطبيق مبسط لتحديث الوضعية
    }
    
    /**
     * الحصول على اسم النموذج
     */
    fun getModelName(): String {
        return basePath.substringAfterLast("/")
    }
    
    /**
     * الحصول على بيانات MOC
     */
    fun getMocData(): ByteArray? = mocData
    
    /**
     * الحصول على بيانات الصور
     */
    fun getTextureData(): Map<String, ByteArray> = textureData
    
    /**
     * الحصول على التعبيرات المتاحة
     */
    fun getAvailableExpressions(): List<String> = expressions.keys.toList()
    
    /**
     * الحصول على الحركات المتاحة
     */
    fun getAvailableMotions(): List<String> = motions.keys.toList()
    
    /**
     * الحصول على التعبير الحالي
     */
    fun getCurrentExpression(): String? = currentExpression
    
    /**
     * الحصول على الحركة الحالية
     */
    fun getCurrentMotion(): String? = currentMotion
    
    /**
     * فحص ما إذا كانت الحركة قيد التشغيل
     */
    fun isMotionPlaying(): Boolean = isPlaying
    
    /**
     * تنظيف الموارد
     */
    fun dispose() {
        mocData = null
        textureData.clear()
        expressions.clear()
        motions.clear()
        parameters.clear()
        parts.clear()
        
        Log.d(TAG, "تم تنظيف موارد النموذج")
    }
}

