package com.livelike.engagementsdk.widget.view.components

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import kotlinx.android.synthetic.main.atom_widget_egg_timer_and_close_button.view.closeButton
import kotlinx.android.synthetic.main.atom_widget_egg_timer_and_close_button.view.eggTimer

class EggTimerCloseButtonView(context: Context, attr: AttributeSet? = null) :
    ConstraintLayout(context, attr) {

    private var dismiss: ((action: DismissAction) -> Unit)? = null

    init {
        View.inflate(context, R.layout.atom_widget_egg_timer_and_close_button, this)
        closeButton.setOnClickListener {
            dismiss?.invoke(DismissAction.TAP_X)
        }
    }

    fun startAnimationFrom(
        progress: Float,
        duration: Float,
        onUpdate: (Float) -> Unit,
        dismissAction: (action: DismissAction) -> Unit,
        showDismissButton: Boolean = true
    ) {
        showEggTimer()
        eggTimer.speed = ANIMATION_BASE_TIME / duration

        // commenting this part, as this is not working properly since timer is getting reset (we haven't called playAnimation(),
        // instead we are calling pauseAnimation)

        // eggTimer.resumeAnimation()

        eggTimer.playAnimation()
        eggTimer.pauseAnimation()
        eggTimer.progress = progress
        eggTimer.resumeAnimation()

        showEggTimer()
        eggTimer.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                eggTimer.removeAllUpdateListeners()
                eggTimer.removeAllAnimatorListeners()
                if (showDismissButton) {
                    showCloseButton(dismissAction)
                }
            }
        })
        eggTimer.addAnimatorUpdateListener {
            if (it.animatedFraction < 1) {
                showEggTimer()
                onUpdate(it.animatedFraction)
            }
        }
    }

    fun showCloseButton(dismissAction: (action: DismissAction) -> Unit) {
        dismiss = dismissAction
        closeButton.visibility = View.VISIBLE
        eggTimer.visibility = View.GONE
    }

    private fun showEggTimer() {
        eggTimer.visibility = View.VISIBLE
        closeButton.visibility = View.GONE
    }

    companion object {
        private const val ANIMATION_BASE_TIME =
            5000f // This value need to be updated if the animation is changed to a different one
    }
}
