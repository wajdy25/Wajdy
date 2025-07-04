package com.animecharacter.testing

import android.content.Context
import com.animecharacter.services.AdvancedVoiceInteractionService
import com.animecharacter.services.Live2DAnimationEngine
import com.animecharacter.utils.EmotionAnalyzer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EmotionAnalysisTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockLive2DAnimationEngine: Live2DAnimationEngine

    private lateinit var emotionAnalyzer: EmotionAnalyzer
    private lateinit var advancedVoiceInteractionService: AdvancedVoiceInteractionService

    @Before
    fun setup() {
        // Mock EmotionAnalyzer to return specific emotions for testing
        emotionAnalyzer = spy(EmotionAnalyzer(mockContext))
        `when`(emotionAnalyzer.analyzeTextEmotion("أنا سعيد جداً")).thenReturn("happy")
        `when`(emotionAnalyzer.analyzeTextEmotion("أنا حزين اليوم")).thenReturn("sad")
        `when`(emotionAnalyzer.analyzeTextEmotion("هذا مدهش!")).thenReturn("surprised")
        `when`(emotionAnalyzer.analyzeTextEmotion("أنا غاضب")).thenReturn("angry")
        `when`(emotionAnalyzer.analyzeTextEmotion("أنا أفكر")).thenReturn("thinking")
        `when`(emotionAnalyzer.analyzeTextEmotion("أنا أحبك")).thenReturn("love")
        `when`(emotionAnalyzer.analyzeTextEmotion("أنا متعب")).thenReturn("sleepy")
        `when`(emotionAnalyzer.analyzeTextEmotion("مرحباً")).thenReturn("neutral")

        advancedVoiceInteractionService = AdvancedVoiceInteractionService(mockContext, mockLive2DAnimationEngine)
    }

    @Test
    fun testHappyEmotion() {
        val recognizedText = "أنا سعيد جداً"
        advancedVoiceInteractionService.handleRecognizedText(recognizedText)
        verify(mockLive2DAnimationEngine).setEmotion("happy")
    }

    @Test
    fun testSadEmotion() {
        val recognizedText = "أنا حزين اليوم"
        advancedVoiceInteractionService.handleRecognizedText(recognizedText)
        verify(mockLive2DAnimationEngine).setEmotion("sad")
    }

    @Test
    fun testSurprisedEmotion() {
        val recognizedText = "هذا مدهش!"
        advancedVoiceInteractionService.handleRecognizedText(recognizedText)
        verify(mockLive2DAnimationEngine).setEmotion("surprised")
    }

    @Test
    fun testAngryEmotion() {
        val recognizedText = "أنا غاضب"
        advancedVoiceInteractionService.handleRecognizedText(recognizedText)
        verify(mockLive2DAnimationEngine).setEmotion("angry")
    }

    @Test
    fun testThinkingEmotion() {
        val recognizedText = "أنا أفكر"
        advancedVoiceInteractionService.handleRecognizedText(recognizedText)
        verify(mockLive2DAnimationEngine).setEmotion("thinking")
    }

    @Test
    fun testLoveEmotion() {
        val recognizedText = "أنا أحبك"
        advancedVoiceInteractionService.handleRecognizedText(recognizedText)
        verify(mockLive2DAnimationEngine).setEmotion("love")
    }

    @Test
    fun testSleepyEmotion() {
        val recognizedText = "أنا متعب"
        advancedVoiceInteractionService.handleRecognizedText(recognizedText)
        verify(mockLive2DAnimationEngine).setEmotion("sleepy")
    }

    @Test
    fun testNeutralEmotion() {
        val recognizedText = "مرحباً"
        advancedVoiceInteractionService.handleRecognizedText(recognizedText)
        verify(mockLive2DAnimationEngine).setEmotion("neutral")
    }
}


