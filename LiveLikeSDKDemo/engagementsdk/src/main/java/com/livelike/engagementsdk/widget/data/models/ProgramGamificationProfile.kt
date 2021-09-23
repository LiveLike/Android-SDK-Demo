package com.livelike.engagementsdk.widget.data.models

import com.google.gson.annotations.SerializedName

data class ProgramGamificationProfile(
    @SerializedName("id")
    val id: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("points")
    val points: Int,
    @SerializedName("points_to_next_badge")
    val pointsToNextBadge: Int,
    @SerializedName("previous_badge")
    val previousBadge: Badge?,
    @SerializedName("next_badge")
    val nextBadge: Badge?,
    @SerializedName("current_badge")
    val currentBadge: Badge?,
    @SerializedName("new_badges")
    val newBadges: List<Badge>?,
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("total_players")
    val totalPlayers: Int,
    @SerializedName("total_points")
    val totalPoints: Int,
    @SerializedName("new_points")
    val newPoints: Int
)
