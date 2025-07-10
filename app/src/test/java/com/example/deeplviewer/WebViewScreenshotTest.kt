package com.example.deeplviewer

import android.R
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.deeplviewer.activity.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewScreenshotTest {
    @Test
    fun test() {
        val activityScenario = launch(MainActivity::class.java)

        onView(withId(R.id.content))
            .check(matches(ViewMatchers.isDisplayed()))
    }
}
