package com.livelike.engagementsdk.core.services.messaging.proxies

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Resource

/**
 *Filtering out the widgets which have been interacted or not supported on android.
 **/
internal class FilteringWidgetsMessagingClient(
    upstream: MessagingClient
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
        try {
            logDebug { "Message received at FilterWidgetMessagingClient" }
            val widgetType = WidgetType.fromString(event.message.get("event").asString ?: "")
            val payload = event.message.get("payload").asJsonObject
            val resource = gson.fromJson(payload, Resource::class.java) ?: null

            resource?.let {
                when (widgetType) {
                    WidgetType.IMAGE_PREDICTION_FOLLOW_UP -> {
                      /*  if (getWidgetPredictionVotedAnswerIdOrEmpty(resource.image_prediction_id).isNotEmpty()) {
                            listener?.onClientMessageEvent(client, event)
                        } else {
                            // Do nothing, filter this event
                        }*/
                        listener?.onClientMessageEvent(client, event)
                    }
                    WidgetType.TEXT_PREDICTION_FOLLOW_UP -> {
                      /*  if (getWidgetPredictionVotedAnswerIdOrEmpty(resource.text_prediction_id).isNotEmpty()) {
                            listener?.onClientMessageEvent(client, event)
                        } else {
                            // Do nothing, filter this event
                        }*/
                        // commented this, since follow up should be received even when user doesn't interact
                        listener?.onClientMessageEvent(client, event)
                    }
                    else -> {
                        if (widgetType != null) {
                            listener?.onClientMessageEvent(client, event)
                        } else {
                            // Do nothing, filter this event
                        }
                    }
                }
            }
        } catch (e: IllegalStateException) {
            logError { e.message }
            listener?.onClientMessageEvent(client, event)
        }
    }
}

internal fun MessagingClient.filter(): FilteringWidgetsMessagingClient {
    return FilteringWidgetsMessagingClient(this)
}
