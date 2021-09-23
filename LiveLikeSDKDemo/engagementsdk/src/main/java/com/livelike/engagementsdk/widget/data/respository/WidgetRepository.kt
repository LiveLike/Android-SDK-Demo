package com.livelike.engagementsdk.widget.data.respository

import com.livelike.engagementsdk.core.data.respository.BaseRepository
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl

internal open class WidgetRepository : BaseRepository() {

    val widgetDataClient = WidgetDataClientImpl()
}
