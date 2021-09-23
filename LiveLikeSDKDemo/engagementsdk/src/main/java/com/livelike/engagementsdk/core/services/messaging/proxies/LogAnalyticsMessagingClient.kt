package com.livelike.engagementsdk.core.services.messaging.proxies

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.utils.logDebug

/**
 * Meessaging Proxy/Pipe for adding analytics for our widgets received.
 */
internal class LogAnalyticsMessagingClient(
    upstream: MessagingClient,
    val analyticsService: AnalyticsService
) :
    MessagingClientProxy(upstream) {
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

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        logDebug { "Message received at LogAnalyticsMessagingClient" }
        listener?.onClientMessageEvent(client, event)
    }
}

internal fun MessagingClient.logAnalytics(
    analyticsService: AnalyticsService
): LogAnalyticsMessagingClient {
    return LogAnalyticsMessagingClient(this, analyticsService)
}
