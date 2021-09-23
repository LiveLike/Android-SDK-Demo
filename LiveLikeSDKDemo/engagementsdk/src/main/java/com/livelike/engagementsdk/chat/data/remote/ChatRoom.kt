package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.chat.Visibility

/**
 * Chat Rooms are abstraction over the chat providers in our infra
 **/
internal data class ChatRoom(
    @SerializedName("channels")
    val channels: Channels,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("upload_url")
    val uploadUrl: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("content_filter")
    val contentFilter: String,
    @SerializedName("report_message_url")
    val reportMessageUrl: String,
    @SerializedName("reported_messages_url")
    val reportedMessagesUrl: String,
    @SerializedName("memberships_url")
    val membershipsUrl: String,
    @SerializedName("sticker_packs_url")
    val stickerPacksUrl: String,
    @SerializedName("reaction_packs_url")
    val reactionPacksUrl: String,
    @SerializedName("visibility")
    val visibility: Visibility? = null,
    @SerializedName("muted_status_url_template")
    val mutedStatusUrlTemplate: String? = null,
    @SerializedName("custom_data")
    val customData: String? = null

)

internal data class Channels(
    @SerializedName("chat")
    val chat: Map<String, String>,
    @SerializedName("reactions")
    val reactions: Map<String, String>
)
