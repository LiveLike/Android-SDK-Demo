package com.livelike.engagementsdk

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.sponsorship.SponsorModel
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.SocialEmbedItem
import com.livelike.engagementsdk.widget.data.models.WidgetUserInteractionBase

data class LiveLikeWidget(

    @field:SerializedName("program_id")
    val programId: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("scheduled_at")
    val scheduledAt: String? = null,

    @field:SerializedName("engagement_count")
    val engagementCount: Int? = null,

    @field:SerializedName("publish_delay")
    val publishDelay: String? = null,

    @field:SerializedName("rewards_url")
    val rewardsUrl: String? = null,

    @field:SerializedName("translatable_fields")
    val translatableFields: List<String?>? = null,

    @field:SerializedName("schedule_url")
    val scheduleUrl: String? = null,

    @field:SerializedName("timeout")
    var timeout: String? = null,

    @field:SerializedName("impression_count")
    val impressionCount: Int? = null,

    @field:SerializedName("engagement_percent")
    val engagementPercent: String? = null,

    @field:SerializedName("options")
    val options: List<OptionsItem?>? = null,

    @field:SerializedName("choices")
    val choices: List<OptionsItem?>? = null,

    @field:SerializedName("program_date_time")
    val programDateTime: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("published_at")
    val publishedAt: String? = null,

    @field:SerializedName("unique_impression_count")
    val uniqueImpressionCount: Int? = null,

    @field:SerializedName("question")
    val question: String? = null,

    @field:SerializedName("kind")
    val kind: String? = null,

    @field:SerializedName("subscribe_channel")
    val subscribeChannel: String? = null,

    @field:SerializedName("created_by")
    val createdBy: CreatedBy? = null,

    @field:SerializedName("url")
    val url: String? = null,

    @field:SerializedName("cheer_type")
    val cheerType: String? = null,

    @field:SerializedName("impression_url")
    val impressionUrl: String? = null,

    @field:SerializedName("reactions")
    val reactions: List<ReactionsItem?>? = null,

    @field:SerializedName("interaction_url")
    val interactionUrl: String? = null,

    @field:SerializedName("custom_data")
    val customData: String? = null,

    @field:SerializedName("status")
    val status: String? = null,

    @field:SerializedName("text")
    val text: String? = null,
    @field:SerializedName("image_url")
    val imageUrl: String? = null,
    @field:SerializedName("video_url")
    val videoUrl: String? = null,
    @field:SerializedName("link_url")
    val linkUrl: String? = null,
    @field:SerializedName("link_label")
    val linkLabel: String? = null,
    @field:SerializedName("text_prediction_id")
    val textPredictionId: String? = null,
    @field:SerializedName("image_prediction_id")
    val imagePredictionId: String? = null,
    @field:SerializedName("text_prediction_url")
    val textPredictionUrl: String? = null,
    @field:SerializedName("correct_option_id")
    val correctOptionId: String? = null,
    @field:SerializedName("title")
    val title: String? = null,
    @field:SerializedName("prompt")
    val prompt: String? = null,
    @field:SerializedName("initial_magnitude")
    val initialMagnitude: Float?,
    @field:SerializedName("average_magnitude")
    val averageMagnitude: Float?,
    @field:SerializedName("vote_url")
    val voteUrl: String,
    @field:SerializedName("claim_url")
    val claimUrl: String? = null,
    @field:SerializedName("reply_url")
    val replyUrl: String? = null,
    @field:SerializedName("confirmation_message")
    val confirmationMessage: String? = null,

    // fields related to social embed widget
    @field:SerializedName("items")
    val socialEmbedItems: List<SocialEmbedItem>?,
    @field:SerializedName("comment")
    val comment: String?,
    @field:SerializedName("widget_interactions_url_template")
    val widgetInteractionUrl: String?,
    @field:SerializedName("sponsors")
    val sponsors: List<SponsorModel>?
) {
    /**
     * Added this method to get WidgetType for integrator understanding and they can use it for they implementation
     */
    fun getWidgetType(): WidgetType? {
        var widgetType = kind
        widgetType = if (widgetType?.contains("follow-up") == true) {
            "$widgetType-updated"
        } else {
            "$widgetType-created"
        }
        return WidgetType.fromString(widgetType)
    }
}

data class CreatedBy(

    @field:SerializedName("image_url")
    val imageUrl: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("id")
    val id: String? = null
)

data class OptionsItem(

    @field:SerializedName("image_url")
    val imageUrl: String? = null,

    @field:SerializedName("vote_url")
    val voteUrl: String? = null,

    @field:SerializedName("description")
    val description: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("translatable_fields")
    val translatableFields: List<String?>? = null,

    @field:SerializedName("vote_count")
    val voteCount: Int? = null,

    @field:SerializedName("is_correct")
    val isCorrect: Boolean? = null,

    @field:SerializedName("answer_url")
    val answerUrl: String? = null,

    @field:SerializedName("answer_count")
    val answerCount: Int? = null
)

data class ReactionsItem(

    @field:SerializedName("sequence")
    val sequence: Int? = null,

    @field:SerializedName("file")
    val file: String? = null,

    @field:SerializedName("react_url")
    val reactUrl: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("count")
    val count: Int? = null,

    @field:SerializedName("mimetype")
    val mimetype: String? = null,

    @field:SerializedName("id")
    val id: String? = null
)
