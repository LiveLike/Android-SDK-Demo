package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.LiveLikeWidget

interface LiveLikeWidgetMediator {

    /**
     * widget data holder
     */
    val widgetData: LiveLikeWidget

    /**
     * call this to cleanup the viewModel and its association
     */
    fun finish()

    /**
     * This will capture the analytics event Widget Became Interactive
     * It should be called when the widget is opened first time for user interaction
     **/
    fun markAsInteractive()
}
