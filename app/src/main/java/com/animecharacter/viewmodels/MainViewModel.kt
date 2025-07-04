package com.animecharacter.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.animecharacter.models.Message
import com.animecharacter.services.AIService
import com.animecharacter.services.FloatingCharacterService
import com.animecharacter.utils.PreferencesHelper

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _connectionStatus = MutableLiveData<Boolean>()
    val connectionStatus: LiveData<Boolean> = _connectionStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val messagesList = mutableListOf<Message>()
    private var aiService: AIService? = null
    private var isServiceBound = false
    
    private val preferencesHelper = PreferencesHelper(application)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AIService.AIServiceBinder
            aiService = binder.getService()
            isServiceBound = true
            _connectionStatus.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            aiService = null
            isServiceBound = false
            _connectionStatus.value = false
        }
    }

    init {
        bindAIService()
        loadInitialMessages()
    }

    private fun bindAIService() {
        val intent = Intent(getApplication(), AIService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun loadInitialMessages() {
        // إضافة رسالة ترحيبية
        if (preferencesHelper.isFirstTime()) {
            val welcomeMessage = Message(
                text = preferencesHelper.getWelcomeMessage(),
                isFromUser = false,
                timestamp = System.currentTimeMillis()
            )
            addMessage(welcomeMessage)
            preferencesHelper.setFirstTime(false)
        }
        
        // تحميل المحادثات المحفوظة إذا كانت مفعلة
        if (preferencesHelper.getSaveConversations()) {
            loadSavedConversations()
        }
    }

    private fun loadSavedConversations() {
        // يمكن تطوير هذه الوظيفة لاحقاً لتحميل المحادثات من قاعدة البيانات
        aiService?.getConversationHistory()?.let { history ->
            messagesList.addAll(history)
            _messages.value = messagesList.toList()
        }
    }

    fun addMessage(message: Message) {
        messagesList.add(message)
        _messages.value = messagesList.toList()
        
        // إشعار الشخصية العائمة بالرسالة الجديدة
        if (!message.isFromUser && FloatingCharacterService.isRunning) {
            notifyFloatingCharacter(message.text)
        }
    }

    fun sendMessageToAI(messageText: String) {
        if (!isServiceBound || aiService == null) {
            _errorMessage.value = "خدمة الذكاء الاصطناعي غير متاحة"
            return
        }

        _isLoading.value = true
        _errorMessage.value = ""

        aiService?.sendMessage(messageText) { response, error ->
            _isLoading.value = false
            
            if (response != null) {
                val aiMessage = Message(
                    text = response,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                addMessage(aiMessage)
            } else {
                _errorMessage.value = error ?: "حدث خطأ غير معروف"
            }
        }
    }

    private fun notifyFloatingCharacter(message: String) {
        // إرسال الرسالة للشخصية العائمة لعرضها
        val intent = Intent(getApplication(), FloatingCharacterService::class.java).apply {
            action = "SHOW_MESSAGE"
            putExtra("message", message)
        }
        getApplication<Application>().startService(intent)
    }

    fun getLastCharacterMessage(): Message? {
        return messagesList.lastOrNull { !it.isFromUser }
    }

    fun clearConversation() {
        messagesList.clear()
        _messages.value = emptyList()
        aiService?.clearConversationHistory()
    }

    fun retryLastMessage() {
        val lastUserMessage = messagesList.lastOrNull { it.isFromUser }
        if (lastUserMessage != null) {
            sendMessageToAI(lastUserMessage.text)
        }
    }

    fun exportConversation(): String {
        val conversation = StringBuilder()
        messagesList.forEach { message ->
            val sender = if (message.isFromUser) "المستخدم" else "الشخصية"
            conversation.append("[$sender - ${message.getFormattedTime()}]: ${message.text}\n\n")
        }
        return conversation.toString()
    }

    override fun onCleared() {
        super.onCleared()
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}

