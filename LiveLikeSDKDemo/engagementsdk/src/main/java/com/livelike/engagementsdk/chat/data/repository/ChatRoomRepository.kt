package com.livelike.engagementsdk.chat.data.repository

import com.livelike.engagementsdk.core.data.respository.BaseRepository
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.publicapis.ChatUserMuteStatus

/**
 * All chat room related service api should be added here
 */
internal object ChatRoomRepository : BaseRepository() {

    suspend fun getUserRoomMuteStatus(url: String): Result<ChatUserMuteStatus> {
        return dataClient.remoteCall<ChatUserMuteStatus>(url, RequestType.GET, accessToken = null)
    }
}
