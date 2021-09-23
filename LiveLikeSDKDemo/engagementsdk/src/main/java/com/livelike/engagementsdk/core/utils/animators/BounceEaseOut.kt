package com.livelike.engagementsdk.core.utils.animators

@Suppress("unused", "CascadeIf", "NAME_SHADOWING")
class BounceEaseOut(duration: Float) : BaseEasingMethod(duration) {

    override fun calculate(t: Float, b: Float, c: Float, d: Float): Float? {
        var t = t
        return if ((t / d) < 1 / 2.75f) {
            c * (7.5625f * t * t) + b
        } else if (t < 2 / 2.75f) {
            t -= 1.5f
            c * (7.5625f * (t / 2.75f) * t + .75f) + b
        } else if (t < 2.5 / 2.75) {
            t -= 2.25f
            c * (7.5625f * (t / 2.75f) * t + .9375f) + b
        } else {
            t -= 2.625f
            c * (7.5625f * (t / 2.75f) * t + .984375f) + b
        }
    }
}
