package com.example.deeplviewer.webview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.annotation.Keep
import com.example.deeplviewer.R

@Keep
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

    @JavascriptInterface
    fun stringToBase64String(s: String): String {
        return Base64.encodeToString(s.toByteArray(), Base64.DEFAULT)
    }

    @JavascriptInterface
    fun getAssetsText(fileName: String): String {
        return context.assets.open(fileName).reader(Charsets.UTF_8).use { it.readText() }
    }
}