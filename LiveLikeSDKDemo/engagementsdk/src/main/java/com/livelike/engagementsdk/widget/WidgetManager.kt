package com.livelike.engagementsdk.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.JsonObject
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.core.services.messaging.proxies.MessagingClientProxy
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.services.network.WidgetDataClient
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getTotalPoints
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.PriorityQueue
import java.util.Queue

internal class WidgetManager(
    upstream: MessagingClient,
    private val dataClient: WidgetDataClient,
    private val currentWidgetViewStream: Stream<Pair<String, SpecifiedWidgetView?>?>,
    private val context: Context,
    widgetInterceptor: WidgetInterceptor?,
    private val analyticsService: AnalyticsService,
    private val sdkConfiguration: EngagementSDK.SdkConfiguration,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository,
    private val animationEventsStream: SubscriptionManager<ViewAnimationEvents>,
    private val widgetThemeAttributes: WidgetViewThemeAttributes?,
    private val livelikeThemeStream: Stream<LiveLikeEngagementTheme>,
    private val widgetStream: Stream<LiveLikeWidget>,
    private val widgetInteractionRepository: WidgetInteractionRepository
) :
    MessagingClientProxy(upstream) {

    data class MessageHolder(
        val messagingClient: MessagingClient,
        val clientMessage: ClientMessage
    ) : Comparable<MessageHolder> {
        override fun compareTo(other: MessageHolder): Int {
            val thisRank = this.clientMessage.message.get("priority")?.asInt ?: 0
            val otherRank = other.clientMessage.message.get("priority")?.asInt ?: 0
            return otherRank.compareTo(thisRank)
        }
    }

    private val messageQueue: Queue<MessageHolder> = PriorityQueue()
    private var widgetOnScreen = false
    private var pendingMessage: MessageHolder? = null
    var widgetInterceptor: WidgetInterceptor? = widgetInterceptor
        set(value) {
            field = value
            widgetInterceptorSubscribe()
        }

    init {
        widgetInterceptorSubscribe()
    }

    private fun widgetInterceptorSubscribe() {
        widgetInterceptor?.let { wi ->
            wi.events.subscribe(javaClass.simpleName) {
                when (it) {
                    WidgetInterceptor.Decision.Show -> showPendingMessage()
                    WidgetInterceptor.Decision.Dismiss -> dismissPendingMessage()
                }
            }
        }
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        widgetOnScreen = false
        upstream.stop()
    }

    override fun start() {
        upstream.start()
    }

    override fun onClientMessageEvents(client: MessagingClient, events: List<ClientMessage>) {
        TODO("Not yet implemented")
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        super.onClientMessageEvent(client, event)
        logDebug { "Message received at WidgetManager" }
        val payload = event.message["payload"].asJsonObject
        widgetStream.onNext(
            gson.fromJson(
                payload.toString(),
                LiveLikeWidget::class.java
            )
        )
        messageQueue.add(MessageHolder(client, event))
        if (!widgetOnScreen) {
            publishNextInQueue()
        }
    }

    private fun publishNextInQueue() {
        if (messageQueue.isNotEmpty()) {
            widgetOnScreen = true
            notifyIntegrator(messageQueue.remove())
        } else {
            widgetOnScreen = false
            currentWidgetViewStream.onNext(null) // sometimes widget view obscure the whole screen chat due t which app seems unresponsive
        }
    }

    private fun showPendingMessage() {
        pendingMessage?.let {
            showWidgetOnScreen(it)
        }
    }

    private fun dismissPendingMessage() {
        publishNextInQueue()
    }

    private fun notifyIntegrator(message: MessageHolder) {
        val widgetType =
            WidgetType.fromString(message.clientMessage.message.get("event").asString ?: "")
        if (widgetInterceptor == null || widgetType == WidgetType.POINTS_TUTORIAL || widgetType == WidgetType.COLLECT_BADGE) {
            showWidgetOnScreen(message)
        } else {
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    // Need to assure we are on the main thread to communicated with the external activity
                    try {
                        widgetInterceptor?.widgetWantsToShow(
                            gson.fromJson(
                                message.clientMessage.message["payload"],
                                LiveLikeWidgetEntity::class.java
                            )
                        )
                    } catch (e: Exception) {
                        logError { "Widget interceptor encountered a problem: $e \n Dismissing the widget" }
                        dismissPendingMessage()
                    }
                }
            }
            pendingMessage = message
        }
    }

    private fun showWidgetOnScreen(msgHolder: MessageHolder) {
        val widgetType = msgHolder.clientMessage.message.get("event").asString ?: ""
        val payload = msgHolder.clientMessage.message["payload"].asJsonObject
        val widgetId = payload["id"].asString

        handler.post {
            currentWidgetViewStream.onNext(
                Pair(
                    widgetType,
                    WidgetProvider()
                        .get(
                            this,
                            WidgetInfos(widgetType, payload, widgetId),
                            context,
                            analyticsService,
                            sdkConfiguration,
                            {
                                checkForPointTutorial()
                                publishNextInQueue()
                            },
                            userRepository,
                            programRepository,
                            animationEventsStream,
                            widgetThemeAttributes ?: WidgetViewThemeAttributes(),
                            livelikeThemeStream.latest(),
                            widgetInteractionRepository = widgetInteractionRepository
                        )
                )
            )
        }

        // Register the impression on the backend
        payload.get("impression_url")?.asString?.let {
            dataClient.registerImpression(it, userRepository.userAccessToken)
        }

        super.onClientMessageEvent(msgHolder.messagingClient, msgHolder.clientMessage)
    }

    private fun checkForPointTutorial() {
        if (shouldShowPointTutorial()) {
            // Check if user scored points
            if (getTotalPoints() != 0) {
                val message = ClientMessage(
                    JsonObject().apply {
                        addProperty("event", "points-tutorial")
                        add(
                            "payload",
                            JsonObject().apply {
                                addProperty("id", "gameification")
                            }
                        )
                        addProperty("priority", 3)
                    }
                )
                onClientMessageEvent(this, message)
            }
        }
    }
}

