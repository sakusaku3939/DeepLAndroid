package com.example.deeplviewer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createWebView(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        createWebView(intent)
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun createWebView(intent: Intent?) {
        val floatingText = intent?.getStringExtra("FLOATING_TEXT") ?: ""

        val webView: WebView = findViewById(R.id.webview)
        val webViewClient = MyWebViewClient(this, webView)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = webViewClient
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl("https://www.deepl.com/translator#en/en/$floatingText")
    }
}