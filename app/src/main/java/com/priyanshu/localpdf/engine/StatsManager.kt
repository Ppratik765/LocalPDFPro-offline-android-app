package com.priyanshu.localpdf.engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StatsState(
    val filesProcessed: Int = 0,
    val spaceSavedMb: Int = 0
)

object StatsManager {
    private const val PREFS_NAME = "localpdf_stats"
    private const val KEY_FILES_PROCESSED = "files_processed"
    private const val KEY_SPACE_SAVED = "space_saved_mb"

    private const val KEY_CUSTOM_URI = "custom_output_uri"

    private lateinit var prefs: SharedPreferences

    private val _statsState = MutableStateFlow(StatsState())
    val statsState: StateFlow<StatsState> = _statsState.asStateFlow()

    private val _customUriFlow = MutableStateFlow<String?>(null)
    val customUriFlow: StateFlow<String?> = _customUriFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _statsState.value = StatsState(
            filesProcessed = prefs.getInt(KEY_FILES_PROCESSED, 0),
            spaceSavedMb = prefs.getInt(KEY_SPACE_SAVED, 0)
        )
        _customUriFlow.value = prefs.getString(KEY_CUSTOM_URI, null)
    }

    fun setCustomOutputUri(uri: String?) {
        prefs.edit().putString(KEY_CUSTOM_URI, uri).apply()
        _customUriFlow.value = uri
    }

    fun getCustomOutputUri(): String? = prefs.getString(KEY_CUSTOM_URI, null)

    fun incrementFilesProcessed(count: Int = 1) {
        val current = _statsState.value
        val newVal = current.filesProcessed + count
        prefs.edit().putInt(KEY_FILES_PROCESSED, newVal).apply()
        _statsState.value = current.copy(filesProcessed = newVal)
    }

    fun incrementSpaceSaved(mb: Int) {
        val current = _statsState.value
        val newVal = current.spaceSavedMb + mb
        prefs.edit().putInt(KEY_SPACE_SAVED, newVal).apply()
        _statsState.value = current.copy(spaceSavedMb = newVal)
    }
}
