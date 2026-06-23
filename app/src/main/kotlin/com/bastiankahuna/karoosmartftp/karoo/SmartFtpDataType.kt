package com.bastiankahuna.karoosmartftp.karoo

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import com.bastiankahuna.karoosmartftp.R
import com.bastiankahuna.karoosmartftp.SettingsStore
import com.bastiankahuna.karoosmartftp.engine.Formatters
import com.bastiankahuna.karoosmartftp.engine.PowerHistory
import com.bastiankahuna.karoosmartftp.engine.WorkoutEngine
import com.bastiankahuna.karoosmartftp.model.RuntimeSnapshot
import com.bastiankahuna.karoosmartftp.model.SegmentType
import com.bastiankahuna.karoosmartftp.model.WorkoutLibrary
import com.bastiankahuna.karoosmartftp.ui.FieldRenderer

class SmartFtpDataType(extension: String) : DataTypeImpl(extension, KarooSmartFtpExtension.FIELD_TYPE_ID) {
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val appContext = context.applicationContext
        val store = SettingsStore(appContext)
        val handler = Handler(Looper.getMainLooper())
        val karooSystem = KarooSystemService(appContext)
        val history = PowerHistory()

        var systemFtp: Int? = null
        var power3s: Int? = null
        var powerInstant: Int? = null
        var cadence: Int? = null
        var speedMetersPerSecond: Double? = null
        var heartRate: Int? = null
        var engine: WorkoutEngine? = null
        var engineWorkoutId: String? = null
        var lastResetSerial = store.resetSerial
        var lastSkipSerial = store.skipSerial
        var lastAnnouncedSegment = -1
        var completionMarked = false
        val consumerIds = mutableListOf<String>()

        fun makeEngineIfNeeded(now: Long) {
            val selectedId = store.selectedWorkoutId
            val resetChanged = lastResetSerial != store.resetSerial
            if (engine == null || engineWorkoutId != selectedId || resetChanged) {
                val workout = WorkoutLibrary.byId(selectedId)
                val restored = if (resetChanged) null else store.loadRuntime(selectedId)
                engine = if (restored != null && !restored.finished) {
                    WorkoutEngine(workout, restored.segmentIndex, restored.remainingMs, restored.completedMs)
                } else {
                    WorkoutEngine(workout)
                }
                engineWorkoutId = selectedId
                lastResetSerial = store.resetSerial
                lastAnnouncedSegment = -1
                completionMarked = false
                history.clear()
                if (resetChanged) engine?.reset(now)
            }
        }

        fun beepFor(snapshot: RuntimeSnapshot) {
            val tones = when (snapshot.segment.type) {
                SegmentType.WORK -> listOf(
                    PlayBeepPattern.Tone(880, 120),
                    PlayBeepPattern.Tone(null, 60),
                    PlayBeepPattern.Tone(1320, 180)
                )
                SegmentType.RECOVERY -> listOf(PlayBeepPattern.Tone(520, 180))
                SegmentType.WARMUP -> listOf(PlayBeepPattern.Tone(660, 120))
                SegmentType.COOLDOWN -> listOf(PlayBeepPattern.Tone(440, 220))
            }
            karooSystem.dispatch(PlayBeepPattern(tones))
        }

        fun alertFor(snapshot: RuntimeSnapshot) {
            val bg = when (snapshot.segment.type) {
                SegmentType.WORK -> R.color.alert_work_bg
                SegmentType.RECOVERY -> R.color.alert_rest_bg
                SegmentType.WARMUP -> R.color.alert_rest_bg
                SegmentType.COOLDOWN -> R.color.alert_rest_bg
            }
            val detail = buildString {
                append(snapshot.segment.title)
                append("  @").append(snapshot.segment.targetPct).append("% FTP")
                append("  Ziel ").append(snapshot.targetWatts).append("W")
                if (snapshot.segment.isPowerGated) append("  Gate ").append(snapshot.minimumWatts).append("W")
                if (snapshot.segment.cadenceLow != null && snapshot.segment.cadenceHigh != null) {
                    append("  Cad ").append(snapshot.segment.cadenceLow).append("-").append(snapshot.segment.cadenceHigh)
                }
            }
            karooSystem.dispatch(
                InRideAlert(
                    id = "smart_ftp_segment_${snapshot.segmentIndex}",
                    icon = R.drawable.ic_smart_ftp,
                    title = snapshot.phaseLabel,
                    detail = detail,
                    autoDismissMs = 2200L,
                    backgroundColor = bg,
                    textColor = R.color.alert_text
                )
            )
        }

