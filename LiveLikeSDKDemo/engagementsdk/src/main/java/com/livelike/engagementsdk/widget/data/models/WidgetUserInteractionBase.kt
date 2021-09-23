package com.livelike.engagementsdk.widget.data.models

import com.google.gson.annotations.SerializedName

abstract class WidgetUserInteractionBase(
    @field:SerializedName("id")
    open val id: String,
    @field:SerializedName("created_at")
    open val createdAt: String,
    @field:SerializedName("url")
    open val url: String?,
    @field:SerializedName("widget_id")
    open val widgetId: String,
    @field:SerializedName("widget_kind")
    open val widgetKind: String
) {

    companion object {
        internal fun <T : WidgetUserInteractionBase> getWidgetClass(widgetKind: String): Class<T> {
            return (
                when {
                    widgetKind == "emoji-slider" -> {
                        EmojiSliderUserInteraction::class.java
                    }
                    widgetKind.contains("quiz") -> {
                        QuizWidgetUserInteraction::class.java
                    }
                    widgetKind.contains("poll") -> {
                        PollWidgetUserInteraction::class.java
                    }
                    widgetKind.contains("prediction") -> {
                        PredictionWidgetUserInteraction::class.java
                    }
                    else -> {
                        CheerMeterUserInteraction::class.java
                    }
                }
                ) as Class<T>
        }
    }
}

class EmojiSliderUserInteraction(
    val magnitude: Float,
    id: String,
    createdAt: String,
    url: String?,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)

class QuizWidgetUserInteraction(
    @field:SerializedName("choice_id")
    val choiceId: String,
    id: String,
    createdAt: String,
    url: String?,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)

class PollWidgetUserInteraction(
    @field:SerializedName("option_id")
    val optionId: String,
    id: String,
    createdAt: String,
    url: String?,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)

class PredictionWidgetUserInteraction(
    @field:SerializedName("option_id")
    val optionId: String,
    id: String,
    createdAt: String,
    url: String?,
    @field:SerializedName("is_correct")
    val isCorrect: Boolean,
    @field:SerializedName("claim_token")
    val claimToken: String?,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)

class CheerMeterUserInteraction(
    @field:SerializedName("vote_count")
    var totalScore: Int,
    id: String,
    createdAt: String,
    url: String?,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)

class TextAskUserInteraction(
    id: String,
    createdAt: String,
    url: String?,
    @field:SerializedName("text")
    val text: String?,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)

enum class WidgetKind(val event: String) {
    CHEER_METER("cheer-meter"),
    PREDICTION("prediction"),
    QUIZ("quiz"),
    POLL("poll"),
    IMAGE_SLIDER("emoji-slider"),
    TEXT_ASK("text-ask");

    companion object {
        private val map = values().associateBy(WidgetKind::event)
        fun fromString(type: String) = map[type] ?: POLL
    }

    fun getType(): String {
        return event
    }
}
