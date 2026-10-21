package com.example.deeplviewer

import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.deeplviewer.activity.MainActivity
import com.example.deeplviewer.diagnostics.Diagnostics
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class DiagnosticsTest {
    @Test fun settingsOffersExportWithoutToggle() {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(
            com.example.deeplviewer.activity.DiagnosticExportActivity::class.java.name,
            android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null), true)
        try {
            ActivityScenario.launch(com.example.deeplviewer.activity.SettingsActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentById(R.id.settings)
                        as com.example.deeplviewer.activity.SettingsActivity.SettingsFragment
                    assertNull(fragment.findPreference<androidx.preference.Preference>("diagnostic_details"))
                    fragment.findPreference<androidx.preference.Preference>("diagnostic_export")!!.performClick()
                }
                assertEquals(1, monitor.hits)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test fun errorScreenExportsSanitizedErrorWithEnvironmentAndLifecycle() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val view = activity.findViewById<WebView>(R.id.webview)
                view.stopLoading()
                activity.testWebViewClient!!.onReceivedError(view, object : WebResourceRequest {
                    override fun getUrl() = Uri.parse("https://www.deepl.com/en/translator?token=SECRET_QUERY#en/de/SECRET_TEXT")
                    override fun isForMainFrame() = true
                    override fun isRedirect() = false
                    override fun hasGesture() = false
                    override fun getMethod() = "GET"
                    override fun getRequestHeaders() = mapOf("Authorization" to "SECRET_AUTH")
                }, null)
                assertTrue(activity.findViewById<View>(R.id.save_diagnostics).hasOnClickListeners())
            }
            val output = ByteArrayOutputStream()
            Diagnostics.export(output)
            val entries = mutableMapOf<String, String>()
            ZipInputStream(output.toByteArray().inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
            assertTrue(entries.keys.containsAll(listOf("summary.txt", "environment.json", "events.jsonl")))
            val events = entries.getValue("events.jsonl").lineSequence().filter { it.isNotBlank() }.map(::JSONObject).toList()
            assertTrue(events.any { it.optString("event") == "activity_created" })
            val failure = events.last { it.optString("event") == "web_error" }
            assertEquals("[description-omitted]", failure.getString("chromium_error"))
            assertTrue(failure.has("view_id"))
            assertTrue(failure.has("load_id"))
            assertTrue(JSONObject(entries.getValue("environment.json")).has("webview_version"))
            assertFalse(entries.values.joinToString().contains("SECRET_"))
            scenario.recreate()
            scenario.onActivity { assertNotNull(it.findViewById<WebView>(R.id.webview)) }
        }
    }
}
