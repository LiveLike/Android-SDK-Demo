package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface VideoAlertWidgetModel : LiveLikeWidgetMediator {

    fun videoAlertLinkClicked(url: String)

    fun registerPlayStarted()
}
