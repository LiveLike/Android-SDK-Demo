package com.livelike.engagementsdk.widget.viewModel

import com.google.gson.JsonParseException
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.debounce
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.formatIsoZoned8601
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.CheerMeterUserInteraction
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.threeten.bp.ZonedDateTime
import java.io.IOException

internal class CheerMeterWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class CheerMeterViewModel(
    val widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    private val sdkConfiguration: EngagementSDK.SdkConfiguration,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository? = null,
    val widgetMessagingClient: WidgetManager? = null,
    val widgetInteractionRepository: WidgetInteractionRepository?
) : BaseViewModel(analyticsService), CheerMeterWidgetmodel {

    var totalVoteCount = 0

    /**
     *this is equal to size of list of options containing vote count to synced with server for each option
     *first request is post to create the vote then after to update the count on that option, patch request will be used
     **/
    var voteStateList: MutableList<CheerMeterVoteState> = mutableListOf<CheerMeterVoteState>()

    val results: Stream<Resource> =
        SubscriptionManager()
    val
            voteEnd: SubscriptionManager<Boolean> =
        SubscriptionManager()
    val data: SubscriptionManager<CheerMeterWidget> =
        SubscriptionManager()
    private var currentWidgetId: String = ""
    private var programId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()
    var isWidgetInteractedEventLogged = false
    var animationEggTimerProgress = 0f
    var animationProgress = 0f

    private val vote = SubscriptionManager<Int>()
    private val debounceVote = vote.debounce()

    init {

        widgetObserver(widgetInfos)
        // restoring the cheer meter score from interaction history
        totalVoteCount = getUserInteraction()?.totalScore ?: 0

        debounceVote.subscribe(this) {
            wouldSendVote()
        }
    }

    fun incrementVoteCount(teamIndex: Int) {
        interactionData.incrementInteraction()
        totalVoteCount++
        voteStateList.getOrNull(teamIndex)?.let {
            it.voteCount++
        }
        vote.onNext(totalVoteCount)
        saveInteraction(totalVoteCount, null)
    }

    private fun wouldSendVote() {
        uiScope.launch {
            voteStateList.forEach {
                pushVoteStateData(it)
            }
        }
    }

    private suspend fun pushVoteStateData(voteState: CheerMeterVoteState) {
        if (voteState.voteCount > 0) {
            val count = voteState.voteCount
            val voteUrl = dataClient.voteAsync(
                voteState.voteUrl,
                body = "{\"vote_count\":${voteState.voteCount}}"
                    .toRequestBody("application/json".toMediaTypeOrNull()),
                accessToken = userRepository.userAccessToken,
                type = voteState.requestType,
                useVoteUrl = false,
                userRepository = userRepository
            )
            voteUrl?.let {
                voteState.voteUrl = it
                voteState.requestType = RequestType.PATCH
            }

            voteState.voteCount -= count
            // TODO  only on success count should be subtracted
        }
    }

    fun voteEnd() {
        currentWidgetType?.let {
            // interaction event will only be fired if interaction cunt is more than 0 and if not logged before
            if (interactionData.interactionCount > 0 && !isWidgetInteractedEventLogged) {
                isWidgetInteractedEventLogged = true
                data.latest()?.resource?.program_id?.let { programId ->
                    analyticsService.trackWidgetInteraction(
                        it.toAnalyticsString(),
                        currentWidgetId,
                        programId,
                        interactionData
                    )
                }
            }
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null) {
            val resource =
                gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {

                resource.getMergedOptions()?.forEach { option ->
                    voteStateList.add(
                        CheerMeterVoteState(
                            0,
                            option.vote_url ?: "",
                            RequestType.POST
                        )
                    )
                }
                subscribeWidgetResults(
                    resource.subscribe_channel,
                    sdkConfiguration,
                    userRepository.currentUserStream,
                    widgetInfos.widgetId,
                    results
                )
                data.onNext(
                    WidgetType.fromString(widgetInfos.type)?.let {
                        CheerMeterWidget(
                            it,
                            resource
                        )
                    }
                )
            }
            currentWidgetId = widgetInfos.widgetId
            programId = data.latest()?.resource?.program_id.toString()
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
            interactionData.widgetDisplayed()

            // this is not needed here, ideally this event should get called when interaction expires
            /* currentWidgetType?.let {
                     analyticsService.trackWidgetInteraction(
                         it.toAnalyticsString(),
                         currentWidgetId,
                         programId,
                         interactionData
                     )

             }*/
        }
    }

    fun startDismissTimout(timeout: String) {
        if (timeout.isNotEmpty()) {
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                if (totalVoteCount == 0) {
                    dismissWidget(DismissAction.TIMEOUT)
                } else {
                    widgetState.onNext(WidgetStates.RESULTS)
                }
            }
        }
    }

    override fun finish() {
        onDismiss()
        cleanUp()
    }

    override fun markAsInteractive() {
        trackWidgetBecameInteractive(currentWidgetType, currentWidgetId, programId)
    }

    fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            data.currentData?.resource?.program_id?.let { programId ->
                analyticsService.trackWidgetDismiss(
                    it.toAnalyticsString(),
                    currentWidgetId,
                    programId,
                    interactionData,
                    false,
                    action
                )
            }
        }
        logDebug { "dismiss Alert Widget, reason:${action.name}" }
        onDismiss()
        cleanUp()
    }

    private fun cleanUp() {
        unsubscribeWidgetResults()
        data.onNext(null)
        results.onNext(null)
        animationEggTimerProgress = 0f
        interactionData.reset()
        currentWidgetId = ""
        currentWidgetType = null
        vote.clear()
        viewModelJob.cancel("Widget Cleanup")
    }

    override fun onClear() {
        cleanUp()
    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)

    override val voteResults: Stream<LiveLikeWidgetResult>
        get() = results.map { it.toLiveLikeWidgetResult() }

    override fun submitVote(optionID: String) {
        trackWidgetEngagedAnalytics(
            currentWidgetType, currentWidgetId,
            programId
        )

        data.currentData?.let { widget ->
            val option = widget.resource.getMergedOptions()?.find { it.id == optionID }
            widget.resource.getMergedOptions()?.indexOf(option)?.let {
                incrementVoteCount(it)
            }
        }
    }

    override fun getUserInteraction(): CheerMeterUserInteraction? {
        return widgetInteractionRepository?.getWidgetInteraction(
            widgetInfos.widgetId,
            WidgetKind.fromString(widgetInfos.type)
        )
    }

    override fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<CheerMeterUserInteraction>>) {
        uiScope.launch {
            try {
                val results =
                    widgetInteractionRepository?.fetchRemoteInteractions(
                        widgetId = widgetInfos.widgetId,
                        widgetKind = widgetInfos.type
                    )

                if (results is Result.Success) {
                    liveLikeCallback.onResponse(
                        results.data.interactions.cheerMeter, null
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

    internal fun saveInteraction(score: Int, url: String?) {
        widgetInteractionRepository?.saveWidgetInteraction(
            CheerMeterUserInteraction(
                score,
                "",
                ZonedDateTime.now().formatIsoZoned8601(),
                url,
                widgetInfos.widgetId,
                widgetInfos.type
            )
        )
    }
}

data class CheerMeterVoteState(
    var voteCount: Int,
    var voteUrl: String,
    var requestType: RequestType
)
