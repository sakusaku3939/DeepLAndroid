package com.example.deeplviewer.helper

import android.content.Context
import android.net.Uri

object UrlHelper {
    /**
     * Builds the start URL for the DeepL translator based on the provided parameters
     */
    fun buildStartUrl(
        context: Context,
        originUrl: String = "https://www.deepl.com/",
        pageType: String = "translator",
        urlParam: String = "#en/en/"
    ): String {
        val configPrefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val buildPageType = configPrefs.getString("pageType", pageType)
            ?: pageType
        val buildUrlParam = configPrefs.getString(
            "urlParam",
            urlParam
        ) ?: urlParam

        return "$originUrl$buildPageType$buildUrlParam"
    }

    /**
     * Builds a URL for the WebView based on the provided start URL and text
     */
    fun buildWebViewUrl(startUrl: String, text: String): String {
        if (text.isEmpty()) return startUrl

        val processedText = text.replace("/", "\\/")
        val encodedText = Uri.encode(processedText)
        return "$startUrl$encodedText"
    }
}