package com.example.deeplviewer.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.deeplviewer.R
import com.example.deeplviewer.config.WebViewConfig
import com.example.deeplviewer.helper.CookieManagerHelper
import com.example.deeplviewer.helper.UrlHelper
import com.example.deeplviewer.webview.MyWebViewClient
import com.example.deeplviewer.webview.WebAppInterface

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var webViewClient: MyWebViewClient
    private lateinit var startUrl: String

    val testWebViewClient: MyWebViewClient?
        get() = if (::webViewClient.isInitialized) webViewClient else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startUrl = UrlHelper.buildStartUrl(this)

        initializeWebView()
        createWebView(intent, savedInstanceState)
        setupSettingsButton()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        createWebView(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveWebViewState(outState)
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            // Remove the WebView from its parent view and destroy it
            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }

    /**
     * Creates and configures the WebView with the provided intent or saved instance state
     */
    private fun createWebView(intent: Intent?, savedInstanceState: Bundle? = null) {
        val receivedText = extractReceivedText(intent, savedInstanceState)

        setupWebViewClient()
        setupCookies()

        // Add JavaScript interface for communication
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // Load the initial URL
        val targetUrl = UrlHelper.buildWebViewUrl(startUrl, receivedText)
        webView.loadUrl(targetUrl)
    }

    /**
     * Initializes the WebView
     */
    private fun initializeWebView() {
        webView = findViewById(R.id.webview)
        WebViewConfig.applyBasicSettings(webView)
        WebViewConfig.applyOptimizedSettings(webView)
        if (com.example.deeplviewer.BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        // Inject window.chrome before any page scripts run so DeepL's browser
        // detection recognises the WebView as Chrome (WebView lacks this object)
        // DeepL's feature system requires window.chrome (absent in WebView) and
        // window.speechSynthesis (absent in WebView) to initialize the translator.
        // Inject both before any page scripts run to prevent feature init timeouts.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            val polyfillJs = assets.open("hide-elements.js").reader().use { it.readText() }
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                """
                if(!window.chrome){window.chrome={};}
                if(!window.speechSynthesis){window.speechSynthesis={getVoices:function(){return[];},speak:function(){},cancel:function(){},pause:function(){},resume:function(){},addEventListener:function(){},removeEventListener:function(){}};}
                """.trimIndent() + "\n" + polyfillJs,
                setOf("https://www.deepl.com")
            )
        }
    }

    /**
     * Extracts the text from the intent or saved instance state
     */
    private fun extractReceivedText(intent: Intent?, savedInstanceState: Bundle?): String {
        return savedInstanceState?.getString("SavedText")
            ?: intent?.getStringExtra("FLOATING_TEXT")
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: ""
    }

    /**
     * Sets up the WebViewClient to handle page loading and animations
     */
    private fun setupWebViewClient() {
        webViewClient = MyWebViewClient(this)
        webView.webViewClient = webViewClient
    }

    /**
     * Sets up cookies for the WebView
     */
    private fun setupCookies() {
        try {
            CookieManagerHelper().apply {
                migrateCookie(this@MainActivity)
                addPrivacyCookie()
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Cookie setup failed", e)
        }
    }

    /**
     * Sets up the settings button to open the SettingsActivity
     */
    private fun setupSettingsButton() {
        findViewById<ImageButton>(R.id.settingButton).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Saves the current state of the WebView to the outState bundle
     */
    private fun saveWebViewState(outState: Bundle) {
        val webView: WebView = findViewById(R.id.webview)
        val url = webView.url ?: ""

        // Extract the page type 'translator' or 'write' from the URL
        val pageTypeRegex = Regex("^https://www\\.deepl\\.com/.*/(translator|write).*$")
        val pageTypeMatch = pageTypeRegex.find(url)
        val pageType = pageTypeMatch?.groupValues?.get(1)

        pageType?.let {
            getSharedPreferences("config", Context.MODE_PRIVATE).edit {
                putString("pageType", it)
            }

            // Save the URL parameter if it changed
            val urlParam = url.substringAfter(it)
            val originUrlParam = startUrl.substringAfter(it)
            val isTextChanged = urlParam != originUrlParam

            if (isTextChanged && urlParam.isNotEmpty()) {
                val urlText = urlParam.substring(originUrlParam.length)
                val inputText = Uri.decode(urlText).replace("\\/", "/")
                outState.putString("SavedText", inputText)
            }
        }

        try {
            CookieManagerHelper().saveCookies(this, webView)
        } catch (e: Exception) {
            Log.w("MainActivity", "Cookie save failed", e)
        }
    }
}