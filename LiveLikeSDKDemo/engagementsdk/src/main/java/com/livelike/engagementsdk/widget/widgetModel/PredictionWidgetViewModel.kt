package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.PredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface PredictionWidgetViewModel : LiveLikeWidgetMediator {

    /**
     * live stream for vote results
     */
    val voteResults: Stream<LiveLikeWidgetResult>

    /**
     * lock the answer for prediction
     */
    fun lockInVote(optionID: String)

    /**
     * it returns the latest user interaction for the widget
     */
    fun getUserInteraction(): PredictionWidgetUserInteraction?

    /**
     * returns widget interactions from remote source
     */
    fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<PredictionWidgetUserInteraction>>)
}
