package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.PollWidgetUserInteraction
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface PollWidgetModel : LiveLikeWidgetMediator {

    val voteResults: Stream<LiveLikeWidgetResult>

    fun submitVote(optionID: String)

    /**
     * it returns the latest user interaction for the widget
     */
    fun getUserInteraction(): PollWidgetUserInteraction?

    /**
     * returns widget interactions from remote source
     */
    fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<PollWidgetUserInteraction>>)
}
