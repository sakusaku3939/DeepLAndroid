package com.example.deeplviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {
    private lateinit var webViewClient: MyWebViewClient
    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private val startUrl by lazy {
        val urlParam = getSharedPreferences("config", Context.MODE_PRIVATE).getString(
            "urlParam",
            defParamValue
        ) ?: defParamValue
        return@lazy "https://www.deepl.com/translator$urlParam"
    }

    companion object {
        private const val REQUEST_SELECT_FILE = 100
        private const val defParamValue = "#en/en/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createWebView(intent, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            switchTheme()
            setThemeSwitchShortcut()
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
        webViewClient = MyWebViewClient(this, webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webView: WebView = findViewById(R.id.webview)
        val url = webView.url ?: ""
        if (url.length > startUrl.length) {
            val inputText =
                Uri.decode(url.substring(startUrl.length))
                    .replace("\\/", "/")
            outState.putString("SavedText", inputText)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun setThemeSwitchShortcut() {
        val darkMode =
            getSharedPreferences("config", Context.MODE_PRIVATE).getBoolean("darkMode", false)
        val label = if (darkMode) R.string.light_mode_label else R.string.dark_mode_label

        val intent = Intent(Intent.ACTION_VIEW, null, this, MainActivity::class.java)
        intent.putExtra("switchTheme", if (darkMode) "light" else "dark")

        val shortcutManager = getSystemService(ShortcutManager::class.java)
        val shortcutInfo = ShortcutInfo.Builder(applicationContext, "theme")
            .setShortLabel(resources.getString(label))
            .setIcon(
                Icon.createWithResource(
                    applicationContext,
                    R.drawable.ic_baseline_brightness_4_24
                )
            )
            .setIntent(intent)
            .build()
        if (shortcutManager.dynamicShortcuts.size > 0) {
            shortcutManager.updateShortcuts(listOf(shortcutInfo))
        } else {
            shortcutManager.dynamicShortcuts = listOf(shortcutInfo)
        }
    }

    private fun switchTheme() {
        val config = getSharedPreferences("config", Context.MODE_PRIVATE)
        when (intent?.getStringExtra("switchTheme")) {
            "light" -> config.edit().putBoolean("darkMode", false).apply()
            "dark" -> config.edit().putBoolean("darkMode", true).apply()
            else -> {}
        }
        if (config.getBoolean("darkMode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
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
