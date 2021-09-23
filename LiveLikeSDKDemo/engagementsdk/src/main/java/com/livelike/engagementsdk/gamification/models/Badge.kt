package com.livelike.engagementsdk.gamification.models

import com.google.gson.annotations.SerializedName

data class Badge(
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("name")
    val name: String,
    @field:SerializedName("badge_icon_url")
    val badgeIconUrl: String,
    @field:SerializedName("description")
    val description: String
)
