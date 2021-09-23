package com.livelike.engagementsdk.widget.view.components.imageslider

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ProgressBar
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.widget.ImageSliderTheme
import com.livelike.engagementsdk.widget.view.getColorCompat
import kotlin.math.roundToInt

/**
 * Inspired by android.widget.AbsSeekBar and https://github.com/bernaferrari/EmojiSlider
 * This widget supports only touch motion.
 */

const val DEFAULT_WIDTH_DP: Int = 263
const val DEFAULT_HEIGHT_DP: Int = 54

internal class ImageSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val desiredWidth: Int
    private val desiredHeight: Int

    // these will be used on onTouch
    private var mScaledTouchSlop = 0
    private var mIsDragging = false
    private val mThumbOffset: Int
    private var mTouchDownX = 0f

    var imageSliderTheme: ImageSliderTheme? = null
        set(value) {
            field = value
            val bgColor = AndroidResource.getColorFromString(value?.body?.background?.color)
            bgColor?.let {
                colorTrack = bgColor
            }
        }

    /**
     * Should the slider ignore touches outside of the thumb?
     * This increases the target area, but might not be good when user is scrolling.
     */
    private var registerTouchOnTrack = true

    /**
     * If false, user won't be able to move the slider.
     */
    var isUserSeekable = true

    /**
     * Initial position of progress in range form `0.0` to `1.0`.
     */
    var progress: Float = INITIAL_POSITION
        set(value) {
            field = value.limitToRange()
            trackDrawable.percentProgress = field
            invalidate()
        }

    /**
     * The track color - default is light-grey.
     */
    var colorTrack: Int
        get() = trackDrawable.trackColor.color
        set(value) {
            trackDrawable.trackColor.color = value
        }

    /**
     * The track progress color for the left side of the slider - default is purple.
     */
    var colorStart: Int
        get() = trackDrawable.colorStart
        set(value) {
            trackDrawable.colorStart = value
        }

    /**
     * The track progress color for the right side of the slider - default is red.
     */
    var colorEnd: Int
        get() = trackDrawable.colorEnd
        set(value) {
            trackDrawable.colorEnd = value
        }

    private val resultGradientCenterColor: Int
    private val resultGradientEndColor: Int

    // ////////////////////////////////////////
    // Drawables
    // ////////////////////////////////////////

    /**
     * Drawable which will contain the emoji already converted into a drawableList.
     */
    var thumbDrawable: Drawable? = null
        set(value) {
            field = value
            thumbDrawable?.callback = this
            visibility = VISIBLE
        }

    /**
     * Drawable which will contain the track: both the background with help from [colorTrack]
     * and the progress by mixing together [colorStart] and [colorEnd]
     */
    private val trackDrawable: TrackDrawable = TrackDrawable()

    var resultDrawable: ResultDrawable? = null

    var averageProgress: Float? = null
        set(value) {
            field = value
            resultDrawable =
                ResultDrawable(context, resultGradientCenterColor, resultGradientEndColor)
            resultDrawable?.bounds = trackDrawable.bounds
            resultDrawable?.totalHeight = trackDrawable.totalHeight
            resultDrawable?.trackHeight = trackDrawable.trackHeight
            resultDrawable?.callback = this
            resultDrawable?.mAverageProgress = value
            resultDrawable?.startLottieAnimation(this)
            resultDrawable?.startGradientAnimation(this)
        }

    /**
     * Current position tracker. Receive current position, in range from `0.0f` to `1.0f`.
     */
    var positionListener: ((Float) -> Unit)? = null

    init {

        val density = context.resources.displayMetrics.density

        desiredWidth = (DEFAULT_WIDTH_DP * density).toInt()
        desiredHeight =
            (density * DEFAULT_HEIGHT_DP).roundToInt()
        mThumbOffset = desiredHeight / 2

        this.trackDrawable.callback = this

        trackDrawable.totalHeight = desiredHeight
        trackDrawable.setTrackHeight(AndroidResource.dpToPx(16).toFloat())
        trackDrawable.invalidateSelf()

        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.ImageSlider)

            try {
                progress = INITIAL_POSITION

                colorStart = array.getProgressGradientStart()
                colorEnd = array.getProgressGradientEnd()
                colorTrack = array.getSliderTrackColor()

                resultGradientCenterColor = array.getColor(
                    R.styleable.ImageSlider_bar_result_color_center,
                    context.getColorCompat(R.color.livelike_image_slider_widget_result_center_color)
                )
                resultGradientEndColor = array.getColor(
                    R.styleable.ImageSlider_bar_result_color_end,
                    context.getColorCompat(R.color.livelike_image_slider_widget_result_end_color)
                )

                registerTouchOnTrack = array.getThumbAllowScrollAnywhere()
                isUserSeekable = array.getIsTouchDisabled()

                invalidateAll()
            } finally {
                array.recycle()
            }
        } else {
            colorStart = context.getColorCompat(R.color.livelike_image_slider_gradient_start)
            colorEnd = context.getColorCompat(R.color.livelike_image_slider_gradient_end)
            colorTrack = context.getColorCompat(R.color.livelike_image_slider_bg)
            resultGradientCenterColor =
                context.getColorCompat(R.color.livelike_image_slider_widget_result_center_color)
            resultGradientEndColor =
                context.getColorCompat(R.color.livelike_image_slider_widget_result_end_color)
        }

        mScaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0)
        val h = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        this.trackDrawable.setBounds(
            0 + Math.max(paddingLeft, mThumbOffset),
            h / 2 - trackDrawable.intrinsicHeight / 2,
            w - Math.max(paddingRight, mThumbOffset),
            h / 2 + trackDrawable.intrinsicHeight / 2
        )
    }

    override fun invalidateDrawable(drawable: Drawable) {
        super.invalidateDrawable(drawable)
        invalidate()
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who is ResultDrawable || super.verifyDrawable(who)
    }

    /**
     * Invalidate all drawables with a hammer. There are so many things happening on screen, this solves
     * any invalidate problem brutally.
     */
    fun invalidateAll() {
        trackDrawable.invalidateSelf()
        thumbDrawable?.invalidateSelf()
        invalidate()
    }

    private fun TypedArray.getProgressGradientStart(): Int {
        return this.getColor(
            R.styleable.ImageSlider_bar_progress_color_start,
            context.getColorCompat(R.color.livelike_image_slider_gradient_start)
        )
    }

    private fun TypedArray.getProgressGradientEnd(): Int {
        return this.getColor(
            R.styleable.ImageSlider_bar_progress_color_end,
            context.getColorCompat(R.color.livelike_image_slider_gradient_end)
        )
    }

    private fun TypedArray.getSliderTrackColor(): Int {
        return this.getColor(
            R.styleable.ImageSlider_bar_track_color,
            context.getColorCompat(R.color.livelike_image_slider_bg)
        )
    }

    private fun TypedArray.getProgress(): Float =
        this.getFloat(R.styleable.ImageSlider_progress_value, progress).limitToRange()

    private fun TypedArray.getThumbAllowScrollAnywhere(): Boolean =
        this.getBoolean(
            R.styleable.ImageSlider_register_touches_outside_thumb, registerTouchOnTrack
        )

    private fun TypedArray.getIsTouchDisabled(): Boolean =
        this.getBoolean(R.styleable.ImageSlider_is_touch_disabled, isUserSeekable)

    private fun Float.limitToRange() = this.coerceAtMost(1f).coerceAtLeast(0f)

    private fun Rect.containsXY(motionEvent: MotionEvent): Boolean =
        this.contains(motionEvent.x.toInt(), motionEvent.y.toInt())

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        trackDrawable.draw(canvas)
        resultDrawable?.draw(canvas)
        drawThumb(canvas)
        resultDrawable?.let {
            canvas.translate(
                (
                    (
                        (
                            averageProgress
                                ?: 0f
                            ) * trackDrawable.bounds.width()
                        ) + trackDrawable.bounds.left
                    ),
                0f
            )
            it.mLottieDrawable.draw(canvas)
        }
    }

    private fun drawThumb(canvas: Canvas) {

        val widthPosition = progress * trackDrawable.bounds.width()

        canvas.save()
        canvas.translate(trackDrawable.bounds.left.toFloat(), trackDrawable.bounds.top.toFloat())

        if (thumbDrawable is ThumbDrawable)
            (thumbDrawable as ThumbDrawable).progress = progress
        thumbDrawable?.updateDrawableBounds(widthPosition.roundToInt())
        thumbDrawable?.draw(canvas)
        canvas.restore()
    }

    private fun Drawable.updateDrawableBounds(widthPosition: Int) {

        val customIntrinsicWidth = this.intrinsicWidth / 2
        val customIntrinsicHeight = this.intrinsicHeight / 2
        val heightPosition = trackDrawable.bounds.height() / 2

        this.setBounds(
            widthPosition - customIntrinsicWidth,
            heightPosition - customIntrinsicHeight,
            widthPosition + customIntrinsicWidth,
            heightPosition + customIntrinsicHeight
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    /**
     * We do not require click event so perform click is not actioned in touch event
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isUserSeekable || !isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isScrollContainer) {
                    mTouchDownX = event.x
                } else {
                    startDrag(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mIsDragging) {
                    trackTouchEvent(event)
                } else {
                    if (Math.abs(event.x - mTouchDownX) > mScaledTouchSlop) {
                        startDrag(event)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mIsDragging) {
                    trackTouchEvent(event)
                    mIsDragging = false
                    isPressed = false
                } else {
                    trackTouchEvent(event)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (mIsDragging) {
                    mIsDragging = false
                    isPressed = false
                }
                // see https://developer.android.com/reference/android/view/ViewGroup.html#onInterceptTouchEvent%28android.view.MotionEvent%29
            }
        }

        return true
    }

    private fun trackTouchEvent(event: MotionEvent) {
        val x = event.x.toInt() - trackDrawable.bounds.left
        progress = x / trackDrawable.bounds.width().toFloat()
        positionListener?.invoke(progress)
    }

    private fun startDrag(event: MotionEvent) {

        val x = event.x.toInt() - trackDrawable.bounds.left
        val y = event.y.toInt() - trackDrawable.bounds.top

        if (thumbDrawable?.bounds?.contains(x, y) == true &&
            !(registerTouchOnTrack && trackDrawable.bounds.containsXY(event))
        ) return

        setViewPressed(true)
        mIsDragging = true
        attemptClaimDrag()
    }

    /**
     * Sets the pressed state for this view.
     *
     * @see .isClickable
     * @see .setClickable
     * @param pressed Pass true to set the View's internal state to "pressed", or false to reverts
     * the View's internal state from a previously set "pressed" state.
     */
    private fun setViewPressed(pressed: Boolean) {
        dispatchSetPressed(pressed)
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private fun attemptClaimDrag() {
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onSaveInstanceState(): Parcelable? {
        // Force our ancestor class to save its state
        val superState = super.onSaveInstanceState()
        return superState?.let { SavedState(it).apply { progress = this@ImageSlider.progress } }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        progress = ss.progress
    }

    internal class SavedState : BaseSavedState {
        var progress: Float = 0f

        /**
         * Constructor called from [ProgressBar.onSaveInstanceState]
         */
        constructor(superState: Parcelable) : super(superState) {}

        /**
         * Constructor called from [.CREATOR]
         */
        private constructor(`in`: Parcel) : super(`in`) {
            progress = `in`.readFloat()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(progress)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        const val INITIAL_POSITION = 0.5f
    }
}
