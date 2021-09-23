package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface AlertWidgetModel : LiveLikeWidgetMediator {

    fun alertLinkClicked(url: String)
}
