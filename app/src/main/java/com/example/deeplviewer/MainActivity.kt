package com.example.deeplviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var webViewClient: MyWebViewClient
    private var uploadMessage: ValueCallback<Array<Uri>>? = null

    companion object {
        private const val REQUEST_SELECT_FILE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createWebView(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        createWebView(this.intent)
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun createWebView(intent: Intent?) {
        val floatingText = intent?.getStringExtra("FLOATING_TEXT")
        val shareText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val receivedText = floatingText ?: (shareText ?: "")

        val defParamValue = "#en/en/"
        val urlParam = getSharedPreferences("config", Context.MODE_PRIVATE).getString(
            "urlParam",
            defParamValue
        ) ?: defParamValue

        val webView: WebView = findViewById(R.id.webview)
        webViewClient = MyWebViewClient(this, webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = webViewClient
        webView.webChromeClient = MyWebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl(
            "https://www.deepl.com/translator$urlParam${
                Uri.encode(
                    receivedText.replace(
                        "/",
                        "\\/"
                    )
                )
            }"
        )
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences("config", Context.MODE_PRIVATE)
            .edit()
            .putString("urlParam", webViewClient.urlParam)
            .apply()
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