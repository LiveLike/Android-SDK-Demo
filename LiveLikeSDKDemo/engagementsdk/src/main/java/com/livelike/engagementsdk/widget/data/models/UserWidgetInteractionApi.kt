package com.livelike.engagementsdk.widget.data.models

import com.google.gson.annotations.SerializedName

/**
* User interaction Api response model class
**/

internal data class UserWidgetInteractionApi(
    val interactions: Interactions
)

internal data class Interactions(
    @field:SerializedName("cheer-meter")
    val cheerMeter: List<CheerMeterUserInteraction>?,
    @field:SerializedName("emoji-slider")
    val emojiSlider: List<EmojiSliderUserInteraction>?,
    @field:SerializedName("text-poll")
    val textPoll: List<PollWidgetUserInteraction>?,
    @field:SerializedName("image-poll")
    val imagePoll: List<PollWidgetUserInteraction>?,
    @field:SerializedName("text-quiz")
    val textQuiz: List<QuizWidgetUserInteraction>?,
    @field:SerializedName("image-quiz")
    val imageQuiz: List<QuizWidgetUserInteraction>?,
    @field:SerializedName("text-prediction")
    val textPrediction: List<PredictionWidgetUserInteraction>?,
    @field:SerializedName("image-prediction")
    val imagePrediction: List<PredictionWidgetUserInteraction>?,
    @field:SerializedName("text-ask")
    val textAsk: List<TextAskUserInteraction>?,

)
