package com.livelike.engagementsdk

import android.content.Context
import android.util.Log
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.livelike.engagementsdk.chat.stickerKeyboard.allMatches
import com.livelike.engagementsdk.chat.stickerKeyboard.countMatches
import com.livelike.engagementsdk.chat.stickerKeyboard.findStickerCodes
import com.livelike.engagementsdk.chat.stickerKeyboard.findStickers
import com.livelike.engagementsdk.core.analytics.AnalyticsSuperProperties
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mixpanel.android.mpmetrics.MixpanelExtension
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The base interface for the analytics. This will log events to any remote analytics provider.
 *
 */
interface AnalyticsService {
    /**
     * Set an analytics event observer {eventObserver}. It will intercept analytics events.
     *
     * @param eventObserver
     */
    fun setEventObserver(eventObserver: (String, JSONObject) -> Unit)

    fun registerSuperProperty(analyticsSuperProperties: AnalyticsSuperProperties, value: Any?)
    fun registerSuperAndPeopleProperty(event: Pair<String, String>)

    fun trackConfiguration(internalAppName: String) // add more info if required in the future
    fun trackWidgetInteraction(
        kind: String,
        id: String,
        programId: String,
        interactionInfo: AnalyticsWidgetInteractionInfo
    )

    fun trackWidgetEngaged(
        kind: String,
        id: String,
        programId: String
    )

    fun trackSessionStarted()
    fun trackMessageSent(
        msgId: String,
        msg: String?,
        hasExternalImage: Boolean = false,
        chatRoomId: String
    )

    fun trackMessageDisplayed(msgId: String, msg: String?, hasExternalImage: Boolean = false)
    fun trackLastChatStatus(status: Boolean)
    fun trackLastWidgetStatus(status: Boolean)
    fun trackWidgetReceived(kind: String, id: String)
    fun trackWidgetDisplayed(kind: String, id: String, programId: String, linkUrl: String? = null)
    fun trackWidgetBecameInteractive(
        kind: String,
        id: String,
        programId: String,
        linkUrl: String? = null
    )

    fun trackWidgetDismiss(
        kind: String,
        id: String,
        programId: String,
        interactionInfo: AnalyticsWidgetInteractionInfo?,
        interactable: Boolean?,
        action: DismissAction
    )

    fun trackInteraction(
        kind: String,
        id: String,
        interactionType: String,
        interactionCount: Int = 1
    )

    fun trackOrientationChange(isPortrait: Boolean)
    fun trackSession(sessionId: String)
    fun trackButtonTap(buttonName: String, extra: JsonObject)
    fun trackUsername(username: String)
    fun trackKeyboardOpen(keyboardType: KeyboardType)
    fun trackKeyboardClose(
        keyboardType: KeyboardType,
        hideMethod: KeyboardHideReason,
        chatMessageId: String? = null
    )

    fun trackFlagButtonPressed()
    fun trackReportingMessage()
    fun trackBlockingUser()
    fun trackCancelFlagUi()
    fun trackPointTutorialSeen(completionType: String, secondsSinceStart: Long)
    fun trackPointThisProgram(points: Int)
    fun trackBadgeCollectedButtonPressed(badgeId: String, badgeLevel: Int)
    fun trackChatReactionPanelOpen(messageId: String)
    fun trackAlertLinkOpened(
        alertId: String,
        programId: String,
        linkUrl: String,
        widgetType: WidgetType?
    )

    fun trackChatReactionSelected(
        chatRoomId: String,
        messageId: String,
        reactionId: String,
        isRemoved: Boolean
    )

    fun trackVideoAlertPlayed(
        kind: String,
        id: String,
        programId: String,
        videoUrl: String
    )

    fun trackMessageLinkClicked(
        chatRoomId: String,
        chatRoomName: String?,
        messageId: String?,
        link: String
    )

    fun destroy()
}

class MockAnalyticsService(private val clientId: String = "") : AnalyticsService {

    override fun setEventObserver(eventObserver: (String, JSONObject) -> Unit) {
        eventObservers[clientId] = eventObserver
    }

