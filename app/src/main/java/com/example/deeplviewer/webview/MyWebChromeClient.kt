package com.example.deeplviewer.webview

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class MyWebChromeClient(
    private val mainWebView: WebView,
) : WebChromeClient() {
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        val tempWebView = WebView(view.context)
        tempWebView.webViewClient = object : WebViewClient() {
            private var handled = false

            private fun handleUrl(url: String) {
                if (handled || url.isEmpty() || url == "about:blank") return
                handled = true
                if (isDeepLInternalUrl(url)) {
                    mainWebView.loadUrl(url)
                } else {
                    openExternalUrl(url)
                }
                mainWebView.post { tempWebView.destroy() }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                handleUrl(request.url.toString())
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                handleUrl(url)
            }
        }
        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = tempWebView
        resultMsg.sendToTarget()
        return true
    }

    companion object {
        val DEEPL_INTERNAL_REGEX =
            Regex("^https://www\\.deepl\\.com/(?:[^/#?]+/)?(translator|write).*$")

        fun isDeepLInternalUrl(url: String): Boolean {
            return DEEPL_INTERNAL_REGEX.matches(url)
        }
    }

    private fun openExternalUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            mainWebView.context.startActivity(intent)
        }
    }
}
