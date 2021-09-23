package com.livelike.engagementsdk.widget.data.models

import com.google.gson.annotations.SerializedName

/**
 * Unclaimed prediction user interaction Api response model class
 **/

data class UnclaimedWidgetInteractionList(
    @field:SerializedName("next")
    val next: String? = null,

    @field:SerializedName("previous")
    val previous: String? = null,

    @field:SerializedName("count")
    val count: Int? = null,

    @field:SerializedName("results")
    val results: List<PredictionWidgetUserInteraction>? = null
)
