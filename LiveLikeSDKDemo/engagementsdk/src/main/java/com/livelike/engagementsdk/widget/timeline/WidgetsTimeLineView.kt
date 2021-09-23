package com.livelike.engagementsdk.widget.timeline

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.livelike.engagementsdk.ContentSession
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.models.WidgetUserInteractionBase
import com.livelike.engagementsdk.widget.util.SmoothScrollerLinearLayoutManager
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.livelike_timeline_view.view.loadingSpinnerTimeline
import kotlinx.android.synthetic.main.livelike_timeline_view.view.timeline_rv
import kotlinx.android.synthetic.main.livelike_timeline_view.view.timeline_snap_live
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WidgetsTimeLineView(
    context: Context,
    private val timeLineViewModel: WidgetTimeLineViewModel,
    sdk: EngagementSDK
) : FrameLayout(context) {

    private var adapter: TimeLineViewAdapter
    private var snapToLiveAnimation: AnimatorSet? = null
    private var showingSnapToLive: Boolean = false
    private var isFirstItemVisible = false
    private var autoScrollTimeline = false
    private var separator: Drawable? = null

    // The minimum amount of items to have below your current scroll position
    // before loading more.
    private val visibleThreshold = 2

    /**
     * For custom widgets to show on this timeline, set implementation of widget view factory
     * @see <a href="https://docs.livelike.com/docs/livelikewidgetviewfactory">Docs reference</a>
     **/
    var widgetViewFactory: LiveLikeWidgetViewFactory? = null
        set(value) {
            adapter.widgetViewFactory = value
            field = value
        }

    /**
     * configuring this controlled will allow to control the timer in widget
     * By default there will be no timer, interaction duration will be kept indefinite
     * to have cms defined interaction timer simple use:
     *  widgetsTimeLineView.widgetTimerController = CMSSpecifiedDurationTimer()
     **/
    var widgetTimerController: WidgetTimerController? = null
        set(value) {
            field = value
            adapter.widgetTimerController = field
        }

    /**
     * this will add custom separator/divider (drawables) between widgets in timeline
     * * @param Drawable
     **/
    fun setSeparator(customSeparator: Drawable?) {
        this.separator = customSeparator
        separator?.let {
            val itemDecoration = DividerItemDecoration(context, VERTICAL)
            itemDecoration.setDrawable(it)
            timeline_rv.addItemDecoration(itemDecoration)
        }
    }

    init {
        inflate(context, R.layout.livelike_timeline_view, this)

        // added a check based on data, since this will be causing issue during rotation of device
        if (timeLineViewModel.timeLineWidgets.isEmpty()) {
            showLoadingSpinnerForTimeline()
        }
        adapter =
            TimeLineViewAdapter(
                context,
                sdk,
                timeLineViewModel
            )
        adapter.widgetTimerController = widgetTimerController
        adapter.list.addAll(timeLineViewModel.timeLineWidgets)
        timeline_rv.layoutManager = SmoothScrollerLinearLayoutManager(context)
        timeline_rv.adapter = adapter
        initListeners()
    }

//    /**
//     * use this function to set timeline view model for the timeline view
//     * make sure to clear this timeline model when scope destroys
//     */
//    fun setTimeLineViewModel(timeLineViewModel: WidgetTimeLineViewModel) {
//        timeLineViewModel.clear()
//        this.timeLineViewModel = timeLineViewModel
//
//    }

    /**
     * will update the value of theme to be applied for all widgets in timeline
     * This will update the theme on the current displayed widget as well
     **/
    fun applyTheme(theme: LiveLikeEngagementTheme) {
        this.adapter.liveLikeEngagementTheme = theme
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscribeForTimelineWidgets()
    }

    private fun subscribeForTimelineWidgets() {
        timeLineViewModel.timeLineWidgetsStream.subscribe(this) { pair ->
            pair?.let {
                // lockInteracatedWidgetsWithoutPatchUrl(pair.second) // will remove this logic when backend adds patch_url
                lockAlreadyInteractedQuizAndEmojiSlider(pair.second)
                wouldLockPredictionWidgets(pair.second) // if follow up is received lock prediction interaction
                // changing timeout value for widgets when widgetTimerController is configured
                widgetTimerController?.run {
                    it.second.forEach { widget ->
                        if (widget.widgetState == WidgetStates.INTERACTING) {
                            widget.liveLikeWidget.timeout = this.timeValue(widget.liveLikeWidget)
                            timeLineViewModel.uiScope.launch {
                                delay(
                                    AndroidResource.parseDuration(
                                        pair.second[0].liveLikeWidget.timeout ?: ""
                                    )
                                )
                                pair.second[0]?.widgetState = WidgetStates.RESULTS
                                adapter.notifyItemChanged(adapter.list.indexOf(widget))
                            }
                        }
                    }
                }
                if (pair.first == WidgetApiSource.REALTIME_API) {
                    adapter.list.addAll(0, pair.second)
                    adapter.notifyItemInserted(0)
                    wouldRetreatToActiveWidgetPosition()
                } else {
                    adapter.list.addAll(pair.second)
                    adapter.notifyItemRangeInserted(
                        adapter.itemCount - pair.second.size,
                        adapter.itemCount
                    )
                    adapter.isLoadingInProgress = false
                }
            }
        }
    }

    private fun lockInteracatedWidgetsWithoutPatchUrl(widgets: List<TimelineWidgetResource>) {
        widgets.forEach {
            val kind = it.liveLikeWidget.kind
            if (kind?.contains(WidgetKind.PREDICTION.event) == true) {
                if ((timeLineViewModel.contentSession as ContentSession)?.widgetInteractionRepository.getWidgetInteraction<WidgetUserInteractionBase>(
                        it.liveLikeWidget.id ?: "",
                        WidgetKind.fromString(kind)
                    ) != null
                ) {
                    it.widgetState = WidgetStates.RESULTS
                }
            }
        }
    }

    private fun lockAlreadyInteractedQuizAndEmojiSlider(widgets: List<TimelineWidgetResource>) {
        widgets.forEach {
            val kind = it.liveLikeWidget.kind
            if (kind == WidgetKind.IMAGE_SLIDER.event || kind?.contains(WidgetKind.QUIZ.event) == true || kind?.contains(WidgetKind.TEXT_ASK.event) == true) {
                if ((timeLineViewModel.contentSession as ContentSession)?.widgetInteractionRepository.getWidgetInteraction<WidgetUserInteractionBase>(
                        it.liveLikeWidget.id ?: "",
                        WidgetKind.fromString(kind)
                    ) != null
                ) {
                    it.widgetState = WidgetStates.RESULTS
                }
            }
        }
    }

    private fun wouldLockPredictionWidgets(widgets: List<TimelineWidgetResource>) {
        var followUpWidgetPredictionIds = widgets.filter {
            it.liveLikeWidget.kind?.contains("follow-up") ?: false
        }.map { it.liveLikeWidget.textPredictionId ?: it.liveLikeWidget.imagePredictionId }

        widgets.forEach { widget ->
            if (followUpWidgetPredictionIds.contains(widget.liveLikeWidget.id)) {
                widget.widgetState = WidgetStates.RESULTS
            }
        }
        adapter.list.forEach { widget ->
            if (followUpWidgetPredictionIds.contains(widget.liveLikeWidget.id) && widget.widgetState == WidgetStates.INTERACTING) {
                widget.widgetState = WidgetStates.RESULTS
                adapter.notifyItemChanged(adapter.list.indexOf(widget))
            }
        }
    }

    /**
     *this will check for visible position, if it is 0 then it will scroll to top
     **/
    private fun wouldRetreatToActiveWidgetPosition() {
        val shouldRetreatToTopPosition =
            (timeline_rv.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() == 0
        if (shouldRetreatToTopPosition) {
            timeline_rv.smoothScrollToPosition(0)
        }
    }

    /**
     * view click listeners
     * snap to live added
     **/
    private fun initListeners() {
        val lm = timeline_rv.layoutManager as LinearLayoutManager
        timeline_rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
                rv: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                val firstVisible = lm.findFirstVisibleItemPosition()
                val topHasBeenReached = firstVisible == 0
                if (!autoScrollTimeline)
                    isFirstItemVisible = if (topHasBeenReached) {
                        hideSnapToLiveForWidgets()
                        true
                    } else {
                        showSnapToLiveForWidgets()
                        false
                    }
                if (topHasBeenReached) {
                    autoScrollTimeline = false
                }

                /**
                 * load more on scrolled (pagination)
                 **/
                if (!adapter.isLoadingInProgress && !adapter.isEndReached) {
                    val totalItemCount = lm.itemCount
                    val lastVisibleItem = lm.findLastVisibleItemPosition()
                    if (totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        timeLineViewModel.loadMore()
                        adapter.isLoadingInProgress = true
                    }
                }
            }
        })

        timeline_snap_live.setOnClickListener {
            autoScrollTimeline = true
            snapToLiveForTimeline()
        }

        timeLineViewModel.widgetEventStream.subscribe(javaClass.simpleName) {
            logDebug { "Widget timeline event stream : $it" }
            when (it) {

                WidgetTimeLineViewModel.WIDGET_LOADING_COMPLETE -> {
                    timeLineViewModel.uiScope.launch {
                        hideLoadingSpinnerForTimeline()
                    }
                }

                WidgetTimeLineViewModel.WIDGET_TIMELINE_END -> {
                    timeLineViewModel.uiScope.launch {
                        adapter.isEndReached = true
                        adapter.notifyItemChanged(adapter.list.size - 1)
                    }
                }

                WidgetTimeLineViewModel.WIDGET_LOADING_STARTED -> {
                    // adding this line for case if in first page the filter widget data is empty and we are loading next page as automatically
                    if (adapter.itemCount == 0) {
                        timeLineViewModel.uiScope.launch {
                            showLoadingSpinnerForTimeline()
                        }
                    }
                }
            }
        }
    }

    private fun showLoadingSpinnerForTimeline() {
        loadingSpinnerTimeline.visibility = View.VISIBLE
        timeline_rv.visibility = View.GONE
        timeline_snap_live.visibility = View.GONE
    }

    private fun hideLoadingSpinnerForTimeline() {
        loadingSpinnerTimeline.visibility = View.GONE
        timeline_rv.visibility = View.VISIBLE
    }

    /**
     * used for hiding the Snap to live button
     * snap to live is mainly responsible for showing user the latest widget
     * if user is already at the latest widget,then usually this icon remain hidden
     **/
    private fun hideSnapToLiveForWidgets() {
        logDebug { "Widget Timeline hide Snap to Live: $showingSnapToLive" }
        if (!showingSnapToLive)
            return
        showingSnapToLive = false
        timeline_snap_live.visibility = View.GONE
        animateSnapToLiveButton()
    }

    /**
     * used for showing the Snap to Live button
     **/
    private fun showSnapToLiveForWidgets() {
        logDebug { "Wdget Timeline show Snap to Live: $showingSnapToLive" }
        if (showingSnapToLive)
            return
        showingSnapToLive = true
        timeline_snap_live.visibility = View.VISIBLE
        animateSnapToLiveButton()
    }

    private fun snapToLiveForTimeline() {
        timeline_rv?.let { rv ->
            hideSnapToLiveForWidgets()
            timeLineViewModel.timeLineWidgets?.size?.let {
                timeline_rv.postDelayed(
                    {
                        rv.smoothScrollToPosition(0)
                    },
                    200
                )
            }
        }
    }

    private fun animateSnapToLiveButton() {
        snapToLiveAnimation?.cancel()

        val translateAnimation = ObjectAnimator.ofFloat(
            timeline_snap_live,
            "translationY",
            if (showingSnapToLive) 0f else AndroidResource.dpToPx(
                TIMELINE_SNAP_TO_LIVE_ANIMATION_DESTINATION
            )
                .toFloat()
        )
        translateAnimation?.duration = TIMELINE_SNAP_TO_LIVE_ANIMATION_DURATION.toLong()
        val alphaAnimation =
            ObjectAnimator.ofFloat(timeline_snap_live, "alpha", if (showingSnapToLive) 1f else 0f)
        alphaAnimation.duration = (TIMELINE_SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION).toLong()
        alphaAnimation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                timeline_snap_live.visibility = if (showingSnapToLive) View.VISIBLE else View.GONE
            }

            override fun onAnimationStart(animation: Animator) {
                timeline_snap_live.visibility = if (showingSnapToLive) View.GONE else View.VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        snapToLiveAnimation = AnimatorSet()
        snapToLiveAnimation?.play(translateAnimation)?.with(alphaAnimation)
        snapToLiveAnimation?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unsubscribeForTimelineWidgets()
    }

    private fun unsubscribeForTimelineWidgets() {
        timeLineViewModel.timeLineWidgetsStream.unsubscribe(this)
        timeLineViewModel.widgetEventStream.unsubscribe(this)
    }

    companion object {
        const val TIMELINE_SNAP_TO_LIVE_ANIMATION_DURATION = 400F
        const val TIMELINE_SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION = 320F
        const val TIMELINE_SNAP_TO_LIVE_ANIMATION_DESTINATION = 50
    }
}
