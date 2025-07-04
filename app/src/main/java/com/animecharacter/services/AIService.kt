package com.animecharacter.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.animecharacter.BuildConfig
import com.animecharacter.models.Message
import com.animecharacter.utils.PreferencesHelper
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONArray
import org.json.JSONObject

class AIService : Service() {

    companion object {
        private const val TAG = "AIService"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val MAX_TOKENS = 150
        private const val TEMPERATURE = 0.7
    }

    private lateinit var requestQueue: RequestQueue
    private lateinit var preferencesHelper: PreferencesHelper
    private val conversationHistory = mutableListOf<Message>()
    
    private val binder = AIServiceBinder()

    inner class AIServiceBinder : Binder() {
        fun getService(): AIService = this@AIService
    }

    override fun onCreate() {
        super.onCreate()
        requestQueue = Volley.newRequestQueue(this)
        preferencesHelper = PreferencesHelper(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun sendMessage(message: String, callback: (String?, String?) -> Unit) {
        // إضافة الرسالة لتاريخ المحادثة
        conversationHistory.add(Message(message, true, System.currentTimeMillis()))
        
        // إنشاء طلب API
        val requestBody = createRequestBody(message)
        
        val jsonRequest = object : JsonObjectRequest(
            Request.Method.POST,
            OPENAI_API_URL,
            requestBody,
            { response ->
                try {
                    val aiResponse = parseResponse(response)
                    if (aiResponse != null) {
                        // إضافة رد الذكاء الاصطناعي لتاريخ المحادثة
                        conversationHistory.add(Message(aiResponse, false, System.currentTimeMillis()))
                        callback(aiResponse, null)
                    } else {
                        callback(null, "فشل في تحليل الاستجابة")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "خطأ في تحليل الاستجابة", e)
                    callback(null, "خطأ في تحليل الاستجابة: ${e.message}")
                }
            },
            { error ->
                Log.e(TAG, "خطأ في طلب API", error)
                val errorMessage = when (error.networkResponse?.statusCode) {
                    401 -> "مفتاح API غير صحيح"
                    429 -> "تم تجاوز حد الاستخدام"
                    500 -> "خطأ في الخادم"
                    else -> "خطأ في الاتصال: ${error.message}"
                }
                callback(null, errorMessage)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "Authorization" to "Bearer ${BuildConfig.OPENAI_API_KEY}",
                    "Content-Type" to "application/json"
                )
            }
        }

        requestQueue.add(jsonRequest)
    }

    private fun createRequestBody(message: String): JSONObject {
        val requestBody = JSONObject()
        
        try {
            // إعداد النموذج
            requestBody.put("model", "gpt-3.5-turbo")
            requestBody.put("max_tokens", MAX_TOKENS)
            requestBody.put("temperature", TEMPERATURE)
            
            // إنشاء رسائل المحادثة
            val messages = JSONArray()
            
            // رسالة النظام لتحديد شخصية الذكاء الاصطناعي
            val systemMessage = JSONObject().apply {
                put("role", "system")
                put("content", createSystemPrompt())
            }
            messages.put(systemMessage)
            
            // إضافة تاريخ المحادثة (آخر 10 رسائل)
            val recentHistory = conversationHistory.takeLast(10)
            for (msg in recentHistory) {
                val messageObj = JSONObject().apply {
                    put("role", if (msg.isFromUser) "user" else "assistant")
                    put("content", msg.text)
                }
                messages.put(messageObj)
            }
            
            // إضافة الرسالة الحالية
            val currentMessage = JSONObject().apply {
                put("role", "user")
                put("content", message)
            }
            messages.put(currentMessage)
            
            requestBody.put("messages", messages)
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في إنشاء طلب API", e)
        }
        
        return requestBody
    }

    private fun createSystemPrompt(): String {
        val characterName = preferencesHelper.getSelectedCharacter()
        val personalityMode = preferencesHelper.getPersonalityMode()
        
        return when (characterName) {
            "ساكورا" -> createSakuraPrompt(personalityMode)
            "ناروتو" -> createNarutoPrompt(personalityMode)
            "لوفي" -> createLuffyPrompt(personalityMode)
            "غوكو" -> createGokuPrompt(personalityMode)
            else -> createDefaultPrompt(personalityMode)
        }
    }

    private fun createSakuraPrompt(personality: String): String {
        val basePrompt = "أنت ساكورا هارونو من أنمي ناروتو. أنت طبيبة نينجا ماهرة وصديقة مخلصة."
        
        return when (personality) {
            "ودود" -> "$basePrompt تحدثي بطريقة ودودة ومشجعة، واهتمي بمشاعر الآخرين."
            "رسمي" -> "$basePrompt تحدثي بطريقة مهنية ومحترمة، مع التركيز على المعلومات المفيدة."
            "مرح" -> "$basePrompt تحدثي بطريقة مرحة ومتفائلة، واستخدمي الدعابة اللطيفة."
            "حكيم" -> "$basePrompt تحدثي بحكمة وخبرة، وقدمي نصائح مفيدة."
            "نشيط" -> "$basePrompt تحدثي بحماس وطاقة عالية، وكوني متحمسة للمساعدة."
            else -> basePrompt
        }
    }

    private fun createNarutoPrompt(personality: String): String {
        val basePrompt = "أنت ناروتو أوزوماكي من أنمي ناروتو. أنت نينجا متفائل ومصمم على تحقيق أحلامك."
        
        return when (personality) {
            "ودود" -> "$basePrompt تحدث بطريقة ودودة ومتحمسة، وشارك تفاؤلك مع الآخرين."
            "رسمي" -> "$basePrompt تحدث بطريقة محترمة ولكن احتفظ بحماسك الطبيعي."
            "مرح" -> "$basePrompt تحدث بطريقة مرحة ومليئة بالطاقة، واستخدم عبارات مثل 'داتيبايو'."
            "حكيم" -> "$basePrompt تحدث بحكمة مكتسبة من تجاربك، وقدم نصائح ملهمة."
            "نشيط" -> "$basePrompt تحدث بحماس شديد وطاقة لا محدودة، وكن متحمساً لكل شيء."
            else -> basePrompt
        }
    }

    private fun createLuffyPrompt(personality: String): String {
        val basePrompt = "أنت مونكي دي لوفي من أنمي ون بيس. أنت قبطان قراصنة مرح ومغامر."
        
        return when (personality) {
            "ودود" -> "$basePrompt تحدث بطريقة ودودة وبسيطة، واهتم بأصدقائك."
            "رسمي" -> "$basePrompt حاول التحدث بطريقة أكثر جدية، لكن احتفظ ببساطتك."
            "مرح" -> "$basePrompt تحدث بطريقة مرحة ومليئة بالضحك، وفكر في المغامرات والطعام."
            "حكيم" -> "$basePrompt تحدث بحكمة بسيطة ولكن عميقة، مستوحاة من تجاربك في البحر."
            "نشيط" -> "$basePrompt تحدث بحماس شديد وطاقة عالية، وكن متحمساً للمغامرات الجديدة."
            else -> basePrompt
        }
    }

    private fun createGokuPrompt(personality: String): String {
        val basePrompt = "أنت سون غوكو من أنمي دراغون بول. أنت محارب قوي وطيب القلب."
        
        return when (personality) {
            "ودود" -> "$basePrompt تحدث بطريقة ودودة وبريئة، واهتم بالآخرين."
            "رسمي" -> "$basePrompt تحدث بطريقة محترمة ولكن احتفظ ببساطتك الطبيعية."
            "مرح" -> "$basePrompt تحدث بطريقة مرحة وبسيطة، وفكر في القتال والطعام."
            "حكيم" -> "$basePrompt تحدث بحكمة مكتسبة من تدريبك وقتالك، وقدم نصائح عن القوة والصداقة."
            "نشيط" -> "$basePrompt تحدث بحماس شديد، خاصة عند الحديث عن القتال والتدريب."
            else -> basePrompt
        }
    }

    private fun createDefaultPrompt(personality: String): String {
        val basePrompt = "أنت شخصية أنمي ودودة ومفيدة. تحب مساعدة الآخرين والتحدث معهم."
        
        return when (personality) {
            "ودود" -> "$basePrompt تحدث بطريقة ودودة ومرحبة."
            "رسمي" -> "$basePrompt تحدث بطريقة مهنية ومحترمة."
            "مرح" -> "$basePrompt تحدث بطريقة مرحة ومتفائلة."
            "حكيم" -> "$basePrompt تحدث بحكمة وقدم نصائح مفيدة."
            "نشيط" -> "$basePrompt تحدث بحماس وطاقة عالية."
            else -> basePrompt
        }
    }

    private fun parseResponse(response: JSONObject): String? {
        return try {
            val choices = response.getJSONArray("choices")
            if (choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                message.getString("content").trim()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تحليل الاستجابة", e)
            null
        }
    }

    fun clearConversationHistory() {
        conversationHistory.clear()
    }

    fun getConversationHistory(): List<Message> {
        return conversationHistory.toList()
    }

    override fun onDestroy() {
        super.onDestroy()
        requestQueue.cancelAll(TAG)
    }
}

