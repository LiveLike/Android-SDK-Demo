package com.livelike.engagementsdk.widget.data.models

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.LiveLikeWidget

data class PublishedWidgetListResponse(
    @field:SerializedName("next")
    val next: String? = null,

    @field:SerializedName("previous")
    val previous: String? = null,

    @field:SerializedName("count")
    val count: Int? = null,

    @field:SerializedName("widget_interactions_url_template")
    val widgetInteractionsUrlTemplate: String,

    @field:SerializedName("results")
    val results: List<LiveLikeWidget>? = null
)
