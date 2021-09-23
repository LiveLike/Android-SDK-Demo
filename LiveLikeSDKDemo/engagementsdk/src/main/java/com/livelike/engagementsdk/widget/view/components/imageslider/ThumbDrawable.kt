package com.livelike.engagementsdk.widget.view.components.imageslider

import android.graphics.Canvas
import android.graphics.Rect
import kotlin.math.pow

internal class ThumbDrawable(
    private val drawableList: List<ScaleDrawable>,
    val initialMagnitude: Float = .5f
) : GenericDrawableCallback() {

    private lateinit var drawable: ScaleDrawable

    var progress = initialMagnitude
        set(value) {
            field = value
            if (drawableList.isNotEmpty()) {
                drawable = getDrawable(progress, drawableList.size)
                drawable.scale = getScale(progress)
            }
        }

    init {
        progress = initialMagnitude
    }

    private fun getScale(progress: Float): Float {
        return if (drawableList.size > 2) {
            if (drawableList.size == 4) {
                2 * (progress - .5f).pow(2f) + 1 // y = 2(x-.5)^2 + 1 is the curve equation for 4
            } else {
                if (progress < .5) {
                    1.5f - progress
                } else {
                    progress + .5f
                }
            }
        } else {
            1 + progress / 2
        }
    }

    private fun getDrawable(progress: Float, size: Int): ScaleDrawable {
        return try {
            drawableList[(progress * size).toInt()]
        } catch (ex: IndexOutOfBoundsException) {
            drawableList[size - 1]
        }
    }

    override fun draw(canvas: Canvas) {
        drawable.draw(canvas)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        drawable.bounds = bounds
    }

    override fun getIntrinsicHeight(): Int = drawable.intrinsicHeight

    override fun getIntrinsicWidth(): Int = drawable.intrinsicWidth
}
