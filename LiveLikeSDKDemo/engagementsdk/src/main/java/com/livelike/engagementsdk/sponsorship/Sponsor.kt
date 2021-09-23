package com.livelike.engagementsdk.sponsorship

import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.validateUuid
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
* Sponsor client allowing to fetch all sponsor related stuff from Livelike's CMS
*/

class Sponsor(private val sdk: EngagementSDK) : ISponsor {

    override fun fetchByProgramId(
        programId: String,
        callback: LiveLikeCallback<List<SponsorModel>>
    ) {

        if (!validateUuid(programId)) {
            callback.onResponse(null, "invalid program ID")
            return
        }

        val programRepository = ProgramRepository(programId, sdk.userRepository)
        sdk.sdkScope.launch {
            sdk.configurationUserPairFlow.collect {
                val result = programRepository.getProgramData(it.second.programDetailUrlTemplate)
                if (result is Result.Success) {
                    callback.onResponse(result.data.sponsors, null)
                } else if (result is Result.Error) {
                    callback.onResponse(null, result.exception.message ?: "Error in fetching data")
                }
            }
        }
    }
}
