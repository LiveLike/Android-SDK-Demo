package com.livelike.engagementsdk.core.services.messaging.proxies

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.sponsorship.SponsorModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates

/**
 * Integrator will pass implementation of this proxy to intercept widgets,
 * SDK will call widgetWantsToShow() and in same sequence/order sdk will listen on events stream for the decision on that widget.
 */
abstract class WidgetInterceptor {
    /** Called when a widget is received from the CMS */
    abstract fun widgetWantsToShow(widgetData: LiveLikeWidgetEntity)

    /** Unlock the widget and show it on screen */
    fun showWidget() {
        events.onNext(Decision.Show)
    }

    /** Dismiss the widget and won't show it on screen */
    fun dismissWidget() {
        events.onNext(Decision.Dismiss)
    }

    internal enum class Decision {
        Show,
        Dismiss
    }

    internal val events: Stream<Decision> =
        SubscriptionManager(false)
}

abstract class WidgetLifeCycleEventsListener {

    abstract fun onWidgetPresented(widgetData: LiveLikeWidgetEntity)
    abstract fun onWidgetInteractionCompleted(widgetData: LiveLikeWidgetEntity)
    abstract fun onWidgetDismissed(widgetData: LiveLikeWidgetEntity)
    abstract fun onWidgetStateChange(state: WidgetStates, widgetData: LiveLikeWidgetEntity)
    abstract fun onUserInteract(widgetData: LiveLikeWidgetEntity)
// TODO    abstract fun onWidgetCancelled(reason: WidgetCancelReason, widgetData: LiveLikeWidgetEntity)
}

enum class WidgetCancelReason {
    TIME_EXPIRED,
    MANUALLY_DISMISSED
}

class LiveLikeWidgetEntity {

    var id: String? = null

    var kind: String = ""

    var height: Int? = null

    @SerializedName("question")
    var title: String = ""

    var options: List<Option>? = null

    internal var timeout: String? = null
        set(value) {
            field = value
            interactionTime = AndroidResource.parseDuration(value ?: "")
        }

    var interactionTime: Long? = null

    var customData: String? = null

    @field:SerializedName("sponsors")
    var sponsors: List<SponsorModel>? = null

    data class Option(

        val id: String,

        val url: String = "",

        val description: String = "",

        val is_correct: Boolean = false,
        var answer_count: Int? = 0,
        var vote_count: Int? = 0
    )

    init {
        interactionTime = AndroidResource.parseDuration(timeout ?: "")
    }
}
