package com.livelike.engagementsdk.core.data.models

import com.google.gson.annotations.SerializedName

class VoteApiResponse {

    @SerializedName("rewards")
    val rewards: List<EarnedReward>? = null

    @SerializedName("claim_token")
    val claimToken: String? = null
}

data class EarnedReward(
    @SerializedName("reward_item_id")
    val rewardId: String,
    @SerializedName("reward_item_amount")
    val rewardItemAmount: Int
)
