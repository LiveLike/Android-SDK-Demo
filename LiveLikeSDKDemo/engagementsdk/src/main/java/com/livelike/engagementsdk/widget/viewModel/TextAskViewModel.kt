package com.livelike.engagementsdk.widget.viewModel

import com.google.gson.JsonParseException
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.formatIsoZoned8601
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.TextAskUserInteraction
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.widgetModel.TextAskWidgetModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.FormBody
import org.threeten.bp.ZonedDateTime
import java.io.IOException

internal class TextAskWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class TextAskViewModel(
    val widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    private val sdkConfiguration: EngagementSDK.SdkConfiguration,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository? = null,
    val widgetMessagingClient: WidgetManager? = null,
    val widgetInteractionRepository: WidgetInteractionRepository?
) : BaseViewModel(analyticsService), TextAskWidgetModel {

    val data: SubscriptionManager<TextAskWidget> =
        SubscriptionManager()

    val results: Stream<Resource> =
        SubscriptionManager()

    private var timeoutStarted = false
    private var currentWidgetId: String = ""
    private var programId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()
    var animationEggTimerProgress = 0f

    init {
        widgetObserver(widgetInfos)
    }

    /** responsible for submitting the user response */
    override fun submitReply(response: String) {
        trackWidgetEngagedAnalytics(
            currentWidgetType, currentWidgetId,
            programId
        )
        uiScope.launch {
            data.currentData?.resource?.reply_url?.let {
                dataClient.submitReplyAsync(
                    it,
                    FormBody.Builder()
                        .add("text", response).build(),
                    accessToken = userRepository.userAccessToken
                )
                saveInteraction(response)
            }
        }
    }

    fun lockAndSubmitReply(response: String) {
        uiScope.launch {
            data.currentData?.resource?.reply_url?.let {
                dataClient.submitReplyAsync(
                    it,
                    FormBody.Builder()
                        .add("text", response).build(),
                    accessToken = userRepository.userAccessToken
                )
                saveInteraction(response)
            }
        }
    }

    override fun getUserInteraction(): TextAskUserInteraction? {
        return widgetInteractionRepository?.getWidgetInteraction(
            widgetInfos.widgetId,
            WidgetKind.fromString(widgetInfos.type)
        )
    }

    override fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<TextAskUserInteraction>>) {
        uiScope.launch {
            try {
                val results =
                    widgetInteractionRepository?.fetchRemoteInteractions(widgetId = widgetInfos.widgetId, widgetKind = widgetInfos.type)

                if (results is Result.Success) {
                    liveLikeCallback.onResponse(
                        results.data.interactions.textAsk, null
                    )
                } else if (results is Result.Error) {
                    liveLikeCallback.onResponse(
                        null, results.exception.message
                    )
                }
            } catch (e: JsonParseException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            } catch (e: IOException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            }
        }
    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null) {
            val resource =
                gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {
                data.onNext(
                    WidgetType.fromString(widgetInfos.type)?.let {
                        TextAskWidget(
                            it,
                            resource
                        )
                    }
                )
            }
            logDebug { "reply url ${data.latest()?.resource?.reply_url}" }
            currentWidgetId = widgetInfos.widgetId
            programId = data.latest()?.resource?.program_id.toString()
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
            interactionData.widgetDisplayed()
        }
    }

    override fun finish() {
        onDismiss()
        cleanUp()
    }

    override fun markAsInteractive() {
        trackWidgetBecameInteractive(currentWidgetType, currentWidgetId, programId)
    }
    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                widgetState.onNext(WidgetStates.RESULTS)
            }
        }
    }

    internal fun saveInteraction(response: String) {
        widgetInteractionRepository?.saveWidgetInteraction(
            TextAskUserInteraction(
                "",
                ZonedDateTime.now().formatIsoZoned8601(),
                getUserInteraction()?.url,
                response,
                widgetInfos.widgetId,
                widgetInfos.type
            )
        )
    }

    /** logic to be added here, when claiming rewards/ points for submitting response
     * presently not available on first iteration */
    internal fun confirmationState() {
        // to be added auto claim rewards logic here
        currentWidgetType?.let {
            analyticsService.trackWidgetInteraction(
                it.toAnalyticsString(),
                currentWidgetId,
                programId,
                interactionData
            )
        }
        uiScope.launch {
            delay(2000)
            dismissWidget(DismissAction.TIMEOUT)
        }
    }

    fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it.toAnalyticsString(),
                currentWidgetId,
                programId,
                interactionData,
                false,
                action
            )
        }
        widgetState.onNext(WidgetStates.FINISHED)
        logDebug { "dismiss AMA Widget, reason:${action.name}" }
        onDismiss()
        cleanUp()
    }

    private fun cleanUp() {
        data.onNext(null)
        timeoutStarted = false
        currentWidgetType = null
        animationEggTimerProgress = 0f
        interactionData.reset()
    }

    override fun onClear() {
        cleanUp()
    }
}
