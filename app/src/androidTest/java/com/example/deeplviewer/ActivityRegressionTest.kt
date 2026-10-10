package com.example.deeplviewer

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.text.SpannableString
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.deeplviewer.helper.UrlHelper
import com.example.deeplviewer.activity.FloatingTextSelection
import com.example.deeplviewer.activity.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityRegressionTest {
    @Test
    fun newIntentAfterNetworkErrorRestoresMainScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var oldWebView: WebView
            val text = "こんにちは / retry"
            scenario.onActivity { activity ->
                oldWebView = activity.findViewById(R.id.webview)
                oldWebView.stopLoading()
                activity.testWebViewClient!!.onReceivedError(oldWebView, mainFrameRequest, null)
                assertNotNull(activity.findViewById<View>(R.id.reload))
                activity.startActivity(
                    Intent(activity.intent)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(Intent.EXTRA_TEXT, text)
                )
            }
            var restored = false
            val deadline = SystemClock.uptimeMillis() + 10000
            while (!restored && SystemClock.uptimeMillis() < deadline) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    val webView = activity.findViewById<WebView>(R.id.webview)
                    restored = webView != null && UrlHelper.extractText(webView.originalUrl) == text
                }
                if (!restored) SystemClock.sleep(100)
            }
            assertTrue("New intent must restore the screen and load its text", restored)
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webview)
                assertNotSame(oldWebView, webView)
                assertNotNull(webView.parent)
                assertTrue(activity.findViewById<View>(R.id.settingButton).hasOnClickListeners())
                assertTrue(activity.findViewById<View>(R.id.writeButton).hasOnClickListeners())
                webView.stopLoading()
                activity.testWebViewClient!!.onPageFinished(webView, webView.originalUrl!!)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.toolbar).visibility)
            }
        }
    }

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
