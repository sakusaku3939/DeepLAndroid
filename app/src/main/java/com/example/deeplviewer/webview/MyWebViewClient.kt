package com.example.deeplviewer.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.deeplviewer.R
import com.example.deeplviewer.activity.MainActivity
import androidx.core.content.edit
import com.example.deeplviewer.helper.UrlHelper
import com.example.deeplviewer.webview.MyWebChromeClient.Companion.DEEPL_INTERNAL_REGEX

class MyWebViewClient(
    private val activity: Activity,
) : WebViewClient() {
    private var isSplashFadeDone: Boolean = false

    var loadFinishedListener: (() -> Unit)? = null

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        return !DEEPL_INTERNAL_REGEX.matches(url)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // Inject polyfills early when DOCUMENT_START_SCRIPT is unavailable.
        // Also guard by API 29+ because isFeatureSupported() returns true on API 28
        // but the actual call crashes with NoClassDefFoundError (WebViewRenderProcessClient
        // is API 29+). onPageStarted fires before HTML parsing, so this runs before
        // DeepL's scripts in practice.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            view.evaluateJavascript(POLYFILL_JS, null)
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (!isSplashFadeDone) {
            isSplashFadeDone = true
            loadFinishedListener?.invoke()
        }

        view.loadJavaScript("persist-language-state.js")

        // Fallback for API < 29 where addDocumentStartJavaScript is unavailable
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            view.loadJavaScript("hide-elements.js")
        }

        switchTheme(view)
        saveDeepLState(view.url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        saveDeepLState(url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest,
        error: WebResourceError?
    ) {
        if (request.isForMainFrame) {
            activity.setContentView(R.layout.network_err)

            val button: ImageButton = activity.findViewById(R.id.reload)
            val listener = ReloadButtonListener()
            button.setOnClickListener(listener)

            val errorDescription =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description.toString() else ""
            Toast.makeText(activity, errorDescription, Toast.LENGTH_LONG).show()
            Log.e("onReceivedError", errorDescription)
        }
    }

    /**
     * Saves the current DeepL page state from the URL.
     */
    private fun saveDeepLState(url: String?) {
        val pageType = UrlHelper.extractPageType(url)
        val urlParam = UrlHelper.extractUrlParam(url)

        if (pageType != null || urlParam != null) {
            activity.getSharedPreferences("config", Context.MODE_PRIVATE).edit {
                pageType?.let {
                    putString("pageType", it)
                }
                urlParam?.let { param ->
                    putString("urlParam", param)
                }
            }
        }
    }

    /**
     * loads a JavaScript file from the assets folder into the WebView
     */
    private fun WebView.loadJavaScript(fileName: String) {
        val jsCode = getAssetsText(this.context, fileName)
        this.evaluateJavascript(jsCode, null)
    }

    /**
     * Reads a text file from the assets folder
     */
    private fun getAssetsText(context: Context, fileName: String): String {
        return context.assets.open(fileName).reader(Charsets.UTF_8).use { it.readText() }
    }

    /**
     * Switches the WebView theme based on the system's dark mode setting
     */
    private fun switchTheme(view: WebView) {
        val uiMode = view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (uiMode != Configuration.UI_MODE_NIGHT_YES) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setAlgorithmicDark(view)
        } else {
            setForceDark(view)
        }
    }

    /**
     * Applies algorithmic darkening to the WebView if supported
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setAlgorithmicDark(view: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, true)
            view.loadJavaScript("patch-darkThemeFix.js")
        } else {
            Toast.makeText(
                activity,
                "Dark mode cannot be used because ALGORITHMIC_DARKENING is not supported",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Forces dark mode on the WebView
     */
    @Suppress("DEPRECATION")
    private fun setForceDark(view: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(view.settings, WebSettingsCompat.FORCE_DARK_ON)
            view.loadJavaScript("patch-darkThemeFix.js")
        } else {
            Toast.makeText(
                activity,
                "Dark mode cannot be used because FORCE_DARK is not supported",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private val POLYFILL_JS = """
            if(!window.chrome){window.chrome={};}
            if(!window.speechSynthesis){window.speechSynthesis={getVoices:function(){return[];},speak:function(){},cancel:function(){},pause:function(){},resume:function(){},addEventListener:function(){},removeEventListener:function(){}};}
        """.trimIndent()
    }

    private inner class ReloadButtonListener : View.OnClickListener {
        override fun onClick(view: View) {
            val i = Intent(activity, MainActivity::class.java)
            activity.finish()
            activity.overridePendingTransition(0, 0)
            activity.startActivity(i)
        }
    }
}
