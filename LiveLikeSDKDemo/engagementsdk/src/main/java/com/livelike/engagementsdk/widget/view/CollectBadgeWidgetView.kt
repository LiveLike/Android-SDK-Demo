package com.livelike.engagementsdk.widget.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.animators.buildRotationAnimator
import com.livelike.engagementsdk.core.utils.animators.buildScaleAnimator
import com.livelike.engagementsdk.core.utils.animators.buildTranslateYAnimator
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.data.models.Badge
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.CollectBadgeWidgetViewModel
import kotlinx.android.synthetic.main.widget_gamification_collect_badge.view.badge_iv
import kotlinx.android.synthetic.main.widget_gamification_collect_badge.view.badge_name_tv
import kotlinx.android.synthetic.main.widget_gamification_collect_badge.view.collect_badge_box
import kotlinx.android.synthetic.main.widget_gamification_collect_badge.view.collect_badge_button

class CollectBadgeWidgetView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr) {

    private var viewModel: CollectBadgeWidgetViewModel? = null
    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as CollectBadgeWidgetViewModel
            viewModel?.run {
                startInteractionTimeout(5000) {
                    removeAllViews()
                }
                animateView(badge)
            }
        }

    init {
        inflate(context, R.layout.widget_gamification_collect_badge, this)
    }

    private fun animateView(badge: Badge) {
        clipParents(false)
        badge_iv.loadImage(badge.imageFile, AndroidResource.dpToPx(80))
        badge_name_tv.text = badge.name
        badge_iv.buildRotationAnimator(1000).start()
        buildScaleAnimator(0f, 1f, 1000).start()
        collect_badge_button.setOnClickListener {
            viewModel?.let {
                it.analyticsService.trackBadgeCollectedButtonPressed(it.badge.id, it.badge.level)
            }
            choreogaphBadgeCollection()
        }
    }

    private fun choreogaphBadgeCollection() {
        collect_badge_button.animate().alpha(0f).setDuration(500).start()
        collect_badge_box.animate().alpha(0f).setDuration(500).start()
        //            badge_iv.animate().setDuration(500).setStartDelay(500)
        //                .translationY(badge_iv.translationY + collect_badge_box.image_height/2)
        val badgeTranslateDownCenter = badge_iv.buildTranslateYAnimator(
            100,
            badge_iv.translationY,
            badge_iv.translationY + collect_badge_box.height / 2,
            LinearInterpolator()
        )
        val badgeScaleUp = badge_iv.buildScaleAnimator(1f, 1.5f, 1200)

        val badgeTranslateDownBox = badge_iv.buildTranslateYAnimator(
            300,
            badge_iv.translationY + collect_badge_box.height / 2,
            badge_iv.translationY + collect_badge_box.height / 2 + AndroidResource.dpToPx(80),
            LinearInterpolator()
        )
        val badgeScaleDown = badge_iv.buildScaleAnimator(1.5f, 0f, 300, LinearInterpolator())

        val animatorSet = AnimatorSet().apply {
            startDelay = 500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(p0: Animator?) {
                    clipParents(true)
                    dismissFunc?.invoke(DismissAction.TIMEOUT)
                }
            })
        }
        animatorSet.play(badgeTranslateDownCenter).with(badgeScaleUp)
            .before(
                AnimatorSet().apply {
                    startDelay = 1000
                    playTogether(badgeTranslateDownBox, badgeScaleDown)
                }
            )
        animatorSet.start()
    }

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }
}
