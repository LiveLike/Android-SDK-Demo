package com.livelike.engagementsdk.core.data.models

import com.google.gson.annotations.SerializedName

internal data class SubmitApiResponse(
    @SerializedName("rewards")
    val rewards: List<EarnedReward>? = null,

    @SerializedName("text")
    val text: String? = null
)
