package com.example.deeplviewer

import com.example.deeplviewer.helper.UrlHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlHelperTest {
    @Test
    fun savedRegionalLanguagesProduceCanonicalPathsAndSharedTextUrls() {
        val languages = listOf("en-US", "en-GB", "pt-BR", "pt-PT", "zh-Hans", "zh-Hant", "es-419")
        val text = "Hello / + # %\nworld"
        for (language in languages) {
            for (stored in listOf(language.lowercase(java.util.Locale.US), language.uppercase(java.util.Locale.US), language)) {
                for (param in listOf("/l/ru/$stored", "#ru/$stored/")) {
                    val url = UrlHelper.buildPageUrl("translator", param, uiLanguage = "en")
                    assertEquals("https://www.deepl.com/en/translator/l/ru/$language", url)
                    assertEquals(url, UrlHelper.buildWebViewUrl(url, ""))
                    val saved = UrlHelper.extractUrlParam(url)
                    assertEquals(url, UrlHelper.buildPageUrl("translator", saved, uiLanguage = "en"))
                    val sharedUrl = UrlHelper.buildWebViewUrl(url, text)
                    org.junit.Assert.assertTrue(sharedUrl.startsWith("https://www.deepl.com/en/translator#ru/$language/"))
                    assertEquals(text, UrlHelper.extractText(sharedUrl))
                    assertEquals("https://www.deepl.com/en/write", UrlHelper.buildPageUrl("write", saved, uiLanguage = "en"))
                }
            }
        }
    }

    @Test
    fun sourceScriptAndDefaultTargetUseCanonicalCase() {
        assertEquals("https://www.deepl.com/en/translator/l/zh-Hant/en-US",
            UrlHelper.buildPageUrl("translator", "/l/ZH-HANT/en-us", uiLanguage = "en"))
        assertEquals("https://www.deepl.com/pt-br/translator/l/en/pt-BR",
            UrlHelper.buildPageUrl("translator", null, uiLanguage = "pt-br"))
        assertEquals("https://www.deepl.com/en-us/translator/l/en/de",
            UrlHelper.buildPageUrl("translator", null, uiLanguage = "en-us"))
    }

    @Test
    fun malformedLanguagePathsAreNotSaved() {
        listOf(
            "https://www.deepl.com/en/translator/l/ru/en-US,/en/translator/l/ru/en-US",
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
        assertEquals("/l/ja/en-US", UrlHelper.extractUrlParam(
            "https://www.deepl.com/ja/translator/l/ja/en-US?source=test#hello,world"
        ))
        assertEquals("/l/ja/en-GB", UrlHelper.extractUrlParam(
            "https://www.deepl.com/ja/translator#ja/en-GB/hello%2Cworld"
        ))
        assertEquals("/l/zh-Hans/en-US", UrlHelper.normalizeUrlParam("/l/ZH-HANS/EN-US"))
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
