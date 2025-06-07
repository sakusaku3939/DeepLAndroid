package com.example.deeplviewer.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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

class MyWebViewClient(
    private val activity: Activity,
) : WebViewClient() {
    private var isSplashFadeDone: Boolean = false
    private var param: String = "#en/en/"

    var loadFinishedListener: (() -> Unit)? = null

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        val regex = Regex("^https://www\\.deepl\\.com/.*/(translator|write).*$")
        val isDeepLTranslatorUrl = regex.matches(url)

        return !isDeepLTranslatorUrl
    }

    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
        // initially hide elements with `visibility: hidden`
        view.loadJavaScript("hide-elements.js")
        view.postDelayed({
            view.evaluateJavascript("hideElementsInitially();", null)
        }, 100)
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (!isSplashFadeDone) {
            isSplashFadeDone = true
            loadFinishedListener?.invoke()
        }

        // finally hide elements with `display: none`
        view.evaluateJavascript("hideElementsFinal();", null)

        view.loadJavaScript("jquery-3.6.0.min.js")

        switchTheme(view)
        saveUrlParam(view.url)
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
     * Saves the URL parameter from the DeepL translator URL
     */
    private fun saveUrlParam(url: String?) {
        Regex("#(.+?)/(.+?)/").find(url ?: "")?.let {
            param = it.value
            activity.getSharedPreferences("config", Context.MODE_PRIVATE)
                .edit {
                    putString("urlParam", param)
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

    private inner class ReloadButtonListener : View.OnClickListener {
        override fun onClick(view: View) {
            val i = Intent(activity, MainActivity::class.java)
            activity.finish()
            activity.overridePendingTransition(0, 0)
            activity.startActivity(i)
        }
    }
}
