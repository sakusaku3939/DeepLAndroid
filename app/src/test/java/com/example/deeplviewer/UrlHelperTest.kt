package com.example.deeplviewer

import com.example.deeplviewer.helper.UrlHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlHelperTest {
    @Test
    fun malformedLanguagePathsAreNotSaved() {
        listOf(
            "https://www.deepl.com/ja/translator/l/en/en-US,/ja/translator/l/en/en-US",
            "https://www.deepl.com/ja/translator/l/en/en-US,",
            "https://www.deepl.com/ja/translator/l/en/en-US%2C",
            "https://www.deepl.com/ja/translator/l/en/en-US/ja/translator/l/en/en-US"
        ).forEach { url -> assertNull(url, UrlHelper.extractUrlParam(url)) }
    }

    @Test
    fun corruptedSavedLanguageDoesNotBecomeARequestUrl() {
        listOf("/l/en/en-us,", "/l/en/en-us%2C", "/l/en/ja?bad", "#en/ja,/")
            .forEach { param ->
                assertNull(param, UrlHelper.normalizeUrlParam(param))
                assertEquals(
                    "https://www.deepl.com/ja/translator/l/en/ja",
                    UrlHelper.buildPageUrl("translator", param, uiLanguage = "ja")
                )
            }
    }

    @Test
    fun languagePathsPreserveRegionalCodesAndTextFragments() {
        assertEquals("/l/ja/en-us", UrlHelper.extractUrlParam(
            "https://www.deepl.com/ja/translator/l/ja/en-US?source=test#hello,world"
        ))
        assertEquals("/l/ja/en-gb", UrlHelper.extractUrlParam(
            "https://www.deepl.com/ja/translator#ja/en-GB/hello%2Cworld"
        ))
        assertEquals("/l/zh-hans/en-us", UrlHelper.normalizeUrlParam("/l/ZH-HANS/EN-US"))
    }

    @Test
    fun sameLanguageWithRegionalVariantsDoesNotBecomeARequestUrl() {
        listOf("/l/en/en-us", "/l/en-gb/en-us", "/l/pt/pt-br").forEach { param ->
            assertNull(param, UrlHelper.normalizeUrlParam(param))
            assertNull(param, UrlHelper.extractUrlParam("https://www.deepl.com/ja/translator$param"))
            assertEquals(
                "https://www.deepl.com/ja/translator/l/en/ja",
                UrlHelper.buildPageUrl("translator", param, uiLanguage = "ja")
            )
        }
    }

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
