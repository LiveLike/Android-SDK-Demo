package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName

internal data class UserChatRoomListResponse(

    @field:SerializedName("next")
    val next: String? = null,

    @field:SerializedName("previous")
    val previous: String? = null,

    @field:SerializedName("count")
    val count: Int? = null,

    @field:SerializedName("results")
    val results: List<ResultsItem?>? = null
)

internal data class ResultsItem(

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("chat_room")
    val chatRoom: ChatRoom? = null,

    @field:SerializedName("url")
    val url: String? = null
)
