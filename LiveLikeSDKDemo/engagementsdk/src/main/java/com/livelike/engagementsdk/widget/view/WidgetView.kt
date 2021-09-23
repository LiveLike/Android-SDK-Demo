package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.livelike.engagementsdk.ContentSession
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.WidgetListener
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetProvider
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.viewModel.WidgetContainerViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetView(context: Context, private val attr: AttributeSet) : FrameLayout(context, attr) {

    internal var engagementSDKTheme: LiveLikeEngagementTheme? = null
    internal var widgetContainerViewModel: WidgetContainerViewModel? =
        WidgetContainerViewModel(SubscriptionManager())
    internal val widgetViewThemeAttributes = WidgetViewThemeAttributes()
    var widgetLifeCycleEventsListener: WidgetLifeCycleEventsListener? = null
        set(value) {
            field = value
            widgetContainerViewModel?.widgetLifeCycleEventsListener = value
        }

    var enableDefaultWidgetTransition = true
        set(value) {
            field = value
            widgetContainerViewModel?.enableDefaultWidgetTransition = value
        }
    var showTimer = true
        set(value) {
            field = value
            widgetContainerViewModel?.showTimer = value
        }

    /**
     * this flag is used to configure visibility of cancel/dismiss button on default widgets
     **/
    var showDismissButton = true
        set(value) {
            field = value
            widgetContainerViewModel?.showDismissButton = value
        }

    init {
        context.obtainStyledAttributes(
            attr,
            R.styleable.WidgetView,
            0, 0
        ).apply {
            try {
                widgetViewThemeAttributes.init(this)
            } finally {
                recycle()
            }
        }
        widgetContainerViewModel?.isLayoutTransitionEnabled =
            context.resources.getBoolean(R.bool.livelike_widget_component_layout_transition_enabled)
        widgetContainerViewModel?.setWidgetContainer(this, widgetViewThemeAttributes)
    }

    private var session: LiveLikeContentSession? = null

    var widgetViewFactory: LiveLikeWidgetViewFactory? = null
        set(value) {
            widgetContainerViewModel?.widgetViewViewFactory = value
            field = value
        }

    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    fun setSession(session: LiveLikeContentSession) {
        this.session = session
        (session as ContentSession?)?.isSetSessionCalled = true
        session.setWidgetViewThemeAttribute(widgetViewThemeAttributes)
        widgetContainerViewModel?.currentWidgetViewStream?.unsubscribe(WidgetContainerViewModel::class.java)
        session.setWidgetContainer(this, widgetViewThemeAttributes)
        session.analyticServiceStream.latest()
            ?.trackOrientationChange(resources.configuration.orientation == 1)
        widgetContainerViewModel = (session as ContentSession?)?.widgetContainer
        widgetContainerViewModel?.widgetLifeCycleEventsListener = widgetLifeCycleEventsListener
        widgetContainerViewModel?.widgetViewViewFactory = widgetViewFactory
        session.livelikeThemeStream.onNext(engagementSDKTheme)
    }

    /**
     * will update the value of theme to be applied for all widgets
     * This will update the theme on the current displayed widget as well
     **/
    fun applyTheme(theme: LiveLikeEngagementTheme) {
        engagementSDKTheme = theme
        (session as? ContentSession)?.livelikeThemeStream?.onNext(engagementSDKTheme)
        if (childCount == 1 && getChildAt(0) is SpecifiedWidgetView) {
            (getChildAt(0) as SpecifiedWidgetView).applyTheme(theme)
        }
    }

    /**
     * this method parse livelike theme from json object and apply if its a valid json
     * refer @applyTheme(theme)
     **/
    fun applyTheme(themeJson: JsonObject): Result<Boolean> {
        val themeResult = LiveLikeEngagementTheme.instanceFrom(themeJson)
        return if (themeResult is Result.Success) {
            applyTheme(themeResult.data)
            Result.Success(true)
        } else {
            themeResult as Result.Error
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthDp = AndroidResource.pxToDp(width)
        if (widthDp < 292 && widthDp != 0) {
            logError { "[CONFIG ERROR] Current WidgetView Width is $widthDp, it must be more than 292dp or won't display on the screen." }
            setMeasuredDimension(0, 0)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private var widgetListener: WidgetListener? = null

    @Deprecated("use widgetStream exposed in LiveLikeContentSession")
    fun setWidgetListener(widgetListener: WidgetListener) {
        this.widgetListener = widgetListener
    }

    fun displayWidget(
        sdk: EngagementSDK,
        liveLikeWidget: LiveLikeWidget,
        showWithInteractionData: Boolean = false
    ) {
        try {
            val jsonObject = GsonBuilder().create().toJson(liveLikeWidget)
            displayWidget(sdk, JsonParser.parseString(jsonObject).asJsonObject, showWithInteractionData)
        } catch (ex: JsonParseException) {
            logDebug { "Invalid json passed for displayWidget" }
            ex.printStackTrace()
        }
    }

    /** displays the widget in the container
     throws error if json invalid
     clears the previous displayed widget (if any)
     only clears if json is valid
     */
    fun displayWidget(
        sdk: EngagementSDK,
        widgetResourceJson: JsonObject,
        showWithInteractionData: Boolean = false
    ) {
        try {
            var widgetType = widgetResourceJson.get("kind").asString
            widgetType = if (widgetType.contains("follow-up")) {
                "$widgetType-updated"
            } else {
                "$widgetType-created"
            }
            val widgetId = widgetResourceJson["id"].asString
            val programId = widgetResourceJson.get("program_id").asString
            widgetContainerViewModel?.analyticsService = sdk.analyticService.latest()
            val widgetInteractionRepository = WidgetInteractionRepository(
                context, programId, sdk.userRepository,
                sdk.configurationStream.latest()?.programDetailUrlTemplate
            )
            uiScope.launch {
                if (showWithInteractionData) {
                    widgetInteractionRepository.fetchRemoteInteractions(
                        widgetId = widgetId,
                        widgetKind = widgetType
                    )
                }
                widgetContainerViewModel?.currentWidgetViewStream?.onNext(
                    Pair(
                        widgetType,
                        WidgetProvider()
                            .get(
                                null,
                                WidgetInfos(widgetType, widgetResourceJson, widgetId),
                                context,
                                sdk.analyticService.latest() ?: MockAnalyticsService(),
                                sdk.configurationStream.latest()!!,
                                {
                                    widgetContainerViewModel?.currentWidgetViewStream?.onNext(null)
                                },
                                sdk.userRepository,
                                null,
                                SubscriptionManager(),
                                widgetViewThemeAttributes,
                                engagementSDKTheme,
                                widgetInteractionRepository
                            )
                    )
                )
            }
        } catch (ex: Exception) {
            logDebug { "Invalid json passed for displayWidget" }
            ex.printStackTrace()
        }
    }

    fun displayWidget(widgetType: String, widgetView: SpecifiedWidgetView) {
        widgetContainerViewModel?.currentWidgetViewStream?.onNext(
            Pair(widgetType, widgetView)
        )
    }

    // clears the displayed widget (if any)
    fun clearWidget() {
        removeAllViews()
    }

    fun getCurrentState(): WidgetStates? {
        if (childCount == 1 && getChildAt(0) is SpecifiedWidgetView) {
            return (getChildAt(0) as SpecifiedWidgetView).getCurrentState()
        }
        return null
    }

    fun setState(widgetStates: WidgetStates) {
        if (childCount == 1 && getChildAt(0) is SpecifiedWidgetView) {
            (getChildAt(0) as SpecifiedWidgetView).setState(widgetStates)
        }
    }

    fun moveToNextState() {
        if (childCount == 1 && getChildAt(0) is SpecifiedWidgetView) {
            (getChildAt(0) as SpecifiedWidgetView).moveToNextState()
        }
    }
}
