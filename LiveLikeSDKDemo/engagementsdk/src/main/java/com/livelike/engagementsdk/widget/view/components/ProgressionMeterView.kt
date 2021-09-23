package com.livelike.engagementsdk.widget.view.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.animators.buildRotationAnimator
import com.livelike.engagementsdk.core.utils.animators.buildScaleAnimator
import com.livelike.engagementsdk.widget.view.loadImage
import kotlinx.android.synthetic.main.atom_gamification_progression_meter.view.gamification_badge_iv
import kotlinx.android.synthetic.main.atom_gamification_progression_meter.view.new_badge_label
import kotlinx.android.synthetic.main.atom_gamification_progression_meter.view.progression_meter_progress_view
import kotlinx.android.synthetic.main.atom_gamification_progression_meter.view.progression_meter_text
import kotlin.math.min

class ProgressionMeterView(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {

    private var progression: Int = 0
        set(value) {
            field = value
            progression_meter_text.text = "$value/$totalPointsToNextbadge"
        }

    private var totalPointsToNextbadge: Int = 0

    init {
        ConstraintLayout.inflate(
            context,
            com.livelike.engagementsdk.R.layout.atom_gamification_progression_meter,
            this
        )
        visibility = View.GONE
    }

    fun animatePointsBadgeProgression(
        currentPointsForNextBadge: Int,
        newPoints: Int,
        totalPointsNextBadge: Int,
        badgeIconURL: String
    ) {
        visibility = View.VISIBLE

        gamification_badge_iv.loadImage(badgeIconURL, AndroidResource.dpToPx(30))
        totalPointsToNextbadge = totalPointsNextBadge

        var colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        var filter = ColorMatrixColorFilter(colorMatrix)
        gamification_badge_iv.colorFilter = filter

        val newBadgeEarned = totalPointsToNextbadge <= currentPointsForNextBadge + newPoints
        if (newBadgeEarned) {
            gamification_badge_iv.postDelayed(
                {
                    colorMatrix = ColorMatrix()
                    colorMatrix.setSaturation(1f)
                    filter = ColorMatrixColorFilter(colorMatrix)
                    gamification_badge_iv.colorFilter = filter
                    gamification_badge_iv.buildRotationAnimator(2000).apply {
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                super.onAnimationEnd(animation)
                                new_badge_label.visibility = View.VISIBLE
                                val listener = object : Animator.AnimatorListener {
                                    override fun onAnimationRepeat(animation: Animator?) {
                                    }

                                    override fun onAnimationEnd(animation: Animator?) {
                                        animate().translationY(60f).alpha(0f).setStartDelay(600).start()
                                    }

                                    override fun onAnimationCancel(animation: Animator?) {
                                    }

                                    override fun onAnimationStart(animation: Animator?) {
                                    }
                                }
                                val animator = new_badge_label.buildScaleAnimator(0f, 1f, 300)
                                animator.addListener(listener)
                                animator.start()
                            }
                        })
                    }.start()
                },
                500
            )
        } else {
            new_badge_label.visibility = View.GONE
        }
        ValueAnimator.ofInt(currentPointsForNextBadge, currentPointsForNextBadge + newPoints)
            .apply {
                addUpdateListener {
                    progression = it.animatedValue as Int
                }
                duration = 1000
                start()
            }
        val startPercentage = (currentPointsForNextBadge / totalPointsToNextbadge.toFloat()) * 100
        val endPercentage = min(
            100f,
            ((currentPointsForNextBadge + newPoints) / totalPointsToNextbadge.toFloat()) * 100
        )
        ValueAnimator.ofInt(
            ((startPercentage * AndroidResource.dpToPx(100)) / 100).toInt(),
            (endPercentage.toInt() * AndroidResource.dpToPx(100) / 100).toInt()
        ).apply {
            addUpdateListener {
                val layoutParams = progression_meter_progress_view.layoutParams
                layoutParams.width = it.animatedValue as Int
                progression_meter_progress_view.layoutParams = layoutParams
            }
            duration = 1000
            start()
        }
//        Add transition out using transition choreography api(TransitionSet),
//        Also we can add some kotlin's DSL from here : https://github.com/arunkumar9t2/transition-x
    }
}
