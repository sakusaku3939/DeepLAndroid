package com.example.deeplviewer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class FloatingTextSelection : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val androidTranslateFloatingText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            } else {
                null
            }

            val floatingText = androidTranslateFloatingText ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("FLOATING_TEXT", floatingText?.toString())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}