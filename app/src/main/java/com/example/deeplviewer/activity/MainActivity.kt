package com.example.deeplviewer.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.deeplviewer.helper.CookieManagerHelper
import com.example.deeplviewer.webview.MyWebViewClient
import com.example.deeplviewer.R
import com.example.deeplviewer.webview.WebAppInterface

class MainActivity : AppCompatActivity() {
    private lateinit var webViewClient: MyWebViewClient
    private val startUrl by lazy {
        return@lazy ORIGIN_URL + getSharedPreferences("config", Context.MODE_PRIVATE).getString(
            "urlParam",
            DEFAULT_PARAM
        )
    }

    companion object {
        private const val ORIGIN_URL = "https://www.deepl.com/translator"
        private const val DEFAULT_PARAM = "#en/en/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createWebView(intent, savedInstanceState)

        findViewById<ImageButton>(R.id.settingButton).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        createWebView(intent)
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun createWebView(intent: Intent?, savedInstanceState: Bundle? = null) {
        val floatingText = intent?.getStringExtra("FLOATING_TEXT")
        val shareText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val savedText = savedInstanceState?.getString("SavedText")
        val receivedText = savedText ?: (floatingText ?: (shareText ?: ""))

        val webView: WebView = findViewById(R.id.webview)
        webViewClient = MyWebViewClient(this)
        webViewClient.loadFinishedListener = {
            val animation = AlphaAnimation(0.0F, 1.0F)
            animation.duration = 100
            webView.startAnimation(animation)
            webView.alpha = 1.0F
        }

        CookieManagerHelper().migrateCookie(this)
        CookieManagerHelper().addPrivacyCookie()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

        webView.webViewClient = webViewClient
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl(
            startUrl + Uri.encode(
                receivedText.replace(
                    "/",
                    "\\/"
                )
            )
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webView: WebView = findViewById(R.id.webview)
        val url = webView.url ?: ""

        val urlParam = url.substringAfter("translator")
        val originUrlParam = startUrl.substringAfter("translator")
        val isTextChanged = urlParam != originUrlParam

        if (isTextChanged && urlParam.isNotEmpty()) {
            val urlText = urlParam.substring(originUrlParam.length)
            val inputText = Uri.decode(urlText).replace("\\/", "/")
            outState.putString("SavedText", inputText)
        }

        CookieManagerHelper().saveCookies(this, webView)
    }
}
