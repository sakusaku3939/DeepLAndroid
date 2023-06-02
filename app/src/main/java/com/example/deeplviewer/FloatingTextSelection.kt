package com.example.deeplviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity
import com.facebook.shimmer.ShimmerFrameLayout
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

            val floatingText = (androidTranslateFloatingText ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT))?.replace("/b".toRegex(), "\n") as String
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

    @SuppressLint("SetJavaScriptEnabled", "RestrictedApi", "VisibleForTests")
    private fun launchPopup(initialText: String) {
        val layout = layoutInflater.inflate(R.layout.popup_layout, null)
        val webView = layout.findViewById<NestedScrollWebView>(R.id.webview)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        val webViewClient = MyWebViewClient(this, true)
        webView.webViewClient = webViewClient
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(layout)
        dialog.setOnDismissListener { finish() }
        dialog.behavior.disableShapeAnimations()
        dialog.show()

        webViewClient.loadFinishedListener = {
            // wait a bit, cause the WebView will change it's height multiple times caused by some lazy-loaded elements
            Handler(Looper.getMainLooper()).postDelayed({
                val animation = AlphaAnimation(0.0F, 1.0F)
                animation.duration = 250
                webView.visibility = View.VISIBLE
                webView.startAnimation(animation)
                layout.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container).hideShimmer()
            }, 750)
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
