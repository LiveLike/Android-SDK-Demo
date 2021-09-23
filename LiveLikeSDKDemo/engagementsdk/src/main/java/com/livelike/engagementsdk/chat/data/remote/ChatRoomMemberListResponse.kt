package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName

data class ChatRoomMemberListResponse(

    @field:SerializedName("next")
    val next: String? = null,

    @field:SerializedName("previous")
    val previous: String? = null,

    @field:SerializedName("count")
    val count: Int? = null,

    @field:SerializedName("results")
    val results: List<ChatRoomMembership?>? = null
)
