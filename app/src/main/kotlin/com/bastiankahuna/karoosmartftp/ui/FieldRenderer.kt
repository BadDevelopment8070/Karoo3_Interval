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

        // Background
        canvas.drawColor(Color.rgb(5, 7, 11))

        // Layout: 8 Felder (2 oben, 4 in der Mitte, 2 unten)
        // Oben: HR | SPEED
        // Mitte: POWER(center+ring) | FLOATER
        // Unten: PHASE | CAD

        val padding = 4f
        val topHeight = heightPx * 0.15f
        val middleHeight = heightPx * 0.60f
        val bottomHeight = heightPx * 0.25f

        // Top row (HR, SPEED)
        drawFieldBox(canvas, paint, 0f, 0f, (widthPx / 2).toFloat(), topHeight, "HR", "${heartRateBpm?.toString() ?: "---"}", isHighlight = false)
        drawFieldBox(canvas, paint, (widthPx / 2).toFloat(), 0f, widthPx.toFloat(), topHeight, "SPEED", "${Formatters.speedKmh(speedMetersPerSecond)} km/h", isHighlight = false)

        // Middle section - Power and Floater
        val powerWidth = (widthPx * 0.6f)
        val floaterWidth = (widthPx * 0.4f)
        
        drawPowerField(canvas, paint, 0f, topHeight, powerWidth, middleHeight, snapshot)
        drawFloaterField(canvas, paint, powerWidth, topHeight, widthPx.toFloat(), middleHeight, snapshot)

        // Bottom row (Phase, Cadence)
        drawFieldBox(canvas, paint, 0f, topHeight + middleHeight, (widthPx / 2).toFloat(), bottomHeight, "PHASE", phaseText(snapshot), isHighlight = false)
        drawFieldBox(canvas, paint, (widthPx / 2).toFloat(), topHeight + middleHeight, widthPx.toFloat(), bottomHeight, "CADENCE", "${snapshot.currentCadence?.toString() ?: "---"}", isHighlight = false)

        return bmp
    }

    private fun drawFieldBox(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        isHighlight: Boolean
    ) {
        val borderColor = Color.rgb(0, 229, 255)
        val textColor = Color.rgb(232, 255, 255)

        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = borderColor
        canvas.drawRect(x + 2, y + 2, x + width - 2, y + height - 2, paint)

        // Label
        paint.style = Paint.Style.FILL
        paint.textSize = height * 0.25f
        paint.color = Color.rgb(127, 251, 255)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, x + width / 2, y + height * 0.35f, paint)

        // Value
        paint.textSize = height * 0.45f
        paint.color = textColor
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(value, x + width / 2, y + height * 0.75f, paint)
    }

    private fun drawPowerField(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        snapshot: RuntimeSnapshot
    ) {
        val borderColor = Color.rgb(0, 229, 255)

        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = borderColor
        canvas.drawRect(x + 2, y + 2, x + width - 2, y + height - 2, paint)

        val centerX = x + width / 2
        val centerY = y + height / 2
        val ringRadius = min(width, height) * 0.35f

        // Draw ring timer (progress around circle)
        val segmentProgress = snapshot.segmentProgressPermille / 1000f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = Color.rgb(255, 176, 0)
        val ringArc = RectF(centerX - ringRadius, centerY - ringRadius, centerX + ringRadius, centerY + ringRadius)
        canvas.drawArc(ringArc, -90f, segmentProgress * 360f, false, paint)

        // Ring border
        paint.color = Color.rgb(127, 251, 255)
        paint.strokeWidth = 2f
        canvas.drawCircle(centerX, centerY, ringRadius, paint)

        // Draw indicator (triangle or dash based on power difference)
        val current = snapshot.currentPower ?: 0
        val target = snapshot.targetWatts
        val diff = current - target

        val indicatorColor = when {
            diff > 50 -> Color.rgb(255, 48, 48) // Red triangle up
            diff < -50 -> Color.rgb(0, 100, 255) // Blue triangle down
            else -> Color.rgb(0, 229, 0) // Green dash
        }

        paint.style = Paint.Style.FILL
        paint.color = indicatorColor

        when {
            diff > 50 -> drawTriangleUp(canvas, paint, centerX, centerY - ringRadius - 15f, 20f)
            diff < -50 -> drawTriangleDown(canvas, paint, centerX, centerY + ringRadius + 15f, 20f)
            else -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawLine(centerX - 15f, centerY + ringRadius + 15f, centerX + 15f, centerY + ringRadius + 15f, paint)
            }
        }

        // Target watts in center
        paint.style = Paint.Style.FILL
        paint.textSize = height * 0.25f
        paint.color = Color.rgb(255, 176, 0)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("${snapshot.targetWatts}W", centerX, centerY + 8f, paint)

        // Current watts below
        paint.textSize = height * 0.20f
        paint.color = Color.rgb(0, 229, 255)
        canvas.drawText("Now: ${current}W", centerX, centerY + 35f, paint)
    }

    private fun drawTriangleUp(canvas: Canvas, paint: Paint, centerX: Float, centerY: Float, size: Float) {
        val path = android.graphics.Path()
        path.moveTo(centerX, centerY - size)
        path.lineTo(centerX - size, centerY + size)
        path.lineTo(centerX + size, centerY + size)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawTriangleDown(canvas: Canvas, paint: Paint, centerX: Float, centerY: Float, size: Float) {
        val path = android.graphics.Path()
        path.moveTo(centerX, centerY + size)
        path.lineTo(centerX - size, centerY - size)
        path.lineTo(centerX + size, centerY - size)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawFloaterField(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        snapshot: RuntimeSnapshot
    ) {
        val borderColor = Color.rgb(0, 229, 255)

        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = borderColor
        canvas.drawRect(x + 2, y + 2, x + width - 2, y + height - 2, paint)

        val centerX = x + width / 2
        val centerY = y + height / 2

        // Floater info
        paint.style = Paint.Style.FILL
        paint.textSize = height * 0.25f
        paint.color = Color.rgb(127, 251, 255)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("FLOATER", centerX, centerY - height * 0.15f, paint)

        paint.textSize = height * 0.30f
        paint.color = Color.rgb(232, 255, 255)
        canvas.drawText("Min: ${snapshot.minimumWatts}W", centerX, centerY + height * 0.10f, paint)

        paint.textSize = height * 0.25f
        paint.color = Color.rgb(0, 229, 255)
        canvas.drawText("FTP: ${snapshot.ftp}", centerX, centerY + height * 0.35f, paint)

        paint.textSize = height * 0.20f
        paint.color = Color.rgb(200, 200, 200)
        canvas.drawText("Seg ${snapshot.segmentIndex + 1}/${snapshot.workout.segments.size}", centerX, centerY + height * 0.55f, paint)
    }

    private fun phaseText(snapshot: RuntimeSnapshot): String {
        return when (snapshot.segment.type) {
            SegmentType.WARMUP -> "WARMUP"
            SegmentType.WORK -> if (snapshot.countdownActive) "WORK" else "HOLD"
            SegmentType.RECOVERY -> "RECOVERY"
            SegmentType.COOLDOWN -> "COOLDOWN"
        }
    }
}