    override fun trackBadgeCollectedButtonPressed(badgeId: String, badgeLevel: Int) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]$badgeId $badgeLevel")
    }

    override fun trackChatReactionPanelOpen(messageId: String) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]$messageId")
    }

    override fun trackChatReactionSelected(
        chatRoomId: String,
        messageId: String,
        reactionId: String,
        isRemoved: Boolean
    ) {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}]$messageId $reactionId $isRemoved"
        )
    }

    override fun trackVideoAlertPlayed(
        kind: String,
        id: String,
        programId: String,
        videoUrl: String
    ) {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $kind $programId $videoUrl"
        )
    }

    override fun trackMessageLinkClicked(
        chatRoomId: String,
        chatRoomName: String?,
        messageId: String?,
        link: String
    ) {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $chatRoomId $chatRoomName $messageId $link"
        )
    }

    override fun destroy() {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}]"
        )
    }

    override fun trackAlertLinkOpened(
        alertId: String,
        programId: String,
        linkUrl: String,
        widgetType: WidgetType?
    ) {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}]$alertId $programId $linkUrl ${widgetType?.getType()}"
        )
    }

    override fun registerSuperProperty(
        analyticsSuperProperties: AnalyticsSuperProperties,
        value: Any?
    ) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]$value")
    }

    override fun trackPointThisProgram(points: Int) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]$points")
    }

    override fun trackPointTutorialSeen(completionType: String, secondsSinceStart: Long) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackFlagButtonPressed() {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackReportingMessage() {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackCancelFlagUi() {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackBlockingUser() {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun registerSuperAndPeopleProperty(event: Pair<String, String>) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $event")
    }

    override fun trackLastChatStatus(status: Boolean) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $status")
    }

    override fun trackLastWidgetStatus(status: Boolean) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $status")
    }

    override fun trackConfiguration(internalAppName: String) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $internalAppName")
    }

    override fun trackWidgetInteraction(
        kind: String,
        id: String,
        programId: String,
        interactionInfo: AnalyticsWidgetInteractionInfo
    ) {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $kind $programId $interactionInfo"
        )
    }

    override fun trackWidgetEngaged(kind: String, id: String, programId: String) {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $kind $programId "
        )
    }

    override fun trackSessionStarted() {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackMessageSent(
        msgId: String,
        msg: String?,
        hasExternalImage: Boolean,
        chatRoomId: String
    ) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $msgId")
    }

    override fun trackMessageDisplayed(msgId: String, msg: String?, hasExternalImage: Boolean) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $msgId")
    }

    override fun trackWidgetReceived(kind: String, id: String) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $kind")
    }

    override fun trackWidgetDisplayed(
        kind: String,
        id: String,
        programId: String,
        linkUrl: String?
    ) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $kind $programId")
    }

    override fun trackWidgetBecameInteractive(
        kind: String,
        id: String,
        programId: String,
        linkUrl: String?
    ) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $kind $programId")
    }

    override fun trackWidgetDismiss(
        kind: String,
        id: String,
        programId: String,
        interactionInfo: AnalyticsWidgetInteractionInfo?,
        interactable: Boolean?,
        action: DismissAction
    ) {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $kind $action $interactionInfo"
        )
    }

    override fun trackInteraction(
        kind: String,
        id: String,
        interactionType: String,
        interactionCount: Int
    ) {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $kind $interactionType"
        )
    }

    override fun trackOrientationChange(isPortrait: Boolean) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $isPortrait")
    }

    override fun trackSession(sessionId: String) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $sessionId")
    }

    override fun trackButtonTap(buttonName: String, extra: JsonObject) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $buttonName")
    }

    override fun trackUsername(username: String) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $username")
    }

    override fun trackKeyboardOpen(keyboardType: KeyboardType) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $keyboardType")
    }

    override fun trackKeyboardClose(
        keyboardType: KeyboardType,
        hideMethod: KeyboardHideReason,
        chatMessageId: String?
    ) {
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $keyboardType $hideMethod"
        )
    }
}

class AnalyticsWidgetInteractionInfo {
    var interactionCount: Int = 0
    var timeOfFirstInteraction: Long = -1
    var timeOfLastInteraction: Long = 0
    var timeOfFirstDisplay: Long = -1

