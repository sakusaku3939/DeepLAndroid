package com.example.deeplviewer.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.deeplviewer.activity.FloatingTextSelection

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {
    override fun onClick() {
        val intent = Intent(applicationContext, FloatingTextSelection::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Intent.EXTRA_TEXT, "")

        @SuppressLint("StartActivityAndCollapseDeprecated")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            startActivityAndCollapse(intent)
        }
    }
}