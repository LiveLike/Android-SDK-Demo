package com.livelike.engagementsdk.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.CHAT_PROVIDER
import com.livelike.engagementsdk.ChatRoomListener
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.chat.services.network.ChatDataClient
import com.livelike.engagementsdk.chat.services.network.ChatDataClientImpl
import com.livelike.engagementsdk.chat.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.syncTo
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.publicapis.toLiveLikeChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.net.URL
import java.util.UUID

/**
 * Created by Shivansh Mittal on 2020-04-08.
 */
internal class ChatSession(
    sdkConfiguration: Stream<EngagementSDK.SdkConfiguration>,
    private val userRepository: UserRepository,
    private val applicationContext: Context,
    private val isPublicRoom: Boolean = true,
    internal val analyticsServiceStream: Stream<AnalyticsService>,
    private val errorDelegate: ErrorDelegate? = null,
    private val currentPlayheadTime: () -> EpochTime
) : LiveLikeChatSession {

    override fun getPlayheadTime(): EpochTime {
        return currentPlayheadTime.invoke()
    }

    private var pubnubClientForMessageCount: PubnubChatMessagingClient? = null
    private lateinit var pubnubMessagingClient: PubnubChatMessagingClient
    private val dataClient: ChatDataClient = ChatDataClientImpl()
    private var isClosed = false
    val chatViewModel: ChatViewModel by lazy {
        ChatViewModel(
            applicationContext,
            userRepository.currentUserStream,
            isPublicRoom,
            null,
            dataClient = dataClient
        )
    }
    override var getCurrentChatRoom: () -> String = { currentChatRoom?.id ?: "" }

    private var chatClient: MessagingClient? = null
    private val contentSessionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var chatRepository: ChatRepository? = null
    private var chatRoomId: String? = null

    private val chatSessionIdleStream: Stream<Boolean> =
        SubscriptionManager(true)
    private var currentChatRoom: ChatRoom? = null
    private val messages = ArrayList<LiveLikeChatMessage>()
    private val deletedMsgList = arrayListOf<String>()

    private val configurationUserPairFlow = flow {
        while (sdkConfiguration.latest() == null || userRepository.currentUserStream.latest() == null) {
            delay(1000)
        }
        emit(Pair(sdkConfiguration.latest()!!, userRepository.currentUserStream.latest()!!))
    }

    init {
        contentSessionScope.launch {
            configurationUserPairFlow.collect { pair ->

                chatViewModel.analyticsService = analyticsServiceStream.latest()!!
                val liveLikeUser = pair.second
                chatRepository =
                    ChatRepository(
                        pair.first.pubNubKey,
                        liveLikeUser.accessToken,
                        liveLikeUser.id,
                        MockAnalyticsService(),
                        pair.first.pubnubPublishKey,
                        origin = pair.first.pubnubOrigin,
                        pubnubHeartbeatInterval = pair.first.pubnubHeartbeatInterval,
                        pubnubPresenceTimeout = pair.first.pubnubPresenceTimeout
                    )
                logDebug { "chatRepository created" }
                // updating urls value will be added in enterChat Room
                chatViewModel.chatRepository = chatRepository
                initializeChatMessaging(currentPlayheadTime)
                chatSessionIdleStream.onNext(true)
            }
        }
    }

    override var shouldDisplayAvatar: Boolean
        get() = chatViewModel.chatAdapter.showChatAvatarLogo
        set(value) {
            chatViewModel.chatAdapter.showChatAvatarLogo = value
        }

    private fun updatingURls(
        clientId: String,
        stickerPackUrl: String,
        reactionPacksUrl: String,
        reportUrl: String?
    ) {
        if (isClosed) {
            logError { "Session is closed" }
            errorDelegate?.onError("Session is closed")
            return
        }
        contentSessionScope.launch {
            configurationUserPairFlow.collect { pair ->
                chatViewModel.stickerPackRepository =
                    StickerPackRepository(clientId, stickerPackUrl)
                chatViewModel.chatReactionRepository =
                    ChatReactionRepository(reactionPacksUrl, pair.second.accessToken)
                chatViewModel.reportUrl = reportUrl
                contentSessionScope.launch {
                    chatViewModel.chatReactionRepository?.preloadImages(
                        applicationContext
                    )
                }
            }
        }
    }

    override fun pause() {
        chatClient?.stop()
    }

    override fun resume() {
        chatClient?.start()
    }

    override fun close() {
        chatClient?.run {
            destroy()
        }
        contentSessionScope.cancel()
        isClosed = true
        chatViewModel.chatAdapter.mRecyclerView = null
    }

    // TODO remove proxy message listener by having pipe in chat data layers/chain that tranforms pubnub channel to room
    private var proxyMsgListener: MessageListener = object : MessageListener {
        override fun onNewMessage(message: LiveLikeChatMessage) {
            logDebug {
                "ContentSession onNewMessage: ${message.message} timestamp:${message.timestamp}"
            }
            this@ChatSession.messages.add(message)
            msgListener?.onNewMessage(message)
        }

        override fun onHistoryMessage(messages: List<LiveLikeChatMessage>) {
            this@ChatSession.messages.addAll(0, messages)
            msgListener?.onHistoryMessage(messages)
        }

        override fun onDeleteMessage(messageId: String) {
            deletedMsgList.add(messageId)
            msgListener?.onDeleteMessage(messageId)
        }
    }

    private var msgListener: MessageListener? = null
    private var chatRoomListener: ChatRoomListener? = null

    private val proxyChatRoomListener = object : ChatRoomListener {
        override fun onChatRoomUpdate(chatRoom: ChatRoomInfo) {
            chatRoomListener?.onChatRoomUpdate(chatRoom)
        }
    }

    private fun initializeChatMessaging(
        currentPlayheadTime: () -> EpochTime
    ) {
        analyticsServiceStream.latest()!!.trackLastChatStatus(true)
        chatClient = chatRepository?.establishChatMessagingConnection()

        pubnubMessagingClient = chatClient as PubnubChatMessagingClient

        currentPlayheadTime.let {
            chatClient =
                chatClient?.syncTo(it)
        }
        chatClient = chatClient?.toChatQueue()
            ?.apply {
                msgListener = proxyMsgListener
                chatRoomListener = this@ChatSession.proxyChatRoomListener
                this.renderer = chatViewModel
                chatViewModel.chatLoaded = false
                chatViewModel.chatListener = this
            }
        logDebug { "initialized Chat Messaging" }
    }

    private fun fetchChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoom>
    ) {
        val requestId = UUID.randomUUID()
        chatSessionIdleStream.subscribe(requestId) {
            if (it == true) {
                if (isClosed) {
                    logError { "Session is closed" }
                    errorDelegate?.onError("Session is closed")
                    return@subscribe
                }
                contentSessionScope.launch {
                    configurationUserPairFlow.collect { pair ->
                        logDebug { "fetch ChatRoom" }
                        chatRepository?.let { chatRepository ->
                            val chatRoomResult =
                                chatRepository.fetchChatRoom(
                                    chatRoomId,
                                    pair.first.chatRoomDetailUrlTemplate
                                )
                            if (chatRoomResult is Result.Success) {
                                liveLikeCallback.onResponse(chatRoomResult.data, null)
                            } else if (chatRoomResult is Result.Error) {
                                errorDelegate?.onError("error in fetching room id $chatRoomId")
                                liveLikeCallback.onResponse(
                                    null,
                                    chatRoomResult.exception.message
                                        ?: "error in fetching room id resource"
                                )
                                logError {
                                    chatRoomResult.exception.message
                                        ?: "error in fetching room id resource"
                                }
                            }
                            chatSessionIdleStream.unsubscribe(requestId)
                        }
                    }
                }
            }
        }
    }

    override fun getMessageCount(
        startTimestamp: Long,
        callback: LiveLikeCallback<Byte>
    ) {
        chatRoomId?.let {
            logDebug { "messageCount $chatRoomId ,$startTimestamp" }
            fetchChatRoom(
                it,
                object : LiveLikeCallback<ChatRoom>() {
                    override fun onResponse(result: ChatRoom?, error: String?) {
                        result?.let { chatRoom ->
                            chatRoom.channels.chat[CHAT_PROVIDER]?.let { channel ->
                                if (pubnubClientForMessageCount == null) {
                                    pubnubClientForMessageCount =
                                        chatRepository?.establishChatMessagingConnection() as PubnubChatMessagingClient
                                }
                                pubnubClientForMessageCount?.getMessageCountV1(
                                    channel,
                                    startTimestamp
                                )
                                    ?.run {
                                        callback.processResult(this)
                                    }
                            }
                        }
                        error?.let {
                            callback.onResponse(null, error)
                        }
                    }
                }
            )
        }
    }

    // TODO: will move to constructor later after discussion
    override fun connectToChatRoom(chatRoomId: String, callback: LiveLikeCallback<Unit>?) {
        if (chatRoomId.isEmpty()) {
            callback?.onResponse(null, "ChatRoom Id cannot be Empty")
            errorDelegate?.onError("ChatRoom Id cannot be Empty")
            return
        }
        if (currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER) == chatRoomId) return // Already in the room
        currentChatRoom?.let { chatRoom ->
            chatClient?.unsubscribe(listOf(chatRoom.channels.chat[CHAT_PROVIDER] ?: ""))
        }
        chatViewModel.apply {
            flushMessages()
        }
        messages.clear()
        deletedMsgList.clear()
        this.chatRoomId = chatRoomId
        fetchChatRoom(
            chatRoomId,
            object : LiveLikeCallback<ChatRoom>() {
                override fun onResponse(result: ChatRoom?, error: String?) {
                    result?.let { chatRoom ->
                        val channel = chatRoom.channels.chat[CHAT_PROVIDER]
                        channel?.let { ch ->
                            contentSessionScope.launch {
                                delay(500)
                                pubnubMessagingClient.addChannelSubscription(ch, 0L)
                                delay(500)
                                chatViewModel.apply {
                                    flushMessages()
                                    updatingURls(
                                        chatRoom.clientId,
                                        chatRoom.stickerPacksUrl,
                                        chatRoom.reactionPacksUrl,
                                        chatRoom.reportMessageUrl
                                    )
                                    delay(1000)
                                    currentChatRoom = chatRoom
                                    chatLoaded = false
                                }
                                this@ChatSession.currentChatRoom = chatRoom
                                pubnubMessagingClient.activeChatRoom = channel
                                callback?.onResponse(Unit, null)
                            }
                        }
                    }
                    error?.let {
                        callback?.onResponse(null, error)
                    }
                }
            }
        )
    }

    override fun setMessageListener(
        messageListener: MessageListener
    ) {
        msgListener = messageListener
    }

    override fun setChatRoomListener(chatRoomListener: ChatRoomListener) {
        this.chatRoomListener = chatRoomListener
    }

    override var avatarUrl: String? = null

    /**
     * TODO: added it into default chat once all functionality related to chat is done
     */
    override fun sendChatMessage(
        message: String?,
        imageUrl: String?,
        imageWidth: Int?,
        imageHeight: Int?,
        liveLikeCallback: LiveLikeCallback<LiveLikeChatMessage>
    ) {
        if (message?.isEmpty() == true) {
            liveLikeCallback.onResponse(null, "Message cannot be empty")
            return
        }
        val timeData = getPlayheadTime()
        ChatMessage(
            when (imageUrl != null) {
                true -> PubnubChatEventType.IMAGE_CREATED
                else -> PubnubChatEventType.MESSAGE_CREATED
            },
            currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER) ?: "",
            message,
            "",
            userRepository.currentUserStream.latest()?.id ?: "empty-id",
            userRepository.currentUserStream.latest()?.nickname ?: "John Doe",
            avatarUrl,
            imageUrl = imageUrl,
            isFromMe = true,
            image_width = imageWidth ?: 100,
            image_height = imageHeight ?: 100,
            timeStamp = timeData.timeSinceEpochInMs.toString()
        ).let { chatMessage ->

            // TODO: need to update for error handling here if pubnub respond failure of message
            liveLikeCallback.onResponse(chatMessage.toLiveLikeChatMessage(), null)

            val hasExternalImage = imageUrl != null
            if (hasExternalImage) {
                contentSessionScope.launch {
                    val uri = Uri.parse(chatMessage.imageUrl)
                    when {
                        uri.scheme != null && uri.scheme.equals("content") -> {
                            applicationContext.contentResolver.openInputStream(uri)
                        }
                        else -> {
                            URL(chatMessage.imageUrl).openConnection().getInputStream()
                        }
                    }?.use {
                        val fileBytes = it.readBytes()
                        val uploadedImageUrl = dataClient.uploadImage(
                            currentChatRoom!!.uploadUrl,
                            null,
                            fileBytes
                        )
                        chatMessage.messageEvent = PubnubChatEventType.IMAGE_CREATED
                        chatMessage.imageUrl = uploadedImageUrl
                        val bitmap =
                            BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                        chatMessage.image_width = imageWidth ?: bitmap.width
                        chatMessage.image_height = imageHeight ?: bitmap.height
                        val m = chatMessage.copy()
                        m.message = ""
                        (chatClient as? ChatEventListener)?.onChatMessageSend(
                            chatMessage,
                            timeData
                        )
                        bitmap.recycle()
                    }
                }
            } else {
                (chatClient as? ChatEventListener)?.onChatMessageSend(chatMessage, timeData)
            }
            currentChatRoom?.id?.let { id ->
                analyticsServiceStream.latest()?.trackMessageSent(
                    chatMessage.id,
                    chatMessage.message,
                    hasExternalImage,
                    id
                )
            }
        }
    }

    override fun loadNextHistory(limit: Int) {
        currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)?.let { channel ->
            if (chatRepository != null) {
                chatRepository?.loadPreviousMessages(channel, limit)
            } else {
                logError { "Chat repo is null" }
                errorDelegate?.onError("Chat Repository is Null")
            }
        }
    }

    override fun getLoadedMessages(): ArrayList<LiveLikeChatMessage> {
        return messages
    }

    override fun getDeletedMessages(): ArrayList<String> {
        return deletedMsgList
    }
}
