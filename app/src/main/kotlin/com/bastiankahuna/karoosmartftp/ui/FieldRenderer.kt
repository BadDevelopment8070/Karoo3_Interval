package com.bastiankahuna.karoosmartftp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.widget.RemoteViews
import com.bastiankahuna.karoosmartftp.R
import com.bastiankahuna.karoosmartftp.engine.Formatters
import com.bastiankahuna.karoosmartftp.model.RuntimeSnapshot
import com.bastiankahuna.karoosmartftp.model.SegmentType
import kotlin.math.max
import kotlin.math.min

object FieldRenderer {
    fun render(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        snapshot: RuntimeSnapshot,
        powerHistory: List<Int>,
        speedMetersPerSecond: Double?,
        heartRateBpm: Int?,
        clockText: String
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.field_smart_ftp)
        val compact = heightPx in 1..260
        val huge = heightPx >= 520

        val timerSize = when {
            huge -> 92f
            compact -> 44f
            else -> 66f
        }
        val titleSize = if (compact) 12f else 16f
        val statusSize = if (compact) 14f else 22f
        val smallSize = if (compact) 11f else 16f

        rv.setTextViewTextSize(R.id.timer, TypedValue.COMPLEX_UNIT_SP, timerSize)
        rv.setTextViewTextSize(R.id.title, TypedValue.COMPLEX_UNIT_SP, titleSize)
        rv.setTextViewTextSize(R.id.status, TypedValue.COMPLEX_UNIT_SP, statusSize)
        rv.setTextViewTextSize(R.id.power_now, TypedValue.COMPLEX_UNIT_SP, smallSize)
        rv.setTextViewTextSize(R.id.power_target, TypedValue.COMPLEX_UNIT_SP, smallSize)
        rv.setTextViewTextSize(R.id.cadence, TypedValue.COMPLEX_UNIT_SP, smallSize)
        rv.setTextViewTextSize(R.id.clock_time, TypedValue.COMPLEX_UNIT_SP, smallSize)
        rv.setTextViewTextSize(R.id.speed_now, TypedValue.COMPLEX_UNIT_SP, smallSize)
        rv.setTextViewTextSize(R.id.heart_rate, TypedValue.COMPLEX_UNIT_SP, smallSize)
        rv.setTextViewTextSize(R.id.footer, TypedValue.COMPLEX_UNIT_SP, smallSize)

        rv.setTextViewText(R.id.title, snapshot.workout.name.uppercase())
        rv.setTextViewText(R.id.timer, Formatters.mmss(snapshot.remainingMs))
        rv.setTextViewText(R.id.status, statusText(snapshot))
        rv.setTextViewText(R.id.clock_time, "TIME $clockText")
        rv.setTextViewText(R.id.speed_now, "SPD ${Formatters.speedKmh(speedMetersPerSecond)} km/h")
        rv.setTextViewText(R.id.heart_rate, "HR ${heartRateBpm?.toString() ?: "---"}")
        rv.setTextViewText(R.id.power_now, "NOW ${snapshot.currentPower?.toString() ?: "---"}W")
        rv.setTextViewText(R.id.power_target, "TGT ${snapshot.targetWatts}W")
        rv.setTextViewText(R.id.cadence, cadenceText(snapshot))
        rv.setTextViewText(R.id.footer, "SEG ${snapshot.segmentIndex + 1}/${snapshot.workout.segments.size}  FTP ${snapshot.ftp}  MIN ${snapshot.minimumWatts}W")
        rv.setProgressBar(R.id.segment_progress, 1000, snapshot.segmentProgressPermille, false)

