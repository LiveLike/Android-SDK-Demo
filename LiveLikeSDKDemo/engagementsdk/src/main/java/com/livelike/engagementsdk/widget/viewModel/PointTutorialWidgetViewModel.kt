package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.model.Resource

internal class PointTutorialWidgetViewModel(
    onDismiss: () -> Unit,
    analyticsService: AnalyticsService,
    val rewardType: RewardsType,
    val programGamificationProfile: ProgramGamificationProfile?
) : WidgetViewModel<Resource>(onDismiss, analyticsService) {

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
        analyticsService.trackPointTutorialSeen(action.name, 5000L)
    }

    //     Actually for this we need to have layers of WidgetViewModel one with interaction and one without interaction. This tutorial and gamification will not call this ideally, we can move this in later tech debt tickets.
    override fun vote(value: String) {
    }
}
