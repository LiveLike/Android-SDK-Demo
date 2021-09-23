package com.livelike.engagementsdk.widget.viewModel

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonParseException
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
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
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.formatIsoZoned8601
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.data.models.QuizWidgetUserInteraction
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.domain.GamificationManager
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.model.Option
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import java.io.IOException

internal class QuizWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class QuizViewModel(
    private val widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    private val sdkConfiguration: EngagementSDK.SdkConfiguration,
    val context: Context,
    var onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository? = null,
    val widgetMessagingClient: WidgetManager? = null,
    val widgetInteractionRepository: WidgetInteractionRepository?
) : BaseViewModel(analyticsService), QuizWidgetModel {

    var points: Int? = null
    val gamificationProfile: Stream<ProgramGamificationProfile>
        get() = programRepository?.programGamificationProfileStream ?: SubscriptionManager()
    val rewardsType: RewardsType
        get() = programRepository?.rewardType ?: RewardsType.NONE
    val data: SubscriptionManager<QuizWidget> =
        SubscriptionManager()
    val results: Stream<Resource> =
        SubscriptionManager()
    val currentVoteId: SubscriptionManager<String?> =
        SubscriptionManager()

    //    var state: Stream<String> =
//        SubscriptionManager() // results
    var adapter: WidgetOptionsViewAdapter? = null
    private var timeoutStarted = false
    var animationProgress = 0f
    internal var animationPath = ""
    var voteUrl: String? = null
    var animationEggTimerProgress = 0f

    private var currentWidgetId: String = ""
    private var programId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()

    internal var timeOutJob: Job? = null

    init {
        widgetObserver(widgetInfos)
    }

    internal fun vote() {
        logDebug { "Quiz Widget selectedPosition:${adapter?.selectedPosition}" }
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) return // Nothing has been clicked

        uiScope.launch {
            adapter?.apply {
                val url = myDataset[selectedPosition].getMergedVoteUrl()
                url?.let {
                    val fetchedUrl = dataClient.voteAsync(
                        url,
                        myDataset[selectedPosition].id,
                        userRepository.userAccessToken,
                        userRepository = userRepository
                    )
                }
            }
            adapter?.notifyDataSetChanged()
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null &&
            (
                    WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_QUIZ ||
                            WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_QUIZ
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
                        QuizWidget(
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
        } else {
            cleanUp()
        }
    }

    fun startDismissTimout(
        timeout: String,
        widgetViewThemeAttributes: WidgetViewThemeAttributes
    ) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            timeOutJob = uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                lockInteractionAndSubmitVote()
            }
        }
    }

    internal suspend fun lockInteractionAndSubmitVote() {
        adapter?.selectionLocked = true
        vote()
        // delay(500) // Just refactoring, this line was already here
        widgetState.onNext(WidgetStates.RESULTS)
        resultsState()
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
        logDebug { "dismiss Quiz Widget, reason:${action.name}" }
        onDismiss()
        cleanUp()
        viewModelJob.cancel()
    }

    private fun resultsState() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            // dismissWidget(DismissAction.TIMEOUT)
            return
        }

        val isUserCorrect =
            adapter?.selectedPosition?.let { adapter?.myDataset?.get(it)?.is_correct } ?: false
        adapter?.selectionLocked = true
        logDebug { "Quiz View ,showing result isUserCorrect:$isUserCorrect" }
        uiScope.launch {
            data.currentData?.resource?.rewards_url?.let {
                userRepository.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository?.programGamificationProfileStream?.onNext(pts)
                    points = pts.newPoints
                    widgetMessagingClient?.let { widgetMessagingClient ->
                        GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                    }
                    interactionData.addGamificationAnalyticsData(pts)
                }
            }
//            state.onNext("results")
            currentWidgetType?.let {
                analyticsService.trackWidgetInteraction(
                    it.toAnalyticsString(),
                    currentWidgetId,
                    programId,
                    interactionData
                )
            }
        }
    }

    private fun cleanUp() {
        vote() // Vote on dismiss
        unsubscribeWidgetResults()
        timeoutStarted = false
        adapter = null
        animationProgress = 0f
        animationPath = ""
        voteUrl = null
        data.onNext(null)
        results.onNext(null)
//        state.onNext(null)
        animationEggTimerProgress = 0f

        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }

    override fun onClear() {
        cleanUp()
    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)

    override val voteResults: Stream<LiveLikeWidgetResult>
        get() = results.map { it.toLiveLikeWidgetResult() }

    override fun lockInAnswer(optionID: String) {
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
                }
                if (option != null) {
                    saveInteraction(option)
                }
            }
        }
    }

    override fun getUserInteraction(): QuizWidgetUserInteraction? {
        return widgetInteractionRepository?.getWidgetInteraction(
            widgetInfos.widgetId,
            WidgetKind.fromString(widgetInfos.type)
        )
    }

    override fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<QuizWidgetUserInteraction>>) {
        uiScope.launch {
            try {
                val results =
                    widgetInteractionRepository?.fetchRemoteInteractions(
                        widgetId = widgetInfos.widgetId,
                        widgetKind = widgetInfos.type
                    )

                if (results is Result.Success) {
                    if (WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_QUIZ) {
                        liveLikeCallback.onResponse(
                            results.data.interactions.textQuiz, null
                        )
                    } else if (WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_QUIZ) {
                        liveLikeCallback.onResponse(
                            results.data.interactions.imageQuiz, null
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

    internal fun saveInteraction(option: Option) {
        widgetInteractionRepository?.saveWidgetInteraction(
            QuizWidgetUserInteraction(
                option.id,
                "",
                ZonedDateTime.now().formatIsoZoned8601(),
                option.getMergedVoteUrl(),
                widgetInfos.widgetId,
                widgetInfos.type
            )
        )
    }

    fun onOptionClicked() {
        interactionData.incrementInteraction()
    }

    override fun finish() {
        onDismiss()
        cleanUp()
    }

    override fun markAsInteractive() {
        trackWidgetBecameInteractive(currentWidgetType, currentWidgetId, programId)
    }
}
