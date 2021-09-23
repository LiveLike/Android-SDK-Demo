package com.livelike.engagementsdk.chat.data.repository

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.CHAT_HISTORY_LIMIT
import com.livelike.engagementsdk.TEMPLATE_CHAT_ROOM_ID
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.UserChatRoomListResponse
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.core.data.respository.BaseRepository
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

internal class ChatRepository(
    private val subscribeKey: String,
    private val authKey: String,
    private val uuid: String,
    private val analyticsService: AnalyticsService,
    private val publishKey: String? = null,
    private val origin: String? = null,
    private val pubnubHeartbeatInterval: Int,
    private val pubnubPresenceTimeout: Int
) : BaseRepository() {

    var pubnubChatMessagingClient: PubnubChatMessagingClient? = null

    @Synchronized
    fun establishChatMessagingConnection(): MessagingClient {
        if (pubnubChatMessagingClient == null)
            pubnubChatMessagingClient =
                PubnubChatMessagingClient(
                    subscriberKey = subscribeKey,
                    authKey = authKey,
                    uuid = uuid,
                    analyticsService = analyticsService,
                    publishKey = publishKey,
                    origin = origin,
                    pubnubHeartbeatInterval = pubnubHeartbeatInterval,
                    pubnubPresenceTimeout = pubnubPresenceTimeout
                )
        return pubnubChatMessagingClient!!
    }

    suspend fun fetchChatRoom(chatRoomId: String, chatRoomTemplateUrl: String): Result<ChatRoom> {
        val remoteURL = chatRoomTemplateUrl.replace(TEMPLATE_CHAT_ROOM_ID, chatRoomId)
        return dataClient.remoteCall<ChatRoom>(remoteURL, RequestType.GET, accessToken = null)
    }

    suspend fun createChatRoom(
        title: String?,
        visibility: Visibility?,
        chatRoomTemplateUrl: String
    ): Result<ChatRoom> {
        val remoteURL = chatRoomTemplateUrl.replace(TEMPLATE_CHAT_ROOM_ID, "")
        val titleRequest = createTitleRequest(title, visibility)
        return dataClient.remoteCall<ChatRoom>(
            remoteURL,
            RequestType.POST,
            accessToken = authKey,
            requestBody = titleRequest
        )
    }

    private fun createTitleRequest(
        title: String?,
        visibility: Visibility?
    ) = when {
        title.isNullOrEmpty()
            .not() && visibility != null ->
            """{"visibility":"${visibility.name}","title":"$title"}"""
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        title.isNullOrEmpty().not() && visibility == null ->
            """{"title":"$title"}"""
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        title.isNullOrEmpty() && visibility != null ->
            """{"visibility":"${visibility.name}"}"""
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        else -> byteArrayOf().toRequestBody(null, 0)
    }

    suspend fun updateChatRoom(
        title: String?,
        visibility: Visibility?,
        chatRoomId: String,
        chatRoomTemplateUrl: String
    ): Result<ChatRoom> {
        val chatRoomResult = fetchChatRoom(chatRoomId, chatRoomTemplateUrl)
        val titleRequest = createTitleRequest(title, visibility)
        return if (chatRoomResult is Result.Success) {
            return dataClient.remoteCall<ChatRoom>(
                chatRoomResult.data.url,
                RequestType.PUT,
                accessToken = authKey,
                requestBody = titleRequest
            )
        } else {
            chatRoomResult as Result.Error
        }
    }

    suspend fun getCurrentUserChatRoomList(membershipUrl: String): Result<UserChatRoomListResponse> {
        return dataClient.remoteCall<UserChatRoomListResponse>(
            membershipUrl,
            accessToken = authKey,
            requestType = RequestType.GET
        )
    }

    suspend fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        chatRoomTemplateUrl: String
    ): Result<Void> {
        val chatRoomResult = fetchChatRoom(chatRoomId, chatRoomTemplateUrl)
        return if (chatRoomResult is Result.Success) {
            dataClient.remoteCall<Void>(
                chatRoomResult.data.membershipsUrl,
                accessToken = authKey,
                requestType = RequestType.DELETE
            )
        } else {
            chatRoomResult as Result.Error
        }
    }

    fun addMessageReaction(channel: String, messagePubnubToken: Long, emojiId: String) {
        pubnubChatMessagingClient?.addMessageAction(channel, messagePubnubToken, emojiId)
    }

    fun removeMessageReaction(
        channel: String,
        messagePubnubToken: Long,
        reactionpubnubToken: Long
    ) {
        pubnubChatMessagingClient?.removeMessageAction(
            channel,
            messagePubnubToken,
            reactionpubnubToken
        )
    }

    fun loadPreviousMessages(channel: String, limit: Int = CHAT_HISTORY_LIMIT) {
        pubnubChatMessagingClient?.loadMessagesWithReactions(
            channel,
            limit
        )
    }
}
