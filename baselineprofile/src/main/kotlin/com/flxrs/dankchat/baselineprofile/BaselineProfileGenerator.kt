package com.flxrs.dankchat.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

private const val TARGET_APP_ID = "com.flxrs.dankchat"
private const val CHANNEL = "flex3rs"

/**
 * Generates the baseline profile via `:app:generateBaselineProfile`.
 *
 * The journey covers cold start, skipping onboarding without a login, joining a channel
 * anonymously, and letting chat with message history render, so startup, the chat connection,
 * message parsing and chat rendering are all compiled ahead of time.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = TARGET_APP_ID, maxIterations = 5) {
            pressHome()
            startActivityAndWait()

            device.completeOnboardingIfShown()
            device.joinChannelIfMissing()

            // Let message history, emotes and live messages arrive and render
            device.wait(Until.hasObject(By.text(channelPattern)), 10_000)
            Thread.sleep(8_000)
            device.scrollChat()
        }
    }

    private val channelPattern = Pattern.compile(CHANNEL, Pattern.CASE_INSENSITIVE)

    private fun UiDevice.completeOnboardingIfShown() {
        if (!wait(Until.hasObject(By.text("Get Started")), 5_000)) {
            return
        }
        clickAndSettle("Get Started")
        clickAndSettle("Skip")
        clickAndSettle("Enable")
        clickAndSettle("Skip")
    }

    private fun UiDevice.joinChannelIfMissing() {
        wait(Until.findObject(By.text("Skip tour")), 3_000)?.click()
        if (hasObject(By.text(channelPattern))) {
            return
        }
        wait(Until.findObject(By.desc("Add channel")), 10_000)?.click()
        val input = wait(Until.findObject(By.clazz("android.widget.EditText")), 5_000) ?: return
        input.text = CHANNEL
        clickAndSettle("OK")
        wait(Until.findObject(By.text("Skip tour")), 3_000)?.click()
    }

    private fun UiDevice.clickAndSettle(text: String) {
        wait(Until.findObject(By.text(text)), 5_000)?.click()
        waitForIdle()
    }

    private fun UiDevice.scrollChat() {
        val centerX = displayWidth / 2
        swipe(centerX, displayHeight / 2, centerX, (displayHeight * 0.8).toInt(), 20)
        waitForIdle()
        swipe(centerX, displayHeight / 2, centerX, (displayHeight * 0.2).toInt(), 20)
        waitForIdle()
    }
}
