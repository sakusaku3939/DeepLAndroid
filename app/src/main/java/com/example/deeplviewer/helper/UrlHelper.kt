package com.example.deeplviewer.helper

import android.content.Context
import java.net.URLEncoder
import java.util.Locale

object UrlHelper {
    private const val DEFAULT_ORIGIN_URL = "https://www.deepl.com/"
    private const val DEFAULT_PAGE_TYPE = "translator"
    private const val TRANSLATOR_PAGE_TYPE = "translator"
    private const val WRITE_PAGE_TYPE = "write"
    private const val DEFAULT_SOURCE_LANGUAGE = "en"
    private const val DEFAULT_FALLBACK_TARGET_LANGUAGE = "de"

    private val currentLanguagePathRegex =
        Regex("^https://www\\.deepl\\.com/(?:[^/#?]+/)?translator/l/([^/#?]+)/([^/#?]+).*$")
    private val legacyLanguageHashRegex =
        Regex("#([^/?#]+?)/([^/?#]+?)/")
    private val pageTypeRegex =
        Regex("^https://www\\.deepl\\.com/(?:[^/#?]+/)?(translator|write).*$")
    private val localizedPagePathRegex =
        Regex("^https://www\\.deepl\\.com/([^/#?]+)/(?:translator|write).*$")

    /**
     * Builds the start URL for the DeepL translator based on the provided parameters
     */
    fun buildStartUrl(
        context: Context,
        originUrl: String = DEFAULT_ORIGIN_URL,
        pageType: String = DEFAULT_PAGE_TYPE,
        urlParam: String? = null
    ): String {
        val configPrefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val buildPageType = configPrefs.getString("pageType", pageType)
            ?: pageType
        val buildUrlParam = normalizeUrlParam(
            configPrefs.getString("urlParam", null)
        ) ?: normalizeUrlParam(urlParam)

        return buildPageUrl(buildPageType, buildUrlParam, originUrl)
    }

    fun buildPageUrl(
        pageType: String,
        urlParam: String?,
        originUrl: String = DEFAULT_ORIGIN_URL,
        uiLanguage: String = getUiLanguage()
    ): String {
        val normalizedPageType = when (pageType) {
            WRITE_PAGE_TYPE -> WRITE_PAGE_TYPE
            else -> TRANSLATOR_PAGE_TYPE
        }
        val normalizedUrlParam = normalizeUrlParam(urlParam)
            ?: if (normalizedPageType == TRANSLATOR_PAGE_TYPE) {
                buildDefaultLanguagePath(uiLanguage)
            } else {
                null
            }
        val translatorParam = if (normalizedPageType == TRANSLATOR_PAGE_TYPE) {
            normalizedUrlParam ?: ""
        } else {
            ""
        }

        return "$originUrl$uiLanguage/$normalizedPageType$translatorParam"
    }

    /**
     * Builds a URL for the WebView based on the provided start URL and text
     */
    fun buildWebViewUrl(startUrl: String, text: String): String {
        if (text.isEmpty()) return startUrl

        val processedText = text.replace("/", "\\/")
        val encodedText = encodeUrlText(processedText)

        return when (extractPageType(startUrl)) {
            TRANSLATOR_PAGE_TYPE -> {
                val languagePair = extractLanguagePair(startUrl)
                    ?: defaultLanguagePair(startUrl)
                val translatorUrl = startUrl.substringBefore("#").substringBefore("/l/")
                "$translatorUrl#${languagePair.source}/${languagePair.target}/$encodedText"
            }

            WRITE_PAGE_TYPE -> {
                val writeUrl = startUrl.substringBefore("#")
                "$writeUrl#$encodedText"
            }

            else -> "$startUrl#$encodedText"
        }
    }

    fun extractPageType(url: String?): String? {
        return pageTypeRegex.find(url ?: "")?.groupValues?.get(1)
    }

    fun extractUrlParam(url: String?): String? {
        val currentMatch = currentLanguagePathRegex.find(url ?: "")
        if (currentMatch != null) {
            return buildLanguagePath(
                currentMatch.groupValues[1],
                currentMatch.groupValues[2]
            )
        }

        val legacyMatch = legacyLanguageHashRegex.find(url ?: "")
        if (legacyMatch != null) {
            return buildLanguagePath(
                legacyMatch.groupValues[1],
                legacyMatch.groupValues[2]
            )
        }

        return null
    }

    fun normalizeUrlParam(urlParam: String?): String? {
        if (urlParam.isNullOrBlank()) return null

        val currentParamMatch = Regex("^/l/([^/]+)/([^/]+)/?$").find(urlParam)
        if (currentParamMatch != null) {
            return buildLanguagePath(
                currentParamMatch.groupValues[1],
                currentParamMatch.groupValues[2]
            )
        }

        val legacyParamMatch = Regex("^#([^/]+)/([^/]+)/?$").find(urlParam)
        if (legacyParamMatch != null) {
            return buildLanguagePath(
                legacyParamMatch.groupValues[1],
                legacyParamMatch.groupValues[2]
            )
        }

        return null
    }

    private fun extractLanguagePair(url: String): LanguagePair? {
        val urlParam = extractUrlParam(url) ?: normalizeUrlParam(url) ?: return null
        val match = Regex("^/l/([^/]+)/([^/]+)$").find(urlParam) ?: return null
        return LanguagePair(match.groupValues[1], match.groupValues[2])
    }

    private fun buildLanguagePath(source: String, target: String): String? {
        val normalizedSource = source.lowercase(Locale.US)
        val normalizedTarget = target.lowercase(Locale.US)

        if (normalizedSource.isBlank() || normalizedTarget.isBlank()) return null
        if (normalizedSource == "auto" || normalizedSource == normalizedTarget) return null

        return "/l/$normalizedSource/$normalizedTarget"
    }

    private fun getUiLanguage(): String {
        return Locale.getDefault().language
            .lowercase(Locale.US)
            .takeIf { Regex("[a-z]{2}").matches(it) }
            ?: "en"
    }

    private fun defaultLanguagePair(url: String): LanguagePair {
        val target = localizedPagePathRegex.find(url)
            ?.groupValues
            ?.get(1)
            ?.lowercase(Locale.US)
            ?.takeIf { Regex("[a-z]{2}(?:-[a-z0-9]{2,8})?").matches(it) }
            ?: getUiLanguage()
        val normalizedTarget = target.takeUnless { it == DEFAULT_SOURCE_LANGUAGE }
            ?: DEFAULT_FALLBACK_TARGET_LANGUAGE

        return LanguagePair(DEFAULT_SOURCE_LANGUAGE, normalizedTarget)
    }

    private fun buildDefaultLanguagePath(uiLanguage: String): String {
        val target = uiLanguage.lowercase(Locale.US)
            .takeIf { Regex("[a-z]{2}(?:-[a-z0-9]{2,8})?").matches(it) }
            ?.takeUnless { it == DEFAULT_SOURCE_LANGUAGE }
            ?: DEFAULT_FALLBACK_TARGET_LANGUAGE

        return "/l/$DEFAULT_SOURCE_LANGUAGE/$target"
    }

    private fun encodeUrlText(text: String): String {
        return URLEncoder.encode(text, Charsets.UTF_8.name())
            .replace("+", "%20")
    }

    private data class LanguagePair(val source: String, val target: String)
}
