package com.livelike.engagementsdk.widget.view.components.imageslider

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * Inspired by android.graphics.drawable.ScaleDrawable, Backported to API level 16
 * */

internal class ScaleDrawable(private val bitmap: Bitmap, scale: Float = 1f) : Drawable() {

    var scale = scale
        set(value) {
            field = value
            val scaleSize = (bitmap.width * scale).toInt()
            scaledBitmap?.recycle()
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaleSize, scaleSize, false)
        }
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var scaledBitmap: Bitmap? = null

    init {
        //        paint.shader = BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT)
        setBounds(0, 0, bitmap.width, bitmap.height)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun draw(canvas: Canvas) {
        rect.set(bounds)
        scaledBitmap?.let { canvas.drawBitmap(it, bounds.left.toFloat(), bounds.top.toFloat(), paint) }
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicHeight(): Int = scaledBitmap?.height ?: 0

    override fun getIntrinsicWidth(): Int = scaledBitmap?.width ?: 0

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }
}
