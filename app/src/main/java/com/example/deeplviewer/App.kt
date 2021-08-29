package com.example.deeplviewer

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        switchTheme()
    }

    private fun switchTheme() {
        val config = getSharedPreferences("config", Context.MODE_PRIVATE)
        var darkThemeMode =
            config.getString(
                "defaultDarkMode",
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()
            )!!

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && darkThemeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()) {
            darkThemeMode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY.toString()
        }
        AppCompatDelegate.setDefaultNightMode(darkThemeMode.toInt())
    }
}