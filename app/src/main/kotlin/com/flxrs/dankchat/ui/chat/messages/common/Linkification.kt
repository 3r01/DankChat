package com.flxrs.dankchat.ui.chat.messages.common

import android.util.Patterns
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

private val DISALLOWED_URL_CHARS = """<>\{}|^"`""".toSet()

fun AnnotatedString.Builder.appendWithLinks(
    text: String,
    linkColor: Color,
    previousChar: Char? = null,
) {
    val matcher = Patterns.WEB_URL.matcher(text)
    var lastIndex = 0

    while (matcher.find()) {
        val start = matcher.start()
        var end = matcher.end()

        val prevChar = if (start > 0) text[start - 1] else previousChar
        if (prevChar != null && !prevChar.isWhitespace()) {
            continue
        }

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
        val url =
            when {
                rawUrl.contains("://") -> rawUrl
                else -> "https://$rawUrl"
            }

        if (start > lastIndex) {
            append(text.substring(lastIndex, start))
        }

        pushStringAnnotation(tag = URL_ANNOTATION_TAG, annotation = url)
        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            append(rawUrl)
        }
        pop()

        lastIndex = end
    }

    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

const val URL_ANNOTATION_TAG = "URL"
