package com.livelike.engagementsdk.widget.timeline

import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * @contentSession: object of LiveLikeContentSession
 * predicate for filtering the widgets to only specific kind of widgets
 */
open class WidgetTimeLineViewModel(
    internal val contentSession: LiveLikeContentSession,
    private val predicate: (LiveLikeWidget) -> Boolean = { _ -> true }
) : ViewModel() {

    /**
     * it contains all the widgets that has been loaded onto this timeline
     **/
    val timeLineWidgets = mutableListOf<TimelineWidgetResource>()

    /**
     * public stream of widgets that can be observed to build custom timeline view or to know source of individual widgets
     * currently there are 2 source of widgets: real-time  or history api(past widgets)
     **/
    val timeLineWidgetsStream: Stream<Pair<WidgetApiSource, List<TimelineWidgetResource>>> =
        SubscriptionManager(false)

    var decideWidgetInteractivity: DecideWidgetInteractivity? = null
    internal val widgetEventStream: Stream<String> =
        SubscriptionManager(true)

    var widgetViewModelCache = mutableMapOf<String, BaseViewModel?>()

    init {
        loadPastPublishedWidgets(LiveLikePagination.FIRST)
        observeForLiveWidgets()
    }

    /**
     * load history widgets (published)
     **/
    private fun loadPastPublishedWidgets(page: LiveLikePagination) {
        widgetEventStream.onNext(WIDGET_LOADING_STARTED)
        contentSession.getPublishedWidgets(
            page,
            object : LiveLikeCallback<List<LiveLikeWidget>>() {
                override fun onResponse(result: List<LiveLikeWidget>?, error: String?) {
                    val filteredWidgets = arrayListOf<TimelineWidgetResource>()
                    result?.let { list ->
                        val widgets = list.map {
                            TimelineWidgetResource(
                                decideWidgetInteraction(it, WidgetApiSource.HISTORY_API),
                                it, WidgetApiSource.HISTORY_API
                            )
                        }
                        filteredWidgets.addAll(widgets.filter { predicate.invoke(it.liveLikeWidget) })
                        timeLineWidgets.addAll(filteredWidgets)
                        logDebug { "timeline widget total:${timeLineWidgets.size}, filtered widget:${filteredWidgets.size}" }
                        uiScope.launch {
                            timeLineWidgetsStream.onNext(
                                Pair(
                                    WidgetApiSource.HISTORY_API,
                                    filteredWidgets
                                )
                            )
                        }
                    }
                    widgetEventStream.onNext(WIDGET_LOADING_COMPLETE)
                    // this means that published result is finished, there are no more to display
                    if (result == null && error == null) {
                        logDebug { "timeline list finished" }
                        widgetEventStream.onNext(WIDGET_TIMELINE_END)
                    } else if (filteredWidgets.isEmpty()) {
                        // to load more widget if the filtered widget is empty, a use case if user wants to show only a specific widget and it is not available in first page
                        // so it will until it reaches end or that page that contain that specific widget
                        loadPastPublishedWidgets(LiveLikePagination.NEXT)
                    }
                }
            }
        )
    }

    /**
     * this call load the next available page of past published widgets on this program.
     **/
    fun loadMore() {
        if (widgetEventStream.latest()?.equals(WIDGET_TIMELINE_END) == true) {
            return
        }
        if (widgetEventStream.latest()?.equals(WIDGET_LOADING_COMPLETE) == true) {
            loadPastPublishedWidgets(LiveLikePagination.NEXT)
        }
    }

    /**
     * observe the live (real time) widgets
     **/
    private fun observeForLiveWidgets() {
        contentSession.widgetStream.subscribe(this) {
            it?.let {
                val widget = TimelineWidgetResource(
                    decideWidgetInteraction(it, WidgetApiSource.REALTIME_API),
                    it,
                    WidgetApiSource.REALTIME_API
                )
                if (predicate.invoke(widget.liveLikeWidget)) {
                    timeLineWidgets.add(0, widget)
                    uiScope.launch {
                        timeLineWidgetsStream.onNext(
                            Pair(
                                WidgetApiSource.REALTIME_API,
                                mutableListOf(widget)
                            )
                        )
                    }
                }
            }
        }
    }

    fun wouldAllowWidgetInteraction(liveLikeWidget: LiveLikeWidget): Boolean {
        return timeLineWidgets.find { it.liveLikeWidget.id == liveLikeWidget.id }?.widgetState == WidgetStates.INTERACTING
    }

    open fun decideWidgetInteraction(
        liveLikeWidget: LiveLikeWidget,
        timeLineWidgetApiSource: WidgetApiSource
    ): WidgetStates {
        var isInteractive = false
        isInteractive = if (decideWidgetInteractivity != null) {
            decideWidgetInteractivity?.wouldAllowWidgetInteraction(liveLikeWidget) ?: false
        } else {
            timeLineWidgetApiSource == WidgetApiSource.REALTIME_API
        }
        return if (isInteractive) WidgetStates.INTERACTING else WidgetStates.RESULTS
    }

    /**
     * Call this method to close down all connections and scopes, it should be called from onClear() method
     * of android viewmodel. In case landscape not supported then onDestroy.
     */
    fun clear() {
        uiScope.cancel()
        contentSession.widgetStream.unsubscribe(this)
        widgetViewModelCache.forEach { entry ->
            entry.value?.onClear()
        }
        widgetViewModelCache.clear()
    }

    /**
     * used for timeline widget loading starting / completed
     **/
    companion object {
        const val WIDGET_LOADING_COMPLETE = "loading-complete"
        const val WIDGET_LOADING_STARTED = "loading-started"
        const val WIDGET_TIMELINE_END = "timeline-reached"
    }
}

// Timeline view will have default implementation
interface DecideWidgetInteractivity {
    //    TODO discuss with team if there is a requirement to add TimeLineWidgetApiSource as param in this function
    fun wouldAllowWidgetInteraction(widget: LiveLikeWidget): Boolean
}

enum class WidgetApiSource {
    REALTIME_API,
    HISTORY_API
}
