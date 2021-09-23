package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import java.util.UUID

internal interface ChatEventListener {
    fun onChatMessageSend(message: ChatMessage, timeData: EpochTime)
}

internal interface ChatRenderer {
    fun displayChatMessage(message: ChatMessage)

    /**
     * called whenever messages are loaded from history call or at fist time load call
     **/
    fun displayChatMessages(messages: List<ChatMessage>)
    fun deleteChatMessage(messageId: String)
    fun updateChatMessageTimeToken(messageId: String, timetoken: String)
    fun loadingCompleted()
    fun addMessageReaction(
        isOwnReaction: Boolean,
        messagePubnubToken: Long,
        chatMessageReaction: ChatMessageReaction
    )

    fun removeMessageReaction(messagePubnubToken: Long, emojiId: String)

    fun errorSendingMessage(error: MessageError)
}

/**
 *  Represents a chat message.
 *  @param message The message user wants to send.
 *  @param senderId This is unique user id.
 *  @param senderDisplayName This is display name user is associated with.
 *  @param id A unique ID to identify the message.
 *  @param timeStamp Message timeStamp.
 */
internal data class ChatMessage(
    var messageEvent: PubnubChatEventType,
    var channel: String,
    var message: String?,
    var custom_data: String?,
    val senderId: String,
    val senderDisplayName: String,
    val senderDisplayPic: String?,
    var id: String = UUID.randomUUID().toString(),
    // PDT video time //NOt using right now for later use FYI @shivansh @Willis
    var timeStamp: String? = null,
    var imageUrl: String? = null,
    var badgeUrlImage: String? = null,
    var isFromMe: Boolean = false,
    var myChatMessageReaction: ChatMessageReaction? = null,
    var emojiCountMap: MutableMap<String, Int> = mutableMapOf(),
    // time of the message
    var timetoken: Long = 0L,
    var image_width: Int? = 100,
    var image_height: Int? = 100,
    var isDeleted: Boolean = false
) {
    // Update the user_id to profile_id as required from backend
    fun toReportMessageJson(): String {
        return """{
                    "channel": "$channel",
                    "profile_id": "$senderId",
                    "nickname": "$senderDisplayName",
                    "message_id": "$id",
                    "message": "${message?.trim()}",
                    "pubnub_timetoken":$timetoken
                }
        """.trimIndent()
    }

    override fun equals(other: Any?): Boolean {
        return id == (other as? ChatMessage)?.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun getUnixTimeStamp(): Long? {
        if (timetoken == 0L) {
            return null
        }
        return try {
            timetoken / 10000
        } catch (ex: ArithmeticException) {
            null
        }
    }
}

internal data class ChatMessageReaction(
    val emojiId: String,
    var pubnubActionToken: Long? = null
)

data class ChatRoomInfo(
    val id: String,
    val title: String? = null,
    val visibility: Visibility? = null,
    val contentFilter: String? = null,
    val customData: String? = null
)

enum class Visibility { everyone, members }

internal const val CHAT_MESSAGE_IMAGE_TEMPLATE = ":message:"
