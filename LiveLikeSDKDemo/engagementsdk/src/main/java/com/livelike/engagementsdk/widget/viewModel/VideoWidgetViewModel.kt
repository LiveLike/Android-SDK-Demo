package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Alert
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.widgetModel.VideoAlertWidgetModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class VideoWidgetViewModel(
    val widgetInfos: WidgetInfos,
    val analyticsService: AnalyticsService,
    private val onDismiss: () -> Unit
) : BaseViewModel(analyticsService), VideoAlertWidgetModel {

    private var timeoutStarted = false
    var data: SubscriptionManager<Alert?> =
        SubscriptionManager()

    private var currentWidgetId: String = ""
    private var programId: String = ""
    var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()

    init {
        data.onNext(gson.fromJson(widgetInfos.payload.toString(), Alert::class.java) ?: null)
        widgetState.onNext(WidgetStates.READY)
        interactionData.widgetDisplayed()
        currentWidgetId = widgetInfos.widgetId
        programId = data?.currentData?.program_id.toString()
        currentWidgetType = WidgetType.fromString(widgetInfos.type)
    }

    override fun videoAlertLinkClicked(url: String) {
        onVideoAlertClickLink(url)
        data.latest()?.program_id?.let {
            trackWidgetEngagedAnalytics(
                currentWidgetType, currentWidgetId,
                it
            )
        }
    }

    override fun registerPlayStarted() {
        trackPlayStarted()
    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)

    override fun markAsInteractive() {
        trackWidgetBecameInteractive(currentWidgetType, currentWidgetId, programId)
    }

    fun startDismissTimeout(timeout: String, onDismiss: () -> Unit) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                dismissWidget(DismissAction.TIMEOUT)
                onDismiss()
                timeoutStarted = false
            }
        }
    }

    private fun cleanup() {
        data.onNext(null)
        timeoutStarted = false
        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }

    internal fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it.toAnalyticsString(),
                currentWidgetId,
                programId,
                interactionData,
                false,
                action
            )
        }
        logDebug { "dismiss Video Widget, reason:${action.name}" }
        onDismiss()
        cleanup()
        viewModelJob.cancel()
    }

    fun onVideoAlertClickLink(linkUrl: String) {
        interactionData.incrementInteraction()
        currentWidgetType?.let { widgetType ->
            data.latest()?.program_id?.let {
                analyticsService.trackAlertLinkOpened(
                    currentWidgetId,
                    it,
                    linkUrl,
                    currentWidgetType
                )
            }
            data.latest()?.program_id?.let {
                analyticsService.trackWidgetInteraction(
                    widgetType.toAnalyticsString(),
                    currentWidgetId,
                    it,
                    interactionData
                )
            }
        }
    }

    fun trackPlayStarted() {
        data.latest()?.program_id?.let {
            currentWidgetType?.toAnalyticsString()?.let { widgetType ->
                analyticsService.trackVideoAlertPlayed(
                    widgetType,
                    currentWidgetId,
                    it,
                    data.latest()?.videoUrl.toString()
                )
            }
        }
    }

    override fun finish() {
        onDismiss()
        cleanup()
    }

    override fun onClear() {
        cleanup()
    }
}
