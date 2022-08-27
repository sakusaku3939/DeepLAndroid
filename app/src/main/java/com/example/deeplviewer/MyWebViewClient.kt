package com.example.deeplviewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.Toast
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MyWebViewClient(
    private val activity: Activity,
    private val isFloating: Boolean = false,
) : WebViewClient() {
    private var isSplashFadeDone: Boolean = false
    private var param: String = "#en/en/"

    var loadFinishedListener: (() -> Unit)? = null

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false

    override fun onPageFinished(view: WebView, url: String) {
        if (!isSplashFadeDone) {
            view.loadJavaScript("jquery-3.6.0.min.js")
            view.loadJavaScript("init.js")

            val config = view.context.getSharedPreferences("config", Context.MODE_PRIVATE)
            val isEnabledSwapLanguageButton =
                config.getBoolean(
                    view.context.getString(R.string.key_switch_lang_button),
                    true
                )
            if (isEnabledSwapLanguageButton) {
                view.loadJavaScript("patch-switchLanguage.js")
            }

            if (isFloating) {
                view.loadJavaScript("patch-ads.js")
            }

            isSplashFadeDone = true
            loadFinishedListener?.invoke()
        }

        view.loadJavaScript("patch-clipboard.js")

        val nightMode =
            (view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (nightMode) {
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) error?.description.toString() else ""
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
}
