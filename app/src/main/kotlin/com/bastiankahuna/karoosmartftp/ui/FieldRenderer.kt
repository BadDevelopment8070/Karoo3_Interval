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
    private val COLOR_BORDER = Color.rgb(0, 229, 255)
    private val COLOR_LABEL = Color.rgb(127, 251, 255)
    private val COLOR_VALUE = Color.rgb(255, 176, 0)
    private val COLOR_TEXT = Color.rgb(0, 229, 255)
    private val COLOR_OK = Color.rgb(0, 229, 0)
    private val COLOR_HIGH = Color.rgb(255, 48, 48)
    private val COLOR_LOW = Color.rgb(0, 100, 255)
    private val BG_COLOR = Color.rgb(5, 7, 11)
    private val PADDING = 6f
    private val BORDER_WIDTH = 2f

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
        rv.setImageViewBitmap(R.id.canvas, createDisplayBitmap(widthPx, heightPx, snapshot, powerHistory, speedMetersPerSecond, heartRateBpm))
        return rv
    }


    private fun createDisplayBitmap(
        widthPx: Int,
        heightPx: Int,
        snapshot: RuntimeSnapshot,
        powerHistory: List<Int>,
        speedMetersPerSecond: Double?,
        heartRateBpm: Int?
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(BG_COLOR)

        val gap = 8f
        val topH = heightPx * 0.15f
        val midH = heightPx * 0.6f
        val botH = heightPx * 0.25f

        // Top row
        drawFieldSimple(canvas, paint, PADDING, PADDING, (widthPx - PADDING * 2) / 2 - gap / 2, topH - PADDING * 2, 
            "HR", "${heartRateBpm?.toString() ?: "---"}", "bpm")
        drawFieldSimple(canvas, paint, PADDING + (widthPx - PADDING * 2) / 2 + gap / 2, PADDING, (widthPx - PADDING * 2) / 2 - gap / 2, topH - PADDING * 2,
            "SPD", "${Formatters.speedKmh(speedMetersPerSecond)}", "km/h")

        // Middle Power Ring
        drawPowerRing(canvas, paint, PADDING, topH + PADDING, widthPx - PADDING * 2, midH - PADDING * 2, snapshot)

        // Bottom row
        val botY = topH + midH + PADDING
        drawPhaseField(canvas, paint, PADDING, botY, (widthPx - PADDING * 2) / 2 - gap / 2, botH - PADDING * 2, snapshot)
        drawCadenceField(canvas, paint, PADDING + (widthPx - PADDING * 2) / 2 + gap / 2, botY, (widthPx - PADDING * 2) / 2 - gap / 2, botH - PADDING * 2, snapshot)

        return bmp
    }

    private fun drawFieldSimple(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String,
        value: String,
        unit: String
    ) {
        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = BORDER_WIDTH
        paint.color = COLOR_BORDER
        canvas.drawRect(x, y, x + w, y + h, paint)

        // Label
        paint.style = Paint.Style.FILL
        paint.textSize = h * 0.22f
        paint.textAlign = Paint.Align.CENTER
        paint.color = COLOR_LABEL
        canvas.drawText(label, x + w / 2, y + h * 0.28f, paint)

        // Value
        paint.textSize = h * 0.55f
        paint.color = COLOR_VALUE
        canvas.drawText(value, x + w / 2, y + h * 0.70f, paint)

        // Unit
        paint.textSize = h * 0.20f
        paint.color = COLOR_LABEL
        canvas.drawText(unit, x + w / 2, y + h * 0.88f, paint)
    }

    private fun drawPowerRing(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        snapshot: RuntimeSnapshot
    ) {
        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = BORDER_WIDTH
        paint.color = COLOR_BORDER
        canvas.drawRect(x, y, x + w, y + h, paint)

        val centerX = x + w / 2
        val centerY = y + h / 2
        val ringRadius = min(w, h) * 0.32f

        // Ring background (grid pattern)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.argb(50, 0, 229, 255)
        for (i in 0..3) {
            val angle = (i * 90) - 90f
            val rad = Math.toRadians(angle.toDouble())
            val ex = centerX + ringRadius * kotlin.math.cos(rad).toFloat()
            val ey = centerY + ringRadius * kotlin.math.sin(rad).toFloat()
            canvas.drawLine(centerX, centerY, ex, ey, paint)
        }

        // Ring progress
        val progress = snapshot.segmentProgressPermille / 1000f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.color = COLOR_VALUE
        val arc = RectF(centerX - ringRadius, centerY - ringRadius, centerX + ringRadius, centerY + ringRadius)
        canvas.drawArc(arc, -90f, progress * 360f, false, paint)

        // Ring border
        paint.strokeWidth = 2f
        paint.color = COLOR_BORDER
        canvas.drawCircle(centerX, centerY, ringRadius, paint)

        // Timer top center
        paint.style = Paint.Style.FILL
        paint.textSize = ringRadius * 0.45f
        paint.textAlign = Paint.Align.CENTER
        paint.color = COLOR_TEXT
        val timeStr = Formatters.mmss(snapshot.remainingMs)
        canvas.drawText(timeStr, centerX, centerY - ringRadius - 5f, paint)

        // Target power center
        paint.textSize = ringRadius * 1.0f
        paint.color = COLOR_VALUE
        canvas.drawText("${snapshot.targetWatts}W", centerX, centerY + 10f, paint)

        // Target label
        paint.textSize = ringRadius * 0.35f
        paint.color = COLOR_LABEL
        canvas.drawText("TARGET POWER", centerX, centerY + ringRadius + 25f, paint)

        // Power indicator (right side)
        val current = snapshot.currentPower ?: 0
        val diff = current - snapshot.targetWatts
        val indicatorX = x + w - 30f
        val indicatorY = centerY

        paint.style = Paint.Style.FILL
        when {
            diff > 50 -> drawTriangleUp(canvas, paint, indicatorX, indicatorY - 15f, COLOR_HIGH)
            diff < -50 -> drawTriangleDown(canvas, paint, indicatorX, indicatorY + 15f, COLOR_LOW)
            else -> {
                paint.strokeWidth = 5f
                paint.style = Paint.Style.STROKE
                paint.color = COLOR_OK
                canvas.drawLine(indicatorX - 12f, indicatorY, indicatorX + 12f, indicatorY, paint)
            }
        }

        // Box around indicator
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = BORDER_WIDTH
        paint.color = COLOR_BORDER
        canvas.drawRect(indicatorX - 20f, indicatorY - 30f, indicatorX + 20f, indicatorY + 30f, paint)
    }

    private fun drawTriangleUp(canvas: Canvas, paint: Paint, x: Float, y: Float, color: Int) {
        paint.color = color
        val path = android.graphics.Path()
        path.moveTo(x, y - 15f)
        path.lineTo(x - 15f, y + 15f)
        path.lineTo(x + 15f, y + 15f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawTriangleDown(canvas: Canvas, paint: Paint, x: Float, y: Float, color: Int) {
        paint.color = color
        val path = android.graphics.Path()
        path.moveTo(x, y + 15f)
        path.lineTo(x - 15f, y - 15f)
        path.lineTo(x + 15f, y - 15f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawPhaseField(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        snapshot: RuntimeSnapshot
    ) {
        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = BORDER_WIDTH
        paint.color = COLOR_BORDER
        canvas.drawRect(x, y, x + w, y + h, paint)

        val phase = when (snapshot.segment.type) {
            SegmentType.WARMUP -> "WARMUP"
            SegmentType.WORK -> "WORK"
            SegmentType.RECOVERY -> "RECOVERY"
            SegmentType.COOLDOWN -> "COOLDOWN"
        }

        // Label
        paint.style = Paint.Style.FILL
        paint.textSize = h * 0.20f
        paint.textAlign = Paint.Align.CENTER
        paint.color = COLOR_LABEL
        canvas.drawText("PHASE", x + w / 2, y + h * 0.25f, paint)

        // Phase name
        paint.textSize = h * 0.50f
        paint.color = COLOR_VALUE
        canvas.drawText(phase, x + w / 2, y + h * 0.65f, paint)

        // Segment info
        paint.textSize = h * 0.22f
        paint.color = COLOR_LABEL
        val segInfo = "${snapshot.segmentIndex + 1} / ${snapshot.workout.segments.size}"
        canvas.drawText(segInfo, x + w / 2, y + h * 0.88f, paint)

        // Progress bar at bottom
        val barH = 3f
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(80, 0, 229, 255)
        canvas.drawRect(x + 4f, y + h - 6f, x + w - 4f, y + h - 6f + barH, paint)

        val progress = snapshot.segmentProgressPermille / 1000f
        paint.color = COLOR_VALUE
        canvas.drawRect(x + 4f, y + h - 6f, x + 4f + (w - 8f) * progress, y + h - 6f + barH, paint)
    }

    private fun drawCadenceField(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        snapshot: RuntimeSnapshot
    ) {
        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = BORDER_WIDTH
        paint.color = COLOR_BORDER
        canvas.drawRect(x, y, x + w, y + h, paint)

        val cadence = snapshot.currentCadence?.toString() ?: "---"

        // Label
        paint.style = Paint.Style.FILL
        paint.textSize = h * 0.20f
        paint.textAlign = Paint.Align.CENTER
        paint.color = COLOR_LABEL
        canvas.drawText("CAD", x + w / 2, y + h * 0.25f, paint)

        // Cadence value
        paint.textSize = h * 0.55f
        paint.color = COLOR_VALUE
        canvas.drawText(cadence, x + w / 2, y + h * 0.70f, paint)

        // Unit
        paint.textSize = h * 0.20f
        paint.color = COLOR_LABEL
        canvas.drawText("rpm", x + w / 2, y + h * 0.88f, paint)
    }
}
