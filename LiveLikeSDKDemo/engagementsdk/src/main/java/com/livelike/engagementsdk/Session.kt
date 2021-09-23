package com.livelike.engagementsdk

import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LeaderboardClient
import com.livelike.engagementsdk.core.data.models.RewardItem
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.data.models.PredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.data.models.WidgetUserInteractionBase
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate

/**
 *  Represents a Content Session which LiveLike uses to deliver widgets and associate user with the Chat
 *  component.
 */
interface LiveLikeContentSession {

    /** The analytics services **/
    val chatSession: LiveLikeChatSession

    var contentSessionleaderBoardDelegate: LeaderBoardDelegate?

    val widgetStream: Stream<LiveLikeWidget>

//    /** All the new incoming widgets on current session will be published on this stream */
//    val widgetStream : Stream<LiveLikeWidget>

    /** Pause the current Chat and widget sessions. This generally happens when ads are presented */
    fun pause()

    /** Resume the current Chat and widget sessions. This generally happens when ads are completed */
    fun resume()

    /** Closes the current session.*/
    fun close()

    /** Return the playheadTime for this session.*/
    fun getPlayheadTime(): EpochTime

    /** Return the content Session Id (Program Id) for this session.*/
    fun contentSessionId(): String

    /** Set the widget container. Recommended to use widgetView.SetSession(session) instead.*/
    fun setWidgetContainer(
        widgetView: FrameLayout,
        widgetViewThemeAttributes: WidgetViewThemeAttributes = WidgetViewThemeAttributes()
    )

    /** Set the user profile pic. to be shown in chatting*/
    fun setProfilePicUrl(url: String?)

    /** Intercepts the widgets and hold them until show() or dismiss() is being called */
    var widgetInterceptor: WidgetInterceptor?

    /** set value of style for widget **/
    fun setWidgetViewThemeAttribute(widgetViewThemeAttributes: WidgetViewThemeAttributes)

    /**
     * if the result is empty that means there is no data further and user reached end of list
     * **/
    fun getPublishedWidgets(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<LiveLikeWidget>>
    )

    /** Returns list of reward item associated to entered program */
    fun getRewardItems(): List<RewardItem>

    /** Returns list of leaderboards associated to entered program */
    fun getLeaderboardClients(
        leaderBoardId: List<String>,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    )

    /**
     * Returns list of interactions for which rewards have not been claimed */
    fun getWidgetInteractionsWithUnclaimedRewards(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<PredictionWidgetUserInteraction>>
    )

    fun getWidgetInteraction(
        widgetId: String,
        widgetKind: String,
        widgetInteractionUrl: String,
        liveLikeCallback: LiveLikeCallback<WidgetUserInteractionBase>
    )
}

/**
 * Returns the new message count whenever a unread message is being posted
 *
 */
interface MessageListener {
    fun onNewMessage(message: LiveLikeChatMessage)
    fun onHistoryMessage(messages: List<LiveLikeChatMessage>)
    fun onDeleteMessage(messageId: String)
}

/**
 * Listener to listen to the updates on ChatRoom
 */
interface ChatRoomListener {
    fun onChatRoomUpdate(chatRoom: ChatRoomInfo)
}

/**
 * Return the new widget id and kind appear on screen
 */
interface WidgetListener {
    fun onNewWidget(liveLikeWidget: LiveLikeWidget)
}

/** A simple representation of an observable stream.
 * Subscription will requires a key to avoid multiple subscription of the same observable.
 */
interface Stream<T> {
// TODO replace all usage of Stream by Flow
    /** Post data to the stream */
    fun onNext(data1: T?)

    /** Add an observable to receive future values of the stream */
    fun subscribe(key: Any, observer: (T?) -> Unit)

    /** Stop the observable at {key} from receiving events */
    fun unsubscribe(key: Any)

    /** Remove all the observable from this stream */
    fun clear()

    /** Get the latest value of the stream */
    fun latest(): T?
}

/** A representation of a widget */
class WidgetInfos(
    /** The type of the widget */
    val type: String,
    /** The data used to define the widget */
    val payload: JsonObject,
    /** The id of the widget */
    var widgetId: String
)
