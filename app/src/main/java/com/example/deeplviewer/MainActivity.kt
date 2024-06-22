package com.example.deeplviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webViewClient: MyWebViewClient
    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private val startUrl by lazy {
        return@lazy originUrl + getSharedPreferences("config", Context.MODE_PRIVATE).getString(
            "urlParam",
            defParamValue
        )
    }

    companion object {
        private const val REQUEST_SELECT_FILE = 100
        private const val originUrl = "https://www.deepl.com/translator"
        private const val defParamValue = "#en/en/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createWebView(intent, savedInstanceState)
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

        val cookieManager = CookieManager.getInstance()
        cookieManager.acceptCookie()
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Load cookies from SharedPreferences
        val sharedPreferences = getSharedPreferences("DeepLCookies", Context.MODE_PRIVATE)
        val savedCookie = sharedPreferences.getString("cookie", null)
        if (savedCookie != null) {
            cookieManager.setCookie(startUrl, savedCookie)
        } else {
            // Set cookie to hide banner
            cookieManager.setCookie(
                "https://www.deepl.com",
                "privacySettings=%7B%22v%22%3A%221%22%2C%22t%22%3A1713052800%2C%22m%22%3A%22LAX%22%2C%22consent%22%3A%5B%22NECESSARY%22%2C%22PERFORMANCE%22%2C%22COMFORT%22%2C%22MARKETING%22%5D%7D"
            );
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

        webView.webViewClient = webViewClient
        webView.webChromeClient = MyWebChromeClient()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECT_FILE -> uploadMessage?.let { message ->
                var result = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                if (result == null) {
                    result = if (intent.data != null) arrayOf(intent.data) else null
                }
                message.onReceiveValue(result)
                uploadMessage = null
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webView: WebView = findViewById(R.id.webview)
        val url = webView.url ?: ""
        if (url.length > startUrl.length) {
            val urlParam = url.substring(startUrl.length)
            val inputText = Uri.decode(urlParam).replace("\\/", "/")
            outState.putString("SavedText", inputText)
        }

        // Save cookies
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(originUrl)
        if (cookies != null) {
            val sharedPreferences = getSharedPreferences("DeepLCookies", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("cookie", cookies)
            editor.apply()
        }
        cookieManager.flush()
    }

    inner class MyWebChromeClient : WebChromeClient() {
        override fun onShowFileChooser(
            mWebView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            if (uploadMessage != null) {
                uploadMessage?.onReceiveValue(null)
                uploadMessage = null
            }

            uploadMessage = filePathCallback

            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
            }

            try {
                startActivityForResult(intent, REQUEST_SELECT_FILE)
            } catch (e: Exception) {
                uploadMessage = null
                Toast.makeText(this@MainActivity, "Cannot Open File Chooser", Toast.LENGTH_SHORT)
                    .show()
                return false
            }

            return true
        }
    }
}
