package com.livelike.engagementsdk.publicapis

import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType

/**
 * User pojo to be exposed, should be minimal in terms of fields
 */
data class LiveLikeUserApi(
    var nickname: String,
    val accessToken: String,
    var userId: String,
    var custom_data: String? = null
)

// this model is not changed since 1.2 release in hurry, we need to fix it may require to bump to major version.
data class LiveLikeChatMessage(val message: String?) {

    constructor(imageUrl: String, image_width: Int = 100, image_height: Int = 100) : this(null) {
        this.imageUrl = imageUrl
        this.image_width = image_width
        this.image_height = image_height
    }

    var nickname: String? = null

    var userPic: String? = null

    /**
     * timestamp represent the time of the chatmessage,
     */
    var timestamp: String? = null

    /**
     * unique identifier
     */
    var id: String? = null

    /**
     * type of the message, the enum class defines the types definition
     */
    var type: ChatMessageType? = null

    /**
     * chat room id of the chat message ,will be keeping for now
     */
    var channel: String? = null

    /**
     * id of the sender of the message
     */
    var senderId: String? = null

    /**
     * width of the image
     */
    var image_width: Int? = null

    /**
     * height of the image
     */
    var image_height: Int? = null

    /**
     * the URL that referenced the image
     */
    var imageUrl: String? = null

    /**
     * data for custom message
     */
    var custom_data: String? = null
}

enum class ChatMessageType(val key: String) {
    MESSAGE_CREATED("message-created"),
    MESSAGE_DELETED("message-deleted"),
    IMAGE_CREATED("image-created"),
    IMAGE_DELETED("image-deleted"),
    CUSTOM_MESSAGE_CREATED("custom-message-created")
}

internal fun PubnubChatEventType.toChatMessageType(): ChatMessageType? {
    return when (this) {
        PubnubChatEventType.MESSAGE_DELETED -> ChatMessageType.MESSAGE_DELETED
        PubnubChatEventType.MESSAGE_CREATED -> ChatMessageType.MESSAGE_CREATED
        PubnubChatEventType.IMAGE_DELETED -> ChatMessageType.IMAGE_DELETED
        PubnubChatEventType.IMAGE_CREATED -> ChatMessageType.IMAGE_CREATED
        PubnubChatEventType.CUSTOM_MESSAGE_CREATED -> ChatMessageType.CUSTOM_MESSAGE_CREATED
        else -> null
    }
}

// TODO: check for the requirement else remove this method
// internal fun PubnubChatMessage.toLiveLikeChatMessage(): LiveLikeChatMessage {
//    // TODO will require to bump to major version as id needs to be string
//    return LiveLikeChatMessage(
//        senderNickname,
//        senderImageUrl,
//        message,
//        "",
//        messageId.hashCode().toLong()
//    )
// }

internal fun ChatMessage.toLiveLikeChatMessage(): LiveLikeChatMessage {
    var epochTimeStamp = 0L
    if (timetoken > 0) {
        epochTimeStamp = timetoken / 10000
    }
    return LiveLikeChatMessage(message).apply {
        this.type = messageEvent.toChatMessageType()
        this.channel = this@toLiveLikeChatMessage.channel
        this.id = this@toLiveLikeChatMessage.id
        this.imageUrl = this@toLiveLikeChatMessage.imageUrl
        this.image_height = this@toLiveLikeChatMessage.image_height
        this.image_width = this@toLiveLikeChatMessage.image_width
        this.nickname = this@toLiveLikeChatMessage.senderDisplayName
        this.userPic = this@toLiveLikeChatMessage.senderDisplayPic
        this.custom_data = this@toLiveLikeChatMessage.custom_data
        this.senderId = this@toLiveLikeChatMessage.senderId
        this.timestamp = epochTimeStamp.toString()
    }
}


internal fun ChatRoom.toLiveLikeChatRoom(): ChatRoomInfo {
    return ChatRoomInfo(
        this.id,
        this.title,
        this.visibility,
        this.contentFilter,
        this.customData
    )
}