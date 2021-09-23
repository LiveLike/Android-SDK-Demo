package com.livelike.engagementsdk.widget.viewModel

import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.services.messaging.LiveLikeWidgetMessagingService
import com.livelike.engagementsdk.widget.services.network.WidgetDataClient
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import kotlinx.coroutines.launch

abstract class BaseViewModel(private val analyticsService: AnalyticsService) :
    ViewModel() {

    private var subscribedWidgetChannelName: String? = null
    internal var isMarkedInteractive: Boolean = false
    internal val widgetState: Stream<WidgetStates> =
        SubscriptionManager<WidgetStates>(emitOnSubscribe = true)
    internal var enableDefaultWidgetTransition = true
    internal var showTimer = true
    internal var showDismissButton: Boolean = true
    internal val dataClient: WidgetDataClient = WidgetDataClientImpl()
    internal val llDataClient = EngagementDataClientImpl()

    internal var timerStartTime: Long? = null

    internal fun voteApi(
        url: String,
        id: String,
        userRepository: UserRepository
    ) {
        uiScope.launch {
            dataClient.voteAsync(
                url,
                id,
                userRepository.userAccessToken,
                userRepository = userRepository
            )
        }
    }

    internal inline fun <reified T> subscribeWidgetResults(
        channelName: String,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        currentUserStream: Stream<LiveLikeUser>,
        widgetId: String,
        results: Stream<T>
    ) {
        subscribedWidgetChannelName = channelName
        LiveLikeWidgetMessagingService.subscribeWidgetChannel(
            channelName,
            this,
            sdkConfiguration,
            currentUserStream
        ) { event ->
            event?.let { event ->
                val widgetType = event.message.get("event").asString ?: ""
                val payload = event.message["payload"].asJsonObject
                if (widgetType.contains("results") && payload.get("id").asString == widgetId) {
                    Handler(Looper.getMainLooper()).post {
                        results.onNext(
                            gson.fromJson(payload.toString(), T::class.java)
                        )
                    }
                }
            }
        }
    }

    fun unsubscribeWidgetResults() {
        subscribedWidgetChannelName?.let {
            LiveLikeWidgetMessagingService.unsubscribeWidgetChannel(it, this)
        }
    }

    fun trackWidgetEngagedAnalytics(currentWidgetType: WidgetType?, currentWidgetId: String, programId: String) {
        currentWidgetType?.let {
            analyticsService.trackWidgetEngaged(
                currentWidgetType.toAnalyticsString(),
                currentWidgetId,
                programId
            )
        }
    }
    protected fun trackWidgetBecameInteractive(
        widgetType: WidgetType?,
        widgetId: String,
        programId: String,
        alertLink: String? = null
    ) {
        if (!isMarkedInteractive) {
            isMarkedInteractive = true
            widgetType?.let { type ->
                analyticsService.trackWidgetBecameInteractive(
                    type.toAnalyticsString(),
                    widgetId,
                    programId
                )
            }
        }
    }

    /**
     * all models should override this to cleanup their resources
     **/
    abstract fun onClear()
}

enum class WidgetStates : Comparable<WidgetStates> {
    READY, // the data has received and ready to use to inject into view
    INTERACTING, // the data is injected into view and shown
    RESULTS, // interaction completed and result to be shown
    FINISHED, // dismiss the widget
}
