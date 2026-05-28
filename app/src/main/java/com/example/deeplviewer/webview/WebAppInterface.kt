package com.example.deeplviewer.webview

import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.annotation.Keep

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

    private companion object {
        val ALLOWED_ASSET_FILES = setOf(
            "DeepL_Logo_lightBlue_v2.svg",
            "DeepL_Text_light.svg"
        )
    }
}
