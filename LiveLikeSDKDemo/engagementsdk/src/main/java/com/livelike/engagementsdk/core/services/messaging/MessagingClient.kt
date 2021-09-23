package com.livelike.engagementsdk.core.services.messaging

import com.google.gson.JsonObject
import com.livelike.engagementsdk.EpochTime

/**
 *  Represents a messaging client which LiveLike uses to communicate with a widget or chat backend source
 */
internal interface MessagingClient {
    fun subscribe(channels: List<String>)
    fun unsubscribe(channels: List<String>)
    fun unsubscribeAll()
    fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime)
    fun stop()
    fun start()
    fun destroy()
    fun addMessagingEventListener(listener: MessagingEventListener)
}

/**
 *  Represents a messaging client triggerListener which will receive MessagingClient messages
 */
internal interface MessagingEventListener {
    // TODO: future task to update code to have 1 onClientMessageEvent method only
    fun onClientMessageEvent(client: MessagingClient, event: ClientMessage)
    fun onClientMessageEvents(client: MessagingClient, events: List<ClientMessage>)
    fun onClientMessageError(client: MessagingClient, error: Error)
    fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus)
}

/**
 * Represents a client message that can be sent from a MessagingClient
 */
internal data class ClientMessage(
    val message: JsonObject,
    val channel: String = "",
    val timeStamp: EpochTime = EpochTime(0),
    val timeoutMs: Long = 4000
)

/**
 * Represents a MessagingClient error that can be sent from a MessagingClient
 */
internal data class Error(val type: String, val message: String)

/**
 * Represents the ConnectionStatus of a MessagingClient
 */
internal enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED
}
