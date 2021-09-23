package com.livelike.engagementsdk.widget.services.messaging.pubnub

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.MessagingClientProxy
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.extractStringOrEmpty
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.parseISODateTime
import com.pubnub.api.PubNub
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Can Replay messages by the no of counts provided if available in history, u can think of it as a Rx replay subject.
 * Need to refactor messaging client(its 2 way currently), Probably by terminology it is ok client can flow in 2 ways.
 * Need to add loggings here, refactor parsing logic(Should be high priority).
 */
internal class PubnubMessagingClientReplay(
    upstream: PubnubMessagingClient,
    var count: Int
) : MessagingClientProxy(upstream) {

    init {
        count = count.coerceAtMost(100)
    }

    private var isConnected: Boolean = false
    private var pubnub: PubNub = upstream.pubnub

    private var channelLastMessageMap = mutableMapOf<String, ClientMessage>()
    private val pendingChannelsForAddingReplay = CopyOnWriteArrayList<String>()

    override fun subscribe(channels: List<String>) {
        super.subscribe(channels)
        if (isConnected) {
            channels.forEach { channel ->
                if (!channelLastMessageMap.containsKey(channel)) {
                    fetchLastMessageFromHistoryToReplay(channel)
                } else {
                    channelLastMessageMap[channel]?.let {
                        listener?.onClientMessageEvent(this, it)
                    }
                }
            }
        } else {
            pendingChannelsForAddingReplay.addAll(channels)
        }
    }

    private fun fetchLastMessageFromHistoryToReplay(channel: String) {
        pubnub.history()
            .channel(channel)
            .count(count)
            .end(0)
            .includeTimetoken(true)
            .async { result, status ->
                result?.let {
                    result.messages.reversed().forEach {
                        val payload = it.entry.asJsonObject.getAsJsonObject("payload")
                        val timeoutReceived = payload.extractStringOrEmpty("timeout")
                        val pdtString = payload.extractStringOrEmpty("program_date_time")
                        var epochTimeMs = 0L
                        pdtString.parseISODateTime()?.let {
                            epochTimeMs = it.toInstant().toEpochMilli()
                        }
                        val timeoutMs = AndroidResource.parseDuration(timeoutReceived)

                        val clientMessage = ClientMessage(
                            it.entry.asJsonObject,
                            channel,
                            EpochTime(epochTimeMs),
                            timeoutMs
                        )
                        logDebug { "$pdtString - Received history message from pubnub: $clientMessage" }
                        listener?.onClientMessageEvent(
                            this@PubnubMessagingClientReplay,
                            clientMessage
                        )
                    }
                }
            }
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        super.onClientMessageEvent(client, event)
        channelLastMessageMap[event.channel] = event
    }

    override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
        super.onClientMessageStatus(client, status)
        isConnected = status == ConnectionStatus.CONNECTED
        if (isConnected) {
            pendingChannelsForAddingReplay.forEach { channel ->
                fetchLastMessageFromHistoryToReplay(channel)
                pendingChannelsForAddingReplay.remove(channel)
            }
        }
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun start() {
        upstream.start()
    }

    override fun onClientMessageEvents(client: MessagingClient, events: List<ClientMessage>) {
    }
}

// Think of it as adding a behaviouralSubject functionalities to the pubnub channels
internal fun PubnubMessagingClient.asBehaviourSubject(): PubnubMessagingClientReplay {
    return PubnubMessagingClientReplay(
        this,
        1
    )
}
