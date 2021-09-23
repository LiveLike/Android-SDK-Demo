package com.livelike.engagementsdk.chat.services.messaging.pubnub

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.REACTION_CREATED
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.ChatMessageReaction
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.chat.MessageError
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEvent
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.CHATROOM_UPDATED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.CUSTOM_MESSAGE_CREATED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.IMAGE_CREATED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.IMAGE_DELETED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.MESSAGE_CREATED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.MESSAGE_DELETED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatMessage
import com.livelike.engagementsdk.chat.data.remote.toPubnubChatEventType
import com.livelike.engagementsdk.chat.data.toChatMessage
import com.livelike.engagementsdk.chat.data.toPubnubChatMessage
import com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs.addPublishedMessage
import com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs.flushPublishedMessage
import com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs.getPublishedMessages
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.core.services.messaging.Error
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.Queue
import com.livelike.engagementsdk.core.utils.extractStringOrEmpty
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.isoUTCDateTimeFormatter
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedPreferences
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.parseISODateTime
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubSubscribeCallbackAdapter
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.PubNubException
import com.pubnub.api.enums.PNOperationType
import com.pubnub.api.enums.PNReconnectionPolicy
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.history.PNFetchMessageItem
import com.pubnub.api.models.consumer.message_actions.PNMessageAction
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class PubnubChatMessagingClient(
    subscriberKey: String,
    authKey: String,
    uuid: String,
    private val analyticsService: AnalyticsService,
    publishKey: String? = null,
    val isDiscardOwnPublishInSubcription: Boolean = true,
    val origin: String? = null,
    private val pubnubHeartbeatInterval: Int,
    private val pubnubPresenceTimeout: Int
) : MessagingClient {

    @Volatile
    private var lastActionTimeToken: Long = 0
    private var connectedChannels: MutableSet<String> = mutableSetOf()

    private val publishQueue =
        Queue<Pair<String, PubnubChatEvent<PubnubChatMessage>>>()

    private val coroutineScope = MainScope()
    private var isPublishRunning = false
    private var firstTimeToken: Long = 0
    private var pubnubChatRoomLastMessageTime: MutableMap<String, ArrayList<String>>? = null

    var activeChatRoom = ""
        set(value) {
            field = value
            firstTimeToken = 0
            subscribe(listOf(value))
            flushPublishedMessage(*connectedChannels.toTypedArray())
        }

    @Synchronized
    fun addChannelSubscription(channel: String, startTimestamp: Long) {
        if (!connectedChannels.contains(channel)) {
            connectedChannels.add(channel)
            flushPublishedMessage(*connectedChannels.toTypedArray())
            val endTimeStamp = Calendar.getInstance().timeInMillis
            pubnub.subscribe().channels(listOf(channel)).execute()
//            getAllMessages(channel, convertToTimeToken(startTimestamp), convertToTimeToken(endTimeStamp))
        }
    }

    private fun convertToTimeToken(timestamp: Long): Long {
        return timestamp * 10000
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        val clientMessage = gson.fromJson(message, ChatMessage::class.java)
        val pubnubChatEvent = PubnubChatEvent(
            clientMessage.messageEvent.key,
            clientMessage.toPubnubChatMessage(
                when (timeSinceEpoch.timeSinceEpochInMs) {
                    0L -> null
                    else -> ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(timeSinceEpoch.timeSinceEpochInMs),
                        org.threeten.bp.ZoneId.of("UTC")
                    ).format(isoUTCDateTimeFormatter)
                }
            )
        )
        publishQueue.enqueue(Pair(channel, pubnubChatEvent))
        if (isDiscardOwnPublishInSubcription) {
            addPublishedMessage(channel, pubnubChatEvent.payload.messageId)
        }
        if (!isPublishRunning) {
            startPublishingFromQueue()
        }
    }

    private fun startPublishingFromQueue() {
        isPublishRunning = true
        coroutineScope.async {
            while (!publishQueue.isEmpty()) {
                publishQueue.peek()?.let { messageChannelPair ->
                    if (publishMessageToPubnub(
                            messageChannelPair.second,
                            messageChannelPair.first
                        )
                    ) {
                        publishQueue.dequeue()
                        delay(100) // ensure messages not more than 5 per second as 100ms is pubnub latency
                    } else {
                        delay(2000) // Linear back-off strategy.
                    }
                }
            }
            isPublishRunning = false
        }
    }

    private suspend fun publishMessageToPubnub(
        pubnubChatEvent: PubnubChatEvent<PubnubChatMessage>,
        channel: String
    ) = suspendCoroutine<Boolean> {
        pubnub.publish()
            .message(
                pubnubChatEvent
            )
            .meta(
                JsonObject().apply {
                    addProperty("sender_id", pubnubChatEvent.payload.senderId)
                    addProperty("language", "en-us")
                }
            )
            .channel(channel)
            .async { result, status ->
                logDebug { "pub status code: " + status.statusCode }
                if (!status.isError) {
                    logDebug { "pub timetoken: " + result?.timetoken!! }
                    it.resume(true)
                } else {
                    if (status.statusCode == 403) {
                        listener?.onClientMessageError(
                            this@PubnubChatMessagingClient,
                            com.livelike.engagementsdk.core.services.messaging.Error(
                                MessageError.DENIED_MESSAGE_PUBLISH.name,
                                ""
                            )
                        )
                        it.resume(true)
                    } else {
                        it.resume(false)
                    }
                }
            }
    }

    override fun stop() {
        pubnub.disconnect()
    }

    override fun start() {
        pubnub.reconnect()
    }

    private val pubnubConfiguration: PNConfiguration = PNConfiguration()
    var pubnub: PubNub
    private var listener: MessagingEventListener? = null

    init {
        pubnubConfiguration.subscribeKey = subscriberKey
        pubnubConfiguration.authKey = authKey
        pubnubConfiguration.uuid = uuid
        pubnubConfiguration.publishKey = publishKey
        pubnubConfiguration.filterExpression =
            "sender_id == '$uuid' || !(content_filter contains 'filtered')"
        if (origin != null) {
            pubnubConfiguration.origin = origin
        }
        pubnubConfiguration.setPresenceTimeoutWithCustomInterval(
            pubnubPresenceTimeout,
            pubnubHeartbeatInterval
        )
        pubnubConfiguration.reconnectionPolicy = PNReconnectionPolicy.EXPONENTIAL
        pubnub = PubNub(pubnubConfiguration)
        val client = this

        // Extract SubscribeCallback?
        pubnub.addListener(object : PubnubSubscribeCallbackAdapter() {
            override fun status(pubnub: PubNub, status: PNStatus) {
                when (status.operation) {
                    // let's combine unsubscribe and subscribe handling for ease of use
                    PNOperationType.PNSubscribeOperation, PNOperationType.PNUnsubscribeOperation -> {
                        // note: subscribe statuses never have traditional
                        // errors, they just have categories to represent the
                        // different issues or successes that occur as part of subscribe
                        when (status.category) {
                            PNStatusCategory.PNConnectedCategory -> {
                                // this is expected for a subscribe, this means there is no error or issue whatsoever
                                listener?.onClientMessageStatus(client, ConnectionStatus.CONNECTED)
                            }

                            PNStatusCategory.PNReconnectedCategory -> {
                                // this usually occurs if subscribe temporarily fails but reconnects. This means
                                // there was an error but there is no longer any issue
                                listener?.onClientMessageStatus(client, ConnectionStatus.CONNECTED)
                            }

                            PNStatusCategory.PNDisconnectedCategory -> {
                                // this is the expected category for an unsubscribe. This means there
                                // was no error in unsubscribing from everything
                                listener?.onClientMessageStatus(
                                    client,
                                    ConnectionStatus.DISCONNECTED
                                )
                            }

                            PNStatusCategory.PNAccessDeniedCategory -> {
                                // this means that PAM does allow this client to subscribe to this
                                // channel and channel group configuration. This is another explicit error
                                listener?.onClientMessageError(
                                    client,
                                    Error("Access Denied", "Access Denied")
                                )
                            }

                            PNStatusCategory.PNTimeoutCategory, PNStatusCategory.PNNetworkIssuesCategory, PNStatusCategory.PNUnexpectedDisconnectCategory -> {
                                // this is usually an issue with the internet connection, this is an error, handle appropriately
                                pubnub.reconnect()
                            }

                            else -> {
                                // Some other category we have yet to handle
                            }
                        }
                    }

                    else -> {
                        // some other Operation Type we are not handling yet.
                    }
                }
            }

            override fun message(pubnub: PubNub, message: PNMessageResult) {
                val channel = message.channel
                if (pubnubChatRoomLastMessageTime == null)
                    pubnubChatRoomLastMessageTime = GsonBuilder().create().fromJson(
                        getSharedPreferences()
                            .getString(
                                PREF_CHAT_ROOM_MSG_RECEIVED,
                                null
                            ),
                        object : TypeToken<MutableMap<String, ArrayList<String>>>() {}.type
                    ) ?: mutableMapOf()
                pubnubChatRoomLastMessageTime?.let { map ->
                    val pubnubChatEvent: PubnubChatEvent<PubnubChatMessage> =
                        gson.fromJson(
                            message.message.asJsonObject,
                            object : TypeToken<PubnubChatEvent<PubnubChatMessage>>() {}.type
                        )
                    val msgId = pubnubChatEvent.payload.messageId

                    val list =
                        when {
                            map.containsKey(channel) -> map[channel]
                            else -> ArrayList()
                        } ?: ArrayList()
                    val event = message.message.asJsonObject.extractStringOrEmpty("event")
                        .toPubnubChatEventType()

                    when (event) {
                        MESSAGE_CREATED, IMAGE_CREATED, CUSTOM_MESSAGE_CREATED -> {
                            if (!list.contains(msgId)) {
                                list.add(msgId)
                                map[channel] = list
                                getSharedPreferences()
                                    .edit()
                                    .putString(
                                        PREF_CHAT_ROOM_MSG_RECEIVED,
                                        GsonBuilder().create().toJson(map)
                                    ).apply()
                                processPubnubChatEvent(
                                    message.message.asJsonObject.apply {
                                        addProperty("pubnubToken", message.timetoken)
                                    },
                                    channel, message.timetoken
                                )?.let {
                                    listener?.onClientMessageEvent(client, it)
                                }
                            } else {
                            }
                        }
                        else -> {
                            processPubnubChatEvent(
                                message.message.asJsonObject.apply {
                                    addProperty("pubnubToken", message.timetoken)
                                },
                                channel, message.timetoken
                            )?.let {
                                listener?.onClientMessageEvent(client, it)
                            }
                        }
                    }
                }
            }

            override fun messageAction(
                pubnub: PubNub,
                pnMessageActionResult: PNMessageActionResult
            ) {
                // check later using coroutine mainscope not executing when private group, so removing for now
                lastActionTimeToken = pnMessageActionResult.messageAction.actionTimetoken
                logDebug { "real time message action : " + pnMessageActionResult.event }
                processPubnubMessageAction(pnMessageActionResult, client)
            }

            override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {
            }
        })
    }

    private fun processPubnubMessageAction(
        pnMessageActionResult: PNMessageActionResult,
        client: PubnubChatMessagingClient
    ) {
        if (pnMessageActionResult.messageAction.type == REACTION_CREATED && pubnub.configuration.uuid != pnMessageActionResult.messageAction.uuid) {
            val clientMessage: ClientMessage
            if (pnMessageActionResult.event == "added") {
                clientMessage = ClientMessage(
                    JsonObject().apply {
                        addProperty("event", ChatViewModel.EVENT_REACTION_ADDED)
                        addProperty(
                            "isOwnReaction",
                            pubnub.configuration.uuid == pnMessageActionResult.messageAction.uuid
                        )
                        addProperty(
                            "actionPubnubToken",
                            pnMessageActionResult.messageAction.actionTimetoken
                        )
                        addProperty(
                            "messagePubnubToken",
                            pnMessageActionResult.messageAction.messageTimetoken
                        )
                        addProperty("emojiId", pnMessageActionResult.messageAction.value)
                    },
                    pnMessageActionResult.channel
                )
                listener?.onClientMessageEvent(client, clientMessage)
            } else {
                clientMessage = ClientMessage(
                    JsonObject().apply {
                        addProperty("event", ChatViewModel.EVENT_REACTION_REMOVED)
                        addProperty(
                            "messagePubnubToken",
                            pnMessageActionResult.messageAction.messageTimetoken
                        )
                        addProperty("emojiId", pnMessageActionResult.messageAction.value)
                    },
                    pnMessageActionResult.channel
                )
                listener?.onClientMessageEvent(client, clientMessage)
            }
        }
    }

    private fun processPubnubChatEvent(
        jsonObject: JsonObject,
        channel: String,
        timeToken: Long,
        actions: HashMap<String, HashMap<String, List<PNFetchMessageItem.Action>>>? = null
    ): ClientMessage? {
        val event = jsonObject.extractStringOrEmpty("event").toPubnubChatEventType()
        if (event != null) {
            val pubnubChatEvent: PubnubChatEvent<PubnubChatMessage> = gson.fromJson(
                jsonObject,
                object : TypeToken<PubnubChatEvent<PubnubChatMessage>>() {}.type
            )
            var clientMessage: ClientMessage? = null
            when (event) {
                MESSAGE_CREATED, IMAGE_CREATED, CUSTOM_MESSAGE_CREATED -> {
                    if (isDiscardOwnPublishInSubcription && getPublishedMessages(channel).contains(
                            pubnubChatEvent.payload.messageId
                        )
                    ) {
                        logError { "discarding as its own recently published message which is broadcasted by pubnub on that channel." }
                        clientMessage = ClientMessage(
                            gson.toJsonTree(
                                pubnubChatEvent.payload.toChatMessage(
                                    channel,
                                    timeToken,
                                    processReactionCounts(actions),
                                    getOwnReaction(actions),
                                    event
                                )
                            ).asJsonObject.apply {
                                addProperty("event", ChatViewModel.EVENT_MESSAGE_TIMETOKEN_UPDATED)
                                addProperty("messageId", pubnubChatEvent.payload.messageId)
                                addProperty("timetoken", timeToken)
                            },
                            channel
                        )
                        return clientMessage
                    }
                    val pdtString = pubnubChatEvent.payload.programDateTime
                    var epochTimeMs = 0L
                    pdtString?.parseISODateTime()?.let {
                        epochTimeMs = it.toInstant().toEpochMilli()
                    }

                    try {
                        clientMessage = ClientMessage(
                            gson.toJsonTree(
                                pubnubChatEvent.payload.toChatMessage(
                                    channel,
                                    timeToken,
                                    processReactionCounts(actions),
                                    getOwnReaction(actions),
                                    event
                                )
                            ).asJsonObject.apply {
                                addProperty("event", ChatViewModel.EVENT_NEW_MESSAGE)
                                addProperty("pubnubMessageToken", pubnubChatEvent.pubnubToken)
                            },
                            channel,
                            EpochTime(epochTimeMs)
                        )
                    } catch (ex: IllegalArgumentException) {
                        logError { ex.message }
                        return null
                    }
                }
                MESSAGE_DELETED, IMAGE_DELETED -> {
                    clientMessage = ClientMessage(
                        JsonObject().apply {
                            addProperty("event", ChatViewModel.EVENT_MESSAGE_DELETED)
                            addProperty("id", pubnubChatEvent.payload.messageId)
                        },
                        channel,
                        EpochTime(0)
                    )
                }
                CHATROOM_UPDATED -> {
                    val pubnubChatRoomEvent: PubnubChatEvent<ChatRoom> = gson.fromJson(
                        jsonObject,
                        object : TypeToken<PubnubChatEvent<ChatRoom>>() {}.type
                    )
                    clientMessage = ClientMessage(
                        gson.toJsonTree(pubnubChatRoomEvent.payload).asJsonObject.apply {
                            addProperty("event", CHATROOM_UPDATED.key)
                        },
                        channel
                    )
                }
            }
            logDebug { "Received message on $channel from pubnub: ${pubnubChatEvent.payload}" }
            return clientMessage
        } else {
            logError { "We don't know how to handle this message" }
        }
        return null
    }

    private fun isMessageModerated(jsonObject: JsonObject): Boolean {
        // added this check since in payload content filter was coming as string (json primitive) instead of array
        var contentfilter = jsonObject.getAsJsonObject("payload")?.get("content_filter")
        return if (contentfilter?.isJsonPrimitive == true) {
            false
        } else {
            jsonObject.getAsJsonObject("payload")?.getAsJsonArray("content_filter")
                ?.size() ?: 0 > 0
        }
    }

    private fun getOwnReaction(actions: java.util.HashMap<String, java.util.HashMap<String, List<PNFetchMessageItem.Action>>>?): ChatMessageReaction? {
        actions?.get(REACTION_CREATED)?.let { reactions ->
            for (value in reactions.keys) {
                reactions[value]?.forEach { action ->
                    if (action.uuid == pubnub.configuration.uuid) {
                        return ChatMessageReaction(value, action.actionTimetoken.toLongOrNull())
                    }
                }
            }
        }
        return null
    }

    private fun processReactionCounts(actions: java.util.HashMap<String, java.util.HashMap<String, List<PNFetchMessageItem.Action>>>?): MutableMap<String, Int> {
        val reactionCountMap = mutableMapOf<String, Int>()
        actions?.get(REACTION_CREATED)?.let { reactions ->
            for (value in reactions.keys) {
                reactionCountMap[value] = reactions[value]?.size ?: 0
            }
        }
        return reactionCountMap
    }

    internal fun loadMessagesWithReactions(
        channel: String,
        chatHistoyLimit: Int = com.livelike.engagementsdk.CHAT_HISTORY_LIMIT
    ) {
        if (firstTimeToken == 0L)
            pubnub.time().async { result, status ->
                firstTimeToken = result?.timetoken ?: 0
                loadMessagesWithReactions(
                    channel,
                    chatHistoyLimit
                )
            }
        else {
            logDebug { "LoadMessages from History for channel $channel ,time:$firstTimeToken" }
            pubnub.fetchMessages()
                .channels(listOf(channel))
                .includeMeta(true)
                .maximumPerChannel(chatHistoyLimit)
                .start(firstTimeToken)
                .includeMessageActions(true)
                .async { result, status ->
                    if (!status.isError && result?.channels?.get(channel)?.isEmpty() == false) {
                        result.channels?.get(channel)?.first()?.timetoken?.let {
                            firstTimeToken = it
                        }
                        val list =
                            result.channels?.get(channel)?.filter { messageItem ->
                                val jsonObject = messageItem.message.asJsonObject.apply {
                                    addProperty("pubnubToken", messageItem.timetoken)
                                }
                                return@filter !isMessageModerated(jsonObject)
                            }?.map { messageItem ->
                                val jsonObject = messageItem.message.asJsonObject.apply {
                                    addProperty("pubnubToken", messageItem.timetoken)
                                }
                                return@map processPubnubChatEvent(
                                    jsonObject,
                                    channel,
                                    messageItem.timetoken,
                                    messageItem.actions
                                )
                            }?.filterNotNull() ?: arrayListOf()
                        listener?.onClientMessageEvents(this, list)
                    }
                    sendLoadingCompletedEvent(channel)
                }
        }
    }

    fun addMessageAction(channel: String, messageTimetoken: Long, value: String) {
        pubnub.addMessageAction()
            .channel(channel)
            .messageAction(
                PNMessageAction().apply {
                    type = REACTION_CREATED
                    this.value = value
                    this.messageTimetoken = messageTimetoken
                }
            ).async { result, status ->
                if (!status.isError) {
                    val clientMessage = ClientMessage(
                        JsonObject().apply {
                            addProperty("event", ChatViewModel.EVENT_REACTION_ADDED)
                            addProperty(
                                "isOwnReaction",
                                true
                            )
                            addProperty(
                                "actionPubnubToken",
                                result?.actionTimetoken
                            )
                            addProperty(
                                "messagePubnubToken",
                                result?.messageTimetoken
                            )
                            addProperty("emojiId", result?.value)
                        },
                        channel
                    )
                    listener?.onClientMessageEvent(
                        this@PubnubChatMessagingClient,
                        clientMessage
                    )
                    logDebug { "own message action added" }
                } else {
                    status.errorData.throwable.printStackTrace()
                }
            }
    }

    fun removeMessageAction(channel: String, messageTimetoken: Long, actionTimetoken: Long) {
        pubnub.removeMessageAction()
            .channel(channel)
            .messageTimetoken(messageTimetoken)
            .actionTimetoken(actionTimetoken)
            .async { result, status ->
                if (!status.isError) {
                    logDebug { "own message action removed" }
                } else {
                    status.errorData.throwable.printStackTrace()
                }
            }
    }

    /**
     * returns the message count between start and end time
     * max count can returned is 100
     **/
    internal fun getMessageCountV1(
        channel: String,
        startTimestamp: Long,
        endTimeStamp: Long = Calendar.getInstance().timeInMillis
    ): Result<Byte> {
        try {
            val pnHistoryResult = pubnub.history()
                .channel(channel)
                .start(convertToTimeToken(startTimestamp))
                .end(convertToTimeToken(endTimeStamp))
                .includeMeta(true)
                .includeTimetoken(true)
                .reverse(false)
                .sync()

            var count: Byte = 0
            pnHistoryResult?.messages?.forEach {
                if (it.meta.isJsonObject && it.meta.asJsonObject.get("content_filter")?.asString?.contains(
                        "filtered"
                    ) == false
                )
                    count++
            }
            return Result.Success(count)
        } catch (e: PubNubException) {
            return Result.Error(e)
        }
    }

    @Deprecated("use getMessageCountV1()")
    internal fun getMessageCount(
        channel: String,
        startTimestamp: Long
    ): Result<Long> {
        return try {
            val countResult = pubnub.messageCounts()
                .channels(listOf(channel))
                .channelsTimetoken(listOf(convertToTimeToken(startTimestamp)))
                .sync()
            logDebug {
                "Count Read channel : $channel lasttimestamp:${
                    convertToTimeToken(
                        startTimestamp
                    )
                } count:${countResult?.channels?.get(channel) ?: 0}"
            }
            Result.Success(countResult?.channels?.get(channel) ?: 0)
        } catch (ex: PubNubException) {
            ex.printStackTrace()
            Result.Error(ex)
        }
    }

    override fun subscribe(channels: List<String>) {
        channels.forEach {
            connectedChannels.add(it)
            loadMessagesWithReactions(it)
        }
        pubnub.subscribe().channels(channels).execute()
    }

    override fun unsubscribe(channels: List<String>) {
        pubnub.unsubscribe().channels(channels).execute()
        channels.forEach {
            connectedChannels.remove(it)
        }
    }

    override fun unsubscribeAll() {
        pubnub.unsubscribeAll()
    }

    private fun sendLoadingCompletedEvent(channel: String) {
        val msg = JsonObject().apply {
            addProperty("event", ChatViewModel.EVENT_LOADING_COMPLETE)
        }
        // TODO: remove one event once the default chat is merged with custom chat
        listener?.onClientMessageEvents(this, arrayListOf())
        listener?.onClientMessageEvent(
            this,
            ClientMessage(
                msg, channel,
                EpochTime(0)
            )
        )
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        // More than one triggerListener?
        this.listener = listener
    }

    override fun destroy() {
        coroutineScope.cancel()
        unsubscribeAll()
        pubnub.destroy()
        flushPublishedMessage(*connectedChannels.toTypedArray())
    }

    companion object {
        private const val PREF_CHAT_ROOM_MSG_RECEIVED = "pubnub message received"
    }
}
