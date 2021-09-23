package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.TextAskUserInteraction
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface TextAskWidgetModel : LiveLikeWidgetMediator {

    /**
     * submit response entered
     */
    fun submitReply(response: String)

    /**
     * it returns the latest user interaction for the widget
     */
    fun getUserInteraction(): TextAskUserInteraction?

    /**
     * returns widget interactions from remote source
     */
    fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<TextAskUserInteraction>>)
}
