package com.example.deeplviewer

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.deeplviewer.activity.FloatingTextSelection
import com.example.deeplviewer.activity.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityRegressionTest {
    @Test
    fun networkErrorCanSaveStateAndRecreate() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webview)
                webView.stopLoading()
                activity.testWebViewClient!!.onReceivedError(webView, mainFrameRequest, null)
                assertNotNull(activity.findViewById<View>(R.id.reload))
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<WebView>(R.id.webview))
            }
        }
    }

    @Test
    fun loadingObserverPreservesAppBarCallback() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webview)
                webView.stopLoading()
                val client = activity.testWebViewClient!!
                var observations = 0
                if (client.hasFinishedInitialLoad) {
                    observations++
                } else {
                    val originalListener = client.loadFinishedListener
                    client.loadFinishedListener = {
                        originalListener?.invoke()
                        observations++
                    }
                }
                client.onPageFinished(webView, "https://www.deepl.com/en/translator")
                client.onPageFinished(webView, "https://www.deepl.com/en/translator")
                assertEquals(1, observations)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.toolbar).visibility)
            }
        }
    }

    @Test
    fun popupAcceptsStyledSelection() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), FloatingTextSelection::class.java)
            .setAction(Intent.ACTION_PROCESS_TEXT)
            .putExtra(Intent.EXTRA_PROCESS_TEXT, SpannableString("こんにちは"))
        ActivityScenario.launch<FloatingTextSelection>(intent).use { scenario ->
            scenario.onActivity { assertNotNull(it) }
        }
    }

    @Test
    fun popupAcceptsTileTextAndMissingExtras() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tileIntent = Intent(context, FloatingTextSelection::class.java)
            .putExtra(Intent.EXTRA_TEXT, "")
        ActivityScenario.launch<FloatingTextSelection>(tileIntent).use { scenario ->
            scenario.onActivity { assertNotNull(it) }
        }
        ActivityScenario.launch(FloatingTextSelection::class.java).use { scenario ->
            scenario.onActivity { assertNotNull(it) }
        }
    }

    private val mainFrameRequest = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse("https://www.deepl.com/en/translator")
        override fun isForMainFrame(): Boolean = true
        override fun isRedirect(): Boolean = false
        override fun hasGesture(): Boolean = false
        override fun getMethod(): String = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

}
