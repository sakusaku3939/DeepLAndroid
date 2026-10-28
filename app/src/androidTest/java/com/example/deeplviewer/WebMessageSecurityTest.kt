package com.example.deeplviewer

import android.content.Context
import android.os.SystemClock
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewFeature
import com.example.deeplviewer.config.WebViewConfig
import com.example.deeplviewer.webview.WebAppInterface
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WebMessageSecurityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
    private lateinit var web: WebView
    private var originalLanguage: String? = null
    private var originalPage: String? = null

    @Before fun setUp() {
        originalLanguage = prefs.getString("urlParam", null)
        originalPage = prefs.getString("pageType", null)
        prefs.edit().putString("urlParam", "/l/ru/en-US").putString("pageType", "translator").commit()
        instrumentation.runOnMainSync {
            assertTrue(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))
            web = WebView(context)
            WebViewConfig.applyBasicSettings(web)
            WebAppInterface.install(web)
        }
    }

    @After fun tearDown() {
        instrumentation.runOnMainSync { if (::web.isInitialized) web.destroy() }
        prefs.edit().putString("urlParam", originalLanguage).putString("pageType", originalPage).commit()
    }

    private fun load(origin: String, html: String = "<html><body></body></html>") {
        val ready = CountDownLatch(1)
        instrumentation.runOnMainSync {
            web.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) { ready.countDown() }
            }
            web.loadDataWithBaseURL(origin, html, "text/html", "UTF-8", origin)
        }
        assertTrue("Page must finish loading", ready.await(10, TimeUnit.SECONDS))
    }

    private fun evaluate(script: String): String {
        val done = CountDownLatch(1)
        var result = ""
        instrumentation.runOnMainSync { web.evaluateJavascript(script) { result = it; done.countDown() } }
        assertTrue(done.await(5, TimeUnit.SECONDS))
        return result
    }

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (!condition() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        assertTrue(condition())
    }

    @Test fun trustedMainFrameSavesLanguagesAndUsesActualUrl() {
        load("https://www.deepl.com/en/translator/l/ru/en-US")
        assertEquals("\"undefined\"", evaluate("typeof Android"))
        prefs.edit().putString("pageType", "write").commit()
        evaluate("DeepLState.postMessage(JSON.stringify({type:'url',url:'https://www.deepl.com/en/write'}))")
        awaitCondition { prefs.getString("pageType", null) == "translator" }
        evaluate("DeepLState.postMessage(JSON.stringify({type:'languages',source:'ru',target:'ja'}))")
        awaitCondition { prefs.getString("urlParam", null) == "/l/ru/ja" }
        assertEquals("translator", prefs.getString("pageType", null))
    }

    @Test fun sameOriginAndOpaqueFramesCannotChangeSettings() {
        load("https://www.deepl.com/en/translator")
        evaluate("""
            window.probes=[];
            window.addEventListener('message', e => window.probes.push(e.data));
            ['same','opaque'].forEach(kind => {
                const frame=document.createElement('iframe');
                if(kind==='opaque')frame.setAttribute('sandbox','allow-scripts');
                frame.srcdoc='<script>if(window.DeepLState)DeepLState.postMessage(JSON.stringify({type:"languages",source:"ru",target:"ja"}));parent.postMessage("'+kind+':"+typeof DeepLState+":"+typeof Android,"*");<\/script>';
                document.body.appendChild(frame);
            });
        """.trimIndent())
        awaitCondition { evaluate("window.probes.length") == "2" }
        assertEquals("true", evaluate("window.probes.includes('same:object:undefined') && window.probes.includes('opaque:undefined:undefined')"))
        SystemClock.sleep(300)
        instrumentation.waitForIdleSync()
        assertEquals("/l/ru/en-US", prefs.getString("urlParam", null))
    }

    @Test fun foreignOriginHasNoBridge() {
        load("https://example.invalid/")
        assertEquals("\"undefined\"", evaluate("typeof DeepLState"))
        assertEquals("\"undefined\"", evaluate("typeof Android"))
        assertEquals("/l/ru/en-US", prefs.getString("urlParam", null))
    }

    @Test fun malformedMessagesCannotChangeSettingsOrCrash() {
        load("https://www.deepl.com/en/translator")
        evaluate("""
            ['broken', '{}', 'null', '[]', 'x'.repeat(513),
             JSON.stringify({type:'languages',source:'ru',target:'/ja'}),
             JSON.stringify({type:'languages',source:5,target:'ja'}),
             JSON.stringify({type:'unknown',source:'ru',target:'ja'})]
             .forEach(value => DeepLState.postMessage(value));
        """.trimIndent())
        SystemClock.sleep(300)
        instrumentation.waitForIdleSync()
        assertEquals("/l/ru/en-US", prefs.getString("urlParam", null))
        evaluate("DeepLState.postMessage(JSON.stringify({type:'languages',source:'ru',target:'en-gb'}))")
        awaitCondition { prefs.getString("urlParam", null) == "/l/ru/en-GB" }
    }
}
