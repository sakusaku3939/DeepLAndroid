package com.example.deeplviewer

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        val toolbar: Toolbar = findViewById(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.sharedPreferencesName = "config"
            val dataPref = findPreference<DropDownPreference>(darkModeKey)
            dataPref?.value = preferenceManager.sharedPreferences.getString(
                darkModeKey,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()
            )
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val preference = findPreference<DropDownPreference>(darkModeKey)
            preference?.onPreferenceChangeListener = this
        }


        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            if (preference.key == darkModeKey) {
                Handler(Looper.getMainLooper()).postDelayed({
                    val data = requireActivity().application.getSharedPreferences(
                        "config",
                        MODE_PRIVATE
                    )
                    var darkThemeMode = data.getString(
                        preference.key,
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()
                    )!!
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && darkThemeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()) {
                        darkThemeMode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY.toString()
                    }
                    AppCompatDelegate.setDefaultNightMode(darkThemeMode.toInt())
                }, 100)
            }
            return true
        }
    }

    companion object {
        private const val darkModeKey = "darkMode"
    }
}