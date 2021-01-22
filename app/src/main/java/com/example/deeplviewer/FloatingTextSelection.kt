package com.example.deeplviewer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class FloatingTextSelection : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val floatingText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("FLOATING_TEXT", floatingText)
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