package com.flxrs.dankchat.ui.chat

import android.util.Patterns
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

private val DISALLOWED_URL_CHARS = """<>\{}|^"`""".toSet()

fun AnnotatedString.Builder.appendWithLinks(text: String, linkColor: Color, previousChar: Char? = null) {
    val matcher = Patterns.WEB_URL.matcher(text)
    var lastIndex = 0

    while (matcher.find()) {
        val start = matcher.start()
        var end = matcher.end()

        // Skip partial matches (preceded by non-whitespace)
        // Check character before match in the original text or the previousChar if at start
        val prevChar = if (start > 0) text[start - 1] else previousChar
        if (prevChar != null && !prevChar.isWhitespace()) {
            continue
        }

        // Extend URL logic from ChatAdapter
        // Find the actual end of the URL by continuing until whitespace or disallowed char
        var fixedEnd = end
        while (fixedEnd < text.length) {
            val c = text[fixedEnd]
            if (c.isWhitespace() || c in DISALLOWED_URL_CHARS) {
                break
            }
            fixedEnd++
        }
        end = fixedEnd

        val rawUrl = text.substring(start, end)
        val url = when {
            rawUrl.contains("://") -> rawUrl
            else                   -> "https://$rawUrl"
        }

        // Append text before URL
        if (start > lastIndex) {
            append(text.substring(lastIndex, start))
        }

        // Append URL with annotation and style — annotation has full URL, display shows original text
        pushStringAnnotation(tag = "URL", annotation = url)
        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            append(rawUrl)
        }
        pop()

        lastIndex = end
    }

    // Append remaining text
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}
