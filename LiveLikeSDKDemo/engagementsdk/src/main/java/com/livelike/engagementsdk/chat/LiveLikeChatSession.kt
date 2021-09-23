package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.ChatRoomListener
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage

/**
 * Created by Shivansh Mittal on 2020-04-08.
 */
interface LiveLikeChatSession {

    /** Return the playheadTime for this session.*/
    fun getPlayheadTime(): EpochTime

    /** Pause the current Chat and widget sessions. This generally happens when ads are presented */
    fun pause()

    /** Resume the current Chat and widget sessions. This generally happens when ads are completed */
    fun resume()

    /** Closes the current session.*/
    fun close()

    /** The current chat room */
    var getCurrentChatRoom: () -> String

    /**
     * To connect to the chatRoom with provided chatRoomId, by default it will load initial messages
     */
    fun connectToChatRoom(chatRoomId: String, callback: LiveLikeCallback<Unit>? = null)

    /** Returns the number of messages published on a chatroom since a given time*/
    fun getMessageCount(startTimestamp: Long, callback: LiveLikeCallback<Byte>)

    /** Register a message count listner for the specified Chat Room */
    fun setMessageListener(messageListener: MessageListener)

    /** Register a chatRoom listener for the specified Chat Room */
    fun setChatRoomListener(chatRoomListener: ChatRoomListener)

    /** Set the value of visibility of chat avatar **/
    var shouldDisplayAvatar: Boolean

    /** Avatar Image Url  **/
    var avatarUrl: String?

    /**
     * send Chat Message to the current ChatRoom
     *
     * @message : text message
     * @imageUrl: image message
     * @imageWidth: image width default is 100, if value is not null then the original width of image will not set
     * @imageHeight: image height default is 100, f value is not null then the original height of image will not set
     * @liveLikeCallback : callback to provide the message object, this callback is not meant the message is sent
     *
     * Note: For the very first for every message livelikeCallback return the ChatMessage object which contains the data added by the user,
     * then the #messageListener will recieve the same chatMessage with uploaded url and timetoken updated ,you can check it with the id in #ChatMessage
     *
     * **/
    fun sendChatMessage(
        message: String?,
        imageUrl: String? = null,
        imageWidth: Int?,
        imageHeight: Int?,
        liveLikeCallback: LiveLikeCallback<LiveLikeChatMessage>
    )

    /**
     * to load Chat History
     * @limit: default is 20,max is 100
     */
    fun loadNextHistory(limit: Int = 20)

    /**
     * To get the loaded message
     */
    fun getLoadedMessages(): ArrayList<LiveLikeChatMessage>

    /**
     * to get the deleted messages from the loaded message
     */
    fun getDeletedMessages(): ArrayList<String>
}
