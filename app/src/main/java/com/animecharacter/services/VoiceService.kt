package com.animecharacter.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.animecharacter.utils.PreferencesHelper
import java.util.*

class VoiceService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceService"
        private const val UTTERANCE_ID = "anime_character_speech"
    }

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var preferencesHelper: PreferencesHelper
    
    private var isListening = false
    private var isSpeaking = false
    private var isInitialized = false
    
    private val binder = VoiceServiceBinder()
    private var voiceCallback: VoiceCallback? = null

    inner class VoiceServiceBinder : Binder() {
        fun getService(): VoiceService = this@VoiceService
    }

    interface VoiceCallback {
        fun onSpeechRecognized(text: String)
        fun onSpeechError(error: String)
        fun onSpeechStarted()
        fun onSpeechEnded()
        fun onTextToSpeechStarted()
        fun onTextToSpeechCompleted()
        fun onTextToSpeechError(error: String)
    }

    override fun onCreate() {
        super.onCreate()
        preferencesHelper = PreferencesHelper(this)
        initializeTextToSpeech()
        initializeSpeechRecognizer()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this, this)
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(speechRecognitionListener)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setupTextToSpeech()
            isInitialized = true
        } else {
            Log.e(TAG, "فشل في تهيئة Text-to-Speech")
            voiceCallback?.onTextToSpeechError("فشل في تهيئة خدمة تحويل النص إلى صوت")
        }
    }

    private fun setupTextToSpeech() {
        // إعداد اللغة
        val language = getLanguageFromPreferences()
        val result = textToSpeech.setLanguage(language)
        
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // استخدام الإنجليزية كبديل
            textToSpeech.setLanguage(Locale.US)
            Log.w(TAG, "اللغة المطلوبة غير مدعومة، تم استخدام الإنجليزية")
        }

        // إعداد معاملات الصوت
        textToSpeech.setSpeechRate(preferencesHelper.getSpeechRate())
        textToSpeech.setPitch(preferencesHelper.getSpeechPitch())

        // إعداد مستمع التقدم
        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener)
    }

    private fun getLanguageFromPreferences(): Locale {
        return when (preferencesHelper.getVoiceLanguage()) {
            "ar-SA" -> Locale("ar", "SA")
            "en-US" -> Locale.US
            "ja-JP" -> Locale.JAPAN
            else -> Locale("ar", "SA")
        }
    }

    fun setVoiceCallback(callback: VoiceCallback) {
        this.voiceCallback = callback
    }

    fun speakText(text: String) {
        if (!isInitialized) {
            voiceCallback?.onTextToSpeechError("خدمة الصوت غير مهيأة")
            return
        }

        if (isSpeaking) {
            stopSpeaking()
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
        }

        val result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
        
        if (result == TextToSpeech.SUCCESS) {
            isSpeaking = true
            voiceCallback?.onTextToSpeechStarted()
        } else {
            voiceCallback?.onTextToSpeechError("فشل في تشغيل النص الصوتي")
        }
    }

    fun stopSpeaking() {
        if (::textToSpeech.isInitialized && isSpeaking) {
            textToSpeech.stop()
            isSpeaking = false
        }
    }

    fun startListening() {
        if (!::speechRecognizer.isInitialized) {
            voiceCallback?.onSpeechError("خدمة التعرف على الكلام غير متاحة")
            return
        }

        if (isListening) {
            stopListening()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferencesHelper.getVoiceLanguage())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }

        try {
            speechRecognizer.startListening(intent)
            isListening = true
            voiceCallback?.onSpeechStarted()
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في بدء الاستماع", e)
            voiceCallback?.onSpeechError("فشل في بدء الاستماع: ${e.message}")
        }
    }

    fun stopListening() {
        if (::speechRecognizer.isInitialized && isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }

    fun isCurrentlyListening(): Boolean = isListening
    fun isCurrentlySpeaking(): Boolean = isSpeaking

    private val speechRecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "جاهز للاستماع")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "بدء الكلام")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // يمكن استخدام هذا لإظهار مستوى الصوت
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // غير مستخدم حالياً
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "انتهاء الكلام")
            isListening = false
            voiceCallback?.onSpeechEnded()
        }

        override fun onError(error: Int) {
            isListening = false
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "خطأ في الصوت"
                SpeechRecognizer.ERROR_CLIENT -> "خطأ في العميل"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "صلاحيات غير كافية"
                SpeechRecognizer.ERROR_NETWORK -> "خطأ في الشبكة"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهت مهلة الشبكة"
                SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم العثور على تطابق"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة التعرف مشغولة"
                SpeechRecognizer.ERROR_SERVER -> "خطأ في الخادم"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهت مهلة الكلام"
                else -> "خطأ غير معروف"
            }
            
            Log.e(TAG, "خطأ في التعرف على الكلام: $errorMessage")
            voiceCallback?.onSpeechError(errorMessage)
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    Log.d(TAG, "تم التعرف على النص: $recognizedText")
                    voiceCallback?.onSpeechRecognized(recognizedText)
                }
            }
            isListening = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    Log.d(TAG, "نتائج جزئية: ${matches[0]}")
                    // يمكن إرسال النتائج الجزئية للواجهة
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // غير مستخدم حالياً
        }
    }

    private val utteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            if (utteranceId == UTTERANCE_ID) {
                isSpeaking = true
                voiceCallback?.onTextToSpeechStarted()
            }
        }

        override fun onDone(utteranceId: String?) {
            if (utteranceId == UTTERANCE_ID) {
                isSpeaking = false
                voiceCallback?.onTextToSpeechCompleted()
            }
        }

        override fun onError(utteranceId: String?) {
            if (utteranceId == UTTERANCE_ID) {
                isSpeaking = false
                voiceCallback?.onTextToSpeechError("خطأ في تشغيل النص الصوتي")
            }
        }
    }

    fun updateVoiceSettings() {
        if (isInitialized) {
            setupTextToSpeech()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}

