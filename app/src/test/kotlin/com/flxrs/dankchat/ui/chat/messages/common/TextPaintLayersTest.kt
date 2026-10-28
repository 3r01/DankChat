package com.flxrs.dankchat.ui.chat.messages.common

import android.app.Application
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextPaintLayersTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `background paint text is drawn beneath transparent foreground text`() {
        composeRule.setContent {
            TextWithMeasuredInlineContent(
                text = AnnotatedString("forsen", SpanStyle(color = Color.Transparent)),
                backgroundTexts = listOf(AnnotatedString("forsen", SpanStyle(color = Color.Red))),
                style = TextStyle(fontSize = 28.sp),
                modifier = Modifier.testTag("paint"),
            )
        }

        val pixels = composeRule.onNodeWithTag("paint").captureToImage().toPixelMap()

        assertTrue((0 until pixels.width).any { x -> (0 until pixels.height).any { y -> pixels[x, y].let { it.red > 0.8f && it.green < 0.2f && it.blue < 0.2f } } })
    }
}
