package com.livelike.engagementsdk.widget.data.respository

import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.widget.data.models.UserWidgetInteractionApi

/**
 * Widget Interaction Remote source responsible for fetching the interaction data from our rest apis
 **/
internal class WidgetInteractionRemoteSource {

    private val engagementDataClientImpl: EngagementDataClientImpl = EngagementDataClientImpl()

    internal suspend fun getWidgetInteractions(url: String, accessToken: String): Result<UserWidgetInteractionApi> {
        return engagementDataClientImpl.remoteCall<UserWidgetInteractionApi>(url, RequestType.GET, accessToken = accessToken)
    }
}
