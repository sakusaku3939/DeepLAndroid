package com.example.deeplviewer

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {
    override fun onClick() {
        val intent = Intent(applicationContext, FloatingTextSelection::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Intent.EXTRA_TEXT, "")
        startActivityAndCollapse(intent)
    }
}