        fun update() {
            val now = System.currentTimeMillis()
            makeEngineIfNeeded(now)

            if (lastSkipSerial != store.skipSerial) {
                engine?.skipToNext(now)
                lastSkipSerial = store.skipSerial
            }

            val ftp = store.effectiveFtp(systemFtp)
            val currentPower = power3s ?: powerInstant
            history.add(currentPower)
            val snapshot = engine!!.tick(now, ftp, currentPower, cadence)
            store.saveRuntime(snapshot.workout.id, engine!!.exportState())

            if (snapshot.justChangedSegment && snapshot.segmentIndex != lastAnnouncedSegment && !snapshot.finished) {
                lastAnnouncedSegment = snapshot.segmentIndex
                beepFor(snapshot)
                alertFor(snapshot)
            }

            if (snapshot.finished && !completionMarked) {
                completionMarked = true
                store.markCompleted()
                karooSystem.dispatch(PlayBeepPattern(listOf(
                    PlayBeepPattern.Tone(880, 100),
                    PlayBeepPattern.Tone(1100, 100),
                    PlayBeepPattern.Tone(1320, 250)
                )))
            }

            emitter.onNext(UpdateGraphicConfig(showHeader = false, formatDataTypeId = DataType.Type.POWER))
            emitter.updateView(
                FieldRenderer.render(
                    context = appContext,
                    widthPx = config.viewSize.first,
                    heightPx = config.viewSize.second,
                    snapshot = snapshot,
                    powerHistory = history.snapshot(),
                    speedMetersPerSecond = speedMetersPerSecond,
                    heartRateBpm = heartRate,
                    clockText = Formatters.clockNow()
                )
            )
            handler.postDelayed(::update, 500L)
        }

        fun streamValue(event: OnStreamState, preferredField: String? = null): Double? {
            val point: DataPoint = (event.state as? StreamState.Streaming)?.dataPoint ?: return null
            if (preferredField != null) {
                point.values[preferredField]?.let { return it }
            }
            return point.values.values.firstOrNull()
        }

        fun onStream(event: OnStreamState, target: String) {
            when (target) {
                "power3s" -> power3s = streamValue(event, DataType.Field.SMOOTHED_3S_AVERAGE_POWER)?.toInt()
                "power" -> powerInstant = streamValue(event, DataType.Field.POWER)?.toInt()
                "cadence" -> cadence = streamValue(event, DataType.Field.CADENCE)?.toInt()
                "speed" -> speedMetersPerSecond = streamValue(event, DataType.Field.SPEED)
                "heartRate" -> heartRate = streamValue(event, DataType.Field.HEART_RATE)?.toInt()
            }
        }

        karooSystem.connect { connected ->
            if (connected == true) {
                consumerIds += karooSystem.addConsumer<UserProfile> { profile ->
                    systemFtp = profile.ftp.takeIf { it > 0 }
                }
                consumerIds += karooSystem.addConsumer<OnStreamState>(OnStreamState.StartStreaming(DataType.Type.SMOOTHED_3S_AVERAGE_POWER)) { event ->
                    onStream(event, "power3s")
                }
                consumerIds += karooSystem.addConsumer<OnStreamState>(OnStreamState.StartStreaming(DataType.Type.POWER)) { event ->
                    onStream(event, "power")
                }
                consumerIds += karooSystem.addConsumer<OnStreamState>(OnStreamState.StartStreaming(DataType.Type.CADENCE)) { event ->
                    onStream(event, "cadence")
                }
                consumerIds += karooSystem.addConsumer<OnStreamState>(OnStreamState.StartStreaming(DataType.Type.SPEED)) { event ->
                    onStream(event, "speed")
                }
                consumerIds += karooSystem.addConsumer<OnStreamState>(OnStreamState.StartStreaming(DataType.Type.HEART_RATE)) { event ->
                    onStream(event, "heartRate")
                }
            }
        }

        emitter.onNext(UpdateGraphicConfig(showHeader = false, formatDataTypeId = DataType.Type.POWER))
        update()

        emitter.setCancellable {
            handler.removeCallbacksAndMessages(null)
            consumerIds.forEach { id -> karooSystem.removeConsumer(id) }
            karooSystem.disconnect()
        }
    }
}
