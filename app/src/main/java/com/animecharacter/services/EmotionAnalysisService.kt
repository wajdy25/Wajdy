package com.animecharacter.services

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.google.cloud.speech.v1.*
import com.google.cloud.language.v1.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * خدمة تحليل المشاعر من الصوت والنص
 * تدعم التحليل المحلي والسحابي للمشاعر
 */
class EmotionAnalysisService(private val context: Context) {
    
    companion object {
        private const val TAG = "EmotionAnalysisService"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    // حالات المشاعر المدعومة
    enum class Emotion(val arabicName: String, val intensity: Float) {
        HAPPY("سعيد", 0.8f),
        SAD("حزين", 0.7f),
        ANGRY("غاضب", 0.9f),
        SURPRISED("متفاجئ", 0.6f),
        FEAR("خائف", 0.8f),
        DISGUST("مشمئز", 0.7f),
        NEUTRAL("محايد", 0.3f),
        EXCITED("متحمس", 0.9f),
        CALM("هادئ", 0.4f),
        CONFUSED("محتار", 0.5f)
    }
    
    // نتيجة تحليل المشاعر
    data class EmotionResult(
        val primaryEmotion: Emotion,
        val confidence: Float,
        val emotionScores: Map<Emotion, Float>,
        val arousal: Float, // مستوى الإثارة
        val valence: Float, // مستوى الإيجابية/السلبية
        val analysisMethod: String
    )
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * تحليل المشاعر من النص
     */
    suspend fun analyzeTextEmotion(text: String): EmotionResult = withContext(Dispatchers.IO) {
        try {
            // تحليل محلي بسيط للنص
            val localResult = analyzeTextLocally(text)
            
            // محاولة التحليل السحابي إذا كان متاحاً
            try {
                val cloudResult = analyzeTextWithCloud(text)
                return@withContext cloudResult ?: localResult
            } catch (e: Exception) {
                Log.w(TAG, "Cloud analysis failed, using local result", e)
                return@withContext localResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Text emotion analysis failed", e)
            return@withContext EmotionResult(
                Emotion.NEUTRAL, 0.5f, mapOf(Emotion.NEUTRAL to 0.5f),
                0.5f, 0.5f, "fallback"
            )
        }
    }
    
    /**
     * تحليل المشاعر من الصوت
     */
    suspend fun analyzeAudioEmotion(audioData: ByteArray): EmotionResult = withContext(Dispatchers.IO) {
        try {
            // استخراج الميزات الصوتية
            val audioFeatures = extractAudioFeatures(audioData)
            
            // تحليل محلي للصوت
            val localResult = analyzeAudioLocally(audioFeatures)
            
            // محاولة التحليل السحابي
            try {
                val cloudResult = analyzeAudioWithCloud(audioData)
                return@withContext cloudResult ?: localResult
            } catch (e: Exception) {
                Log.w(TAG, "Cloud audio analysis failed, using local result", e)
                return@withContext localResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio emotion analysis failed", e)
            return@withContext EmotionResult(
                Emotion.NEUTRAL, 0.5f, mapOf(Emotion.NEUTRAL to 0.5f),
                0.5f, 0.5f, "fallback"
            )
        }
    }
    
    /**
     * بدء تسجيل الصوت لتحليل المشاعر في الوقت الفعلي
     */
    fun startRealTimeEmotionAnalysis(callback: (EmotionResult) -> Unit) {
        if (isRecording) return
        
        try {
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            audioRecord?.startRecording()
            isRecording = true
            
            coroutineScope.launch {
                val buffer = ByteArray(bufferSize)
                val audioBuffer = ByteArrayOutputStream()
                
                while (isRecording) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        audioBuffer.write(buffer, 0, bytesRead)
                        
                        // تحليل كل ثانيتين
                        if (audioBuffer.size() >= SAMPLE_RATE * 2 * 2) { // 2 seconds of 16-bit audio
                            val audioData = audioBuffer.toByteArray()
                            audioBuffer.reset()
                            
                            val emotion = analyzeAudioEmotion(audioData)
                            withContext(Dispatchers.Main) {
                                callback(emotion)
                            }
                        }
                    }
                    delay(100) // تحديث كل 100ms
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start real-time emotion analysis", e)
        }
    }
    
    /**
     * إيقاف تحليل المشاعر في الوقت الفعلي
     */
    fun stopRealTimeEmotionAnalysis() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    /**
     * تحليل النص محلياً باستخدام قواعد بسيطة
     */
    private fun analyzeTextLocally(text: String): EmotionResult {
        val lowerText = text.lowercase()
        
        // كلمات مفتاحية للمشاعر المختلفة
        val emotionKeywords = mapOf(
            Emotion.HAPPY to listOf("سعيد", "فرح", "مبسوط", "رائع", "ممتاز", "جميل", "حلو", "😊", "😄", "😃"),
            Emotion.SAD to listOf("حزين", "زعلان", "مكتئب", "تعبان", "متضايق", "😢", "😭", "☹️"),
            Emotion.ANGRY to listOf("غاضب", "زعلان", "متنرفز", "مستاء", "😠", "😡", "🤬"),
            Emotion.SURPRISED to listOf("متفاجئ", "مندهش", "مصدوم", "😲", "😮", "😯"),
            Emotion.EXCITED to listOf("متحمس", "متشوق", "مبسوط", "🤩", "😍", "🥳"),
            Emotion.CALM to listOf("هادئ", "مرتاح", "مطمئن", "😌", "😊"),
            Emotion.CONFUSED to listOf("محتار", "مش فاهم", "مرتبك", "😕", "🤔", "😵")
        )
        
        val scores = mutableMapOf<Emotion, Float>()
        
        // حساب النقاط لكل مشاعر
        for ((emotion, keywords) in emotionKeywords) {
            var score = 0f
            for (keyword in keywords) {
                if (lowerText.contains(keyword)) {
                    score += 1f
                }
            }
            scores[emotion] = score / keywords.size
        }
        
        // إضافة نقاط افتراضية للحالة المحايدة
        scores[Emotion.NEUTRAL] = 0.3f
        
        // العثور على أعلى نقاط
        val primaryEmotion = scores.maxByOrNull { it.value }?.key ?: Emotion.NEUTRAL
        val confidence = scores[primaryEmotion] ?: 0.5f
        
        // حساب Arousal و Valence
        val arousal = when (primaryEmotion) {
            Emotion.ANGRY, Emotion.EXCITED, Emotion.SURPRISED -> 0.8f
            Emotion.HAPPY, Emotion.FEAR -> 0.6f
            Emotion.SAD, Emotion.DISGUST -> 0.4f
            else -> 0.3f
        }
        
        val valence = when (primaryEmotion) {
            Emotion.HAPPY, Emotion.EXCITED, Emotion.CALM -> 0.8f
            Emotion.SURPRISED -> 0.6f
            Emotion.NEUTRAL, Emotion.CONFUSED -> 0.5f
            Emotion.SAD, Emotion.FEAR -> 0.3f
            Emotion.ANGRY, Emotion.DISGUST -> 0.2f
        }
        
        return EmotionResult(
            primaryEmotion, confidence, scores.toMap(),
            arousal, valence, "local_text"
        )
    }
    
    /**
     * استخراج الميزات الصوتية
     */
    private fun extractAudioFeatures(audioData: ByteArray): AudioFeatures {
        // تحويل البيانات الصوتية إلى مصفوفة من الأرقام
        val samples = ByteBuffer.wrap(audioData)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .array()
            .map { it.toFloat() / Short.MAX_VALUE }
        
        // حساب الميزات الأساسية
        val energy = samples.map { it * it }.average().toFloat()
        val zeroCrossingRate = calculateZeroCrossingRate(samples)
        val pitch = estimatePitch(samples)
        val spectralCentroid = calculateSpectralCentroid(samples)
        
        return AudioFeatures(energy, zeroCrossingRate, pitch, spectralCentroid)
    }
    
    /**
     * تحليل الصوت محلياً
     */
    private fun analyzeAudioLocally(features: AudioFeatures): EmotionResult {
        val scores = mutableMapOf<Emotion, Float>()
        
        // قواعد بسيطة لتحليل المشاعر من الميزات الصوتية
        
        // الطاقة العالية = إثارة أو غضب
        if (features.energy > 0.7f) {
            scores[Emotion.ANGRY] = 0.7f
            scores[Emotion.EXCITED] = 0.6f
        }
        
        // الطاقة المنخفضة = حزن أو هدوء
        if (features.energy < 0.3f) {
            scores[Emotion.SAD] = 0.6f
            scores[Emotion.CALM] = 0.5f
        }
        
        // النغمة العالية = سعادة أو تفاجؤ
        if (features.pitch > 200f) {
            scores[Emotion.HAPPY] = 0.6f
            scores[Emotion.SURPRISED] = 0.5f
        }
        
        // النغمة المنخفضة = حزن أو غضب
        if (features.pitch < 100f) {
            scores[Emotion.SAD] = 0.5f
            scores[Emotion.ANGRY] = 0.4f
        }
        
        // معدل عبور الصفر العالي = توتر
        if (features.zeroCrossingRate > 0.1f) {
            scores[Emotion.FEAR] = 0.5f
            scores[Emotion.CONFUSED] = 0.4f
        }
        
        scores[Emotion.NEUTRAL] = 0.4f
        
        val primaryEmotion = scores.maxByOrNull { it.value }?.key ?: Emotion.NEUTRAL
        val confidence = scores[primaryEmotion] ?: 0.5f
        
        val arousal = features.energy
        val valence = if (features.pitch > 150f) 0.7f else 0.4f
        
        return EmotionResult(
            primaryEmotion, confidence, scores.toMap(),
            arousal, valence, "local_audio"
        )
    }
    
    /**
     * تحليل النص باستخدام الخدمات السحابية
     */
    private suspend fun analyzeTextWithCloud(text: String): EmotionResult? {
        // هنا يمكن دمج Google Cloud Natural Language API أو Azure Text Analytics
        // للبساطة، سنعيد null للإشارة إلى عدم التوفر
        return null
    }
    
    /**
     * تحليل الصوت باستخدام الخدمات السحابية
     */
    private suspend fun analyzeAudioWithCloud(audioData: ByteArray): EmotionResult? {
        // هنا يمكن دمج Azure Speech Services أو Google Cloud Speech API
        // للبساطة، سنعيد null للإشارة إلى عدم التوفر
        return null
    }
    
    // فئات مساعدة
    data class AudioFeatures(
        val energy: Float,
        val zeroCrossingRate: Float,
        val pitch: Float,
        val spectralCentroid: Float
    )
    
    // دوال مساعدة لحساب الميزات الصوتية
    private fun calculateZeroCrossingRate(samples: List<Float>): Float {
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0) != (samples[i-1] >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / samples.size
    }
    
    private fun estimatePitch(samples: List<Float>): Float {
        // تقدير بسيط للنغمة باستخدام autocorrelation
        val minPeriod = SAMPLE_RATE / 500 // 500 Hz max
        val maxPeriod = SAMPLE_RATE / 50  // 50 Hz min
        
        var bestPeriod = minPeriod
        var maxCorrelation = 0f
        
        for (period in minPeriod..maxPeriod) {
            var correlation = 0f
            val limit = samples.size - period
            
            for (i in 0 until limit) {
                correlation += samples[i] * samples[i + period]
            }
            
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestPeriod = period
            }
        }
        
        return SAMPLE_RATE.toFloat() / bestPeriod
    }
    
    private fun calculateSpectralCentroid(samples: List<Float>): Float {
        // حساب مبسط للمركز الطيفي
        val fftSize = 512
        val windowedSamples = samples.take(fftSize)
        
        // تطبيق نافذة Hamming
        val windowed = windowedSamples.mapIndexed { i, sample ->
            sample * (0.54 - 0.46 * kotlin.math.cos(2 * kotlin.math.PI * i / (fftSize - 1))).toFloat()
        }
        
        // حساب مبسط للمركز الطيفي
        var weightedSum = 0f
        var magnitudeSum = 0f
        
        for (i in windowed.indices) {
            val magnitude = kotlin.math.abs(windowed[i])
            weightedSum += i * magnitude
            magnitudeSum += magnitude
        }
        
        return if (magnitudeSum > 0) weightedSum / magnitudeSum else 0f
    }
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        stopRealTimeEmotionAnalysis()
        coroutineScope.cancel()
    }
}

