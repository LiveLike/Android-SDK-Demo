package com.livelike.engagementsdk.gamification.models

import com.google.gson.annotations.SerializedName

data class BadgeProgress(
    @SerializedName("badge")
    val badge: Badge,
    @SerializedName("badge_progression")
    val progressionList: List<BadgeProgression>
)

data class BadgeProgression(
    @SerializedName("current_reward_amount")
    val currentRewardAmount: Int,
    @SerializedName("reward_item_id")
    val rewardItemId: String,
    @SerializedName("reward_item_name")
    val rewardItemName: String,
    @SerializedName("reward_item_threshold")
    val rewardItemThreshold: Int
)
