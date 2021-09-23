package com.livelike.engagementsdk.core.utils.animators

import android.view.animation.Interpolator

/**
 * Reference : https://github.com/MasayukiSuda/EasingInterpolator
 */
class BounceEaseOutInterpolator : Interpolator {

    override fun getInterpolation(p0: Float): Float {
        var elapsedTimeRate = p0
        return if (elapsedTimeRate < 1 / 2.75) {
            (7.5625 * elapsedTimeRate.toDouble() * elapsedTimeRate.toDouble()).toFloat()
        } else if (elapsedTimeRate < 2 / 2.75) {
            elapsedTimeRate = (elapsedTimeRate - (1.5 / 2.75)).toFloat()
            (7.5625 * elapsedTimeRate.toDouble() * elapsedTimeRate.toDouble() + 0.75).toFloat()
        } else if (elapsedTimeRate < 2.5 / 2.75) {
            elapsedTimeRate = (elapsedTimeRate - (2.25 / 2.75)).toFloat()
            (7.5625 * elapsedTimeRate.toDouble() * elapsedTimeRate.toDouble() + 0.9375).toFloat()
        } else {
            elapsedTimeRate = (elapsedTimeRate - (2.625 / 2.75)).toFloat()
            (7.5625 * elapsedTimeRate.toDouble() * elapsedTimeRate.toDouble() + 0.984375).toFloat()
        }
    }
}
