package com.example.deeplviewer.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.example.deeplviewer.R
import com.example.deeplviewer.config.WebViewConfig
import com.example.deeplviewer.helper.WebViewUrlHelper
import com.example.deeplviewer.webview.MyWebViewClient
import com.example.deeplviewer.webview.NestedScrollWebView
import com.example.deeplviewer.webview.WebAppInterface
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog


class FloatingTextSelection : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var webViewClient: MyWebViewClient
    private lateinit var layout: View

    private val startUrl by lazy {
        val configPrefs = getSharedPreferences("config", Context.MODE_PRIVATE)
        val urlParam = configPrefs.getString(
            "urlParam",
            DEFAULT_PARAM
        ) ?: DEFAULT_PARAM
        val pageType = configPrefs.getString("pageType", DEFAULT_PAGE_TYPE) ?: DEFAULT_PAGE_TYPE

        ORIGIN_URL + pageType + urlParam
    }

    companion object {
        private const val ORIGIN_URL = "https://www.deepl.com/"
        private const val DEFAULT_PARAM = "#en/en/"
        private const val DEFAULT_PAGE_TYPE = "translator"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val androidTranslateFloatingText =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
                } else {
                    null
                }

            val floatingText = (androidTranslateFloatingText
                ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)) as String
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

    /**
     * Launches the DeepL WebView in fullscreen mode with the selected text
     */
    private fun launchFullscreen(initialText: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("FLOATING_TEXT", initialText)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    /**
     * Launches a popup with the DeepL WebView and the selected text
     */
    private fun launchPopup(initialText: String) {
        initializeWebView()
        setupWebViewClient()

        // Add JavaScript interface for communication
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webViewClient.loadFinishedListener = {
            setWebViewHeight()
            showWebViewWithAnimation()
        }

        showBottomSheetDialog()

        // Load the initial URL
        val targetUrl = WebViewUrlHelper.buildUrl(startUrl, initialText)
        webView.loadUrl(targetUrl)
    }

    /**
     * Initializes the WebView
     */
    private fun initializeWebView() {
        layout = layoutInflater.inflate(R.layout.popup_layout, null)
        webView = layout.findViewById<NestedScrollWebView>(R.id.webview)

        WebViewConfig.applyBasicSettings(webView)
    }

    /**
     * Sets up the WebViewClient
     */
    private fun setupWebViewClient() {
        webViewClient = MyWebViewClient(this)
        webView.webViewClient = webViewClient
    }

    /**
     * Shows the BottomSheetDialog with the WebView
     */
    @SuppressLint("RestrictedApi", "VisibleForTests")
    private fun showBottomSheetDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(layout)
        dialog.setOnDismissListener { finish() }
        dialog.behavior.disableShapeAnimations()
        dialog.show()
    }

    /**
     * Sets the height of the WebView to 70% of the screen height
     */
    private fun setWebViewHeight() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val webViewHeight = (screenHeight * 0.7).toInt()

        val layoutParams = webView.layoutParams
        layoutParams.height = webViewHeight
        webView.layoutParams = layoutParams

        webView.measure(
            View.MeasureSpec.makeMeasureSpec(webView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(webViewHeight, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, webView.width, webViewHeight)
    }

    /**
     * Shows the WebView with a fade-in animation
     */
    private fun showWebViewWithAnimation() {
        val animation = AlphaAnimation(0.0F, 1.0F)
        animation.duration = 250
        webView.visibility = View.VISIBLE
        webView.startAnimation(animation)
        layout.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container).hideShimmer()
    }
}
