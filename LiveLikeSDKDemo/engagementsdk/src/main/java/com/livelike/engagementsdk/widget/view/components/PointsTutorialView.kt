package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getTotalPoints
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.pointTutorialSeen
import com.livelike.engagementsdk.widget.view.wouldShowProgressionMeter
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.PointTutorialWidgetViewModel
import kotlinx.android.synthetic.main.atom_widget_points_tutorial.view.pointsAnimation
import kotlinx.android.synthetic.main.atom_widget_points_tutorial.view.pointsTutoView
import kotlinx.android.synthetic.main.atom_widget_points_tutorial.view.points_progression_meter_switcher
import kotlinx.android.synthetic.main.atom_widget_points_tutorial.view.progressionMeterView

class PointsTutorialView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr) {

    private var viewModel: PointTutorialWidgetViewModel? = null
    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as PointTutorialWidgetViewModel
            viewModel?.run {
                startInteractionTimeout(5000) {
                    removeAllViews()
                }
                pointsAnimation.playAnimation()
                pointsTutoView.startAnimation(getTotalPoints())

                if (rewardType == RewardsType.BADGES) {
                    postDelayed(
                        {
                            points_progression_meter_switcher.showNext()
                            progressionMeterView.visibility = View.GONE
                            wouldShowProgressionMeter(
                                rewardType,
                                programGamificationProfile,
                                progressionMeterView
                            )
                        },
                        1300
                    )
                }
            }
        }

    init {
        pointTutorialSeen()
        inflate(context, R.layout.atom_widget_points_tutorial, this)
    }

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }
}