    // gamification
    var pointEarned: Int = 0
    var badgeEarned: String? = null
    var badgeLevelEarned: Int? = null
    var pointsInCurrentLevel: Int? = null
    var pointsToNextLevel: Int? = null

    fun incrementInteraction() {
        interactionCount += 1

        val timeNow = System.currentTimeMillis()
        if (timeOfFirstInteraction < 0) {
            timeOfFirstInteraction = timeNow
        }
        timeOfLastInteraction = timeNow
    }

    fun widgetDisplayed() {
        timeOfFirstDisplay = System.currentTimeMillis()
    }

    fun reset() {
        interactionCount = 0
        timeOfFirstInteraction = -1
        timeOfLastInteraction = -1
    }

    override fun toString(): String {
        return "interactionCount: $interactionCount, timeOfFirstInteraction:$timeOfFirstInteraction, timeOfLastInteraction: $timeOfLastInteraction"
    }
}

class AnalyticsWidgetSpecificInfo {
    var responseChanges: Int = 0
    var finalAnswerIndex: Int = -1
    var totalOptions: Int = 0
    var userVotePercentage: Int = 0
    var votePosition: Int = 0
    var widgetResult: String = ""

    fun reset() {
        responseChanges = 0
        finalAnswerIndex = -1
        totalOptions = 0
        userVotePercentage = 0
        votePosition = 0
        widgetResult = ""
    }
}

internal var eventObservers: MutableMap<String, ((String, JSONObject) -> Unit)?> = mutableMapOf()

