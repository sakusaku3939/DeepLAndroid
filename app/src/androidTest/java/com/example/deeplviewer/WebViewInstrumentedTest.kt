//package com.example.deeplviewer
//
//import android.util.Log
//import androidx.test.ext.junit.rules.ActivityScenarioRule
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import com.example.deeplviewer.activity.MainActivity
//import com.github.takahirom.roborazzi.captureRoboImage
//import org.junit.Rule
//import org.junit.Test
//import org.junit.runner.RunWith
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.TimeUnit
//
//@RunWith(AndroidJUnit4::class)
//class WebViewInstrumentedTest {
//
//    @get:Rule
//    val activityRule = ActivityScenarioRule(MainActivity::class.java)
//
//    @Test
//    fun captureWebViewTopScreen() {
//        // WebViewの読み込み完了を待機
//        waitForWebViewLoad()
//
//        // スクリーンショットを撮影
//        activityRule.scenario.onActivity { activity ->
//            val webView = activity.findViewById<android.webkit.WebView>(R.id.webview)
//            webView.captureRoboImage("webViewTopScreen.png")
//        }
//    }
//
//    private fun waitForWebViewLoad() {
//        val latch = CountDownLatch(1)
//
//        activityRule.scenario.onActivity { activity ->
//            val webViewClient = activity.testWebViewClient
//            webViewClient?.loadFinishedListener = {
//                latch.countDown()
//            }
//        }
//
//        // 最大30秒待機
//        latch.await(30, TimeUnit.SECONDS)
//
//        // 追加で3秒待機（レンダリング完了のため）
//        Thread.sleep(3000)
//    }
//}