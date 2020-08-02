package com.example.deeplviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie("https://www.deepl.com/translator"
                , "privacySettings=%7B%22v%22%3A%221%22%2C%22t%22%3A1596240000%2C%22consent%22%3A%5B%22NECESSARY%22%2C%22PERFORMANCE%22%2C%22COMFORT%22%5D%7D;" +
                "domain=.deepl.com;" +
                "path=/;" +
                "max-age=31536000;")

        val webView: WebView = findViewById(R.id.webview)
        val webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                return true
            }
            override fun onPageFinished(view: WebView, url: String) {
                view.loadUrl("javascript:" +
                        "\$('#dl_translator').siblings().hide();" +
                        "\$('.dl_header_menu_v2__buttons__menu').hide();" +
                        "\$('footer').hide();" +
                        "\$('a').css('pointer-events','none');" +
                        "")
                webView.alpha = 1.0F
            }
        }
        webView.webViewClient = webViewClient
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("https://www.deepl.com/translator")
    }
}
