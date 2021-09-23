package com.livelike.engagementsdk.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.FontFamilyProvider
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.WidgetType.ALERT
import com.livelike.engagementsdk.widget.WidgetType.CHEER_METER
import com.livelike.engagementsdk.widget.WidgetType.COLLECT_BADGE
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_POLL
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_PREDICTION
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_PREDICTION_FOLLOW_UP
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_QUIZ
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_SLIDER
import com.livelike.engagementsdk.widget.WidgetType.POINTS_TUTORIAL
import com.livelike.engagementsdk.widget.WidgetType.SOCIAL_EMBED
import com.livelike.engagementsdk.widget.WidgetType.TEXT_ASK
import com.livelike.engagementsdk.widget.WidgetType.TEXT_POLL
import com.livelike.engagementsdk.widget.WidgetType.TEXT_PREDICTION
import com.livelike.engagementsdk.widget.WidgetType.TEXT_PREDICTION_FOLLOW_UP
import com.livelike.engagementsdk.widget.WidgetType.TEXT_QUIZ
import com.livelike.engagementsdk.widget.WidgetType.VIDEO_ALERT
import com.livelike.engagementsdk.widget.data.models.Badge
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.view.AlertWidgetView
import com.livelike.engagementsdk.widget.view.CheerMeterView
import com.livelike.engagementsdk.widget.view.CollectBadgeWidgetView
import com.livelike.engagementsdk.widget.view.EmojiSliderWidgetView
import com.livelike.engagementsdk.widget.view.PollView
import com.livelike.engagementsdk.widget.view.PredictionView
import com.livelike.engagementsdk.widget.view.QuizView
import com.livelike.engagementsdk.widget.view.SocialEmbedWidgetView
import com.livelike.engagementsdk.widget.view.TextAskView
import com.livelike.engagementsdk.widget.view.components.EggTimerCloseButtonView
import com.livelike.engagementsdk.widget.view.components.PointsTutorialView
import com.livelike.engagementsdk.widget.view.components.VideoAlertWidgetView
import com.livelike.engagementsdk.widget.viewModel.AlertWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.CheerMeterViewModel
import com.livelike.engagementsdk.widget.viewModel.CollectBadgeWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.EmojiSliderWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.PointTutorialWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.PollViewModel
import com.livelike.engagementsdk.widget.viewModel.PredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.QuizViewModel
import com.livelike.engagementsdk.widget.viewModel.SocialEmbedViewModel
import com.livelike.engagementsdk.widget.viewModel.TextAskViewModel
import com.livelike.engagementsdk.widget.viewModel.VideoWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.atom_widget_tag_view.view.tagTextView
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.tagView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.txtTitleBackground
import java.util.Calendar
import kotlin.math.min

internal class WidgetProvider {
    fun get(
        widgetMessagingClient: WidgetManager? = null,
        widgetInfos: WidgetInfos,
        context: Context,
        analyticsService: AnalyticsService,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        onDismiss: () -> Unit,
        userRepository: UserRepository,
        programRepository: ProgramRepository? = null,
        animationEventsStream: SubscriptionManager<ViewAnimationEvents>,
        widgetThemeAttributes: WidgetViewThemeAttributes,
        liveLikeEngagementTheme: LiveLikeEngagementTheme?,
        widgetInteractionRepository: WidgetInteractionRepository? = null
    ): SpecifiedWidgetView? {
        val specifiedWidgetView = when (WidgetType.fromString(widgetInfos.type)) {
            ALERT -> AlertWidgetView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = AlertWidgetViewModel(widgetInfos, analyticsService, onDismiss)
            }

            VIDEO_ALERT -> VideoAlertWidgetView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = VideoWidgetViewModel(widgetInfos, analyticsService, onDismiss)
            }

            TEXT_QUIZ, IMAGE_QUIZ -> QuizView(context).apply {
                widgetViewThemeAttributes = widgetThemeAttributes
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = QuizViewModel(
                    widgetInfos,
                    analyticsService,
                    sdkConfiguration,
                    context,
                    onDismiss,
                    userRepository,
                    programRepository,
                    widgetMessagingClient,
                    widgetInteractionRepository
                )
            }

