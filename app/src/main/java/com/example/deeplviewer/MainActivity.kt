package com.example.deeplviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    lateinit var webViewClient: MyWebViewClient

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
        val defParamValue = "#en/en/"
        val urlParam = getSharedPreferences("config", Context.MODE_PRIVATE).getString(
            "urlParam",
            defParamValue
        ) ?: defParamValue

        val webView: WebView = findViewById(R.id.webview)
        webViewClient = MyWebViewClient(this, webView)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = webViewClient
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl("https://www.deepl.com/translator$urlParam$floatingText")
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences("config", Context.MODE_PRIVATE)
            .edit()
            .putString("urlParam", webViewClient.urlParam)
            .apply()
    }
}