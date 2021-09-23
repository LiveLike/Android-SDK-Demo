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
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AlertWidgetViewModel(
    val widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    private val onDismiss: () -> Unit
) : BaseViewModel(analyticsService), AlertWidgetModel {
    private var timeoutStarted = false
    var data: SubscriptionManager<Alert?> =
        SubscriptionManager()

    private var currentWidgetId: String = ""
    private var programId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()

    init {
        data.onNext(gson.fromJson(widgetInfos.payload.toString(), Alert::class.java) ?: null)
        widgetState.onNext(WidgetStates.READY)
        interactionData.widgetDisplayed()
        currentWidgetId = widgetInfos.widgetId
        programId = data?.currentData?.program_id.toString()
        currentWidgetType = WidgetType.fromString(widgetInfos.type)
    }

    fun onClickLink(linkUrl: String) {
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
        logDebug { "dismiss Alert Widget, reason:${action.name}" }
        onDismiss()
        cleanup()
        viewModelJob.cancel()
    }

    override fun finish() {
        onDismiss()
        cleanup()
    }

    override fun markAsInteractive() {
        trackWidgetBecameInteractive(currentWidgetType, currentWidgetId, programId)
    }

    override fun alertLinkClicked(url: String) {
        onClickLink(url)
        data.latest()?.program_id?.let {
            trackWidgetEngagedAnalytics(
                currentWidgetType, currentWidgetId,
                it
            )
        }
    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)

    fun startDismissTimout(timeout: String, onDismiss: () -> Unit) {
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

    override fun onClear() {
        cleanup()
    }
}
