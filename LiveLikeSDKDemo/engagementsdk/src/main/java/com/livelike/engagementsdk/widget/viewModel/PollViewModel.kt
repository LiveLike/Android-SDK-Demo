package com.livelike.engagementsdk.widget.viewModel

import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonParseException
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.AnalyticsWidgetSpecificInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
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
import com.livelike.engagementsdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.engagementsdk.widget.data.models.PollWidgetUserInteraction
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.domain.GamificationManager
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.model.Option
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import java.io.IOException

internal class PollWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class PollViewModel(
    private val widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    val sdkConfiguration: EngagementSDK.SdkConfiguration,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository? = null,
    private val widgetMessagingClient: WidgetManager? = null,
    val widgetInteractionRepository: WidgetInteractionRepository?
) : BaseViewModel(analyticsService), PollWidgetModel {
    lateinit var onWidgetInteractionCompleted: () -> Unit

    //    TODO remove points for all view models and make it follow dry, move it to gamification stream
    var points: SubscriptionManager<Int?> =
        SubscriptionManager(false)
    val gamificationProfile: Stream<ProgramGamificationProfile>
        get() = programRepository?.programGamificationProfileStream ?: SubscriptionManager()
    val rewardsType: RewardsType
        get() = programRepository?.rewardType ?: RewardsType.NONE
    val data: SubscriptionManager<PollWidget> =
        SubscriptionManager()
    val results: SubscriptionManager<Resource> =
        SubscriptionManager()
    val currentVoteId: SubscriptionManager<String?> =
        SubscriptionManager()
    private val debouncer = currentVoteId.debounce()
    var lastestVotedOptionId: String? = ""

    var adapter: WidgetOptionsViewAdapter? = null
    var timeoutStarted = false
    var animationResultsProgress = 0f
    private var animationPath = ""
    var voteUrl: String? = null
    var animationEggTimerProgress = 0f
    private var currentWidgetId: String = ""
    private var programId: String = ""
    private var currentWidgetType: WidgetType? = null

    private val interactionData = AnalyticsWidgetInteractionInfo()
    private val widgetSpecificInfo = AnalyticsWidgetSpecificInfo()
    private var latestPollUserInteraction: PollWidgetUserInteraction? = null

    init {

//        sdkConfiguration.pubNubKey.let {
//            pubnub =
//                PubnubMessagingClient.getInstance(it, userRepository.currentUserStream.latest()?.id)
//                    ?.asBehaviourSubject()
//            pubnub?.addMessagingEventListener(object : MessagingEventListener {
//                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
//                    val widgetType = event.message.get("event").asString ?: ""
//                    logDebug { "type is : $widgetType" }
//                    val payload = event.message["payload"].asJsonObject
//                    Handler(Looper.getMainLooper()).post {
//                        results.onNext(
//                            gson.fromJson(payload.toString(), Resource::class.java) ?: null
//                        )
//                    }
//                }
//
//                override fun onClientMessageError(client: MessagingClient, error: Error) {}
//                override fun onClientMessageStatus(
//                    client: MessagingClient,
//                    status: ConnectionStatus
//                ) {
//                }
//            })
//        }

        debouncer.subscribe(javaClass.simpleName) {
            if (it != null) vote()
        }

        widgetObserver(widgetInfos)
    }

    private fun vote() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) return // Nothing has been clicked
        logDebug { "PollWidget Vote: position:${adapter?.selectedPosition}" }

        uiScope.launch {
            adapter?.run {
                val option = myDataset[selectedPosition]
                if (lastestVotedOptionId != option.id) {
                    var url = option.getMergedVoteUrl()
                    lastestVotedOptionId = option.id
                    url?.let {
                        dataClient.voteAsync(
                            it,
                            option.id,
                            userRepository.userAccessToken,
                            userRepository = userRepository,
                            patchVoteUrl = getUserInteraction()?.url
                        )
                    }
                }
            }
        }
    }

    internal fun saveInteraction(option: Option) {
        widgetInteractionRepository?.saveWidgetInteraction(
            PollWidgetUserInteraction(
                option.id,
                "",
                ZonedDateTime.now().formatIsoZoned8601(),
                getUserInteraction()?.url,
                widgetInfos.widgetId,
                widgetInfos.type
            )
        )
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null &&
            (
                    WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_POLL ||
                            WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_POLL
                    )
        ) {
            val resource =
                gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {
                subscribeWidgetResults(
                    resource.subscribe_channel,
                    sdkConfiguration,
                    userRepository.currentUserStream,
                    widgetInfos.widgetId,
                    results
                )
                data.onNext(
                    WidgetType.fromString(widgetInfos.type)?.let {
                        PollWidget(
                            it,
                            resource
                        )
                    }
                )
            }
            currentWidgetId = widgetInfos.widgetId
            programId = data.currentData?.resource?.program_id.toString()
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
            interactionData.widgetDisplayed()
        }
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                widgetState.onNext(WidgetStates.RESULTS)
            }
        }
    }

    fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it.toAnalyticsString(),
                currentWidgetId,
                programId,
                interactionData,
                adapter?.selectionLocked,
                action
            )
        }
        widgetState.onNext(WidgetStates.FINISHED)
        logDebug { "dismiss Poll Widget, reason:${action.name}" }
        onDismiss()
        cleanUp()
    }

    internal fun confirmationState() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismissWidget(DismissAction.TIMEOUT)
            return
        }
        adapter?.selectionLocked = true
        onWidgetInteractionCompleted.invoke()

        uiScope.launch {
            data.currentData?.resource?.rewards_url?.let {
                userRepository.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository?.programGamificationProfileStream?.onNext(pts)
                    publishPoints(pts.newPoints)
                    widgetMessagingClient?.let { widgetMessagingClient ->
                        GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                    }
                    interactionData.addGamificationAnalyticsData(pts)
                }
            }

            currentWidgetType?.let {
                analyticsService.trackWidgetInteraction(
                    it.toAnalyticsString(),
                    currentWidgetId,
                    programId,
                    interactionData
                )
            }
            delay(3000)
            dismissWidget(DismissAction.TIMEOUT)
        }
    }

    private fun publishPoints(pts: Int) {
        this.points.onNext(pts)
    }

    private fun cleanUp() {
        vote() // Vote on dismiss
        unsubscribeWidgetResults()
        timeoutStarted = false
        adapter = null
        animationResultsProgress = 0f
        animationPath = ""
        voteUrl = null
        data.onNext(null)
        results.onNext(null)
        animationEggTimerProgress = 0f
        currentVoteId.onNext(null)
        lastestVotedOptionId = ""
        interactionData.reset()
        widgetSpecificInfo.reset()
        currentWidgetId = ""
        currentWidgetType = null
        viewModelJob.cancel("Widget Cleanup")
    }

    override fun onClear() {
        cleanUp()
    }

    var firstClick = true

    fun onOptionClicked() {
        if (firstClick) {
            firstClick = false
        }
        interactionData.incrementInteraction()
    }

    override val voteResults: Stream<LiveLikeWidgetResult>
        get() = results.map { it.toLiveLikeWidgetResult() }

    override fun submitVote(optionID: String) {
        trackWidgetEngagedAnalytics(
            currentWidgetType, currentWidgetId,
            programId
        )

        data.currentData?.let { widget ->
            val option = widget.resource.getMergedOptions()?.find { it.id == optionID }
            widget.resource.getMergedOptions()?.indexOf(option)?.let { position ->
                val url = widget.resource.getMergedOptions()!![position].getMergedVoteUrl()
                url?.let {
                    voteApi(it, widget.resource.getMergedOptions()!![position].id, userRepository)
                    if (option != null) {
                        saveInteraction(option)
                    }
                }
            }
        }
    }

    override fun getUserInteraction(): PollWidgetUserInteraction? {
        return widgetInteractionRepository?.getWidgetInteraction(
            widgetInfos.widgetId,
            WidgetKind.fromString(widgetInfos.type)
        )
    }

    override fun loadInteractionHistory(
        liveLikeCallback: LiveLikeCallback<List<PollWidgetUserInteraction>>
    ) {
        uiScope.launch {
            try {
                val results =
                    widgetInteractionRepository?.fetchRemoteInteractions(
                        widgetId = widgetInfos.widgetId,
                        widgetKind = widgetInfos.type
                    )

                if (results is Result.Success) {
                    if (WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_POLL) {
                        liveLikeCallback.onResponse(
                            results.data.interactions.textPoll, null
                        )
                    } else if (WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_POLL) {
                        liveLikeCallback.onResponse(
                            results.data.interactions.imagePoll, null
                        )
                    }
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

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)

    override fun finish() {
        onDismiss()
        cleanUp()
    }

    override fun markAsInteractive() {
        trackWidgetBecameInteractive(currentWidgetType, currentWidgetId, programId)
    }
}
