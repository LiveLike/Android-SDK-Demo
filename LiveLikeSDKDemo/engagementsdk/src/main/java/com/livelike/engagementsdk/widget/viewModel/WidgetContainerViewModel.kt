package com.livelike.engagementsdk.widget.viewModel

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.gson.JsonPrimitive
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.util.SwipeDismissTouchListener
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.livelike.engagementsdk.widget.widgetModel.FollowUpWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PredictionWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.TextAskWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.VideoAlertWidgetModel

// TODO remove view references from this view model, also clean content session for same.

class WidgetContainerViewModel(val currentWidgetViewStream: Stream<Pair<String, SpecifiedWidgetView?>?>) {

    private lateinit var currentWidgetId: String
    private lateinit var currentWidgetType: String
    var enableDefaultWidgetTransition: Boolean = true
        set(value) {
            field = value
            if (value) {
                widgetContainer?.setOnTouchListener(
                    swipeDismissTouchListener
                )
            } else {
                widgetContainer?.setOnTouchListener(null)
            }
        }
    var showTimer: Boolean = true
    internal var showDismissButton: Boolean = true

    var widgetLifeCycleEventsListener: WidgetLifeCycleEventsListener? = null
    private lateinit var widgetViewThemeAttributes: WidgetViewThemeAttributes
    private var dismissWidget: ((action: DismissAction) -> Unit)? = null
    private var widgetContainer: FrameLayout? = null
    var analyticsService: AnalyticsService? = null
    private lateinit var programId: String

    // Swipe to dismiss
    var swipeDismissTouchListener: View.OnTouchListener? = null

    var widgetViewViewFactory: LiveLikeWidgetViewFactory? = null
    var isLayoutTransitionEnabled: Boolean? = null

    @SuppressLint("ClickableViewAccessibility")
    fun setWidgetContainer(
        widgetContainer: FrameLayout,
        widgetViewThemeAttributes: WidgetViewThemeAttributes
    ) {
        this.widgetContainer = widgetContainer
        this.widgetViewThemeAttributes = widgetViewThemeAttributes
        swipeDismissTouchListener = SwipeDismissTouchListener(
            widgetContainer,
            null,
            object : SwipeDismissTouchListener.DismissCallbacks {
                override fun canDismiss(token: Any?): Boolean {
                    return true
                }

                override fun onDismiss(view: View?, token: Any?) {
                    if (dismissWidget == null) {
                        analyticsService?.trackWidgetDismiss(
                            currentWidgetType, currentWidgetId, programId, null, null,
                            DismissAction.SWIPE
                        )
                    } else {
                        dismissWidget?.invoke(DismissAction.SWIPE)
                    }
                    if (currentWidgetViewStream.latest() != null) {
                        currentWidgetViewStream.onNext(null)
                    }
                    dismissWidget = null
                    removeViews()
                }
            }
        )
        if (enableDefaultWidgetTransition) {
            widgetContainer.setOnTouchListener(
                swipeDismissTouchListener
            )
        }
        currentWidgetViewStream.subscribe(WidgetContainerViewModel::class.java) { pair ->
            if (pair != null)
                widgetObserver(pair?.second, pair?.first)
            else {
                removeViews()
            }
        }
        // Show / Hide animation
        // changes because of ES-1572
        if (isLayoutTransitionEnabled!!) {
            widgetContainer.layoutTransition = LayoutTransition()
        }
    }

