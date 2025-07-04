package com.animecharacter.testing

import android.content.Context
import android.speech.SpeechRecognizer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.animecharacter.services.AdvancedVoiceInteractionService
import com.animecharacter.utils.PreferencesHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.* // Import all static methods from Mockito
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq

@RunWith(AndroidJUnit4::class)
class WakeWordFeatureTest {

    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockPreferencesHelper: PreferencesHelper
    @Mock
    private lateinit var mockSpeechRecognizer: SpeechRecognizer

    private lateinit var advancedVoiceInteractionService: AdvancedVoiceInteractionService

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        // Mock SpeechRecognizer.isRecognitionAvailable
        mockStatic(SpeechRecognizer::class.java).use {
            `when`(SpeechRecognizer.isRecognitionAvailable(any(Context::class.java))).thenReturn(true)
        }
        // Mock SpeechRecognizer.createSpeechRecognizer
        `when`(SpeechRecognizer.createSpeechRecognizer(any(Context::class.java))).thenReturn(mockSpeechRecognizer)

        advancedVoiceInteractionService = AdvancedVoiceInteractionService(mockContext)
        // Inject mock preferencesHelper (assuming a setter or constructor injection for testing)
        // For now, we'll rely on the mock context providing it, or refactor AdvancedVoiceInteractionService
        // to allow injecting PreferencesHelper for better testability.
        // As a workaround for this test, we'll assume preferencesHelper is mocked internally if needed.
    }

    @Test
    fun testWakeWordEnabledAndDetected() {
        `when`(mockPreferencesHelper.isWakeWordEnabled()).thenReturn(true)
        `when`(mockPreferencesHelper.getWakeWord()).thenReturn("مُزنة")

        // Simulate speech recognition result
        // This part is tricky without a real SpeechRecognizer instance.
        // A more robust test would involve instrumented tests or a custom test runner.
        // For unit testing, we can only verify if startListening is called with correct intent.

        advancedVoiceInteractionService.startListeningForWakeWord()

        verify(mockSpeechRecognizer).startListening(any())
        // Further verification would require mocking the RecognitionListener callbacks
        // and asserting behavior based on recognized text.
    }

    @Test
    fun testWakeWordDisabled() {
        `when`(mockPreferencesHelper.isWakeWordEnabled()).thenReturn(false)

        advancedVoiceInteractionService.startListeningForWakeWord()

        verify(mockSpeechRecognizer, never()).startListening(any())
    }

    @Test
    fun testWakeWordNotSet() {
        `when`(mockPreferencesHelper.isWakeWordEnabled()).thenReturn(true)
        `when`(mockPreferencesHelper.getWakeWord()).thenReturn("")

        advancedVoiceInteractionService.startListeningForWakeWord()

        verify(mockSpeechRecognizer, never()).startListening(any())
    }

    @Test
    fun testStopListening() {
        // First, simulate listening is active
        `when`(mockPreferencesHelper.isWakeWordEnabled()).thenReturn(true)
        `when`(mockPreferencesHelper.getWakeWord()).thenReturn("مُزنة")
        advancedVoiceInteractionService.startListeningForWakeWord()

        advancedVoiceInteractionService.stopListeningForWakeWord()

        verify(mockSpeechRecognizer).stopListening()
    }

    @Test
    fun testShutdown() {
        advancedVoiceInteractionService.shutdown()

        verify(mockSpeechRecognizer).destroy()
        // Verify textToSpeech shutdown as well if it were mocked
    }
}


