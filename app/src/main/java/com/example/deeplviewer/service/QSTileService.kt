package com.example.deeplviewer.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import android.annotation.SuppressLint
import com.example.deeplviewer.activity.FloatingTextSelection

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {
    override fun onClick() {
        val intent = Intent(applicationContext, FloatingTextSelection::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Intent.EXTRA_TEXT, "")

        @SuppressLint("StartActivityAndCollapseDeprecated")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startActivity(intent)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}