package com.livelike.engagementsdk.widget.timeline

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.livelike.engagementsdk.ContentSession
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.WidgetProvider
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.livelike_timeline_item.view.widget_view

internal class TimeLineViewAdapter(
    private val context: Context,
    private val sdk: EngagementSDK,
    private val timeLineViewModel: WidgetTimeLineViewModel
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var widgetViewFactory: LiveLikeWidgetViewFactory? = null
    val list: ArrayList<TimelineWidgetResource> = arrayListOf()
    var isLoadingInProgress = false
    var isEndReached = false
    var liveLikeEngagementTheme: LiveLikeEngagementTheme? = null

    var widgetTimerController: WidgetTimerController? = null

    override fun onCreateViewHolder(p0: ViewGroup, viewtype: Int): RecyclerView.ViewHolder {
        return when (viewtype) {

            VIEW_TYPE_DATA -> {
                TimeLineItemViewHolder(
                    LayoutInflater.from(p0.context).inflate(
                        R.layout.livelike_timeline_item,
                        p0,
                        false
                    )
                )
            }

            VIEW_TYPE_PROGRESS -> {
                ProgressViewHolder(
                    LayoutInflater.from(p0.context).inflate(
                        R.layout.livelike_progress_item,
                        p0,
                        false
                    )
                )
            }

            else -> throw IllegalArgumentException("Different View type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == list.size - 1 && isLoadingInProgress && !isEndReached) VIEW_TYPE_PROGRESS else VIEW_TYPE_DATA
    }

    override fun onBindViewHolder(itemViewHolder: RecyclerView.ViewHolder, p1: Int) {
        if (itemViewHolder is TimeLineItemViewHolder) {
            val timelineWidgetResource = list[p1]
            // val liveLikeWidget = timelineWidgetResource.liveLikeWidget
            liveLikeEngagementTheme?.let {
                itemViewHolder.itemView.widget_view.applyTheme(it)
            }
            itemViewHolder.itemView.widget_view.enableDefaultWidgetTransition = false
            itemViewHolder.itemView.widget_view.showTimer = widgetTimerController != null
            itemViewHolder.itemView.widget_view.showDismissButton = false
            itemViewHolder.itemView.widget_view.widgetViewFactory = widgetViewFactory
            displayWidget(itemViewHolder, timelineWidgetResource)
            itemViewHolder.itemView.widget_view.setState(
                maxOf(
                    timelineWidgetResource.widgetState,
                    itemViewHolder.itemView.widget_view.getCurrentState() ?: WidgetStates.READY
                )
            )
        }
    }

    private fun displayWidget(
        itemViewHolder: RecyclerView.ViewHolder,
        timelineWidgetResource: TimelineWidgetResource
    ) {

        val liveLikeWidget = timelineWidgetResource.liveLikeWidget
        val widgetResourceJson =
            JsonParser.parseString(GsonBuilder().create().toJson(liveLikeWidget)).asJsonObject
        var widgetType = widgetResourceJson.get("kind").asString
        widgetType = if (widgetType.contains("follow-up")) {
            "$widgetType-updated"
        } else {
            "$widgetType-created"
        }
        val widgetId = widgetResourceJson["id"].asString
        itemViewHolder.itemView.widget_view?.run {
            // TODO segregate widget view and viewmodel creation
            val widgetView = WidgetProvider()
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
                    (timeLineViewModel.contentSession as ContentSession).widgetInteractionRepository
                )
            timeLineViewModel.widgetViewModelCache[widgetId]?.let {
                widgetView?.widgetViewModel = it
            }
            timeLineViewModel.widgetViewModelCache[widgetId] = widgetView?.widgetViewModel
            widgetView?.widgetViewModel?.showDismissButton = false
            widgetView?.let { view ->
                displayWidget(widgetType, view)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    override fun getItemId(position: Int): Long {
        return list[position].liveLikeWidget.id.hashCode().toLong()
    }

    companion object {
        private const val VIEW_TYPE_DATA = 0
        private const val VIEW_TYPE_PROGRESS = 1 // for load more // progress view type
    }
}

class TimeLineItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

class ProgressViewHolder(view: View) : RecyclerView.ViewHolder(view)

data class TimelineWidgetResource(
    var widgetState: WidgetStates,
    val liveLikeWidget: LiveLikeWidget,
    var apiSource: WidgetApiSource // this has been added to show/hide animation . if real time widget animation will be shown else not
)
