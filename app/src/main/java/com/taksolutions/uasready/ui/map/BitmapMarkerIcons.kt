package com.taksolutions.uasready.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Marker icons drawn once per distinct label and reused. Previously every map recomposition
 * redrew a bitmap for every airport marker.
 */
class BitmapMarkerIcons(private val context: Context) : MarkerIconFactory {
    private val airportCache = HashMap<String, Drawable>()
    private val pin: Drawable by lazy { createSmallRedPinDrawable(context) }

    override fun launchPin(): Drawable = pin

    override fun airportLabel(ident: String, frequencyMhz: String): Drawable =
        airportCache.getOrPut("$ident|$frequencyMhz") { createAirportMarkerDrawable(context, ident, frequencyMhz) }
}

// 50% Decreased Size Red Location Pin
internal fun createSmallRedPinDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val width = (14 * density).toInt() // Reduced 50% from 28dp
    val height = (20 * density).toInt() // Reduced 50% from 40dp
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val radius = width / 2f
    val path = Path().apply {
        arcTo(0f, 0f, width.toFloat(), width.toFloat(), 180f, 180f, true)
        quadTo(width.toFloat(), width * 0.85f, width / 2f, height.toFloat())
        quadTo(0f, width * 0.85f, 0f, radius)
        close()
    }

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(220, 38, 38) // Vivid Red
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(153, 27, 27)
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }

    canvas.drawPath(path, pinPaint)
    canvas.drawPath(path, borderPaint)
    canvas.drawCircle(width / 2f, radius, radius * 0.4f, dotPaint)

    return BitmapDrawable(context.resources, bitmap)
}

// High-Contrast Outdoor-Readable Airport & CTAF Frequency Marker Pill
internal fun createAirportMarkerDrawable(context: Context, ident: String, freqMhz: String): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val labelText = "$ident $freqMhz"

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 9.5f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(56, 189, 248) // Aviation Sky Cyan
        textSize = 9.5f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val iconStr = "✈ "
    val iconWidth = iconPaint.measureText(iconStr)
    val textWidth = textPaint.measureText(labelText)
    val totalContentWidth = iconWidth + textWidth

    val fontMetrics = textPaint.fontMetrics
    val textHeight = fontMetrics.descent - fontMetrics.ascent

    val padH = 6f * density
    val padV = 3f * density
    val width = (totalContentWidth + padH * 2).toInt().coerceAtLeast((36 * density).toInt())
    val height = (textHeight + padV * 2).toInt().coerceAtLeast((18 * density).toInt())

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val rect = android.graphics.RectF(0.6f * density, 0.6f * density, width - 0.6f * density, height - 0.6f * density)
    val cornerRadius = 4f * density

    // Background fill (Deep Slate / Aviation Dark)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(15, 23, 42) // Slate 900
        style = Paint.Style.FILL
    }
    // Border stroke (Vivid Sky Cyan)
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(56, 189, 248) // Sky 400
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }

    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

    // Draw Icon and Text
    val textY = padV - fontMetrics.ascent
    canvas.drawText(iconStr, padH, textY, iconPaint)
    canvas.drawText(labelText, padH + iconWidth, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
