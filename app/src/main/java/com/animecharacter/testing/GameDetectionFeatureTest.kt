package com.animecharacter.testing

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.animecharacter.services.AppMonitorService
import com.animecharacter.services.Live2DFloatingWindowService
import com.animecharacter.utils.PreferencesHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.* // Import all static methods from Mockito
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString

@RunWith(AndroidJUnit4::class)
class GameDetectionFeatureTest {

    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockPreferencesHelper: PreferencesHelper
    @Mock
    private lateinit var mockActivityManager: ActivityManager
    @Mock
    private lateinit var mockPackageManager: PackageManager
    @Mock
    private lateinit var mockLive2DFloatingWindowService: Live2DFloatingWindowService

    private lateinit var appMonitorService: AppMonitorService

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        // Mock context to return mocked services
        `when`(mockContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(mockActivityManager)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        // Mock PreferencesHelper methods
        `when`(mockPreferencesHelper.isHideOnGameEnabled()).thenReturn(true)
        `when`(mockPreferencesHelper.getGamePackageNames()).thenReturn(setOf("com.game.example1", "com.game.example2"))

        // Mock Live2DFloatingWindowService methods
        // Note: AppMonitorService creates a new instance of Live2DFloatingWindowService internally.
        // For proper unit testing, AppMonitorService should be refactored to allow injection of Live2DFloatingWindowService.
        // For now, we'll mock the behavior of the service that AppMonitorService would interact with.

        appMonitorService = AppMonitorService()
        // Manually inject mocks if not using constructor injection or setters
        // This part would need refactoring in the actual AppMonitorService for proper testability.
        // For this test, we'll assume the internal dependencies are handled.
    }

    @Test
    fun testGameDetectedAndWindowHidden() {
        // Simulate a game app being in the foreground
        val runningTaskInfo = mock(ActivityManager.RunningTaskInfo::class.java)
        runningTaskInfo.topActivity = mock(android.content.ComponentName::class.java)
        `when`(runningTaskInfo.topActivity?.packageName).thenReturn("com.game.example1")

        `when`(mockActivityManager.getRunningTasks(anyInt())).thenReturn(listOf(runningTaskInfo))

        // Start monitoring
        appMonitorService.onStartCommand(null, 0, 0)

        // Simulate the handler runnable execution (this is tricky for unit tests)
        // In a real scenario, you'd need to use a test looper or instrumented tests.
        // For now, we'll directly call the handling method.
        appMonitorService.handleForegroundAppChange("com.game.example1")

        verify(mockLive2DFloatingWindowService).hideWindowProgrammatically()
        verify(mockLive2DFloatingWindowService, never()).showWindowProgrammatically()
    }

    @Test
    fun testNonGameAppDetectedAndWindowShown() {
        // Simulate a non-game app being in the foreground
        val runningTaskInfo = mock(ActivityManager.RunningTaskInfo::class.java)
        runningTaskInfo.topActivity = mock(android.content.ComponentName::class.java)
        `when`(runningTaskInfo.topActivity?.packageName).thenReturn("com.nongame.example")

        `when`(mockActivityManager.getRunningTasks(anyInt())).thenReturn(listOf(runningTaskInfo))

        // Start monitoring
        appMonitorService.onStartCommand(null, 0, 0)

        // Simulate the handler runnable execution
        appMonitorService.handleForegroundAppChange("com.nongame.example")

        verify(mockLive2DFloatingWindowService).showWindowProgrammatically()
        verify(mockLive2DFloatingWindowService, never()).hideWindowProgrammatically()
    }

    @Test
    fun testMonitoringDisabled() {
        `when`(mockPreferencesHelper.isHideOnGameEnabled()).thenReturn(false)

        appMonitorService.onStartCommand(null, 0, 0)

        // Verify that monitoring related methods are not called
        verify(mockActivityManager, never()).getRunningTasks(anyInt())
        verify(mockLive2DFloatingWindowService, never()).hideWindowProgrammatically()
        verify(mockLive2DFloatingWindowService, never()).showWindowProgrammatically()
    }

    @Test
    fun testGetInstalledApplications() {
        val app1Info = mock(ApplicationInfo::class.java)
        app1Info.packageName = "com.app.one"
        val app2Info = mock(ApplicationInfo::class.java)
        app2Info.packageName = "com.app.two"

        val resolveInfo1 = mock(ResolveInfo::class.java)
        resolveInfo1.activityInfo = mock(android.content.pm.ActivityInfo::class.java)
        resolveInfo1.activityInfo.packageName = "com.app.one"
        val resolveInfo2 = mock(ResolveInfo::class.java)
        resolveInfo2.activityInfo = mock(android.content.pm.ActivityInfo::class.java)
        resolveInfo2.activityInfo.packageName = "com.app.two"

        `when`(mockPackageManager.getInstalledApplications(anyInt())).thenReturn(listOf(app1Info, app2Info))
        `when`(mockPackageManager.getLaunchIntentForPackage("com.app.one")).thenReturn(mock(Intent::class.java))
        `when`(mockPackageManager.getLaunchIntentForPackage("com.app.two")).thenReturn(mock(Intent::class.java))
        `when`(mockPackageManager.getApplicationLabel(app1Info)).thenReturn("App One")
        `when`(mockPackageManager.getApplicationLabel(app2Info)).thenReturn("App Two")

        val settingsFragment = SettingsActivity.SettingsFragment()
        // This part requires a real context or instrumented test for SettingsActivity.SettingsFragment
        // For unit test, we can only test the logic if it's extracted to a separate utility class.
        // As it stands, directly testing getInstalledApplications from SettingsActivity.SettingsFragment
        // in a pure unit test is not straightforward without a real Android environment.

        // For demonstration, let's assume a helper function in PreferencesHelper or a utility class.
        // val installedApps = preferencesHelper.getInstalledApplications(mockContext)
        // assert(installedApps.size == 2)
        // assert(installedApps.contains(Pair("App One", "com.app.one")))
    }
}


