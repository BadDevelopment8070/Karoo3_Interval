package com.bastiankahuna.karoosmartftp.model

enum class SegmentType { WARMUP, WORK, RECOVERY, COOLDOWN }

data class WorkoutSegment(
    val type: SegmentType,
    val title: String,
    val durationSec: Int,
    val targetPct: Int,
    val cadenceLow: Int? = null,
    val cadenceHigh: Int? = null,
    val note: String = ""
) {
    val isPowerGated: Boolean get() = type == SegmentType.WORK
    val isLowCadence: Boolean get() = cadenceLow != null && cadenceHigh != null
}

data class WorkoutDefinition(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val segments: List<WorkoutSegment>
) {
    val totalDurationSec: Int get() = segments.sumOf { it.durationSec }
    val workDurationSec: Int get() = segments.filter { it.type == SegmentType.WORK }.sumOf { it.durationSec }
}

data class RuntimeSnapshot(
    val workout: WorkoutDefinition,
    val segmentIndex: Int,
    val segment: WorkoutSegment,
    val remainingMs: Long,
    val segmentElapsedMs: Long,
    val segmentDurationMs: Long,
    val ftp: Int,
    val currentPower: Int?,
    val currentCadence: Int?,
    val countdownActive: Boolean,
    val finished: Boolean,
    val justChangedSegment: Boolean,
    val totalCompletedMs: Long
) {
    val targetWatts: Int get() = (ftp * segment.targetPct / 100.0).toInt()
    val minimumWatts: Int get() = (ftp * (segment.targetPct - 5) / 100.0).toInt()
    val segmentProgressPermille: Int
        get() = if (segmentDurationMs <= 0) 1000 else ((segmentElapsedMs.coerceAtLeast(0) * 1000) / segmentDurationMs).toInt().coerceIn(0, 1000)

    val phaseLabel: String
        get() = when {
            finished -> "FINISHED"
            segment.type == SegmentType.WARMUP -> "WARMUP"
            segment.type == SegmentType.RECOVERY -> "RECOVERY"
            segment.type == SegmentType.COOLDOWN -> "COOLDOWN"
            !countdownActive -> "HOLD"
            segment.isLowCadence -> "K3 LOW CADENCE"
            else -> "WORK"
        }
}
