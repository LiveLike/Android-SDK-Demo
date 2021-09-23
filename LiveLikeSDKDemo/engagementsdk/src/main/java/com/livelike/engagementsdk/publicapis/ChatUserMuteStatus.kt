package com.livelike.engagementsdk.publicapis

import com.google.gson.annotations.SerializedName

data class ChatUserMuteStatus(
    @SerializedName("is_muted")
    val isMuted: Boolean
)