enum class WidgetType(val event: String) {
    CHEER_METER("cheer-meter-created"),
    TEXT_PREDICTION("text-prediction-created"),
    TEXT_PREDICTION_FOLLOW_UP("text-prediction-follow-up-updated"),
    IMAGE_PREDICTION("image-prediction-created"),
    IMAGE_PREDICTION_FOLLOW_UP("image-prediction-follow-up-updated"),
    TEXT_QUIZ("text-quiz-created"),
    IMAGE_QUIZ("image-quiz-created"),
    TEXT_POLL("text-poll-created"),
    IMAGE_POLL("image-poll-created"),
    POINTS_TUTORIAL("points-tutorial"),
    COLLECT_BADGE("collect-badge"),
    ALERT("alert-created"),
    IMAGE_SLIDER("emoji-slider-created"),
    SOCIAL_EMBED("social-embed-created"),
    VIDEO_ALERT("video-alert-created"),
    TEXT_ASK("text-ask-created");

    companion object {
        private val map = values().associateBy(WidgetType::event)
        fun fromString(type: String) = map[type]
    }

    fun getType(): String {
        return event.replace("created", "").replace("updated", "")
            .replace("-", "")
    }
}

internal fun MessagingClient.asWidgetManager(
    dataClient: WidgetDataClient,
    widgetInfosStream: SubscriptionManager<Pair<String, SpecifiedWidgetView?>?>,
    context: Context,
    widgetInterceptor: WidgetInterceptor?,
    analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    userRepository: UserRepository,
    programRepository: ProgramRepository,
    animationEventsStream: SubscriptionManager<ViewAnimationEvents>,
    widgetThemeAttributes: WidgetViewThemeAttributes?,
    livelikeThemeStream: Stream<LiveLikeEngagementTheme>,
    widgetStream: Stream<LiveLikeWidget>,
    widgetInteractionRepository: WidgetInteractionRepository
): WidgetManager {
    return WidgetManager(
        this,
        dataClient,
        widgetInfosStream,
        context,
        widgetInterceptor,
        analyticsService,
        sdkConfiguration,
        userRepository,
        programRepository,
        animationEventsStream,
        widgetThemeAttributes,
        livelikeThemeStream,
        widgetStream,
        widgetInteractionRepository
    )
}
