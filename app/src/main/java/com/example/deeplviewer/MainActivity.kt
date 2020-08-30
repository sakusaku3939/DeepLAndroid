package com.example.deeplviewer

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.webkit.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    @SuppressLint("AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie(
            "https://www.deepl.com/translator",
            "privacySettings=%7B%22v%22%3A%221%22%2C%22t%22%3A1598745600%2C%22m%22%3A%22LAX%22%2C%22consent%22%3A%5B%22NECESSARY%22%2C%22PERFORMANCE%22%2C%22COMFORT%22%5D%7D;" +
                    "domain=.deepl.com;" +
                    "path=/;" +
                    "max-age=31536000;"
        )

        val webView: WebView = findViewById(R.id.webview)
        val webViewClient = object : WebViewClient() {
            var isSplashFadeDone = false
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                view.loadUrl(
                    "javascript:" +
                            "\$('button').css('-webkit-tap-highlight-color','rgba(0, 0, 0, 0)');" +
                            "\$('#dl_translator').siblings().hide();" +
                            "\$('.dl_header_menu_v2__buttons__menu').hide();" +
                            "\$('.dl_header_menu_v2__buttons__item').hide();" +
                            "\$('.dl_header_menu_v2__links__item').hide();" +
                            "\$('.lmt__language_container_sec').hide();" +
                            "\$('.docTrans_translator_upload_button__inner_button').hide();" +
                            "\$('.lmt__target_toolbar__save').hide();" +
                            "\$('footer').hide();" +
                            "\$('a').css('pointer-events','none');" +
                            "\$('.lmt__translations_as_text__copy_button, .lmt__target_toolbar__copy').on('click',function(){" +
                            "const text = \$('.lmt__translations_as_text__text_btn').eq(0).text();" +
                            "Android.copyClipboard(text);});"
                )

                if (!isSplashFadeDone) {
                    isSplashFadeDone = true
                    val animation = AlphaAnimation(0.0F, 1.0F)
                    animation.duration = 100
                    webView.startAnimation(animation)
                }

                webView.alpha = 1.0F
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest,
                error: WebResourceError?
            ) {
                setContentView(R.layout.network_err)
                val button: ImageButton = findViewById(R.id.reload)
                val listener = ReloadButtonListener()
                button.setOnClickListener(listener)
            }

            private inner class ReloadButtonListener : View.OnClickListener {
                override fun onClick(view: View) {
                    val i = Intent(this@MainActivity, MainActivity::class.java)
                    finish()
                    overridePendingTransition(0, 0)
                    startActivity(i)
                    overridePendingTransition(0, 0)
                }
            }
        }
        @SuppressLint("SetJavaScriptEnabled")
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = webViewClient
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl("https://www.deepl.com/translator")
    }
}

class WebAppInterface(private val context: Context) {
    @JavascriptInterface
    fun copyClipboard(text: String) {
        val clipboard: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("translation_text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.copy_clipboard), Toast.LENGTH_SHORT)
            .show()
    }
}