        val statusColor = when {
            snapshot.finished -> Color.rgb(0, 229, 255)
            snapshot.segment.type == SegmentType.WORK && !snapshot.countdownActive -> Color.rgb(255, 48, 48)
            snapshot.segment.type == SegmentType.WORK -> Color.rgb(0, 255, 170)
            snapshot.segment.type == SegmentType.RECOVERY -> Color.rgb(255, 176, 0)
            else -> Color.rgb(232, 255, 255)
        }
        rv.setTextColor(R.id.status, statusColor)
        rv.setTextColor(R.id.timer, if (snapshot.countdownActive || snapshot.segment.type != SegmentType.WORK) Color.rgb(255, 176, 0) else Color.rgb(255, 48, 48))
        rv.setImageViewBitmap(R.id.graph, graphBitmap(widthPx, if (compact) 80 else 130, snapshot, powerHistory))
        return rv
    }

    private fun statusText(snapshot: RuntimeSnapshot): String {
        if (snapshot.finished) return "WORKOUT COMPLETE"
        val base = when (snapshot.segment.type) {
            SegmentType.WARMUP -> "WARMUP"
            SegmentType.RECOVERY -> "RECOVERY"
            SegmentType.COOLDOWN -> "COOLDOWN"
            SegmentType.WORK -> if (snapshot.countdownActive) "WORK ACTIVE" else "HOLD: POWER < MIN"
        }
        val cad = cadenceWarning(snapshot)
        return if (cad.isNotBlank()) "$base  |  $cad" else base
    }

    private fun cadenceText(snapshot: RuntimeSnapshot): String {
        val cadence = snapshot.currentCadence?.toString() ?: "---"
        val target = if (snapshot.segment.cadenceLow != null && snapshot.segment.cadenceHigh != null) {
            " ${snapshot.segment.cadenceLow}-${snapshot.segment.cadenceHigh}"
        } else ""
        return "CAD $cad$target"
    }

    private fun cadenceWarning(snapshot: RuntimeSnapshot): String {
        val c = snapshot.currentCadence ?: return ""
        val low = snapshot.segment.cadenceLow ?: return ""
        val high = snapshot.segment.cadenceHigh ?: return ""
        return when {
            c < low -> "CAD LOW"
            c > high -> "CAD HIGH"
            else -> "CAD OK"
        }
    }

    private fun graphBitmap(widthPx: Int, heightPx: Int, snapshot: RuntimeSnapshot, history: List<Int>): Bitmap {
        val w = widthPx.coerceAtLeast(320)
        val h = heightPx.coerceAtLeast(70)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(5, 7, 11))

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(90, 0, 229, 255)
        for (i in 0..5) {
            val y = i * h / 5f
            canvas.drawLine(0f, y, w.toFloat(), y, paint)
        }
        for (i in 0..8) {
            val x = i * w / 8f
            canvas.drawLine(x, 0f, x, h.toFloat(), paint)
        }

        val maxY = max(250, max(snapshot.targetWatts + 120, (history.maxOrNull() ?: 0) + 80))
        fun yFor(power: Int): Float = h - (power.toFloat() / maxY.toFloat()).coerceIn(0f, 1f) * h

        val targetY = yFor(snapshot.targetWatts)
        val minY = yFor(snapshot.minimumWatts)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(55, 255, 176, 0)
        canvas.drawRect(0f, targetY, w.toFloat(), minY, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.rgb(255, 176, 0)
        canvas.drawLine(0f, targetY, w.toFloat(), targetY, paint)
        paint.strokeWidth = 2f
        paint.color = Color.rgb(255, 48, 48)
        canvas.drawLine(0f, minY, w.toFloat(), minY, paint)

        if (history.size > 1) {
            paint.strokeWidth = 5f
            paint.color = Color.rgb(0, 229, 255)
            val step = w.toFloat() / (history.size - 1).toFloat()
            for (i in 1 until history.size) {
                canvas.drawLine((i - 1) * step, yFor(history[i - 1]), i * step, yFor(history[i]), paint)
            }
        }

        val progress = snapshot.segmentProgressPermille / 1000f
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(95, 0, 255, 170)
        canvas.drawRoundRect(RectF(0f, h - 10f, w * progress, h.toFloat()), 5f, 5f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(232, 255, 255)
        paint.textSize = min(28f, h * 0.22f)
        paint.typeface = android.graphics.Typeface.MONOSPACE
        canvas.drawText("TGT ${snapshot.targetWatts}W", 10f, min(h - 15f, 30f), paint)
        paint.color = if (snapshot.countdownActive || snapshot.segment.type != SegmentType.WORK) Color.rgb(0, 255, 170) else Color.rgb(255, 48, 48)
        canvas.drawText(if (snapshot.segment.type == SegmentType.WORK) "GATE ${snapshot.minimumWatts}W" else "TIME RUN", w * 0.55f, min(h - 15f, 30f), paint)
        return bmp
    }
}
