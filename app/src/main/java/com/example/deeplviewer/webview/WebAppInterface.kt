package com.example.deeplviewer.webview

import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import androidx.core.content.edit
import com.example.deeplviewer.helper.UrlHelper

@Keep
class WebAppInterface(private val context: Context) {
    @JavascriptInterface
    fun stringToBase64String(s: String): String {
        return Base64.encodeToString(s.toByteArray(), Base64.DEFAULT)
    }

    @JavascriptInterface
    fun getAssetsText(fileName: String): String {
        if (fileName !in ALLOWED_ASSET_FILES) return ""
        return context.assets.open(fileName).reader(Charsets.UTF_8).use { it.readText() }
    }

    @JavascriptInterface
    fun saveDeepLUrl(url: String) {
        val pageType = UrlHelper.extractPageType(url)
        val urlParam = UrlHelper.extractUrlParam(url)

        saveDeepLState(pageType, urlParam)
    }

    @JavascriptInterface
    fun saveDeepLLanguage(sourceLanguage: String, targetLanguage: String) {
        val target = targetLanguage.normalizeLanguageCode() ?: return

        val configPrefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val savedSourceLanguage = Regex("^/l/([^/]+)/[^/]+$")
            .find(configPrefs.getString("urlParam", null) ?: "")
            ?.groupValues
            ?.get(1)
            ?: "en"
        val source =
            sourceLanguage.normalizeLanguageCode().takeUnless { it == null || it == "auto" }
                ?: savedSourceLanguage
        val urlParam = UrlHelper.normalizeUrlParam("/l/$source/$target") ?: return

        saveDeepLState(pageType = "translator", urlParam = urlParam)
    }

    private fun String.normalizeLanguageCode(): String? {
        val languageCode = trim().lowercase()
        return languageCode.takeIf { LANGUAGE_CODE_REGEX.matches(it) }
    }

    private fun saveDeepLState(pageType: String?, urlParam: String?) {
        if (pageType == null && urlParam == null) return

        context.getSharedPreferences("config", Context.MODE_PRIVATE).edit {
            pageType?.let {
                putString("pageType", it)
            }
            urlParam?.let {
                putString("urlParam", it)
            }
        }
    }

    private companion object {
        val LANGUAGE_CODE_REGEX = Regex("^[a-z]{2,3}(?:-[a-z0-9]{2,8})*$")
        val ALLOWED_ASSET_FILES = setOf(
            "DeepL_Logo_lightBlue_v2.svg",
            "DeepL_Text_light.svg"
        )
    }
}
