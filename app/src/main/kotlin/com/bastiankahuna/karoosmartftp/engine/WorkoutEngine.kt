package com.bastiankahuna.karoosmartftp.engine

import com.bastiankahuna.karoosmartftp.model.RuntimeSnapshot
import com.bastiankahuna.karoosmartftp.model.SegmentType
import com.bastiankahuna.karoosmartftp.model.WorkoutDefinition

class WorkoutEngine(
    private val workout: WorkoutDefinition,
    restoredSegmentIndex: Int = 0,
    restoredRemainingMs: Long? = null,
    restoredTotalCompletedMs: Long = 0L
) {
    private var segmentIndex: Int = restoredSegmentIndex.coerceIn(0, workout.segments.lastIndex)
    private var remainingMs: Long = restoredRemainingMs ?: durationMs(segmentIndex)
    private var totalCompletedMs: Long = restoredTotalCompletedMs.coerceAtLeast(0L)
    private var lastTickMs: Long? = null
    private var finished: Boolean = false
    private var changedSinceLastSnapshot: Boolean = false

    fun reset(nowMs: Long) {
        segmentIndex = 0
        remainingMs = durationMs(0)
        totalCompletedMs = 0L
        lastTickMs = nowMs
        finished = false
        changedSinceLastSnapshot = true
    }

    fun skipToNext(nowMs: Long) {
        advanceSegment()
        lastTickMs = nowMs
        changedSinceLastSnapshot = true
    }

    fun tick(nowMs: Long, ftp: Int, powerWatts: Int?, cadenceRpm: Int?): RuntimeSnapshot {
        val deltaMs = (lastTickMs?.let { nowMs - it } ?: 0L).coerceIn(0L, 2000L)
        lastTickMs = nowMs

        if (!finished && deltaMs > 0L) {
            val segment = workout.segments[segmentIndex]
            val countdownActive = shouldCountdown(segment.type, ftp, segment.targetPct, powerWatts)
            if (countdownActive) {
                consumeTime(deltaMs)
            }
        }

        val snapshot = snapshot(ftp, powerWatts, cadenceRpm)
        changedSinceLastSnapshot = false
        return snapshot
    }

    fun snapshot(ftp: Int, powerWatts: Int?, cadenceRpm: Int?): RuntimeSnapshot {
        val segment = workout.segments[segmentIndex]
        val duration = durationMs(segmentIndex)
        val elapsed = if (finished) duration else (duration - remainingMs).coerceIn(0L, duration)
        val active = !finished && shouldCountdown(segment.type, ftp, segment.targetPct, powerWatts)
        return RuntimeSnapshot(
            workout = workout,
            segmentIndex = segmentIndex,
            segment = segment,
            remainingMs = if (finished) 0L else remainingMs.coerceAtLeast(0L),
            segmentElapsedMs = elapsed,
            segmentDurationMs = duration,
            ftp = ftp,
            currentPower = powerWatts,
            currentCadence = cadenceRpm,
            countdownActive = active,
            finished = finished,
            justChangedSegment = changedSinceLastSnapshot,
            totalCompletedMs = totalCompletedMs
        )
    }

    fun exportState(): EngineState = EngineState(segmentIndex, remainingMs, totalCompletedMs, finished)

    private fun consumeTime(deltaMs: Long) {
        var remainingDelta = deltaMs
        while (remainingDelta > 0L && !finished) {
            if (remainingDelta < remainingMs) {
                remainingMs -= remainingDelta
                totalCompletedMs += remainingDelta
                remainingDelta = 0L
            } else {
                remainingDelta -= remainingMs
                totalCompletedMs += remainingMs
                advanceSegment()
            }
        }
    }

    private fun advanceSegment() {
        if (segmentIndex >= workout.segments.lastIndex) {
            finished = true
            remainingMs = 0L
            changedSinceLastSnapshot = true
        } else {
            segmentIndex += 1
            remainingMs = durationMs(segmentIndex)
            changedSinceLastSnapshot = true
        }
    }

    private fun shouldCountdown(type: SegmentType, ftp: Int, targetPct: Int, powerWatts: Int?): Boolean {
        if (type != SegmentType.WORK) return true
        val minWatts = (ftp * (targetPct - 5) / 100.0).toInt()
        return (powerWatts ?: 0) >= minWatts
    }

    private fun durationMs(index: Int): Long = workout.segments[index].durationSec * 1000L
}

data class EngineState(
    val segmentIndex: Int,
    val remainingMs: Long,
    val totalCompletedMs: Long,
    val finished: Boolean
)
