package com.example.deeplviewer

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference


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

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "config"
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val darkMode = findPreference<DropDownPreference>(getString(R.string.key_dark_mode))
            darkMode?.setOnPreferenceChangeListener { _, newValue ->

                var darkThemeMode = newValue as String
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && darkThemeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()) {
                    darkThemeMode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY.toString()
                }
                AppCompatDelegate.setDefaultNightMode(darkThemeMode.toInt())
                return@setOnPreferenceChangeListener true
            }

            val translateButton =
                findPreference<SwitchPreference>(getString(R.string.key_switch_translate_button))
            translateButton?.setOnPreferenceChangeListener { _, newValue ->
                val packageName = requireContext().packageName
                val packageManager = requireContext().packageManager

                val showComponentName =
                    ComponentName(packageName, "$packageName.FloatingTextSelection_show")
                val hideComponentName =
                    ComponentName(packageName, "$packageName.FloatingTextSelection_hide")

                if (newValue == true) {
                    packageManager.setComponentEnabledSetting(
                        showComponentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    packageManager.setComponentEnabledSetting(
                        hideComponentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } else {
                    packageManager.setComponentEnabledSetting(
                        hideComponentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    packageManager.setComponentEnabledSetting(
                        showComponentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
                return@setOnPreferenceChangeListener true
            }

            val versionButton = findPreference<Preference>(getString(R.string.key_version))
            versionButton?.summary = "v${BuildConfig.VERSION_NAME}"
            versionButton?.setOnPreferenceClickListener {
                startActivity(
                    Intent(Intent.ACTION_VIEW, getString(R.string.link_github_release).toUri())
                )
                return@setOnPreferenceClickListener true
            }
        }
    }
}
