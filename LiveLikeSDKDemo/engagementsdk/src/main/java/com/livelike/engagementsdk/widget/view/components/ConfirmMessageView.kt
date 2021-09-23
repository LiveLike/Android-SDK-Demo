package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.R
import kotlinx.android.synthetic.main.atom_widget_confirmation_message.view.confirmMessageAnimation

class ConfirmMessageView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    init {
        inflate(context, R.layout.atom_widget_confirmation_message, this)
    }

    var text: String = ""
        set(value) {
            field = value
//            confirmMessageText.text = value
        }

    fun startAnimation(animationPath: String, progress: Float) {
        confirmMessageAnimation.setAnimation(animationPath)
        confirmMessageAnimation.progress = progress
        if (progress != 1f) {
            confirmMessageAnimation.resumeAnimation()
        }
    }

    fun subscribeToAnimationUpdates(onUpdate: (Float) -> Unit) {
        confirmMessageAnimation.addAnimatorUpdateListener { onUpdate(it.animatedFraction) }
    }
}
