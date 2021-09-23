@file:Suppress("UNNECESSARY_SAFE_CALL")

package com.livelike.engagementsdk.widget.view

import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.data.models.Badge
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.view.components.ProgressionMeterView

/**
 * extensions related to gamification views like progression meter
 */

/** TODO later this extension to more concrete widgetView so we pass less params here*/
internal fun SpecifiedWidgetView.wouldShowProgressionMeter(
    rewardsType: RewardsType?,
    latest: ProgramGamificationProfile?,
    progressionMeterView: ProgressionMeterView
) {
    latest?.let {
        logDebug { "showing Gamification Meter ,Reward Type:$rewardsType newBadge:${it.newBadges} currentBadge:${it.currentBadge} points:${it.points} newPoints:${it.newPoints}" }
        if (rewardsType!! == RewardsType.BADGES) {
            val nextBadgeToDisplay: Badge? = if (it.newBadges == null || it.newBadges.isEmpty()) {
                it.nextBadge
            } else {
                it.newBadges?.maxOrNull()!!
            }
            nextBadgeToDisplay?.let { nextBadge ->
                progressionMeterView.animatePointsBadgeProgression(
                    it.points - it.newPoints,
                    it.newPoints,
                    nextBadge.points,
                    nextBadge.imageFile
                )
            }
        }
    }
}

internal fun AnalyticsWidgetInteractionInfo.addGamificationAnalyticsData(programGamificationProfile: ProgramGamificationProfile) {
    pointEarned = programGamificationProfile.newPoints

    programGamificationProfile.newBadges?.maxOrNull()?.let {
        badgeEarned = it.id
        badgeLevelEarned = it.level
    }
    programGamificationProfile.currentBadge?.let { currentBadge ->
        pointsInCurrentLevel = programGamificationProfile.points - currentBadge.points
        programGamificationProfile.nextBadge?.let { nextBadge ->
            pointsToNextLevel = nextBadge.points - programGamificationProfile.points
        }
    }
}
