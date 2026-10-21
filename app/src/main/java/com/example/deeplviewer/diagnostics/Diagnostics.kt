package com.example.deeplviewer.diagnostics

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import com.example.deeplviewer.BuildConfig
import com.example.deeplviewer.R
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Diagnostics {
    private const val MAX_BYTES = 512 * 1024L
    private lateinit var app: Application
    private lateinit var directory: File
    private val lock = Any()
    private val session = UUID.randomUUID().toString()
    private val dropped = AtomicInteger()
    private val writer = ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, ArrayBlockingQueue(256),
        { task -> Thread(task, "diagnostic-writer") }, { _, _ -> dropped.incrementAndGet() })
    @Volatile private var webSettings = JSONObject()

    fun initialize(application: Application) {
        app = application
        directory = File(app.noBackupFilesDir, "diagnostics")
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                writer.shutdown()
                if (Thread.currentThread().name != "diagnostic-writer") writer.awaitTermination(750, TimeUnit.MILLISECONDS)
                synchronized(lock) {
                    directory.mkdirs()
                    File(directory, "crash.txt").writeText("time_ms=${System.currentTimeMillis()} session=$session\n" + DiagnosticPrivacy.crash(error))
                }
            } catch (_: Exception) {
                // Logging must never replace the original crash handler.
            } finally {
                previousHandler?.uncaughtException(thread, error)
            }
        }
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private fun log(activity: Activity, state: String) = event("activity_$state", mapOf("activity" to activity.javaClass.simpleName))
            override fun onActivityCreated(a: Activity, b: Bundle?) { log(a, "created") }
            override fun onActivityStarted(a: Activity) { log(a, "started") }
            override fun onActivityResumed(a: Activity) { log(a, "resumed") }
            override fun onActivityPaused(a: Activity) { log(a, "paused") }
            override fun onActivityStopped(a: Activity) { log(a, "stopped") }
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) { log(a, "save_state") }
            override fun onActivityDestroyed(a: Activity) { log(a, "destroyed") }
        })
        val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        runCatching {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { event("network_available", networkState()) }
                override fun onLost(network: Network) { event("network_lost", networkState()) }
                override fun onCapabilitiesChanged(n: Network, c: NetworkCapabilities) { event("network_capabilities", networkState()) }
                override fun onLinkPropertiesChanged(n: Network, p: android.net.LinkProperties) { event("network_properties", networkState()) }
            }
            if (Build.VERSION.SDK_INT >= 24) connectivity.registerDefaultNetworkCallback(callback)
            else connectivity.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback)
        }.onFailure { event("network_observer_unavailable", mapOf("exception" to it.javaClass.simpleName)) }
        event("process_started", mapOf("environment" to environment()))
    }

    fun event(name: String, fields: Map<String, Any?> = emptyMap()) {
        if (!::app.isInitialized) return
        val record = JSONObject(fields).put("event", name).put("time_ms", System.currentTimeMillis())
            .put("elapsed_ms", SystemClock.elapsedRealtime()).put("session", session).toString() + "\n"
        writer.execute {
            runCatching {
                synchronized(lock) {
                    directory.mkdirs()
                    val file = File(directory, "events.jsonl")
                    if (file.length() + record.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
                        val old = File(directory, "events.previous.jsonl")
                        check(!old.exists() || old.delete()) { "Cannot rotate diagnostics" }
                        check(!file.exists() || file.renameTo(old)) { "Cannot rotate diagnostics" }
                    }
                    file.appendText(record)
                }
            }.onFailure { dropped.incrementAndGet() }
        }
    }

    fun webView(view: WebView) {
        val s = view.settings
        webSettings = JSONObject().put("ua_tokens", DiagnosticPrivacy.userAgent(s.userAgentString))
            .put("javascript", s.javaScriptEnabled).put("dom_storage", s.domStorageEnabled)
            .put("cache_mode", s.cacheMode).put("mixed_content_mode", s.mixedContentMode)
            .put("cookies", CookieManager.getInstance().acceptCookie())
            .put("third_party_cookies", CookieManager.getInstance().acceptThirdPartyCookies(view))
        event("webview_configured", mapOf("settings" to webSettings, "environment" to environment()))
    }

    @Suppress("DEPRECATION")
    fun networkState(): Map<String, Any?> = runCatching {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = if (Build.VERSION.SDK_INT >= 23) cm.activeNetwork else null
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val links = network?.let { cm.getLinkProperties(it) }
        mapOf<String, Any?>(
            "connected" to (cm.activeNetworkInfo?.isConnected == true),
            "transport" to (caps?.let { c -> listOf(0 to "cellular", 1 to "wifi", 3 to "ethernet", 4 to "vpn").filter { c.hasTransport(it.first) }.joinToString(",") { it.second } } ?: "unknown"),
            "validated" to if (Build.VERSION.SDK_INT >= 23) caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) else null,
            "captive_portal" to if (Build.VERSION.SDK_INT >= 23) caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) else null,
            "metered" to cm.isActiveNetworkMetered,
            "proxy_configured" to (if (Build.VERSION.SDK_INT >= 23) cm.defaultProxy != null else links?.httpProxy != null),
            "private_dns_active" to if (Build.VERSION.SDK_INT >= 28) links?.isPrivateDnsActive else null,
            "dns_server_count" to links?.dnsServers?.size
        )
    }.getOrElse { mapOf("unavailable" to it.javaClass.simpleName) }

    private fun environment(): JSONObject {
        val provider = runCatching { WebViewCompat.getCurrentWebViewPackage(app) }.getOrNull()
        val prefs = app.getSharedPreferences("config", Context.MODE_PRIVATE)
        val power = app.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val memory = android.app.ActivityManager.MemoryInfo()
        (app.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(memory)
        return JSONObject().put("schema_version", 1).put("app_version", BuildConfig.VERSION_NAME)
            .put("app_code", BuildConfig.VERSION_CODE).put("debug", BuildConfig.DEBUG)
            .put("android", Build.VERSION.RELEASE).put("api", Build.VERSION.SDK_INT)
            .put("power_save", power.isPowerSaveMode).put("low_memory", memory.lowMemory)
            .put("data_saver_status", if (Build.VERSION.SDK_INT >= 24) connectivity.restrictBackgroundStatus else "unavailable")
            .put("webview_package", provider?.packageName ?: "unknown").put("webview_version", provider?.versionName ?: "unknown")
            .put("details", true)
            .put("auto_delete_cookies", prefs.getBoolean(app.getString(R.string.key_auto_delete_cookies), false))
            .put("network", JSONObject(networkState())).put("webview_settings", webSettings)
            .put("dropped_events", dropped.get())
    }

    /** Called on a worker thread. A queued barrier includes all preceding writes. */
    fun export(output: OutputStream) {
        val barrier = java.util.concurrent.CountDownLatch(1)
        // Blocking insertion is safe here: export never runs on the UI/writer thread.
        writer.prestartCoreThread()
        check(writer.queue.offer(Runnable { barrier.countDown() }, 5, TimeUnit.SECONDS)) { "Diagnostic queue timeout" }
        check(barrier.await(15, TimeUnit.SECONDS)) { "Diagnostic writer timeout" }
        val env = environment().toString(2)
        val snapshot = synchronized(lock) {
            listOf("events.previous.jsonl", "events.jsonl", "crash.txt").associateWith { name ->
                File(directory, name).takeIf { it.exists() }?.readText().orEmpty()
            }
        }
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, value: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            val events = listOf("events.previous.jsonl", "events.jsonl").joinToString("") { name ->
                snapshot.getValue(name)
            }
            val recentFailures = events.lineSequence().filter { line ->
                runCatching { JSONObject(line).optString("event") in setOf("web_error", "http_error", "ssl_error", "renderer_gone") }.getOrDefault(false)
            }.toList().takeLast(10).joinToString("\n")
            entry("summary.txt", "DeepLAndroid diagnostics\nExported at ${System.currentTimeMillis()} (Unix ms)\n$env\n\nRecent failures\n$recentFailures\n\nPrivacy: text, cookies, headers, query/fragment and unknown URL paths omitted. UA device tokens omitted. Network values describe Android-observable state, not proof that a blocker is absent. Events capped at two 512 KiB files; crash is the latest retained crash, possibly from a previous session.\n")
            entry("environment.json", env)
            entry("events.jsonl", events)
            snapshot.getValue("crash.txt").takeIf { it.isNotEmpty() }?.let { entry("crash.txt", it) }
        }
    }
}
