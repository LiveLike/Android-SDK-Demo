package com.livelike.engagementsdk.widget.viewModel

import com.google.gson.JsonParseException
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.formatIsoZoned8601
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.EmojiSliderUserInteraction
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.model.ImageSliderEntity
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import kotlinx.coroutines.launch
import okhttp3.FormBody
import org.threeten.bp.ZonedDateTime
import java.io.IOException

internal class EmojiSliderWidgetViewModel(
    widgetInfos: WidgetInfos,
    analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    onDismiss: () -> Unit,
    userRepository: UserRepository,
    programRepository: ProgramRepository? = null,
    widgetMessagingClient: WidgetManager? = null,
    val widgetInteractionRepository: WidgetInteractionRepository?
) : WidgetViewModel<ImageSliderEntity>(
    widgetInfos,
    sdkConfiguration,
    userRepository,
    programRepository,
    widgetMessagingClient,
    onDismiss,
    analyticsService
),
    ImageSliderWidgetModel {

    init {
        widgetObserver(widgetInfos)
        // restoring the emoji slider position from interaction history
        currentVote.onNext(getUserInteraction()?.magnitude?.toString())
    }

    override fun confirmInteraction() {
        currentVote.currentData?.let {
            vote(it)
        }
        super.confirmInteraction()
    }

    override fun vote(value: String) {
        uiScope.launch {
            data.latest()?.voteUrl?.let {
                val fetchedUrl = dataClient.voteAsync(
                    it, "", userRepository?.userAccessToken,
                    FormBody.Builder()
                        .add("magnitude", value).build(),
                    userRepository = userRepository
                )
            }
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos) {
        val resource =
            gson.fromJson(widgetInfos.payload.toString(), ImageSliderEntity::class.java) ?: null
        resource?.apply {
            subscribeWidgetResults(
                resource.subscribe_channel,
                sdkConfiguration,
                userRepository.currentUserStream,
                widgetInfos.widgetId,
                results
            )
            data.onNext(resource)
            widgetState.onNext(WidgetStates.READY)
        }
        currentWidgetId = widgetInfos.widgetId
        programId = data.currentData?.program_id.toString()
        currentWidgetType = WidgetType.fromString(widgetInfos.type)
        interactionData.widgetDisplayed()
    }

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it.toAnalyticsString(),
                currentWidgetId,
                programId,
                interactionData,
                false,
                action
            )

            logDebug { "dismiss EmojiSlider Widget, reason:${action.name}" }
        }
    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos?.payload, LiveLikeWidget::class.java)

    override val voteResults: Stream<LiveLikeWidgetResult>
        get() = results.map { it.toLiveLikeWidgetResult() }

    override fun finish() {
        onDismiss()
        onClear()
    }

    override fun markAsInteractive() {
        trackWidgetBecameInteractive(currentWidgetType, currentWidgetId, programId)
    }

    override fun lockInVote(magnitude: Double) {
        data.latest()?.program_id?.let {
            trackWidgetEngagedAnalytics(
                currentWidgetType, currentWidgetId,
                it
            )
        }
        vote(magnitude.toString())
        saveInteraction(magnitude.toFloat(), data.latest()?.voteUrl)
    }

    override fun getUserInteraction(): EmojiSliderUserInteraction? {
        return widgetInteractionRepository?.getWidgetInteraction(
            widgetInfos.widgetId,
            WidgetKind.fromString(widgetInfos.type)
        )
    }

    override fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<EmojiSliderUserInteraction>>) {
        uiScope.launch {
            try {
                val results =
                    widgetInteractionRepository?.fetchRemoteInteractions(
                        widgetId = widgetInfos.widgetId,
                        widgetKind = widgetInfos.type
                    )

                if (results is Result.Success) {
                    liveLikeCallback.onResponse(
                        results.data.interactions.emojiSlider, null
                    )
                } else if (results is Result.Error) {
                    liveLikeCallback.onResponse(
                        null, results.exception.message
                    )
                }
            } catch (e: JsonParseException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            } catch (e: IOException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            }
        }
    }

    internal fun saveInteraction(magnitude: Float, url: String?) {
        widgetInteractionRepository?.saveWidgetInteraction(
            EmojiSliderUserInteraction(
                magnitude,
                "",
                ZonedDateTime.now().formatIsoZoned8601(),
                url,
                widgetInfos.widgetId,
                widgetInfos.type
            )
        )
    }

    override fun onClear() {
        super.onClear()
        unsubscribeWidgetResults()
    }
}
