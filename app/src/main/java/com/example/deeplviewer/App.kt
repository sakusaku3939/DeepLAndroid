package com.example.deeplviewer

import android.app.Application
import android.content.Context
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        switchTheme()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            removeOldAppShortcut()
        }
    }

    private fun switchTheme() {
        val config = getSharedPreferences("config", Context.MODE_PRIVATE)
        var darkThemeMode =
            config.getString(
                getString(R.string.key_dark_mode),
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()
            )!!

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && darkThemeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()) {
            darkThemeMode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY.toString()
        }
        AppCompatDelegate.setDefaultNightMode(darkThemeMode.toInt())
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun removeOldAppShortcut() {
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        shortcutManager.removeDynamicShortcuts(
            listOf(
                "theme"//version 5.0(17) Used to launch this app with darkTheme or lightTheme.
            )
        )
    }
}