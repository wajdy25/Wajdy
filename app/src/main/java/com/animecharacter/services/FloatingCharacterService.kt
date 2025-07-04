package com.animecharacter.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.animecharacter.R
import com.animecharacter.activities.MainActivity
import com.animecharacter.utils.PreferencesHelper
import com.animecharacter.views.FloatingCharacterView
import pl.droidsonroids.gif.GifImageView

class FloatingCharacterService : Service() {

    companion object {
        var isRunning = false
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_character_channel"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var characterImageView: GifImageView
    private lateinit var chatBubbleView: TextView
    private lateinit var preferencesHelper: PreferencesHelper
    
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        preferencesHelper = PreferencesHelper(this)
        createNotificationChannel()
        initializeFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            showFloatingCharacter()
            startForeground(NOTIFICATION_ID, createNotification())
            isRunning = true
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_floating_title))
            .setContentText(getString(R.string.notification_floating_text))
            .setSmallIcon(R.drawable.ic_character_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun initializeFloatingView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // إنشاء التخطيط للشخصية العائمة
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_character_layout, null)
        
        // الحصول على المراجع للعناصر
        characterImageView = floatingView.findViewById(R.id.characterGifImageView)
        chatBubbleView = floatingView.findViewById(R.id.chatBubbleTextView)
        
        // إعداد معاملات النافذة
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = preferencesHelper.getCharacterPositionX()
            y = preferencesHelper.getCharacterPositionY()
        }

        // إعداد أحداث اللمس
        setupTouchListener()
        
        // تحديث مظهر الشخصية من الإعدادات
        updateCharacterAppearance()
    }

    private fun setupTouchListener() {
        floatingView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true
                        layoutParams?.x = initialX + deltaX
                        layoutParams?.y = initialY + deltaY
                        windowManager.updateViewLayout(floatingView, layoutParams)
                    }
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // نقرة واحدة - إظهار/إخفاء فقاعة المحادثة
                        toggleChatBubble()
                    } else {
                        // حفظ الموقع الجديد
                        preferencesHelper.saveCharacterPosition(
                            layoutParams?.x ?: 0,
                            layoutParams?.y ?: 0
                        )
                    }
                    isDragging = false
                    true
                }
                
                else -> false
            }
        }
    }

    private fun showFloatingCharacter() {
        try {
            windowManager.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCharacterAppearance() {
        // تحديث صورة الشخصية
        val characterName = preferencesHelper.getSelectedCharacter()
        val characterImageRes = getCharacterImageResource(characterName)
        characterImageView.setImageResource(characterImageRes)
        
        // تحديث الحجم
        val size = preferencesHelper.getCharacterSize()
        val layoutParams = characterImageView.layoutParams
        layoutParams.width = size
        layoutParams.height = size
        characterImageView.layoutParams = layoutParams
        
        // تحديث الشفافية
        val alpha = preferencesHelper.getCharacterTransparency()
        floatingView.alpha = alpha / 100f
        
        // إخفاء فقاعة المحادثة في البداية
        chatBubbleView.visibility = View.GONE
    }

    private fun getCharacterImageResource(characterName: String): Int {
        return when (characterName) {
            "ساكورا" -> R.raw.sakura_animation
            "ناروتو" -> R.raw.naruto_animation
            "لوفي" -> R.raw.luffy_animation
            "غوكو" -> R.raw.goku_animation
            else -> R.raw.default_character_animation
        }
    }

    private fun toggleChatBubble() {
        if (chatBubbleView.visibility == View.VISIBLE) {
            hideChatBubble()
        } else {
            showChatBubble("مرحباً! كيف يمكنني مساعدتك؟")
        }
    }

    fun showChatBubble(message: String) {
        chatBubbleView.text = message
        chatBubbleView.visibility = View.VISIBLE
        
        // إخفاء الفقاعة تلقائياً بعد 5 ثوانٍ
        chatBubbleView.postDelayed({
            hideChatBubble()
        }, 5000)
    }

    private fun hideChatBubble() {
        chatBubbleView.visibility = View.GONE
    }

    fun updateCharacterState(state: CharacterState) {
        when (state) {
            CharacterState.IDLE -> {
                // حالة الخمول - الرسوم المتحركة العادية
                characterImageView.setImageResource(getCharacterImageResource(preferencesHelper.getSelectedCharacter()))
            }
            CharacterState.LISTENING -> {
                // حالة الاستماع - رسوم متحركة خاصة
                characterImageView.setImageResource(R.raw.character_listening_animation)
            }
            CharacterState.THINKING -> {
                // حالة التفكير - رسوم متحركة خاصة
                characterImageView.setImageResource(R.raw.character_thinking_animation)
            }
            CharacterState.SPEAKING -> {
                // حالة التحدث - رسوم متحركة خاصة
                characterImageView.setImageResource(R.raw.character_speaking_animation)
            }
        }
    }

    fun showMessage(message: String) {
        showChatBubble(message)
        updateCharacterState(CharacterState.SPEAKING)
        
        // العودة لحالة الخمول بعد انتهاء الرسالة
        chatBubbleView.postDelayed({
            updateCharacterState(CharacterState.IDLE)
        }, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        
        try {
            if (::floatingView.isInitialized) {
                windowManager.removeView(floatingView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    enum class CharacterState {
        IDLE,
        LISTENING,
        THINKING,
        SPEAKING
    }
}

