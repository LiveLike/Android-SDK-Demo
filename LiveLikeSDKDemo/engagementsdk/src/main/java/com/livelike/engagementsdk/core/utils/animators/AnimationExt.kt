package com.livelike.engagementsdk.core.utils.animators

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.TimeInterpolator
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.RotateAnimation

/**
 * All builders related to building view and property animations
 * Refer for here more animators :
 * https://developer.android.com/guide/topics/graphics/prop-animation, https://www.programcreek.com/java-api-examples/index.php?api=android.animation.PropertyValuesHolder
 */

@Suppress("UNUSED_PARAMETER")
internal fun View.buildScaleAnimator(
    fromScale: Float,
    toScale: Float,
    duration: Long,
    interpolator: TimeInterpolator = BounceEaseOutInterpolator()
): ObjectAnimator {

    val kf0 = Keyframe.ofFloat(0f, fromScale)
    val kf1 = Keyframe.ofFloat(1f, toScale)
    val scaleX = PropertyValuesHolder.ofKeyframe("scaleX", kf0, kf1)
    val scaleY = PropertyValuesHolder.ofKeyframe("scaleY", kf0, kf1)
    val scaleAnimation =
        ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY)
    scaleAnimation.interpolator = BounceEaseOutInterpolator()
//     Debug later why BounceEaseOut shared by shu not workin for now using BounceEaseOutInterpolator
//                scaleAnimation.setEvaluator(BounceEaseOut(duration.toFloat()))
    scaleAnimation.duration = duration
    return scaleAnimation
}

internal fun View.buildRotationAnimator(
    duration: Long,
    interpolator: TimeInterpolator = BounceEaseOutInterpolator()
): ObjectAnimator {

    return ObjectAnimator.ofFloat(this, "rotation", 0f, 360f).apply {
        this.duration = duration
        this.interpolator = interpolator
    }
}

internal fun View.buildTranslateYAnimator(
    duration: Long,
    fromY: Float? = null,
    toY: Float,
    interpolator: TimeInterpolator
): ObjectAnimator {
    return ObjectAnimator.ofFloat(this, "translationY", fromY ?: translationY, toY).apply {
        this.duration = duration
        this.interpolator = interpolator
    }
}

internal fun buildRotationAnimation(
    duration: Long,
    interpolator: Interpolator = BounceEaseOutInterpolator()
): RotateAnimation {
    return RotateAnimation(0f, 360f).apply {
        this.interpolator = interpolator
        this.duration = duration
        fillAfter = false
        repeatCount = 1
    }
}
