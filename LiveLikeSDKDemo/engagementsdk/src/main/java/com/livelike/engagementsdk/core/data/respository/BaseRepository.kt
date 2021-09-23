package com.livelike.engagementsdk.core.data.respository

import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl

internal abstract class BaseRepository {

    protected val dataClient = EngagementDataClientImpl()
}
