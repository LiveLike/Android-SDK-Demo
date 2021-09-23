package com.livelike.engagementsdk.widget.model

import com.google.gson.annotations.SerializedName

internal data class ImageSliderEntity(
    @field:SerializedName("initial_magnitude")
    val initialMagnitude: Float?,
    @field:SerializedName("average_magnitude")
    val averageMagnitude: Float?,
    @field:SerializedName("vote_url")
    val voteUrl: String

) : Resource() {
    override fun toLiveLikeWidgetResult(): LiveLikeWidgetResult {
        return LiveLikeWidgetResult(getMergedOptions(), averageMagnitude)
    }
}
