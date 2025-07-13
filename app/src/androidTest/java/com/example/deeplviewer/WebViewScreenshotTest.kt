package com.example.deeplviewer

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.deeplviewer.activity.MainActivity
import com.karumi.shot.ScreenshotTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest : ScreenshotTest {

    @Test
    fun showsWelcomeMessage() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            compareScreenshot(activity)
        }
    }
}