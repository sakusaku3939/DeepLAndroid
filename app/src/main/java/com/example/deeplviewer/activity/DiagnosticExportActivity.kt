package com.example.deeplviewer.activity

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.deeplviewer.R
import com.example.deeplviewer.diagnostics.Diagnostics

/** Separate Activity: popup translation can finish onPause without losing the picker result. */
class DiagnosticExportActivity : AppCompatActivity() {
    private lateinit var model: ExportModel
    private var destination: Uri? = null
    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) {
            Diagnostics.event("export_cancelled")
            finish()
        } else {
            destination = uri
            model.save(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            setText(R.string.diagnostics_saving)
            gravity = android.view.Gravity.CENTER
        })
        model = ViewModelProvider(this)[ExportModel::class.java]
        model.result.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
        }
        destination = savedInstanceState?.getString("destination")?.let(Uri::parse)
        if (destination != null) model.save(destination!!)
        else if (savedInstanceState == null) {
            Diagnostics.event("export_requested")
            runCatching { createDocument.launch("DeepLAndroid-diagnostics-${System.currentTimeMillis()}.zip") }
                .onFailure {
                    Toast.makeText(this, R.string.diagnostics_save_failed, Toast.LENGTH_LONG).show()
                    finish()
                }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("destination", destination?.toString())
        super.onSaveInstanceState(outState)
    }

    class ExportModel(application: Application) : AndroidViewModel(application) {
        val result = MutableLiveData<Int>()
        private var started = false
        fun save(uri: Uri) {
            if (started) return
            started = true
            Thread({
                val outcome = runCatching {
                    val resolver = getApplication<Application>().contentResolver
                    requireNotNull(resolver.openOutputStream(uri, "wt")).use(Diagnostics::export)
                }
                if (outcome.isFailure) {
                    Diagnostics.event("export_failed", mapOf("exception" to outcome.exceptionOrNull()!!.javaClass.simpleName))
                    runCatching { android.provider.DocumentsContract.deleteDocument(getApplication<Application>().contentResolver, uri) }
                }
                result.postValue(if (outcome.isSuccess) R.string.diagnostics_saved else R.string.diagnostics_save_failed)
            }, "diagnostic-export").start()
        }
    }
}
