package com.example.deeplviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog


class FloatingTextSelection : AppCompatActivity() {

    private val startUrl by lazy {
        val urlParam = getSharedPreferences("config", Context.MODE_PRIVATE).getString(
            "urlParam",
            defParamValue
        ) ?: defParamValue
        return@lazy "https://www.deepl.com/translator$urlParam"
    }

    companion object {
        private const val defParamValue = "#en/en/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val androidTranslateFloatingText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            } else {
                null
            }

            val floatingText = (androidTranslateFloatingText ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)) as String
            val config = getSharedPreferences("config", Context.MODE_PRIVATE)
            val usePopup = config.getBoolean(getString(R.string.key_switch_popup_mode), true)

            if (usePopup) {
                launchPopup(floatingText)
            } else {
                launchFullscreen(floatingText)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun launchFullscreen(initialText: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("FLOATING_TEXT", initialText)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun launchPopup(initialText: String) {
        val layout = layoutInflater.inflate(R.layout.popup_layout, null)
        val webView = layout.findViewById<NestedScrollWebView>(R.id.webview)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        val webViewClient = MyWebViewClient(this)
        webView.webViewClient = webViewClient
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(layout)
        dialog.setOnDismissListener { finish() }

        webViewClient.loadFinishedListener = {
            dialog.show()
        }

        webView.loadUrl(
            startUrl + Uri.encode(
                initialText.replace(
                    "/",
                    "\\/"
                )
            )
        )
    }
}
