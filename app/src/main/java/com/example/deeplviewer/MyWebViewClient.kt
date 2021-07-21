package com.example.deeplviewer

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.Toast
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MyWebViewClient(
    private val activity: MainActivity,
    private val webView: WebView
) : WebViewClient() {
    private var isSplashFadeDone: Boolean = false
    private var param: String = "#en/en/"

    val urlParam: String get() = param

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(intent)
        return true
    }

    override fun onPageFinished(view: WebView, url: String) {
        view.loadUrl(
            "javascript:" +
                    """
                        $('button').css('-webkit-tap-highlight-color','rgba(0, 0, 0, 0)');
                        $('#dl_translator').siblings().hide();
                        $('.dl_header_menu_v2__buttons__menu').hide();
                        $('.dl_header_menu_v2__buttons__item').hide();
                        $('.dl_header_menu_v2__links').children().not('#dl_menu_translator_simplified').hide();
                        $('.dl_header_menu_v2__separator').hide();
                        $('.lmt__bottom_text--mobile').hide();
                        $('#dl_cookieBanner').hide();
                        $('.lmt__language_container_sec').hide();
                        $('.docTrans_translator_upload_button__inner_button').hide();
                        $('.lmt__target_toolbar__save').hide();
                        $('footer').hide();
                        $('a').css('pointer-events','none');
                        $('.lmt__sides_container').css('margin-bottom','32px');
                        $('.lmt__translations_as_text__copy_button, .lmt__target_toolbar__copy').on('click',function() {
                            const text = $('.lmt__translations_as_text__text_btn').eq(0).text();
                            Android.copyClipboard(text);
                        });
                    """
        )

        if (!isSplashFadeDone) {
            isSplashFadeDone = true
            val animation = AlphaAnimation(0.0F, 1.0F)
            animation.duration = 100
            webView.startAnimation(animation)
        }
        webView.alpha = 1.0F

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            val nightMode = (webView.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val forceDarkMode = if (nightMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            WebSettingsCompat.setForceDark(webView.settings, forceDarkMode)
        }

        Regex("""#(.+?)/(.+?)/""").find(webView.url ?: "")?.let { param = it.value }
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
}