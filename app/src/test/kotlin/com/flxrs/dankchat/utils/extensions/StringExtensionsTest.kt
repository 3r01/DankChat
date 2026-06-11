package com.flxrs.dankchat.utils.extensions

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class StringExtensionsTest {
    @Test
    fun `codePointSlice returns substring for valid ascii range`() {
        assertEquals(expected = "bad", actual = "this is a bad message".codePointSlice(10, 13))
    }

    @Test
    fun `codePointSlice slices full string`() {
        assertEquals(expected = "automod", actual = "automod".codePointSlice(0, 7))
    }

    @Test
    fun `codePointSlice counts surrogate pairs as single code points`() {
        // each emoji is one code point but two utf-16 chars
        assertEquals(expected = "bad", actual = "🐍🐍 bad".codePointSlice(3, 6))
    }

    @Test
    fun `codePointSlice returns null when begin is past the end`() {
        // begin 14 on a string of length 13, real automod boundary from a play console crash
        assertEquals(expected = null, actual = "thirteenchars".codePointSlice(14, 15))
    }

    @Test
    fun `codePointSlice returns null when end is past the end`() {
        assertEquals(expected = null, actual = "short".codePointSlice(2, 10))
    }

    @Test
    fun `codePointSlice returns null for degenerate ranges`() {
        assertEquals(expected = null, actual = "text".codePointSlice(-1, 2))
        assertEquals(expected = null, actual = "text".codePointSlice(2, 2))
        assertEquals(expected = null, actual = "text".codePointSlice(3, 1))
    }
}
