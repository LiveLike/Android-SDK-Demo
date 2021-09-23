package com.livelike.engagementsdk.widget.view.components.imageslider

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Animatable
import android.os.SystemClock
import android.view.animation.DecelerateInterpolator
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import kotlin.math.max

internal class ResultDrawable(
    val context: Context,
    private val centerColor: Int,
    private val sideColor: Int
) : GenericDrawableCallback(), Animatable, Runnable {

    private var isDrawResultGradient: Boolean = false
    private var mRunning = false
    private var mStartTime: Long = 0
    private val mDurationMs = 500 // in ms

    val mLottieDrawable: LottieDrawable = LottieDrawable()

    val mInterpolator = DecelerateInterpolator()

    var mAverageProgress: Float? = null
        set(value) {
            field = value
            value?.let { updateShader(bounds, value) }
        }

    private val resultGradient = Paint(1)
    internal var totalHeight: Int = 0
    internal var trackHeight: Float = 0f

    init {
        LottieCompositionFactory.fromAsset(context, "image_slider_result.json")
            .addListener { composition ->
                mLottieDrawable.composition = composition
                mLottieDrawable.repeatCount = 0
            }
        resultGradient.color = sideColor
    }

    fun startLottieAnimation(callback: Callback) {
        mLottieDrawable.callback = callback
        mLottieDrawable.start()
    }

    fun startGradientAnimation(callback: Callback) {
        this.callback = callback
        isDrawResultGradient = true
        start()
    }

    override fun draw(canvas: Canvas) {
        if (isDrawResultGradient) {
            canvas.save()
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            val barRect = RectF()
            if (isRunning) {
                val elapsed = (SystemClock.uptimeMillis() - mStartTime).toFloat()
                val rawProgress = elapsed / mDurationMs
                val progress = mInterpolator.getInterpolation(rawProgress)
                alpha = (progress * 255).toInt()
                barRect.set(
                    0f,
                    bounds.height() / 2f - trackHeight / 2,
                    bounds.width().toFloat() * progress,
                    bounds.height() / 2f + trackHeight / 2
                )
            } else {
                alpha = 255
                barRect.set(
                    0f,
                    bounds.height() / 2f - trackHeight / 2,
                    bounds.width().toFloat(),
                    bounds.height() / 2f + trackHeight / 2
                )
            }
            canvas.drawRoundRect(barRect, trackHeight / 2, trackHeight / 2, resultGradient)
            canvas.restore()
        }
    }

    private fun updateShader(rect: Rect, value: Float) {
        val left = max(value - .3f, 0f)
        val right = max(value + .3f, 0f)
        resultGradient.shader = LinearGradient(
            0.0f,
            0.0f,
            rect.right.toFloat(),
            rect.top.toFloat(),
            intArrayOf(sideColor, sideColor, centerColor, sideColor, sideColor),
            floatArrayOf(0f, left, value, right, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun setAlpha(alpha: Int) {
        this.resultGradient.alpha = alpha
    }

    override fun start() {
        if (isRunning) {
            stop()
        }
        mRunning = true
        mStartTime = SystemClock.uptimeMillis()
        invalidateSelf()
        scheduleSelf(this, mStartTime + FRAME_DELAY)
    }

    override fun stop() {
        unscheduleSelf(this)
        mRunning = false
    }

    override fun run() {
        invalidateSelf()
        val uptimeMillis = SystemClock.uptimeMillis()
        if (uptimeMillis + FRAME_DELAY < mStartTime + mDurationMs) {
            scheduleSelf(this, uptimeMillis + FRAME_DELAY)
        } else {
            mRunning = false
            invalidateSelf()
        }
    }

    override fun isRunning(): Boolean {
        return mRunning
    }

    companion object {
        private const val FRAME_DELAY = (1000 / 60).toLong() // 60 fps
    }
}
