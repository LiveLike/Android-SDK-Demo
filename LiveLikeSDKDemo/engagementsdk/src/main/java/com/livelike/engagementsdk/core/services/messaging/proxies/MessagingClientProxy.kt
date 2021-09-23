package com.livelike.engagementsdk.core.services.messaging.proxies

import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.core.services.messaging.Error
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.core.utils.logDebug

internal abstract class MessagingClientProxy(val upstream: MessagingClient) :
    MessagingClient,
    MessagingEventListener {

    var listener: MessagingEventListener? = null

    init {
        upstream.addMessagingEventListener(this)
    }

    override fun subscribe(channels: List<String>) {
        upstream.subscribe(channels)
    }

    override fun unsubscribe(channels: List<String>) {
        upstream.unsubscribe(channels)
    }

    override fun unsubscribeAll() {
        upstream.unsubscribeAll()
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        this.listener = listener
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        logDebug { "Message received at MessagingClientProxy" }
        listener?.onClientMessageEvent(client, event)
    }

    override fun onClientMessageError(client: MessagingClient, error: Error) {
        listener?.onClientMessageError(client, error)
    }

    override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
        listener?.onClientMessageStatus(client, status)
    }

    override fun destroy() {
        upstream.destroy()
    }
}
