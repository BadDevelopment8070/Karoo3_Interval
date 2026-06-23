package com.bastiankahuna.karoosmartftp

import android.content.Context
import com.bastiankahuna.karoosmartftp.engine.EngineState
import com.bastiankahuna.karoosmartftp.model.WorkoutLibrary

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("smart_ftp_settings", Context.MODE_PRIVATE)

    var selectedWorkoutId: String
        get() = prefs.getString(KEY_WORKOUT_ID, WorkoutLibrary.all.first().id) ?: WorkoutLibrary.all.first().id
        set(value) = prefs.edit().putString(KEY_WORKOUT_ID, value).apply()

    var useSystemFtp: Boolean
        get() = prefs.getBoolean(KEY_USE_SYSTEM_FTP, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_SYSTEM_FTP, value).apply()

    var manualFtp: Int
        get() = prefs.getInt(KEY_MANUAL_FTP, 250)
        set(value) = prefs.edit().putInt(KEY_MANUAL_FTP, value.coerceIn(50, 700)).apply()

    var resetSerial: Int
        get() = prefs.getInt(KEY_RESET_SERIAL, 0)
        set(value) = prefs.edit().putInt(KEY_RESET_SERIAL, value).apply()

    fun effectiveFtp(systemFtp: Int?): Int {
        val sys = systemFtp?.takeIf { it > 0 }
        return if (useSystemFtp && sys != null) sys else manualFtp.coerceAtLeast(50)
    }

    fun resetWorkout() {
        prefs.edit()
            .putInt(KEY_RESET_SERIAL, resetSerial + 1)
            .remove(KEY_RUNTIME_WORKOUT_ID)
            .remove(KEY_RUNTIME_SEGMENT_INDEX)
            .remove(KEY_RUNTIME_REMAINING_MS)
            .remove(KEY_RUNTIME_COMPLETED_MS)
            .remove(KEY_RUNTIME_FINISHED)
            .apply()
    }

    fun skipRequestedSegment() {
        prefs.edit().putInt(KEY_SKIP_SERIAL, skipSerial + 1).apply()
    }

    val skipSerial: Int get() = prefs.getInt(KEY_SKIP_SERIAL, 0)

    fun saveRuntime(workoutId: String, state: EngineState) {
        prefs.edit()
            .putString(KEY_RUNTIME_WORKOUT_ID, workoutId)
            .putInt(KEY_RUNTIME_SEGMENT_INDEX, state.segmentIndex)
            .putLong(KEY_RUNTIME_REMAINING_MS, state.remainingMs)
            .putLong(KEY_RUNTIME_COMPLETED_MS, state.totalCompletedMs)
            .putBoolean(KEY_RUNTIME_FINISHED, state.finished)
            .apply()
    }

    fun loadRuntime(workoutId: String): RestoredRuntime? {
        if (prefs.getString(KEY_RUNTIME_WORKOUT_ID, null) != workoutId) return null
        if (!prefs.contains(KEY_RUNTIME_SEGMENT_INDEX) || !prefs.contains(KEY_RUNTIME_REMAINING_MS)) return null
        return RestoredRuntime(
            segmentIndex = prefs.getInt(KEY_RUNTIME_SEGMENT_INDEX, 0),
            remainingMs = prefs.getLong(KEY_RUNTIME_REMAINING_MS, 0L),
            completedMs = prefs.getLong(KEY_RUNTIME_COMPLETED_MS, 0L),
            finished = prefs.getBoolean(KEY_RUNTIME_FINISHED, false)
        )
    }

    fun markCompleted() {
        val count = prefs.getInt(KEY_COMPLETED_COUNT, 0)
        prefs.edit()
            .putInt(KEY_COMPLETED_COUNT, count + 1)
            .putLong(KEY_LAST_COMPLETED_AT, System.currentTimeMillis())
            .apply()
    }

    val completedCount: Int get() = prefs.getInt(KEY_COMPLETED_COUNT, 0)
    val lastCompletedAt: Long get() = prefs.getLong(KEY_LAST_COMPLETED_AT, 0L)

    companion object {
        private const val KEY_WORKOUT_ID = "selected_workout_id"
        private const val KEY_USE_SYSTEM_FTP = "use_system_ftp"
        private const val KEY_MANUAL_FTP = "manual_ftp"
        private const val KEY_RESET_SERIAL = "reset_serial"
        private const val KEY_SKIP_SERIAL = "skip_serial"
        private const val KEY_RUNTIME_WORKOUT_ID = "runtime_workout_id"
        private const val KEY_RUNTIME_SEGMENT_INDEX = "runtime_segment_index"
        private const val KEY_RUNTIME_REMAINING_MS = "runtime_remaining_ms"
        private const val KEY_RUNTIME_COMPLETED_MS = "runtime_completed_ms"
        private const val KEY_RUNTIME_FINISHED = "runtime_finished"
        private const val KEY_COMPLETED_COUNT = "completed_count"
        private const val KEY_LAST_COMPLETED_AT = "last_completed_at"
    }
}

data class RestoredRuntime(
    val segmentIndex: Int,
    val remainingMs: Long,
    val completedMs: Long,
    val finished: Boolean
)
