package com.livelike.engagementsdk.widget.domain

import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.core.data.models.RewardItem

interface UserProfileDelegate {
    fun userProfile(userProfile: LiveLikeUser, reward: com.livelike.engagementsdk.widget.domain.Reward, rewardSource: RewardSource)
}

enum class RewardSource {
    WIDGETS
}

data class Reward(
    val rewardItem: RewardItem,
    val amount: Int
)
