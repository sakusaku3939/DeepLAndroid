package com.example.deeplviewer.webview

import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceResponse
import android.webkit.RenderProcessGoneDetail
import com.example.deeplviewer.diagnostics.Diagnostics
import com.example.deeplviewer.diagnostics.DiagnosticPrivacy
import com.example.deeplviewer.activity.DiagnosticExportActivity
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
import com.example.deeplviewer.helper.CookieManagerHelper
import com.example.deeplviewer.webview.MyWebChromeClient.Companion.DEEPL_INTERNAL_REGEX

class MyWebViewClient(
    private val activity: Activity,
) : WebViewClient() {
    private val viewId = java.util.UUID.randomUUID().toString()
    private var loadId = 0
    private var awaitingStart = false

    private fun record(name: String, url: String?, fields: Map<String, Any?> = emptyMap()) {
        Diagnostics.event(name, mapOf("view_id" to viewId, "load_id" to loadId,
            "url" to DiagnosticPrivacy.url(url)) + fields)
    }

    fun loadUrl(view: WebView, url: String) {
        loadId++
        awaitingStart = true
        record("load_requested", url)
        view.loadUrl(url)
    }

    var hasFinishedInitialLoad: Boolean = false
        private set

    var loadFinishedListener: (() -> Unit)? = null

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        val blocked = !DEEPL_INTERNAL_REGEX.matches(url)
        record("navigation_decision", url, mapOf("blocked" to blocked,
            "main_frame" to request?.isForMainFrame,
            "redirect" to if (Build.VERSION.SDK_INT >= 24) request?.isRedirect else null,
            "gesture" to request?.hasGesture()))
        return blocked
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!awaitingStart) loadId++
        awaitingStart = false
        record("page_started", url, mapOf("network" to org.json.JSONObject(Diagnostics.networkState())))
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
        record("page_finished", url, mapOf("attached" to view.isAttachedToWindow))
        CookieManagerHelper().persistCookies(activity)
        if (!hasFinishedInitialLoad) {
            hasFinishedInitialLoad = true
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
        record("history_updated", url, mapOf("reload" to isReload))
        saveDeepLState(url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest,
        error: WebResourceError?
    ) {
        record("web_error", request.url.toString(), mapOf(
            "main_frame" to request.isForMainFrame,
            "android_error_code" to if (Build.VERSION.SDK_INT >= 23) error?.errorCode else null,
            "chromium_error" to DiagnosticPrivacy.chromiumError(if (Build.VERSION.SDK_INT >= 23) error?.description?.toString() else null),
            "network" to org.json.JSONObject(Diagnostics.networkState())))
        if (request.isForMainFrame) {
            if (activity is com.example.deeplviewer.activity.FloatingTextSelection) {
                activity.showNetworkError()
            } else {
                activity.setContentView(R.layout.network_err)
            }
            record("error_screen_shown", request.url.toString())
            activity.findViewById<View>(R.id.save_diagnostics).setOnClickListener {
                activity.startActivity(Intent(activity, DiagnosticExportActivity::class.java))
            }

            val button: ImageButton = activity.findViewById(R.id.reload)
            val listener = ReloadButtonListener()
            button.setOnClickListener(listener)

            val errorDescription =
                DiagnosticPrivacy.chromiumError(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description?.toString() else null)
            Toast.makeText(activity, errorDescription, Toast.LENGTH_LONG).show()
            Log.e("onReceivedError", errorDescription)
        }
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        record("page_visible", url)
        super.onPageCommitVisible(view, url)
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
        record("http_error", request.url.toString(), mapOf("status" to response.statusCode,
            "main_frame" to request.isForMainFrame))
        super.onReceivedHttpError(view, request, response)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        record("ssl_error", error.url, mapOf("primary_error" to error.primaryError))
        super.onReceivedSslError(view, handler, error)
    }

    @RequiresApi(26)
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        record("renderer_gone", view.url, mapOf("crashed" to detail.didCrash(), "priority" to detail.rendererPriorityAtExit()))
        return super.onRenderProcessGone(view, detail)
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

    private fun patchDarkTheme(view: WebView) {
        val images = listOf("brand-symbol-light.svg", "brand-wordmark-light.svg").map { fileName ->
            val bytes = view.context.assets.open(fileName).use { it.readBytes() }
            org.json.JSONObject.quote("data:image/svg+xml;base64," +
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
        }
        val script = getAssetsText(view.context, "patch-darkThemeFix.js")
        view.evaluateJavascript("$script(${images.joinToString(",")});", null)
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
            patchDarkTheme(view)
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
            patchDarkTheme(view)
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
            record("retry_clicked", null)
            val i = Intent(activity, MainActivity::class.java)
            activity.finish()
            activity.overridePendingTransition(0, 0)
            activity.startActivity(i)
        }
    }
}
