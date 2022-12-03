package com.example.deeplviewer

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

class MyWebViewClient(
    private val activity: Activity,
) : WebViewClient() {
    private var isSplashFadeDone: Boolean = false
    private var param: String = "#en/en/"

    var loadFinishedListener: (() -> Unit)? = null

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false

    override fun onPageFinished(view: WebView, url: String) {
        if (!isSplashFadeDone) {
            view.loadJavaScript("jquery-3.6.0.min.js")
            view.loadJavaScript("init.js")

            // Test method to display the clicked class and id
            // view.loadJavaScript("test.js")

            isSplashFadeDone = true
            loadFinishedListener?.invoke()
        }

        view.loadJavaScript("patch-clipboard.js")
        switchTheme(view)

        Regex("#(.+?)/(.+?)/").find(view.url ?: "")?.let {
            param = it.value
            activity.getSharedPreferences("config", Context.MODE_PRIVATE)
                .edit()
                .putString("urlParam", param)
                .apply()
        }
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

    private inner class ReloadButtonListener : View.OnClickListener {
        override fun onClick(view: View) {
            val i = Intent(activity, MainActivity::class.java)
            activity.finish()
            activity.overridePendingTransition(0, 0)
            activity.startActivity(i)
        }
    }

    private fun getAssetsText(context: Context, fileName: String): String {
        return context.assets.open(fileName).reader(Charsets.UTF_8).use { it.readText() }
    }

    private fun WebView.loadJavaScript(fileName: String) {
        this.loadUrl("javascript:${getAssetsText(this.context, fileName)}")
    }

    private fun switchTheme(view: WebView) {
        val uiMode = view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (uiMode != Configuration.UI_MODE_NIGHT_YES) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setAlgorithmicDark(view)
        } else {
            setForceDark(view)
        }
    }

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
}