class MixpanelAnalytics(val context: Context, token: String?, private val clientId: String) :
    AnalyticsService {

    private var mixpanel: MixpanelAPI = MixpanelExtension.getUniqueInstance(
        context,
        token ?: "5c82369365be76b28b3716f260fbd2f5",
        clientId
    )

    private var parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    init {
        trackSessionStarted()
        JSONObject().apply {
            put("Client ID", clientId)
            put("SDK Version", BuildConfig.SDK_VERSION)
            put("Official App Name", getApplicationName(context))
            put("Bundle Id", context.packageName)
            put("Operating System", "Android")
            mixpanel.registerSuperProperties(this)
            mixpanel.people.set(this)
        }
        val packageName = context.packageName ?: ""

        context.getSharedPreferences("$packageName-analytics", Context.MODE_PRIVATE).apply {
            if (getBoolean("firstSdkOpen", true)) {
                edit().putBoolean("firstSdkOpen", false).apply()
                JSONObject().apply {
                    val currentDate = parser.format(Date())
                    put("First SDK Open", currentDate)
                    mixpanel.registerSuperPropertiesOnce(this)
                    mixpanel.people.set(this)
                }
            }
        }
    }

    override fun setEventObserver(eventObserver: (String, JSONObject) -> Unit) {
        eventObservers[clientId] = eventObserver
    }

    override fun trackFlagButtonPressed() {
        mixpanel.track(KEY_FLAG_BUTTON_PRESSED)
        eventObservers[clientId]?.invoke(KEY_KEYBOARD_SELECTED, JSONObject())
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackReportingMessage() {
        val properties = JSONObject()
        properties.put(KEY_REASON, "Reporting Message")
        mixpanel.track(KEY_FLAG_ACTION_SELECTED, properties)
        eventObservers[clientId]?.invoke(KEY_FLAG_ACTION_SELECTED, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackBlockingUser() {
        val properties = JSONObject()
        properties.put(KEY_REASON, "Blocking User")
        mixpanel.track(KEY_FLAG_ACTION_SELECTED, properties)
        eventObservers[clientId]?.invoke(KEY_FLAG_ACTION_SELECTED, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackCancelFlagUi() {
        val properties = JSONObject()
        properties.put(KEY_REASON, "Blocking User")
        mixpanel.track(KEY_FLAG_ACTION_SELECTED, properties)
        eventObservers[clientId]?.invoke(KEY_FLAG_ACTION_SELECTED, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun registerSuperAndPeopleProperty(event: Pair<String, String>) {
        JSONObject().apply {
            put(event.first, event.second)
            mixpanel.registerSuperProperties(this)
            mixpanel.people.set(this)
            eventObservers[clientId]?.invoke(event.first, this)
        }
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $event")
    }

    private fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(
            stringId
        )
    }

    override fun trackLastChatStatus(status: Boolean) {
        JSONObject().apply {
            put("Last Chat Status", if (status) "Enabled" else "Disabled")
            mixpanel.registerSuperProperties(this)
            mixpanel.people.set(this)
            eventObservers[clientId]?.invoke("Last Chat Status", this)
        }
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $status")
    }

    override fun trackLastWidgetStatus(status: Boolean) {
        JSONObject().apply {
            put("Last Widget Status", if (status) "Enabled" else "Disabled")
            mixpanel.registerSuperProperties(this)
            mixpanel.people.set(this)
            eventObservers[clientId]?.invoke("Last Widget Status", this)
        }
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $status")
    }

    override fun trackConfiguration(internalAppName: String) {
        JSONObject().apply {
            put("Internal App Name", internalAppName)
            mixpanel.registerSuperPropertiesOnce(this)
            mixpanel.people.set(this)
        }
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $internalAppName")
    }

    private fun getKeyboardType(kType: KeyboardType): String {
        return when (kType) {
            KeyboardType.STANDARD -> "Standard"
            KeyboardType.STICKER -> "Sticker"
        }
    }

    override fun trackKeyboardClose(
        keyboardType: KeyboardType,
        hideMethod: KeyboardHideReason,
        chatMessageId: String?
    ) {
        val properties = JSONObject()
        properties.put("Keyboard Type", getKeyboardType(keyboardType))

        val hideReason = when (hideMethod) {
            KeyboardHideReason.TAP_OUTSIDE -> "Dismissed Via Tap Outside"
            KeyboardHideReason.MESSAGE_SENT -> "Sent Message"
            KeyboardHideReason.CHANGING_KEYBOARD_TYPE -> "Dismissed Via Changing Keyboard Type"
            KeyboardHideReason.BACK_BUTTON -> "Dismissed Via Back Button"
            KeyboardHideReason.EXPLICIT_CALL -> "Dismissed Via explicit call"
        }
        properties.put("Keyboard Hide Method", hideReason)
        chatMessageId?.apply {
            properties.put(CHAT_MESSAGE_ID, chatMessageId)
        }
        mixpanel.track(KEY_KEYBOARD_HIDDEN, properties)
        eventObservers[clientId]?.invoke(KEY_KEYBOARD_HIDDEN, properties)
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $keyboardType $hideMethod"
        )
    }

    override fun trackKeyboardOpen(keyboardType: KeyboardType) {
        val properties = JSONObject()
        properties.put("Keyboard Type", getKeyboardType(keyboardType))
        mixpanel.track(KEY_KEYBOARD_SELECTED, properties)
        eventObservers[clientId]?.invoke(KEY_KEYBOARD_SELECTED, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $keyboardType")
    }

    override fun trackWidgetInteraction(
        kind: String,
        id: String,
        programId: String,
        interactionInfo: AnalyticsWidgetInteractionInfo
    ) {
        val properties = JSONObject()
        val timeOfLastInteraction = parser.format(Date(interactionInfo.timeOfLastInteraction))
        properties.put("Widget Type", kind)
        properties.put("Widget ID", id)
        properties.put(PROGRAM_ID, programId)
        // this properties are not being used, and has been decided to remove (https://livelike.atlassian.net/browse/ES-2471)
       /* properties.put(
            "First Tap Time",
            parser.format(Date(interactionInfo.timeOfFirstInteraction))
        )
        properties.put("Last Tap Time", timeOfLastInteraction)
        properties.put("No of Taps", interactionInfo.interactionCount)*/
        properties.put("Points Earned", interactionInfo.pointEarned)

        interactionInfo.badgeEarned?.let {
            properties.put("Badge Earned", interactionInfo.badgeEarned)
            properties.put("Badge Level Earned", interactionInfo.badgeLevelEarned)
        }
        interactionInfo.pointsInCurrentLevel?.let { properties.put("Points In Current Level", it) }
        interactionInfo.pointsToNextLevel?.let { properties.put("Points To Next Level", it) }

        mixpanel.track(KEY_WIDGET_INTERACTION, properties)
        eventObservers[clientId]?.invoke(KEY_WIDGET_INTERACTION, properties)

        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $kind $programId $interactionInfo"
        )
    }

    override fun trackWidgetEngaged(kind: String, id: String, programId: String) {
        val properties = JSONObject()
        properties.put("Widget Type", kind)
        properties.put("Widget ID", id)
        properties.put(PROGRAM_ID, programId)
        mixpanel.track(KEY_WIDGET_ENGAGED, properties)
        eventObservers[clientId]?.invoke(KEY_WIDGET_ENGAGED, properties)
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $kind $id $programId"
        )
    }

    override fun trackSessionStarted() {
        val firstTimeProperties = JSONObject()
        val timeNow = parser.format(Date(System.currentTimeMillis()))
        firstTimeProperties.put("Session started", timeNow)
        mixpanel.registerSuperPropertiesOnce(firstTimeProperties)
        eventObservers[clientId]?.invoke("Session started", firstTimeProperties)

        val properties = JSONObject()
        properties.put("Last Session started", timeNow)
        mixpanel.registerSuperProperties(properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackPointTutorialSeen(completionType: String, secondsSinceStart: Long) {
        val properties = JSONObject()
        properties.put("Completion Type", completionType)
        properties.put("Dismiss Seconds Since Start", secondsSinceStart)
        mixpanel.track(KEY_POINT_TUTORIAL_COMPLETED, properties)
        eventObservers[clientId]?.invoke(KEY_POINT_TUTORIAL_COMPLETED, properties)
    }

    override fun trackPointThisProgram(points: Int) {
        JSONObject().apply {
            put(AnalyticsSuperProperties.POINTS_THIS_PROGRAM.key, points)
            mixpanel.registerSuperProperties(this)
            eventObservers[clientId]?.invoke(
                AnalyticsSuperProperties.POINTS_THIS_PROGRAM.key,
                this
            )
        }
    }

    override fun trackBadgeCollectedButtonPressed(badgeId: String, badgeLevel: Int) {
        val properties = JSONObject()
        properties.put("Badge ID", badgeId)
        properties.put("Level", badgeLevel)
        mixpanel.track(KEY_EVENT_BADGE_COLLECTED_BUTTON_PRESSED, properties)
        eventObservers[clientId]?.invoke(KEY_EVENT_BADGE_COLLECTED_BUTTON_PRESSED, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]$badgeId $badgeLevel")
    }

    override fun trackChatReactionPanelOpen(messageId: String) {
        val properties = JSONObject()
        properties.put(CHAT_MESSAGE_ID, messageId)
        mixpanel.track(KEY_EVENT_CHAT_REACTION_PANEL_OPEN, properties)
        eventObservers[clientId]?.invoke(KEY_EVENT_CHAT_REACTION_PANEL_OPEN, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]$messageId")
    }

    override fun trackChatReactionSelected(
        chatRoomId: String,
        messageId: String,
        reactionId: String,
        isRemoved: Boolean
    ) {
        val properties = JSONObject()
        properties.put(CHAT_MESSAGE_ID, messageId)
        properties.put(CHAT_REACTION_ID, reactionId)
        properties.put(CHAT_ROOM_ID, chatRoomId)
        val event = when (isRemoved) {
            true -> KEY_EVENT_CHAT_REACTION_REMOVED
            else -> KEY_EVENT_CHAT_REACTION_ADDED
        }
        mixpanel.track(
            event, properties
        )
        eventObservers[clientId]?.invoke(event, properties)
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}]$messageId $reactionId $isRemoved"
        )
    }

    override fun trackVideoAlertPlayed(
        kind: String,
        id: String,
        programId: String,
        videoUrl: String
    ) {
        val properties = JSONObject()
        properties.put("Widget ID", id)
        properties.put("Widget Type", kind)
        properties.put(PROGRAM_ID, programId)
        properties.put(VIDEO_URL, videoUrl)
        mixpanel.track(KEY_EVENT_VIDEO_ALERT_PLAY_STARTED, properties)
        eventObservers[clientId]?.invoke(KEY_EVENT_VIDEO_ALERT_PLAY_STARTED, properties)
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}]$id $programId $videoUrl"
        )
    }

    override fun trackMessageLinkClicked(
        chatRoomId: String,
        chatRoomName: String?,
        messageId: String?,
        link: String
    ) {
        val properties = JSONObject()
        properties.put(CHAT_ROOM_ID, chatRoomId)
        properties.put("Chat Room Title", chatRoomName)
        properties.put(CHAT_MESSAGE_ID, messageId)
        properties.put("Chat Message Link", link)
        mixpanel.track(KEY_EVENT_CHAT_MESSAGE_LINK_CLICKED, properties)
        eventObservers[clientId]?.invoke(KEY_EVENT_CHAT_MESSAGE_LINK_CLICKED, properties)
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}]$chatRoomId $chatRoomName $messageId $link"
        )
    }

    override fun destroy() {
        mixpanel.flush()
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}]"
        )
    }

    override fun trackAlertLinkOpened(
        alertId: String,
        programId: String,
        linkUrl: String,
        widgetType: WidgetType?
    ) {
        val properties = JSONObject()
        properties.put(ALERT_ID, alertId)
        properties.put(PROGRAM_ID, programId)
        properties.put(LINK_URL, linkUrl)
        properties.put(WIDGET_TYPE, widgetType?.toAnalyticsString() ?: "")
        mixpanel.track(KEY_EVENT_ALERT_LINK_OPENED, properties)
        eventObservers[clientId]?.invoke(KEY_EVENT_ALERT_LINK_OPENED, properties)
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}]$alertId $programId $linkUrl"
        )
    }

    override fun registerSuperProperty(
        analyticsSuperProperties: AnalyticsSuperProperties,
        value: Any?
    ) {
        JSONObject().apply {
            put(analyticsSuperProperties.key, value ?: JsonNull.INSTANCE)
            mixpanel.registerSuperProperties(this)
            if (analyticsSuperProperties.isPeopleProperty) {
                mixpanel.people.set(this)
            }
            eventObservers[clientId]?.invoke(analyticsSuperProperties.key, this)
            Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]$value")
        }
    }

    override fun trackMessageSent(
        msgId: String,
        msg: String?,
        hasExternalImage: Boolean,
        chatRoomId: String
    ) {
        val properties = JSONObject()
        properties.put(CHAT_MESSAGE_ID, msgId)
        properties.put("Character Length", (if (hasExternalImage) 0 else msg?.length ?: 0))
        properties.put("Sticker Count", msg?.findStickers()?.countMatches())
        properties.put(
            "Sticker Shortcodes",
            if (hasExternalImage) arrayListOf() else msg?.findStickerCodes()?.allMatches()
        )
        properties.put("Has External Image", hasExternalImage)
        properties.put("Chat Room ID", chatRoomId)
        mixpanel.track(KEY_CHAT_MESSAGE_SENT, properties)
        eventObservers[clientId]?.invoke(KEY_CHAT_MESSAGE_SENT, properties)

        val superProp = JSONObject()
        val timeNow = parser.format(Date(System.currentTimeMillis()))
        superProp.put("Time of Last Chat Message", timeNow)
        mixpanel.registerSuperProperties(superProp)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $msgId")
    }

    override fun trackMessageDisplayed(msgId: String, msg: String?, hasExternalImage: Boolean) {
        val properties = JSONObject()
        properties.put(CHAT_MESSAGE_ID, msgId)
        properties.put("Message ID", msgId)
        properties.put(
            "Sticker Shortcodes",
            msg?.findStickerCodes()?.allMatches() ?: listOf<String>()
        )
        mixpanel.track(KEY_CHAT_MESSAGE_DISPLAYED, properties)
        eventObservers[clientId]?.invoke(KEY_CHAT_MESSAGE_DISPLAYED, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $msgId")
    }

    override fun trackWidgetDisplayed(
        kind: String,
        id: String,
        programId: String,
        linkUrl: String?
    ) {
        val properties = JSONObject()
        properties.put("Widget Type", kind)
        properties.put("Widget ID", id)
        properties.put(PROGRAM_ID, programId)
        linkUrl?.let { properties.put(LINK_URL, it) }
        mixpanel.track(KEY_WIDGET_DISPLAYED, properties)
        eventObservers[clientId]?.invoke(KEY_WIDGET_DISPLAYED, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $kind $programId")
    }

    override fun trackWidgetBecameInteractive(
        kind: String,
        id: String,
        programId: String,
        linkUrl: String?
    ) {
        val properties = JSONObject()
        properties.put("Widget Type", kind)
        properties.put("Widget ID", id)
        properties.put(PROGRAM_ID, programId)
        linkUrl?.let { properties.put(LINK_URL, it) }
        mixpanel.track(KEY_WIDGET_BECAME_INTERACTIVE, properties)
        eventObservers[clientId]?.invoke(KEY_WIDGET_BECAME_INTERACTIVE, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $kind $programId")
    }

    override fun trackWidgetReceived(kind: String, id: String) {
        val properties = JSONObject()
        properties.put("Widget Type", kind)
        properties.put("Widget Id", id)
        mixpanel.track(KEY_WIDGET_RECEIVED, properties)
        mixpanel.registerSuperProperties(properties)
        eventObservers[clientId]?.invoke(KEY_WIDGET_RECEIVED, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $kind")
    }

    override fun trackWidgetDismiss(
        kind: String,
        id: String,
        programId: String,
        interactionInfo: AnalyticsWidgetInteractionInfo?,
        interactable: Boolean?,
        action: DismissAction
    ) {

        if (action == DismissAction.TIMEOUT) {
            return
        }
        val dismissAction = when (action) {
            DismissAction.TAP_X -> "Tap X"
            DismissAction.SWIPE -> "Swipe"
            else -> ""
        }

        val interactionState =
            if (interactable == null) null else (if (interactable) "Open To Interaction" else "Closed To Interaction")
        val packageName = context.packageName ?: ""
        context.getSharedPreferences("$packageName-analytics", Context.MODE_PRIVATE).apply {
            val properties = JSONObject()
            properties.put("Widget Type", kind)
            properties.put("Widget ID", id)
            properties.put("Dismiss Action", dismissAction)
            properties.put(PROGRAM_ID, programId)

            interactionInfo?.let {
                properties.put("Number Of Taps", interactionInfo.interactionCount)
                val timeNow = System.currentTimeMillis()
                val timeSinceLastTap =
                    (timeNow - interactionInfo.timeOfLastInteraction).toFloat() / 1000
                val timeSinceStart = (timeNow - interactionInfo.timeOfFirstDisplay).toFloat() / 1000
                properties.put("Dismiss Seconds Since Last Tap", timeSinceLastTap)
                properties.put("Dismiss Seconds Since Start", timeSinceStart)
            }
            properties.put("Interactable State", interactionState)
            properties.put("Last Widget Type", getString("lastWidgetType", ""))
            mixpanel.track(KEY_WIDGET_USER_DISMISS, properties)
            eventObservers[clientId]?.invoke(KEY_WIDGET_USER_DISMISS, properties)

            edit().putString("lastWidgetType", kind).apply()
            Log.d(
                "[Analytics]",
                "[${object {}.javaClass.enclosingMethod?.name}] $kind $action $programId $interactionInfo"
            )
        }
    }

    private var lastOrientation: Boolean? = null

    override fun trackInteraction(
        kind: String,
        id: String,
        interactionType: String,
        interactionCount: Int
    ) {
        val properties = JSONObject()
        properties.put("kind", kind)
        properties.put("id", id)
        properties.put("interactionType", interactionType)
        properties.put("interactionCount", interactionCount)
        mixpanel.track(KEY_WIDGET_INTERACTION, properties)
        eventObservers[clientId]?.invoke(KEY_WIDGET_INTERACTION, properties)
        Log.d(
            "[Analytics]",
            "[${object {}.javaClass.enclosingMethod?.name}] $kind $interactionType"
        )
    }

    override fun trackOrientationChange(isPortrait: Boolean) {
        if (lastOrientation == isPortrait) return // return if the orientation stays the same
        lastOrientation = isPortrait
        JSONObject().apply {
            put("Device Orientation", if (isPortrait) "Portrait" else "Landscape")
            mixpanel.registerSuperProperties(this)
        }
        JSONObject().apply {
            put("Last Device Orientation", if (isPortrait) "Portrait" else "Landscape")
            mixpanel.people.set(this)
        }
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $isPortrait")
    }

    override fun trackButtonTap(buttonName: String, extra: JsonObject) {
        val properties = JSONObject()
        properties.put("buttonName", buttonName)
        properties.put("extra", extra)
        mixpanel.track(KEY_ACTION_TAP, properties)
        eventObservers[clientId]?.invoke(KEY_ACTION_TAP, properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $buttonName")
    }

    override fun trackSession(sessionId: String) {
        mixpanel.identify(sessionId)
        mixpanel.people.identify(sessionId)
    }

    override fun trackUsername(username: String) {
        mixpanel.people.set("Nickname", username)
        val properties = JSONObject()
        properties.put("Nickname", username)
        mixpanel.registerSuperProperties(properties)
        eventObservers[clientId]?.invoke("Nickname", properties)
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}] $username")
    }

    companion object {
        const val KEY_CHAT_MESSAGE_SENT = "Chat Message Sent"
        const val KEY_CHAT_MESSAGE_DISPLAYED = "Chat Message Displayed"
        const val KEY_WIDGET_RECEIVED = "Widget_Received"
        const val KEY_WIDGET_DISPLAYED = "Widget Displayed"
        const val KEY_WIDGET_BECAME_INTERACTIVE = "Widget Became Interactive"
        const val KEY_WIDGET_INTERACTION = "Widget Interacted"
        const val KEY_WIDGET_ENGAGED = "Widget Engaged"
        const val KEY_WIDGET_USER_DISMISS = "Widget Dismissed"
        const val KEY_ORIENTATION_CHANGED = "Orientation_Changed"
        const val KEY_ACTION_TAP = "Action_Tap"
        const val KEY_KEYBOARD_SELECTED = "Keyboard Selected"
        const val KEY_KEYBOARD_HIDDEN = "Keyboard Hidden"
        const val KEY_FLAG_BUTTON_PRESSED = "Chat Flag Button Pressed"
        const val KEY_FLAG_ACTION_SELECTED = "Chat Flag Action Selected"
        const val KEY_POINT_TUTORIAL_COMPLETED = "Points Tutorial Completed"
        const val KEY_REASON = "Reason"
        const val KEY_EVENT_BADGE_COLLECTED_BUTTON_PRESSED = "Badge Collected Button Pressed"
        const val KEY_EVENT_CHAT_REACTION_PANEL_OPEN = "Chat Reaction Panel Opened"
        const val KEY_EVENT_CHAT_REACTION_ADDED = "Chat Reaction Added"
        const val KEY_EVENT_CHAT_REACTION_REMOVED = "Chat Reaction Removed"
        const val KEY_EVENT_ALERT_LINK_OPENED = "Alert Link Opened"
        const val KEY_EVENT_VIDEO_ALERT_PLAY_STARTED = "Video Alert Play Started"
        const val KEY_EVENT_CHAT_MESSAGE_LINK_CLICKED = "Chat Message Link Clicked"
    }
}

enum class KeyboardHideReason {
    MESSAGE_SENT,
    CHANGING_KEYBOARD_TYPE,
    TAP_OUTSIDE,
    BACK_BUTTON,
    EXPLICIT_CALL // it was added to expose control to integrators.
}

enum class KeyboardType {
    STANDARD,
    STICKER
}

enum class DismissAction {
    TIMEOUT,
    SWIPE,
    TAP_X
}

const val CHAT_MESSAGE_ID = "Chat Message ID"
const val ALERT_ID = "Alert Id"
const val PROGRAM_ID = "Program ID"
const val LINK_URL = "Link URL"
const val VIDEO_URL = "Video URL"
const val CHAT_REACTION_ID = "Chat Reaction ID"
const val CHAT_ROOM_ID = "Chat Room ID"
const val WIDGET_TYPE = "Widget Type"
