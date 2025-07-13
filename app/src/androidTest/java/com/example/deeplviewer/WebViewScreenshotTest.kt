package com.example.deeplviewer

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.deeplviewer.activity.MainActivity
import com.example.deeplviewer.webview.MyWebViewClient
import com.karumi.shot.ScreenshotTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WebViewScreenshotTest : ScreenshotTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun captureWebViewTopScreen() {
        waitForWebViewLoad()

        activityRule.scenario.onActivity { activity ->
            val webView = activity.findViewById<WebView>(R.id.webview)
            compareScreenshot(webView, name = "webview_top_screen")
        }
    }

    private fun waitForWebViewLoad() {
        val latch = CountDownLatch(1)
        var webViewClient: MyWebViewClient? = null

        activityRule.scenario.onActivity { activity ->
            webViewClient = activity.testWebViewClient
            webViewClient?.loadFinishedListener = {
                latch.countDown()
            }
        }

        checkNotNull(webViewClient) { "WebViewClient is not available" }

        // Wait for load completion (max 30 seconds)
        val loadCompleted = latch.await(30, TimeUnit.SECONDS)

        if (!loadCompleted) {
            throw AssertionError("WebView load timeout: failed to complete within 30 seconds")
        }

        // Wait for rendering to complete (using SystemClock for instrumentation tests)
        SystemClock.sleep(5000)
    }
}