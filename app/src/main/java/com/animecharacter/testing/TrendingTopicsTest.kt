package com.animecharacter.testing

import android.content.Context
import com.animecharacter.managers.SocialMediaIntegrationManager
import com.animecharacter.services.TrendingTopicsService
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TrendingTopicsTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockTrendingTopicsService: TrendingTopicsService

    private lateinit var socialMediaIntegrationManager: SocialMediaIntegrationManager

    @Before
    fun setup() {
        socialMediaIntegrationManager = SocialMediaIntegrationManager(mockContext)
        // Mock the trendingTopicsService within the manager
        socialMediaIntegrationManager.trendingTopicsService = mockTrendingTopicsService
    }

    @Test
    fun testFetchTrendingTopicsSuccess() = runBlocking {
        val mockTopics = listOf("Topic A", "Topic B", "Topic C")
        `when`(mockTrendingTopicsService.fetchTrendingTopics()).thenReturn(mockTopics)

        val fetchedTopics = socialMediaIntegrationManager.fetchTrendingTopics()

        assert(fetchedTopics == mockTopics)
        verify(mockTrendingTopicsService, times(1)).fetchTrendingTopics()
    }

    @Test
    fun testFetchTrendingTopicsFailure() = runBlocking {
        `when`(mockTrendingTopicsService.fetchTrendingTopics()).thenReturn(emptyList())

        val fetchedTopics = socialMediaIntegrationManager.fetchTrendingTopics()

        assert(fetchedTopics.isEmpty())
        verify(mockTrendingTopicsService, times(1)).fetchTrendingTopics()
    }
}


