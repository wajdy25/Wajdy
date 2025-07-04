package com.animecharacter.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.animecharacter.R
import com.animecharacter.adapters.MessagesAdapter
import com.animecharacter.databinding.ActivityMainBinding
import com.animecharacter.models.Message
import com.animecharacter.services.AIService
import com.animecharacter.services.FloatingCharacterService
import com.animecharacter.services.VoiceService
import com.animecharacter.utils.PermissionHelper
import com.animecharacter.utils.PreferencesHelper
import com.animecharacter.viewmodels.MainViewModel
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var preferencesHelper: PreferencesHelper
    
    private var isListening = false
    private var isSpeaking = false

    // طلب الصلاحيات
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initializeVoiceFeatures()
        } else {
            showPermissionDeniedDialog()
        }
    }

    // طلب صلاحية النافذة العائمة
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            startFloatingCharacterService()
        } else {
            showOverlayPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupUI()
        setupObservers()
        checkPermissions()
    }

    private fun initializeComponents() {
        // تهيئة المكونات الأساسية
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        permissionHelper = PermissionHelper(this)
        preferencesHelper = PreferencesHelper(this)
        
        // تهيئة محول الرسائل
        messagesAdapter = MessagesAdapter(mutableListOf())
        
        // تهيئة Text-to-Speech
        textToSpeech = TextToSpeech(this, this)
        
        // تهيئة Speech Recognition
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }
    }

    private fun setupUI() {
        // إعداد شريط الأدوات
        setSupportActionBar(binding.toolbar)
        
        // إعداد RecyclerView للرسائل
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = messagesAdapter
        }
        
        // إعداد أحداث الأزرار
        binding.sendButton.setOnClickListener {
            sendMessage()
        }
        
        binding.micButton.setOnClickListener {
            toggleVoiceRecognition()
        }
        
        binding.speakerButton.setOnClickListener {
            toggleTextToSpeech()
        }
        
        binding.floatingCharacterButton.setOnClickListener {
            toggleFloatingCharacter()
        }
        
        binding.settingsFab.setOnClickListener {
            openSettings()
        }
        
        // إعداد حدث الإرسال عند الضغط على Enter
        binding.messageEditText.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
        
        // تحديث واجهة المستخدم بناءً على الإعدادات المحفوظة
        updateUIFromPreferences()
    }

    private fun setupObservers() {
        // مراقبة حالة الاتصال
        viewModel.connectionStatus.observe(this) { isConnected ->
            updateConnectionStatus(isConnected)
        }
        
        // مراقبة الرسائل الجديدة
        viewModel.messages.observe(this) { messages ->
            messagesAdapter.updateMessages(messages)
            scrollToBottom()
        }
        
        // مراقبة حالة التحميل
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.sendButton.isEnabled = !isLoading
        }
        
        // مراقبة الأخطاء
        viewModel.errorMessage.observe(this) { error ->
            if (error.isNotEmpty()) {
                showErrorMessage(error)
            }
        }
    }

    private fun checkPermissions() {
        // فحص صلاحية الميكروفون
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            initializeVoiceFeatures()
        }
    }

    private fun initializeVoiceFeatures() {
        // تهيئة ميزات الصوت بعد الحصول على الصلاحيات
        binding.micButton.isEnabled = true
        binding.speakerButton.isEnabled = true
    }

    private fun sendMessage() {
        val messageText = binding.messageEditText.text.toString().trim()
        if (messageText.isNotEmpty()) {
            // إضافة رسالة المستخدم
            val userMessage = Message(
                text = messageText,
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
            viewModel.addMessage(userMessage)
            
            // مسح حقل الإدخال
            binding.messageEditText.text?.clear()
            
            // إرسال الرسالة إلى خدمة الذكاء الاصطناعي
            viewModel.sendMessageToAI(messageText)
        }
    }

    private fun toggleVoiceRecognition() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (::speechRecognizer.isInitialized) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA") // العربية السعودية
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            speechRecognizer.startListening(intent)
            isListening = true
            updateMicButtonState()
        }
    }

    private fun stopListening() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening()
            isListening = false
            updateMicButtonState()
        }
    }

    private fun toggleTextToSpeech() {
        if (isSpeaking) {
            stopSpeaking()
        } else {
            // تشغيل آخر رسالة من الشخصية
            val lastCharacterMessage = viewModel.getLastCharacterMessage()
            if (lastCharacterMessage != null) {
                speakText(lastCharacterMessage.text)
            }
        }
    }

    private fun speakText(text: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            isSpeaking = true
            updateSpeakerButtonState()
        }
    }

    private fun stopSpeaking() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            isSpeaking = false
            updateSpeakerButtonState()
        }
    }

    private fun toggleFloatingCharacter() {
        if (Settings.canDrawOverlays(this)) {
            if (FloatingCharacterService.isRunning) {
                stopFloatingCharacterService()
            } else {
                startFloatingCharacterService()
            }
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        overlayPermissionLauncher.launch(intent)
    }

    private fun startFloatingCharacterService() {
        val intent = Intent(this, FloatingCharacterService::class.java)
        startForegroundService(intent)
        updateFloatingCharacterButtonState()
    }

    private fun stopFloatingCharacterService() {
        val intent = Intent(this, FloatingCharacterService::class.java)
        stopService(intent)
        updateFloatingCharacterButtonState()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun updateUIFromPreferences() {
        // تحديث اسم الشخصية
        val characterName = preferencesHelper.getSelectedCharacter()
        binding.characterNameText.text = characterName
        
        // تحديث صورة الشخصية
        val characterImage = preferencesHelper.getCharacterImage(characterName)
        // تحميل الصورة باستخدام Glide أو مكتبة أخرى
        
        // تحديث الرسالة الترحيبية
        val welcomeMessage = preferencesHelper.getWelcomeMessage()
        binding.welcomeMessageText.text = welcomeMessage
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        val statusDrawable = if (isConnected) {
            R.drawable.status_online
        } else {
            R.drawable.status_offline
        }
        binding.statusIndicator.setBackgroundResource(statusDrawable)
    }

    private fun updateMicButtonState() {
        val iconRes = if (isListening) R.drawable.ic_mic_off else R.drawable.ic_mic
        val textRes = if (isListening) R.string.listening else R.string.mic_button
        
        binding.micButton.setIconResource(iconRes)
        binding.micButton.text = getString(textRes)
    }

    private fun updateSpeakerButtonState() {
        val iconRes = if (isSpeaking) R.drawable.ic_volume_off else R.drawable.ic_volume_up
        val textRes = if (isSpeaking) R.string.speaking else R.string.speaker_button
        
        binding.speakerButton.setIconResource(iconRes)
        binding.speakerButton.text = getString(textRes)
    }

    private fun updateFloatingCharacterButtonState() {
        val textRes = if (FloatingCharacterService.isRunning) {
            R.string.stop_floating_character
        } else {
            R.string.start_floating_character
        }
        binding.floatingCharacterButton.text = getString(textRes)
    }

    private fun scrollToBottom() {
        if (messagesAdapter.itemCount > 0) {
            binding.messagesRecyclerView.scrollToPosition(messagesAdapter.itemCount - 1)
        }
    }

    private fun showErrorMessage(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_microphone_title)
            .setMessage(R.string.permission_microphone_message)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton(R.string.deny_permission, null)
            .show()
    }

    private fun showOverlayPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_overlay_title)
            .setMessage(R.string.permission_overlay_message)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton(R.string.deny_permission, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                openSettings()
                true
            }
            R.id.action_character_selection -> {
                val intent = Intent(this, CharacterSelectionActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("ar", "SA"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // استخدام الإنجليزية كبديل
                textToSpeech.setLanguage(Locale.US)
            }
            
            // تخصيص إعدادات الصوت
            textToSpeech.setSpeechRate(preferencesHelper.getSpeechRate())
            textToSpeech.setPitch(preferencesHelper.getSpeechPitch())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // تنظيف الموارد
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }

    override fun onResume() {
        super.onResume()
        updateFloatingCharacterButtonState()
        updateUIFromPreferences()
    }
}