    private fun widgetObserver(widgetView: SpecifiedWidgetView?, widgetType: String?) {
        removeViews()
        var customView: View? = null

        if (WidgetType.fromString(widgetType!!) == WidgetType.TEXT_PREDICTION_FOLLOW_UP ||
            WidgetType.fromString(widgetType!!) == WidgetType.IMAGE_PREDICTION_FOLLOW_UP
        ) {
            customView =
                widgetViewViewFactory?.createPredictionFollowupWidgetView(
                    widgetView?.widgetViewModel as FollowUpWidgetViewModel,
                    WidgetType.fromString(widgetType!!) == WidgetType.IMAGE_PREDICTION_FOLLOW_UP
                )
        }

        if (customView == null) {
            when (widgetView?.widgetViewModel) {
                is CheerMeterWidgetmodel -> {
                    customView =
                        widgetViewViewFactory?.createCheerMeterView(widgetView.widgetViewModel as CheerMeterWidgetmodel)
                }
                is AlertWidgetModel -> {
                    customView =
                        widgetViewViewFactory?.createAlertWidgetView(widgetView.widgetViewModel as AlertWidgetModel)
                }
                is QuizWidgetModel -> {
                    customView =
                        widgetViewViewFactory?.createQuizWidgetView(
                            widgetView.widgetViewModel as QuizWidgetModel,
                            WidgetType.fromString(widgetType!!) == WidgetType.IMAGE_QUIZ
                        )
                }
                is PredictionWidgetViewModel -> {
                    customView =
                        widgetViewViewFactory?.createPredictionWidgetView(
                            widgetView.widgetViewModel as PredictionWidgetViewModel,
                            WidgetType.fromString(widgetType!!) == WidgetType.IMAGE_PREDICTION
                        )
                }
                is PollWidgetModel -> {
                    customView =
                        widgetViewViewFactory?.createPollWidgetView(
                            widgetView.widgetViewModel as PollWidgetModel,
                            WidgetType.fromString(widgetType!!) == WidgetType.IMAGE_POLL
                        )
                }
                is ImageSliderWidgetModel -> {
                    customView =
                        widgetViewViewFactory?.createImageSliderWidgetView(
                            widgetView.widgetViewModel as ImageSliderWidgetModel
                        )
                }

                is VideoAlertWidgetModel -> {
                    customView =
                        widgetViewViewFactory?.createVideoAlertWidgetView(
                            widgetView.widgetViewModel as VideoAlertWidgetModel
                        )
                }

                is TextAskWidgetModel -> {
                    customView =
                        widgetViewViewFactory?.createTextAskWidgetView(
                            widgetView.widgetViewModel as TextAskWidgetModel
                        )
                }
            }
        }
        if (customView != null) {
            displayWidget(customView)
        } else if (widgetView != null) {
            widgetView.widgetViewModel?.enableDefaultWidgetTransition =
                enableDefaultWidgetTransition
            widgetView.widgetViewModel?.showTimer = showTimer
            widgetView.widgetViewModel?.showDismissButton = showDismissButton
            displayWidget(widgetView)
        }
        if (widgetContainer != null) {
            widgetView?.widgetId?.let { widgetId ->
                var linkUrl: String? = null

                if (widgetView.widgetInfos.payload.get("link_url") is JsonPrimitive) {
                    linkUrl = widgetView.widgetInfos.payload.get("link_url")?.asString
                }

                if (widgetView.widgetInfos.payload.get("program_id") is JsonPrimitive) {
                    programId = widgetView.widgetInfos.payload.get("program_id").asString
                }

                currentWidgetType = WidgetType.fromString(
                    widgetType ?: ""
                )?.toAnalyticsString() ?: ""
                currentWidgetId = widgetId
                analyticsService?.trackWidgetDisplayed(
                    currentWidgetType, widgetId, programId,
                    linkUrl
                )
            }
        }
    }

    private fun displayWidget(view: View) {

        if (view is SpecifiedWidgetView) {
            dismissWidget = view.dismissFunc
            view.widgetViewThemeAttributes.apply {
                widgetWinAnimation = widgetViewThemeAttributes.widgetWinAnimation
                widgetLoseAnimation = widgetViewThemeAttributes.widgetLoseAnimation
                widgetDrawAnimation = widgetViewThemeAttributes.widgetDrawAnimation
            }
            view.widgetLifeCycleEventsListener = widgetLifeCycleEventsListener
            logDebug { "NOW - Show WidgetInfos" }
        }

        (view.parent as ViewGroup?)?.removeAllViews() // Clean the view parent in case of reuse
        widgetContainer?.addView(view)
    }

    internal fun removeViews() {
        logDebug { "NOW - Dismiss WidgetInfos" }
        widgetContainer?.removeAllViews()
        widgetContainer?.apply {
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                !isInLayout
            } else {
                    true
                }
            ) requestLayout()
        }
    }
}
