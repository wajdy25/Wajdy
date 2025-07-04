package com.animecharacter.testing

import android.content.Context
import com.animecharacter.managers.SocialMediaAchievementManager
import com.animecharacter.services.AdvancedVoiceInteractionService
import com.animecharacter.utils.PreferencesHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.text.SimpleDateFormat
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class DailyReportTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAdvancedVoiceInteractionService: AdvancedVoiceInteractionService

    @Mock
    private lateinit var mockPreferencesHelper: PreferencesHelper

    @Mock
    private lateinit var mockRewardManager: com.animecharacter.managers.RewardManager

    private lateinit var socialMediaAchievementManager: SocialMediaAchievementManager

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @Before
    fun setup() {
        `when`(mockPreferencesHelper.getDailyAchievement(anyString())).thenReturn(null)
        `when`(mockPreferencesHelper.getCompletedGoalsForDate(anyString())).thenReturn(emptyList())
        `when`(mockPreferencesHelper.getTotalCountForType(any(), any())).thenReturn(0)
        `when`(mockPreferencesHelper.isMilestoneReached(any(), anyInt(), any())).thenReturn(false)
        `when`(mockPreferencesHelper.getCurrentStreak(any())).thenReturn(0)

        socialMediaAchievementManager = SocialMediaAchievementManager(
            mockContext,
            mockAdvancedVoiceInteractionService,
            mockRewardManager
        )
    }

    @Test
    fun testGenerateDailyReport() {
        val today = dateFormat.format(Date())
        val report = socialMediaAchievementManager.generateDailyReport(today)

        // تحقق من أن التقرير ليس فارغاً
        assert(report.date == today)
        assert(report.totalInteractions == 0)
        assert(report.goalsCompleted.isEmpty())
        assert(report.newUnlocks.isEmpty())

        // تحقق من استدعاء خدمة الصوت
        verify(mockAdvancedVoiceInteractionService, times(1)).speak(anyString())
    }

    @Test
    fun testGenerateDailyReportWithData() {
        val today = dateFormat.format(Date())
        val mockAchievement = com.animecharacter.models.DailyAchievement(
            date = today,
            platform = com.animecharacter.models.SocialMediaPlatform.INSTAGRAM,
            newFollowers = 10,
            newLikes = 50
        )
        `when`(mockPreferencesHelper.getDailyAchievement(today)).thenReturn(mockAchievement)

        val report = socialMediaAchievementManager.generateDailyReport(today)

        assert(report.totalInteractions == 60)
        verify(mockAdvancedVoiceInteractionService, times(1)).speak(anyString())
    }
}


