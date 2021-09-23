package com.livelike.engagementsdk.core.data.models

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.sponsorship.SponsorModel
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate

internal data class Program(
    val programUrl: String,
    val timelineUrl: String,
    val rankUrl: String,
    val id: String,
    val title: String,
    val widgetsEnabled: Boolean,
    val chatEnabled: Boolean,
    val subscribeChannel: String,
    val chatChannel: String,
    val analyticsProps: Map<String, String>,
    val rewardsType: String,
    val leaderboardUrl: String,
    val stickerPacksUrl: String,
    val reactionPacksUrl: String,
    val chatRooms: List<ChatRoom>?,
    val defaultChatRoom: ChatRoom?,
    val reportUrl: String?,
    val leaderboards: List<LeaderBoardResource>,
    val rewardItems: List<RewardItem>,
    val sponsors: List<SponsorModel>,
    val widgetInteractionUrl: String,
    val unclaimedWidgetInteractionsUrlTemplate: String,

)

internal data class LeaderBoardResource(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("client_id") val client_id: String,
    @SerializedName("name") val name: String,
    @SerializedName("reward_item_id") val reward_item_id: String,
    @SerializedName("is_locked") val is_locked: Boolean,
    @SerializedName("entries_url") val entries_url: String,
    @SerializedName("entry_detail_url_template") val entry_detail_url_template: String,
    @SerializedName("reward_item") val rewardItem: RewardItem
)

internal fun LeaderBoardResource.toLeadBoard(): LeaderBoard {
    return LeaderBoard(id, name, rewardItem.toReward())
}

internal fun LeaderboardClient.toLeaderBoard(): LeaderBoard {
    return LeaderBoard(id, name, rewardItem.toReward())
}

data class LeaderBoard(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("reward_item") val rewardItem: LeaderBoardReward
)

data class LeaderBoardForClient(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("reward_item") val rewardItem: RewardItem
)

data class RewardItem(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("client_id") val client_id: String,
    @SerializedName("name") val name: String
)

internal fun RewardItem.toReward(): LeaderBoardReward {
    return LeaderBoardReward(id, name)
}

data class LeaderBoardReward(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

internal data class LeaderBoardEntryResult(
    val previous: String? = null,
    val next: String? = null,
    val count: Int? = null,
    val results: List<LeaderBoardEntry>? = null
)

data class LeaderBoardEntry(
    @SerializedName("percentile_rank") val percentile_rank: Double,
    @SerializedName("profile_id") val profile_id: String,
    @SerializedName("rank") val rank: Int,
    @SerializedName("score") val score: Int,
    @SerializedName("profile_nickname") val profile_nickname: String,
    @SerializedName("profile") val profile: Profile
)

data class Profile(
    val custom_data: String? = null,
    val nickname: String,
    var id: String
)

data class LeaderboardClient(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("rewardItem") val rewardItem: RewardItem,
    @SerializedName("currentUserPlacement") val currentUserPlacement: LeaderboardPlacement,
    @SerializedName("leaderboardDelegate") val leaderBoardDelegate: LeaderBoardDelegate?
)

data class LeaderboardPlacement(
    @SerializedName("rank") val rank: Int,
    @SerializedName("rankPercentile") val rankPercentile: String,
    @SerializedName("score") val score: Int
)

data class LeaderBoardEntryPaginationResult(
    val count: Int = 0,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val list: List<LeaderBoardEntry>? = null
)

internal data class ProgramModel(
    @SerializedName("url")
    val programUrl: String?,
    @SerializedName("timeline_url")
    val timelineUrl: String?,
    @SerializedName("rank_url")
    val rankUrl: String?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("widgets_enabled")
    val widgetsEnabled: Boolean?,
    @SerializedName("chat_enabled")
    val chatEnabled: Boolean?,
    @SerializedName("subscribe_channel")
    val subscribeChannel: String?,
    @SerializedName("sendbird_channel")
    val chatChannel: String?,
    @SerializedName("analytics_properties")
    val analyticsProps: Map<String, String>?,
    @SerializedName("rewards_type")
    val rewardsType: String?, // none, points, bagdes
    val leaderboard_url: String?,
    val sticker_packs_url: String?,
    val reaction_packs_url: String?,
    @SerializedName("report_url")
    val reportUrl: String?,
    @SerializedName("chat_rooms")
    val chatRooms: List<ChatRoom>?,
    @SerializedName("default_chat_room")
    val defaultChatRoom: ChatRoom?,
    val leaderboards: List<LeaderBoardResource>,
    @SerializedName("reward_items")
    val rewardItems: List<RewardItem>,
    @field:SerializedName("sponsors")
    val sponsors: List<SponsorModel>,
    @SerializedName("widget_interactions_url_template")
    val widgetInteractionUrl: String?,
    @SerializedName("unclaimed_widget_interactions_url_template")
    val unclaimedWidgetInteractionsUrlTemplate: String?,

)

internal fun ProgramModel.toProgram(): Program {
    return Program(
        programUrl ?: "",
        timelineUrl ?: "",
        rankUrl ?: "",
        id ?: "",
        title ?: "",
        widgetsEnabled ?: true,
        chatEnabled ?: true,
        subscribeChannel ?: "",
        chatChannel ?: "",
        analyticsProps ?: mapOf(),
        rewardsType ?: "",
        leaderboard_url ?: "",
        sticker_packs_url ?: "",
        reaction_packs_url ?: "",
        chatRooms,
        defaultChatRoom,
        reportUrl,
        leaderboards,
        rewardItems,
        sponsors,
        widgetInteractionUrl ?: "",
        unclaimedWidgetInteractionsUrlTemplate ?: "",
    )
}

enum class RewardsType(val key: String) {
    NONE("none"),
    POINTS("points"),
    BADGES("badges");
}
