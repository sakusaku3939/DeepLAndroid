package com.example.deeplviewer.helper

import android.net.Uri

object WebViewUrlHelper {
    /**
     * Builds a URL for the WebView based on the provided start URL and text
     */
    fun buildUrl(startUrl: String, text: String): String {
        if (text.isEmpty()) return startUrl

        val processedText = text.replace("/", "\\/")
        val encodedText = Uri.encode(processedText)
        return "$startUrl$encodedText"
    }
}