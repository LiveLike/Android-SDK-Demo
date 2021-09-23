package com.livelike.engagementsdk.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.CHAT_PROVIDER
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.services.network.ChatDataClient
import com.livelike.engagementsdk.chat.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getBlockedUsers
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

internal class ChatViewModel(
    private val applicationContext: Context,
    val userStream: Stream<LiveLikeUser>,
    val isPublicRoom: Boolean,
    val animationEventsStream: SubscriptionManager<ViewAnimationEvents>? = null,
    val programRepository: ProgramRepository? = null,
    private val dataClient: ChatDataClient
) : ChatRenderer, ViewModel() {

    var chatListener: ChatEventListener? = null
    var analyticsService: AnalyticsService = MockAnalyticsService()
        set(value) {
            field = value
            chatAdapter.analyticsService = value
        }
    var chatAdapter: ChatRecyclerAdapter =
        ChatRecyclerAdapter(analyticsService, ::reportChatMessage)
    var messageList = mutableListOf<ChatMessage>()
    var cacheList = mutableListOf<ChatMessage>()
    var deletedMessages = hashSetOf<String>()

    internal val eventStream: Stream<String> =
        SubscriptionManager(true)
    var currentChatRoom: ChatRoom? = null
        set(value) {
            field = value
            chatAdapter.chatRoomId = value?.id
            chatAdapter.isPublicChat = isPublicRoom
            chatAdapter.chatRoomName = value?.title
        }

    var avatarUrl: String? = null

    var stickerPackRepository: StickerPackRepository? = null
        set(value) {
            field = value
            value?.let {
                stickerPackRepositoryStream.onNext(value)
            }
            value?.let { chatAdapter.stickerPackRepository = value }
        }
    val stickerPackRepositoryStream: Stream<StickerPackRepository> = SubscriptionManager()
    var chatReactionRepository: ChatReactionRepository? = null
        set(value) {
            field = value
            value?.let {
                chatAdapter.chatReactionRepository = value
            }
        }
    var chatRepository: ChatRepository? = null
        set(value) {
            field = value
            chatAdapter.chatRepository = value
        }
    var reportUrl: String? = null
    var isLastItemVisible = false

    internal var chatLoaded = false
        set(value) {
            field = value
            logDebug { "chatload:$field" }
            if (field) {
                eventStream.onNext(EVENT_LOADING_COMPLETE)
            } else {
                eventStream.onNext(EVENT_LOADING_STARTED)
            }
        }

    override fun displayChatMessages(messages: List<ChatMessage>) {
        Log.d("custom", "messages")

        messages.forEach {
            replaceImageMessageContentWithImageUrl(it)
        }

        messageList.addAll(
            0,
            messages.filter {
                !deletedMessages.contains(it.id) && !getBlockedUsers()
                    .contains(it.senderId) && (it.messageEvent != PubnubChatEventType.CUSTOM_MESSAGE_CREATED)
            }.map {
                it.isFromMe = userStream.latest()?.id == it.senderId
                it
            }
        )

        notifyNewChatMessages()
    }

    override fun displayChatMessage(message: ChatMessage) {
        logDebug {
            "Chat display message: ${message.message} check1:${
            message.channel != currentChatRoom?.channels?.chat?.get(
                CHAT_PROVIDER
            )
            } check blocked:${
            getBlockedUsers()
                .contains(message.senderId)
            } check deleted:${deletedMessages.contains(message.id)}"
        }
        if (message.channel != currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)) return

        // if custom message is received, ignore that, custom messages doesn't need to be shown in UI
        if (message.messageEvent == PubnubChatEventType.CUSTOM_MESSAGE_CREATED) return

        if (getBlockedUsers()
            .contains(message.senderId)
        ) {
            logDebug { "user is blocked" }
            return
        }
        if (deletedMessages.contains(message.id.toLowerCase())) {
            logDebug { "the message is deleted by producer" }
            return
        }

        replaceImageMessageContentWithImageUrl(message)
        messageList.add(
            message.apply {
                isFromMe = userStream.latest()?.id == senderId
            }
        )

        notifyNewChatMessages()
    }

    private fun notifyNewChatMessages() {
        if (chatLoaded) {
            uiScope.launch {
                chatAdapter.submitList(ArrayList(messageList))
                eventStream.onNext(EVENT_NEW_MESSAGE)
            }
        }
    }

    private fun replaceImageMessageContentWithImageUrl(
        message: ChatMessage
    ) {
        val imageUrl = message.imageUrl
        if (message.messageEvent == PubnubChatEventType.IMAGE_CREATED && !imageUrl.isNullOrEmpty()) {
            message.message = CHAT_MESSAGE_IMAGE_TEMPLATE.replace("message", imageUrl)
        }
    }

    override fun removeMessageReaction(messagePubnubToken: Long, emojiId: String) {
        messageList.forEachIndexed { index, chatMessage ->
            chatMessage.apply {
                if (this.timetoken == messagePubnubToken) {
                    emojiCountMap[emojiId] = (emojiCountMap[emojiId] ?: 0) - 1
                    uiScope.launch { chatAdapter.notifyItemChanged(index) }
                    return@forEachIndexed
                }
                // remember case not handled for now if same user removes its reaction while using 2 devices
            }
        }
    }

    override fun errorSendingMessage(error: MessageError) {
        if (error.equals(MessageError.DENIED_MESSAGE_PUBLISH)) {
            messageList.remove(messageList.findLast { it.isFromMe })
            chatAdapter.submitList(messageList)
            eventStream.onNext(EVENT_MESSAGE_CANNOT_SEND)
        }
    }

    override fun addMessageReaction(
        isOwnReaction: Boolean,
        messagePubnubToken: Long,
        chatMessageReaction: ChatMessageReaction
    ) {
        logDebug { "add Message Reaction OwnReaction:$isOwnReaction" }
        messageList.forEachIndexed { index, chatMessage ->
            if (chatMessage != null) { // added null check in reference to ES-1533 (though crash not reproducible at all)
                chatMessage.apply {
                    if (this.timetoken == messagePubnubToken) {
                        if (isOwnReaction) {
                            if (chatMessage.myChatMessageReaction?.emojiId == chatMessageReaction.emojiId) {
                                chatMessage.myChatMessageReaction?.pubnubActionToken =
                                    chatMessageReaction.pubnubActionToken
                                // added notifyItemChange for reaction own ,reference to ES-1734
                                uiScope.launch { chatAdapter.notifyItemChanged(index) }
                            }
                        } else {
                            val emojiId = chatMessageReaction.emojiId
                            emojiCountMap[emojiId] = (emojiCountMap[emojiId] ?: 0) + 1
                            uiScope.launch { chatAdapter.notifyItemChanged(index) }
                        }
                        return@forEachIndexed
                    }
                }
            }
        }
    }

    override fun deleteChatMessage(messageId: String) {
        deletedMessages.add(messageId)
        if (chatLoaded) {
            logDebug { "message is deleted from producer so changing its text" }
            messageList.find {
                it.id.toLowerCase() == messageId
            }?.apply {
                message =
                    applicationContext.getString(R.string.livelike_chat_message_deleted_message)
                isDeleted = true
            }
            uiScope.launch {
                chatAdapter.submitList(ArrayList(messageList.toSet()))
                chatAdapter.currentChatReactionPopUpViewPos = -1
                val index = messageList.indexOfFirst { it.id == messageId }
                if (index != -1 && index < chatAdapter.itemCount) {
                    chatAdapter.notifyItemChanged(index)
                }
                eventStream.onNext(EVENT_MESSAGE_DELETED)
            }
        }
    }

    override fun updateChatMessageTimeToken(messageId: String, timetoken: String) {
        uiScope.launch {
            messageList.find {
                it.id == messageId
            }?.let { cm ->
                cm.timetoken = timetoken.toLong()
                chatAdapter.submitList(ArrayList(messageList))
                chatAdapter.notifyItemChanged(messageList.indexOf(cm))
                eventStream.onNext(EVENT_NEW_MESSAGE)
            }
        }
    }

    override fun loadingCompleted() {
        logDebug { "Chat loading Completed : $chatLoaded" }
        if (!chatLoaded) {
            chatLoaded = true
            logDebug { "Chat retrieving message from local cache ${cacheList.size} , MessageList :${messageList.size}" }
            if (messageList.isEmpty() && cacheList.isNotEmpty()) {
                messageList.addAll(cacheList)
            }
            chatAdapter.submitList(ArrayList(messageList.toSet()))
        } else {
            eventStream.onNext(EVENT_LOADING_COMPLETE)
        }
    }

    private fun reportChatMessage(message: ChatMessage) {
        uiScope.launch {
            reportUrl?.let { reportUrl ->
                dataClient.reportMessage(
                    reportUrl,
                    message,
                    userStream.latest()?.accessToken
                )
            }
        }
    }

    internal fun refreshWithDeletedMessage() {
        messageList.removeAll { deletedMessages.contains(it.id.toLowerCase()) }
        uiScope.launch {
            chatAdapter.submitList(ArrayList(messageList))
        }
    }

    fun flushMessages() {
        cacheList = mutableListOf()
        deletedMessages = hashSetOf()
        messageList = mutableListOf()
        chatAdapter.submitList(messageList)
    }

    fun loadPreviousMessages() {
        currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)?.let { channel ->
            if (chatRepository != null) {
                logDebug { "Chat loading previous messages size:${messageList.size},all Message size:${messageList.size},deleted Message:${deletedMessages.size}," }
                chatRepository?.loadPreviousMessages(channel)
            } else {
                eventStream.onNext(EVENT_LOADING_COMPLETE)
                logError { "Chat repo is null" }
            }
        }
    }

    fun uploadAndPostImage(context: Context, chatMessage: ChatMessage, timedata: EpochTime) {
        val url =
            Uri.parse(chatMessage.message?.substring(1, (chatMessage.message?.length ?: 0) - 1))
        uiScope.launch(Dispatchers.IO) {
            context.contentResolver.openAssetFileDescriptor(
                url,
                "r"
            )?.use {
                try {
                    val fileBytes = it.createInputStream().readBytes()
                    val imageUrl = dataClient.uploadImage(
                        currentChatRoom!!.uploadUrl,
                        null,
                        fileBytes
                    )
                    chatMessage.messageEvent = PubnubChatEventType.IMAGE_CREATED
                    chatMessage.imageUrl = imageUrl
                    val bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                    chatMessage.image_width = bitmap.width
                    chatMessage.image_height = bitmap.height
                    val m = chatMessage.copy()
                    m.message = ""
                    chatListener?.onChatMessageSend(m, timedata)
                    bitmap.recycle()
                } catch (e: Exception) {
                    e.printStackTrace()
                    logError { e.message }
                }
            }
        }
        Glide.with(context.applicationContext)
            .`as`(ByteArray::class.java)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<ByteArray>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onResourceReady(
                    fileBytes: ByteArray,
                    transition: Transition<in ByteArray>?
                ) {
                    try {
                        uiScope.launch(Dispatchers.IO) {
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        logError { e.message }
                    }
                }
            })
    }

    companion object {
        const val EVENT_NEW_MESSAGE = "new-message"
        const val EVENT_MESSAGE_DELETED = "deletion"
        const val EVENT_MESSAGE_TIMETOKEN_UPDATED = "id-updated"
        const val EVENT_LOADING_COMPLETE = "loading-complete"
        const val EVENT_LOADING_STARTED = "loading-started"
        const val EVENT_REACTION_ADDED = "reaction-added"
        const val EVENT_REACTION_REMOVED = "reaction-removed"
        const val EVENT_MESSAGE_CANNOT_SEND =
            "message_cannot_send" // case 0 : occurs when user is muted inside a room and sends a message
    }
}
