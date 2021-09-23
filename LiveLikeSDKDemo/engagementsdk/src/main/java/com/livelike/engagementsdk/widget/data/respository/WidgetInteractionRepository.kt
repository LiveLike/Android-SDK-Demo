package com.livelike.engagementsdk.widget.data.respository

import android.content.Context
import com.livelike.engagementsdk.TEMPLATE_PROGRAM_ID
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.CheerMeterUserInteraction
import com.livelike.engagementsdk.widget.data.models.UserWidgetInteractionApi
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.models.WidgetUserInteractionBase

/**
 * Repository that handles user's widget interaction data. It knows what data sources need to be
 * triggered to get widget interaction and where to store the data.
 **/
internal class WidgetInteractionRepository(
    val context: Context,
    val programID: String,
    val userRepository: UserRepository,
    val programUrlTemplate: String?
) {

    private val widgetInteractionRemoteSource: WidgetInteractionRemoteSource =
        WidgetInteractionRemoteSource()
    private val llDataClient = EngagementDataClientImpl()
    private val programRepository = ProgramRepository(programID, userRepository)

    private val widgetInteractionMap = mutableMapOf<String, WidgetUserInteractionBase>()

    fun <T : WidgetUserInteractionBase> saveWidgetInteraction(widgetInteraction: T) {
        widgetInteractionMap[widgetInteraction.widgetId] = widgetInteraction
    }

    fun <T : WidgetUserInteractionBase> getWidgetInteraction(
        widgetId: String,
        widgetKind: WidgetKind
    ): T? {
        return widgetInteractionMap[widgetId] as T?
    }

    /**
     * Responsible for fetching interactions and storing those locally
     **/
    internal suspend fun fetchAndStoreWidgetInteractions(
        url: String,
        accessToken: String
    ): Result<UserWidgetInteractionApi> {

        val widgetInteractionsResult =
            widgetInteractionRemoteSource.getWidgetInteractions(url, accessToken)

        if (widgetInteractionsResult is com.livelike.engagementsdk.core.services.network.Result.Success) {
            val interactionList = mutableListOf<WidgetUserInteractionBase>()
            widgetInteractionsResult.data.interactions?.let { interactions ->
                interactions.cheerMeter?.let { interactionList.addAll(it) }
                interactions.emojiSlider?.let { interactionList.addAll(it) }
                interactions.textPoll?.let { interactionList.addAll(it) }
                interactions.textPrediction?.let { interactionList.addAll(it) }
                interactions.textQuiz?.let { interactionList.addAll(it) }
                interactions.imagePoll?.let { interactionList.addAll(it) }
                interactions.imagePrediction?.let { interactionList.addAll(it) }
                interactions.imageQuiz?.let { interactionList.addAll(it) }
                interactions.textAsk?.let { interactionList.addAll(it) }
            }
            interactionList.forEach {
                if (it is CheerMeterUserInteraction && widgetInteractionMap[it.widgetId] != null) {
                    it.totalScore += (widgetInteractionMap[it.widgetId] as CheerMeterUserInteraction).totalScore
                }
                widgetInteractionMap[it.widgetId] = it
            }
        }
        return widgetInteractionsResult
    }

    /**
     * Responsible for fetching program if program is null in program repository, since interaction url is
     * present in program resource. And fetch interaction based on that interaction url
     *
     **/
    internal suspend fun fetchRemoteInteractions(
        widgetInteractionUrl: String? = null,
        widgetId: String,
        widgetKind: String
    ): Result<UserWidgetInteractionApi>? {
        var results: Result<UserWidgetInteractionApi>? = null
        if (programRepository.program == null) {
            programUrlTemplate?.let {

                val programResults = programRepository.getProgramData(
                    programUrlTemplate.replace(
                        TEMPLATE_PROGRAM_ID,
                        programID
                    )
                )

                if (programResults is Result.Success) {
                    results = userRepository.userAccessToken?.let {
                        fetchAndStoreWidgetInteractions(
                            getInteractionUrl(widgetInteractionUrl, widgetId, widgetKind),
                            it
                        )
                    }
                } else if (programResults is Result.Error) {
                    logError { "Unable to fetch program details ${programResults.exception.message}" }
                }
            }
        } else {
            results = userRepository.userAccessToken?.let {
                fetchAndStoreWidgetInteractions(
                    getInteractionUrl(widgetInteractionUrl, widgetId, widgetKind),
                    it
                )
            }
        }
        return results
    }

    /**
     * get interaction url with appended widget kind and widget id
     **/
    private fun getInteractionUrl(
        widgetInteractionUrl: String? = null,
        widgetId: String,
        widgetKind: String
    ): String {
        var url =
            userRepository.currentUserStream.latest()?.id?.let {
                (widgetInteractionUrl ?: programRepository.program?.widgetInteractionUrl)?.replace(
                    "{profile_id}",
                    it
                )
            } ?: ""

        if (url.isNotEmpty() && widgetInteractionUrl == null) {
            url += "?${getWidgetKindBasedOnType(widgetKind)}_id=${widgetId}"
        }
        return url
    }

    private fun getWidgetKindBasedOnType(widgetKind: String): String {
        return when (WidgetType.fromString(widgetKind)) {
            WidgetType.TEXT_POLL -> "text_poll"
            WidgetType.IMAGE_POLL -> "image_poll"
            WidgetType.TEXT_QUIZ -> "text_quiz"
            WidgetType.IMAGE_QUIZ -> "image_quiz"
            WidgetType.TEXT_PREDICTION -> "text_prediction"
            WidgetType.IMAGE_PREDICTION -> "image_prediction"
            WidgetType.CHEER_METER -> "cheer_meter"
            WidgetType.IMAGE_SLIDER -> "emoji_slider"
            WidgetType.TEXT_PREDICTION_FOLLOW_UP -> "text_prediction"
            WidgetType.IMAGE_PREDICTION_FOLLOW_UP -> "image_prediction"
            WidgetType.TEXT_ASK -> "text_ask"
            else -> ""
        }
    }

    fun clearInteractionMap() {
        widgetInteractionMap.clear()
    }
}
