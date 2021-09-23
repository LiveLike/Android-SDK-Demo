package com.livelike.engagementsdk.chat.data

import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.ChatMessageReaction
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.remote.PubnubChatMessage

internal fun ChatMessage.toPubnubChatMessage(programDateTime: String?): PubnubChatMessage {

    return PubnubChatMessage(
        id,
        message,
        senderId,
        senderDisplayPic,
        senderDisplayName,
        programDateTime,
        imageUrl = imageUrl,
        image_width = image_width,
        image_height = image_height,
        custom_data = ""
    )
}

internal fun PubnubChatMessage.toChatMessage(
    channel: String,
    timetoken: Long,
    emojiCountMap: MutableMap<String, Int>,
    myReaction: ChatMessageReaction?,
    event: PubnubChatEventType
): ChatMessage {
    return ChatMessage(
        event,
        channel,
        message,
        custom_data,
        senderId,
        senderNickname,
        senderImageUrl,
        messageId,
        emojiCountMap = emojiCountMap,
        myChatMessageReaction = myReaction,
        imageUrl = imageUrl,
        image_width = image_width,
        image_height = image_height,
        timetoken = timetoken
    )
}
