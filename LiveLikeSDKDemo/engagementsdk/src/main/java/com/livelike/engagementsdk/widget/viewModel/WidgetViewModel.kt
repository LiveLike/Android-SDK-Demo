package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.AnalyticsWidgetSpecificInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.domain.GamificationManager
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO inherit all widget viewModels from here and  add widget common code here.
internal abstract class WidgetViewModel<T : Resource>(
    protected val onDismiss: () -> Unit,
    val analyticsService: AnalyticsService
) : BaseViewModel(analyticsService) {

    var timeOutJob: Job? = null
    lateinit var widgetInfos: WidgetInfos
    lateinit var sdkConfiguration: EngagementSDK.SdkConfiguration
    var widgetMessagingClient: WidgetManager? = null
    var programRepository: ProgramRepository? = null
    lateinit var userRepository: UserRepository

    constructor(
        widgetInfos: WidgetInfos,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        userRepository: UserRepository,
        programRepository: ProgramRepository? = null,
        widgetMessagingClient: WidgetManager? = null,
        onDismiss: () -> Unit,
        analyticsService: AnalyticsService
    ) : this(onDismiss, analyticsService) {
        this.widgetInfos = widgetInfos
        this.sdkConfiguration = sdkConfiguration
        this.userRepository = userRepository
        this.programRepository = programRepository
        this.widgetMessagingClient = widgetMessagingClient
    }

    var timeoutStarted = false

    val data: SubscriptionManager<T> =
        SubscriptionManager()
    val results: SubscriptionManager<T> =
        SubscriptionManager()

    val state: Stream<WidgetState> =
        SubscriptionManager()
    val currentVote: SubscriptionManager<String?> =
        SubscriptionManager()

    val gamificationProfile: Stream<ProgramGamificationProfile>
        get() = programRepository?.programGamificationProfileStream ?: SubscriptionManager()
    val rewardsType: RewardsType
        get() = programRepository?.rewardType ?: RewardsType.NONE

    var animationEggTimerProgress = 0f

    var currentWidgetId: String = ""
    var programId: String = ""
    var currentWidgetType: WidgetType? = null

    val interactionData = AnalyticsWidgetInteractionInfo()
    val widgetSpecificInfo = AnalyticsWidgetSpecificInfo()

    internal open fun confirmInteraction() {
        if (currentVote.latest() != null) {
            currentWidgetType?.let {
                programRepository?.programId?.let { programId ->
                    analyticsService.trackWidgetInteraction(
                        it.toAnalyticsString(),
                        currentWidgetId,
                        programId,
                        interactionData
                    )
                }
            }
            uiScope.launch {
                data.currentData?.rewards_url?.let {
                    userRepository?.getGamificationReward(it, analyticsService)?.let { pts ->
                        programRepository?.programGamificationProfileStream?.onNext(pts)
                        widgetMessagingClient?.let { widgetMessagingClient ->
                            GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                        }
                        interactionData.addGamificationAnalyticsData(pts)
                    }
                }
                delay(2000)
                state.onNext(WidgetState.SHOW_RESULTS)
                delay(2000)
                state.onNext(WidgetState.SHOW_GAMIFICATION)
            }
        }
    }

    abstract fun vote(value: String)

    fun startInteractionTimeout(timeout: Long, function: (() -> Unit)? = null) {
        if (!timeoutStarted) {
            timeoutStarted = true
            timeOutJob = uiScope.launch {
                delay(timeout)
                onInteractionCompletion(function)
            }
        }
    }

    /**
     * will be called after timer completion or can be called manually after interaction finished
     **/
    internal fun onInteractionCompletion(function: (() -> Unit)?) {
        if (currentVote.latest() == null) {
            dismissWidget(DismissAction.TIMEOUT)
            function?.invoke()
        } else {
            state.onNext(WidgetState.LOCK_INTERACTION)
            widgetState.onNext(WidgetStates.RESULTS)
        }
        timeoutStarted = false
    }

    // FYI Right now this Widgetmodel is inherited by tutorial and gamification widgets, so dismiss analytics should be added in more concrete class.
    open fun dismissWidget(action: DismissAction) {
        state.onNext(WidgetState.DISMISS)
        onDismiss()
        onClear()
    }

    override fun onClear() {
        viewModelJob.cancel()
        timeoutStarted = false
    }
}

enum class WidgetState {
    LOCK_INTERACTION, // It is to indicate current interaction is done.
    SHOW_RESULTS, // It is to tell view to show results
    SHOW_GAMIFICATION,
    DISMISS
}
