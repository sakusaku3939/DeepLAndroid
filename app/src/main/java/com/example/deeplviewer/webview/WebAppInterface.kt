package com.example.deeplviewer.webview

import android.content.Context
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.edit
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.deeplviewer.R
import com.example.deeplviewer.helper.UrlHelper
import org.json.JSONObject

class WebAppInterface private constructor(private val context: Context) {
    private fun saveDeepLUrl(url: String) {
        val pageType = UrlHelper.extractPageType(url)
        val urlParam = UrlHelper.extractUrlParam(url)

        saveDeepLState(pageType, urlParam)
    }

    private fun saveDeepLLanguage(sourceLanguage: String, targetLanguage: String) {
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

    companion object {
        private val LANGUAGE_CODE_REGEX = Regex("^[a-z]{2,3}(?:-[a-z0-9]{2,8})*$")

        /** Install once per WebView, before loading any page. */
        fun install(webView: WebView) {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                Toast.makeText(webView.context, R.string.webview_update_for_language_saving, Toast.LENGTH_LONG).show()
                return
            }
            val bridge = WebAppInterface(webView.context)
            WebViewCompat.addWebMessageListener(webView, "DeepLState", setOf("https://www.deepl.com")) {
                    view, message, origin, isMainFrame, _ ->
                if (!isMainFrame || origin.scheme != "https" || origin.host != "www.deepl.com" ||
                    origin.port !in listOf(-1, 443) || message.type != WebMessageCompat.TYPE_STRING) return@addWebMessageListener
                val data = message.data ?: return@addWebMessageListener
                if (data.length > 512) return@addWebMessageListener
                val payload = runCatching { JSONObject(data) }.getOrNull() ?: return@addWebMessageListener
                when (payload.opt("type")) {
                    "url" -> view.url?.let(bridge::saveDeepLUrl)
                    "languages" -> {
                        val source = payload.opt("source") as? String ?: return@addWebMessageListener
                        val target = payload.opt("target") as? String ?: return@addWebMessageListener
                        bridge.saveDeepLLanguage(source, target)
                    }
                }
            }
        }
    }
}