            IMAGE_PREDICTION, IMAGE_PREDICTION_FOLLOW_UP,
            TEXT_PREDICTION, TEXT_PREDICTION_FOLLOW_UP -> PredictionView(context).apply {
                widgetViewThemeAttributes = widgetThemeAttributes
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = PredictionViewModel(
                    widgetInfos,
                    context,
                    analyticsService,
                    sdkConfiguration,
                    onDismiss,
                    userRepository,
                    programRepository,
                    widgetMessagingClient,
                    widgetInteractionRepository
                )
            }
            TEXT_POLL, IMAGE_POLL -> PollView(context).apply {
                widgetViewThemeAttributes = widgetThemeAttributes
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = PollViewModel(
                    widgetInfos,
                    analyticsService,
                    sdkConfiguration,
                    onDismiss,
                    userRepository,
                    programRepository,
                    widgetMessagingClient,
                    widgetInteractionRepository
                )
            }
            POINTS_TUTORIAL -> PointsTutorialView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = PointTutorialWidgetViewModel(
                    onDismiss,
                    analyticsService,
                    programRepository?.rewardType ?: RewardsType.NONE,
                    programRepository?.programGamificationProfileStream?.latest()
                )
            }
            COLLECT_BADGE -> CollectBadgeWidgetView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = CollectBadgeWidgetViewModel(
                    gson.fromJson(
                        widgetInfos.payload,
                        Badge::class.java
                    ),
                    onDismiss, analyticsService, animationEventsStream
                )
            }
            CHEER_METER -> CheerMeterView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewThemeAttributes = widgetThemeAttributes
                widgetViewModel = CheerMeterViewModel(
                    widgetInfos,
                    analyticsService,
                    sdkConfiguration,
                    onDismiss,
                    userRepository,
                    programRepository,
                    widgetMessagingClient,
                    widgetInteractionRepository
                )
            }
            IMAGE_SLIDER -> EmojiSliderWidgetView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = EmojiSliderWidgetViewModel(
                    widgetInfos, analyticsService, sdkConfiguration, onDismiss,
                    userRepository, programRepository, widgetMessagingClient,
                    widgetInteractionRepository
                )
            }
            SOCIAL_EMBED -> SocialEmbedWidgetView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = SocialEmbedViewModel(
                    widgetInfos, analyticsService, onDismiss
                )
            }

            TEXT_ASK -> TextAskView(context).apply {
                widgetViewThemeAttributes = widgetThemeAttributes
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = TextAskViewModel(
                    widgetInfos,
                    analyticsService,
                    sdkConfiguration,
                    onDismiss,
                    userRepository,
                    programRepository,
                    widgetMessagingClient,
                    widgetInteractionRepository
                )
            }
            else -> null
        }
        logDebug { "Widget created from provider, type: ${WidgetType.fromString(widgetInfos.type)}" }
        specifiedWidgetView?.widgetId = widgetInfos.widgetId
        specifiedWidgetView?.widgetInfos = widgetInfos
        return specifiedWidgetView
    }
}

abstract class SpecifiedWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    internal var fontFamilyProvider: FontFamilyProvider? = null

    // initially it will be false, when widget moves to interaction state it will be turned on to show it to user in result state
    protected var showResultAnimation: Boolean = false

    var widgetId: String = ""
    lateinit var widgetInfos: WidgetInfos
    open var widgetViewModel: BaseViewModel? = null
    open var dismissFunc: ((action: DismissAction) -> Unit)? = null
    open var widgetViewThemeAttributes: WidgetViewThemeAttributes = WidgetViewThemeAttributes()
    open var widgetsTheme: WidgetsTheme? = null

    var widgetLifeCycleEventsListener: WidgetLifeCycleEventsListener? = null

    lateinit var widgetData: LiveLikeWidgetEntity


    init {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        orientation = VERTICAL
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetData =
            gson.fromJson(widgetInfos.payload.toString(), LiveLikeWidgetEntity::class.java)
        postDelayed(
            {
                widgetData.height = height
                widgetLifeCycleEventsListener?.onWidgetPresented(widgetData)
            },
            500
        )
        subscribeWidgetStateAndPublishToLifecycleListener()
    }
