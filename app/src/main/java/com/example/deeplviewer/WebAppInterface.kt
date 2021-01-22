package com.example.deeplviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

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