package com.example.deeplviewer

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.deeplviewer.activity.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import android.util.Log // Log クラスをインポート

@RunWith(AndroidJUnit4::class)
class WebViewScreenshotTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var webViewBitmap: Bitmap

    // Logcat でフィルタリングしやすいようにタグを定義
    private val TAG = "WebViewScreenshotTest"

    @Before
    fun setup() {
        Log.d(TAG, "setup: テストセットアップ開始")
        val latch = CountDownLatch(1)

        activityRule.scenario.onActivity { activity ->
            Log.d(TAG, "setup: onActivity コールバック実行")
            activity.testWebViewClient?.loadFinishedListener = loadFinishedListener@{
                Log.d(TAG, "setup: loadFinishedListener が呼び出されました")
                val webView = activity.findViewById<android.webkit.WebView>(R.id.webview)

                // WebView が null でないことを確認
                if (webView == null) {
                    Log.e(TAG, "setup: WebView が見つかりませんでした！")
                    latch.countDown() // 見つからない場合も待機を解除
                    return@loadFinishedListener
                }

                // WebView の描画が完了するまで少し待つことで、JavaScript の適用が保証される
                webView.postDelayed({ // UIスレッドで実行
                    Log.d(TAG, "setup: postDelayed 内の処理開始")
                    Log.d(TAG, "setup: WebView width=${webView.width}, height=${webView.height}")

                    // WebView の幅または高さが0の場合、描画されていない可能性がある
                    if (webView.width == 0 || webView.height == 0) {
                        Log.e(TAG, "setup: WebView の幅または高さが0です。描画されていない可能性があります。")
                        latch.countDown()
                        return@postDelayed
                    }

                    webViewBitmap = captureWebViewScreenshot(webView)
                    if (::webViewBitmap.isInitialized && webViewBitmap != null) {
                        Log.d(TAG, "setup: スクリーンショットのビットマップが正常に取得されました。")
                    } else {
                        Log.e(TAG, "setup: スクリーンショットのビットマップ取得に失敗しました。")
                    }
                    latch.countDown()
                }, 1000) // 例: 1000ミリ秒 (1秒) 待つ。ページの複雑さにより調整が必要。
            }
        }
        Log.d(TAG, "setup: ロード完了を待機中...")
        // ロード完了を最大30秒待機
        val awaitSuccess = latch.await(30, TimeUnit.SECONDS)
        if (awaitSuccess) {
            Log.d(TAG, "setup: ロード完了待機が成功しました。")
        } else {
            Log.e(TAG, "setup: ロード完了待機がタイムアウトしました！")
        }
    }

    @Test
    fun testWebViewContentScreenshot() {
        Log.d(TAG, "testWebViewContentScreenshot: テストメソッド開始")
        // webViewBitmap が初期化され、null でないことを確認
        if (!::webViewBitmap.isInitialized || webViewBitmap == null) {
            Log.e(TAG, "testWebViewContentScreenshot: webViewBitmap が初期化されていないか、null です。")
            // テストを失敗させるか、スキップする
            org.junit.Assert.fail("WebView Bitmap was not initialized or is null.")
            return
        }

        // 例: スクリーンショットをファイルに保存
        saveBitmapToFile(webViewBitmap, "webview_screenshot.png")

        // ここでは簡単に、ビットマップが null でないことを確認するだけにする
        assert(webViewBitmap != null)
        Log.d(TAG, "testWebViewContentScreenshot: テストメソッド終了")
    }

    private fun captureWebViewScreenshot(webView: android.webkit.WebView): Bitmap {
        Log.d(TAG, "captureWebViewScreenshot: スクリーンショットキャプチャ開始")
        val width = webView.width
        val height = webView.height
        Log.d(TAG, "captureWebViewScreenshot: WebView のサイズ: width=$width, height=$height")

        if (width <= 0 || height <= 0) {
            Log.e(TAG, "captureWebViewScreenshot: WebView のサイズが不正です (width=$width, height=$height)。空のBitmapを返します。")
            // サイズが不正な場合は、空のBitmapを返すか、例外をスローする
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // 最小サイズのBitmapを返す
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas) // WebView の内容を描画
        Log.d(TAG, "captureWebViewScreenshot: スクリーンショットキャプチャ完了")
        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap, fileName: String) {
        Log.d(TAG, "saveBitmapToFile: ファイル保存処理開始。ファイル名: $fileName")
        val storageDir = File(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "screenshots"
        )
        Log.d(TAG, "saveBitmapToFile: 保存ディレクトリ: ${storageDir.absolutePath}")

        if (!storageDir.exists()) {
            val created = storageDir.mkdirs()
            Log.d(TAG, "saveBitmapToFile: ディレクトリ作成試行: ${storageDir.absolutePath}, 成功: $created")
            if (!created) {
                Log.e(TAG, "saveBitmapToFile: スクリーンショットディレクトリの作成に失敗しました！")
                return
            }
        }
        val file = File(storageDir, fileName)
        Log.d(TAG, "saveBitmapToFile: 保存先のファイルパス: ${file.absolutePath}")

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d(TAG, "saveBitmapToFile: スクリーンショットが正常に保存されました: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "saveBitmapToFile: スクリーンショットの保存中にエラーが発生しました: ${e.message}", e)
        }
    }
}