import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.deeplviewer.activity.MainActivity
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.example.deeplviewer.R
import com.example.deeplviewer.webview.MyWebViewClient

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebViewScreenshotTest {

    @Test
    fun roborazziTest() {
        // ① ページ読み込み完了を待つ
        val latch = CountDownLatch(1)
        val scenario = launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            (activity.findViewById<WebView>(R.id.webview).webViewClient as MyWebViewClient)
                .loadFinishedListener = { latch.countDown() }
        }
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(2, TimeUnit.SECONDS)

        // ② 手動で measure/layout を通す
        scenario.onActivity { activity ->
            val root = activity.findViewById<View>(android.R.id.content)
            val w = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val h = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
            root.measure(w, h)
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        }

        // ③ 画面全体をキャプチャ（WebView 単体ではなく ROOT）
        onView(isRoot())
            .captureRoboImage()
    }
}