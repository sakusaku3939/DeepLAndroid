package com.example.deeplviewer

import com.example.deeplviewer.helper.UrlHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlHelperTest {
    @Test
    fun translatorTextRoundTripsSpecialCharacters() {
        val text = "こんにちは / + # %\nsecond line"
        val url = UrlHelper.buildWebViewUrl("https://www.deepl.com/en/translator/l/ja/en-us", text)
        assertEquals(text, UrlHelper.extractText(url))
    }

    @Test
    fun writeTextRoundTripsIncludingSlashesAndNewlines() {
        val text = "Improve this / sentence.\nA+B #1 100%"
        val url = UrlHelper.buildWebViewUrl("https://www.deepl.com/en/write", text)
        assertEquals(text, UrlHelper.extractText(url))
    }

    @Test
    fun clearedTextAndMissingTextAreDistinct() {
        assertEquals("", UrlHelper.extractText("https://www.deepl.com/en/write#"))
        assertEquals("", UrlHelper.extractText("https://www.deepl.com/en/translator#ja/en/"))
        assertNull(UrlHelper.extractText("https://www.deepl.com/en/write"))
        assertNull(UrlHelper.extractText("https://www.deepl.com/en/translator/l/ja/en"))
    }

    @Test
    fun literalPlusIsPreservedAndMalformedInputIsIgnored() {
        assertEquals("A+B", UrlHelper.extractText("https://www.deepl.com/en/write#A+B"))
        assertNull(UrlHelper.extractText("https://www.deepl.com/en/write#%ZZ"))
        assertNull(UrlHelper.extractText(null))
    }
}
