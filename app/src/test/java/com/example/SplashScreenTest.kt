package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SplashScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `splash screen renders logo animation and finishes cleanly once`() {
        composeRule.mainClock.autoAdvance = false
        var finishedCalls = 0

        composeRule.setContent {
            MyApplicationTheme {
                SplashScreen(onSplashFinished = { finishedCalls++ })
            }
        }

        composeRule.onNodeWithTag("splash_screen").assertIsDisplayed()

        // Advance past the initial typography + touch trigger so the ApkForge logo appears
        composeRule.mainClock.advanceTimeBy(1600L)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("splash_logo").assertIsDisplayed()
        composeRule.onNodeWithTag("splash_title").assertIsDisplayed()
        composeRule.onNodeWithTag("splash_progress").assertIsDisplayed()

        // Tapping triggers transition cleanly and guards against duplicate callbacks
        composeRule.onNodeWithTag("splash_screen").performClick()
        composeRule.waitForIdle()
        assertEquals(1, finishedCalls)

        composeRule.onNodeWithTag("splash_screen").performClick()
        composeRule.waitForIdle()
        assertEquals(1, finishedCalls)
    }

    @Test
    fun `apkforge brand logo and mipmap launcher icons resolve cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.apkforge_brand_logo_1794592633199))
        assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
        assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round))
    }
}
