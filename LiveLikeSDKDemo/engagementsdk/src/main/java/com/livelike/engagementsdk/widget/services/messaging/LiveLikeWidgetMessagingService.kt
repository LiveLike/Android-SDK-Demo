package com.livelike.engagementsdk.widget.services.messaging

import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.core.services.messaging.Error
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubMessagingClient

internal object LiveLikeWidgetMessagingService {

    private var messagingClient: MessagingClient? = null
    private var widgetEventStream: Stream<ClientMessage> = SubscriptionManager()
    private val channelSubscribeCountMap = mutableMapOf<String, Int>()

    private fun initMessagingClient(
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        currentUserStream: Stream<LiveLikeUser>
    ) {
        if (messagingClient == null) {
            messagingClient = PubnubMessagingClient.getInstance(
                sdkConfiguration.pubNubKey,
                currentUserStream.latest()?.id,
                sdkConfiguration.pubnubHeartbeatInterval,
                sdkConfiguration.pubnubPresenceTimeout
            )
            messagingClient?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    widgetEventStream.onNext(event)
                }

                override fun onClientMessageEvents(
                    client: MessagingClient,
                    events: List<ClientMessage>
                ) {
                }

                override fun onClientMessageError(client: MessagingClient, error: Error) {
                }

                override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
                }
            })
        }
    }

    internal fun subscribeWidgetChannel(
        channelName: String,
        key: Any,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        currentUserStream: Stream<LiveLikeUser>,
        observer: (ClientMessage?) -> Unit
    ) {
        initMessagingClient(sdkConfiguration, currentUserStream)
        channelSubscribeCountMap[channelName] = (channelSubscribeCountMap[channelName] ?: 0) + 1
        messagingClient?.subscribe(mutableListOf(channelName))
        widgetEventStream?.subscribe(key, observer)
    }

    internal fun unsubscribeWidgetChannel(
        channelName: String,
        key: Any
    ) {
        widgetEventStream?.unsubscribe(key)
        channelSubscribeCountMap[channelName] = (channelSubscribeCountMap[channelName] ?: 0) - 1
        if ((channelSubscribeCountMap[channelName] ?: 0) <= 0) {
            messagingClient?.unsubscribe(mutableListOf(channelName))
        }
    }
}
