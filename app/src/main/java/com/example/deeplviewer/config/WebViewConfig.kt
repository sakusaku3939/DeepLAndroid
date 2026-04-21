package com.example.deeplviewer.config

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView

object WebViewConfig {
    @SuppressLint("SetJavaScriptEnabled")
    fun applyBasicSettings(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true

            // cache settings
            cacheMode = WebSettings.LOAD_DEFAULT

            // Spoof a modern Chrome UA so DeepL doesn't reject the WebView as unsupported.
            // The default WebView UA has "Version/4.0" and "; wv" markers, and uses an old
            // Chrome version — all of which cause DeepL's browser detection to fail.
            userAgentString = userAgentString
                .replace("; wv", "")
                .replace(Regex("Version/[\\d.]+ "), "")
                .replace(Regex("Chrome/[\\d.]+"), "Chrome/131.0.0.0")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun applyOptimizedSettings(webView: WebView) {
        webView.settings.apply {
            // security settings
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                // off screen pre-rasterization (API 23+)
                offscreenPreRaster = true
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // safe browsing (API 26+)
                safeBrowsingEnabled = true
            }

            // mixed content mode
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
    }
}