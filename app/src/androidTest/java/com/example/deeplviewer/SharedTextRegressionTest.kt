package com.example.deeplviewer

import android.content.Intent
import android.graphics.Typeface
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.deeplviewer.activity.MainActivity
import com.example.deeplviewer.helper.UrlHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedTextRegressionTest {
    @Test
    fun sharePreservesPlainText() {
        assertSharedText("こんにちは / plain + text\nsecond line")
    }

    @Test
    fun sharePreservesStyledText() {
        val text = SpannableString("こんにちは / styled + text\nsecond line")
        text.setSpan(StyleSpan(Typeface.BOLD), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        assertSharedText(text)
    }

    private fun assertSharedText(text: CharSequence) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        val intent = Intent(instrumentation.targetContext, MainActivity::class.java)
            .setAction(Intent.ACTION_SEND)
            .setType("text/plain")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(Intent.EXTRA_TEXT, text)
        var activity: MainActivity? = null
        try {
            instrumentation.targetContext.startActivity(intent)
            activity = instrumentation.waitForMonitorWithTimeout(monitor, 10000) as? MainActivity
            assertNotNull("Sharing must launch MainActivity", activity)
            val launchedActivity = activity!!
            var originalUrl: String? = null
            val deadline = SystemClock.uptimeMillis() + 5000
            while (originalUrl == null && SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync {
                    val webView = launchedActivity.findViewById<WebView>(R.id.webview)
                    originalUrl = webView.originalUrl
                    if (originalUrl != null) webView.stopLoading()
                }
                if (originalUrl == null) SystemClock.sleep(50)
            }
            assertEquals("Shared text must reach the translation URL", text.toString(),
                UrlHelper.extractText(originalUrl))
        } finally {
            activity?.let { launchedActivity ->
                instrumentation.runOnMainSync { launchedActivity.finish() }
            }
            instrumentation.removeMonitor(monitor)
        }
    }
}
