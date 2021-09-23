package com.livelike.engagementsdk.core.services.network

import com.google.gson.JsonObject
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.core.data.models.Program

internal interface DataClient {
    fun getProgramData(url: String, responseCallback: (program: Program?, error: String?) -> Unit)
    fun getUserData(
        profileUrl: String,
        accessToken: String,
        responseCallback: (livelikeUser: LiveLikeUser?) -> Unit
    )

    fun createUserData(profileUrl: String, responseCallback: (livelikeUser: LiveLikeUser) -> Unit)
    suspend fun patchUser(profileUrl: String, userJson: JsonObject, accessToken: String?)
}

internal interface EngagementSdkDataClient {
    fun getEngagementSdkConfig(
        url: String,
        responseCallback: (config: Result<EngagementSDK.SdkConfiguration>) -> Unit
    )
}