/**
 * would inflate and add sponsor ui as a widget view footer if sponsor exists
 */
    protected fun wouldInflateSponsorUi() {
        widgetData.sponsors?.let {
            if(it.isNotEmpty()){
                val sponsor = it[0]
                val sponsorView = inflate(context, R.layout.default_sponsor_ui, null)
                addView(sponsorView)
                val sponsorImageView = sponsorView.findViewById<ImageView>(R.id.sponsor_iv)
                Glide.with(context).load(sponsor.logoUrl).into(sponsorImageView)
            }
        }
    }

    private fun subscribeWidgetStateAndPublishToLifecycleListener() {
        widgetViewModel?.widgetState?.subscribe(this) {
            it?.let {
                widgetLifeCycleEventsListener?.onWidgetStateChange(it, widgetData)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        widgetViewModel?.widgetState?.unsubscribe(this)
        widgetLifeCycleEventsListener?.onWidgetDismissed(widgetData)
    }

    fun onWidgetInteractionCompleted() {
        widgetLifeCycleEventsListener?.onWidgetInteractionCompleted(widgetData)
    }

    internal fun showTimer(
        time: String,
        v: EggTimerCloseButtonView?,
        onUpdate: (Float) -> Unit,
        dismissAction: (action: DismissAction) -> Unit
    ) {
        if (widgetViewModel?.showTimer == false) {
            v?.visibility = View.GONE
            return
        }

        var animationLength = AndroidResource.parseDuration(time).toFloat()
        var remainingAnimationLength = animationLength
        if (widgetViewModel?.timerStartTime != null) {
            remainingAnimationLength = animationLength - (Calendar.getInstance().timeInMillis - (widgetViewModel?.timerStartTime ?: 0)).toFloat()
        } else {
            widgetViewModel?.timerStartTime = Calendar.getInstance().timeInMillis
        }
        val animationEggTimerProgress = (animationLength - remainingAnimationLength) / animationLength

        if ((animationEggTimerProgress ?: 0f) < 1f) {
            animationEggTimerProgress?.let {
                v?.startAnimationFrom(
                    it,
                    remainingAnimationLength,
                    onUpdate,
                    dismissAction,
                    widgetViewModel?.showDismissButton ?: true
                )
            }
        }
    }

    protected fun applyThemeOnTitleView(it: WidgetBaseThemeComponent) {
        titleView?.componentTheme = it.title
        AndroidResource.updateThemeForView(titleTextView, it.title, fontFamilyProvider)
        if (it.header?.background != null) {
            txtTitleBackground?.background = AndroidResource.createDrawable(it.header)
        }
        AndroidResource.setPaddingForView(txtTitleBackground, it.header?.padding)
    }

    /**
     * this method in used to apply theme on tag view
     **/
    protected fun applyThemeOnTagView(it: WidgetBaseThemeComponent){
        tagView?.componentTheme = it.tag
        AndroidResource.updateThemeForView(tagTextView, it.tag, fontFamilyProvider)
    }


    /**
     * this method in used to set tag view with style changes (default appearance)
     **/
    protected fun setTagViewWithStyleChanges(tag: String) {
        if(tag.isNotEmpty()){
            tagView.tag = tag
            tagView.visibility = View.VISIBLE
            AndroidResource.updateDefaultThemeForTagView(titleTextView,titleView)
        }else{
            tagView.visibility = View.GONE
            titleTextView.isAllCaps = true
        }
    }

    /**
     * override this method in respective widgets to respect runtime unified json theme updation
     **/
    open fun applyTheme(theme: WidgetsTheme) {
        widgetsTheme = theme
    }

    fun applyTheme(theme: LiveLikeEngagementTheme) {
        fontFamilyProvider = theme.fontFamilyProvider
        applyTheme(theme.widgets)
    }

    open fun getCurrentState(): WidgetStates? {
        return widgetViewModel?.widgetState?.latest()
    }

    open fun setState(widgetStates: WidgetStates) {
        val nextStateOrdinal = widgetStates.ordinal
        widgetViewModel?.widgetState?.onNext(
            WidgetStates.values()[
                min(
                    nextStateOrdinal,
                    WidgetStates.FINISHED.ordinal
                )
            ]
        )
    }

    open fun moveToNextState() {
        val nextStateOrdinal = (widgetViewModel?.widgetState?.latest()?.ordinal ?: 0) + 1
        widgetViewModel?.widgetState?.onNext(
            WidgetStates.values()[
                min(
                    nextStateOrdinal,
                    WidgetStates.FINISHED.ordinal
                )
            ]
        )
    }
}
