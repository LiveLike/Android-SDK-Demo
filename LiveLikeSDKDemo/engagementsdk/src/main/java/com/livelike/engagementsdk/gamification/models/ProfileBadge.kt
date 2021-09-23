package com.livelike.engagementsdk.gamification.models

import com.google.gson.annotations.SerializedName

data class ProfileBadge(
    @SerializedName("awarded_at")
    val awardedAt: String,
    @SerializedName("badge")
    val badge: Badge
)
