package com.mndublo.odolens.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private var logFile: File? = null
    private val _logsFlow = MutableStateFlow<String>("")
    val logsFlow: StateFlow<String> = _logsFlow.asStateFlow()

    fun init(context: Context) {
        logFile = File(context.filesDir, "debug_logs.txt")
        loadLogs()
    }

    private fun loadLogs() {
        logFile?.let { file ->
            if (file.exists()) {
                _logsFlow.value = file.readText()
            }
        }
    }

    fun log(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val formattedMsg = "[$timestamp] $message\n"
        
        logFile?.let { file ->
            try {
                file.appendText(formattedMsg)
                _logsFlow.value = _logsFlow.value + formattedMsg
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clear() {
        logFile?.let { file ->
            if (file.exists()) {
                file.writeText("")
            }
            _logsFlow.value = ""
        }
    }
}
