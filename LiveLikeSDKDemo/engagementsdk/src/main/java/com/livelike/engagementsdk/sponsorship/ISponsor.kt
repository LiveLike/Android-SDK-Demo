package com.livelike.engagementsdk.sponsorship

import com.livelike.engagementsdk.publicapis.LiveLikeCallback

interface ISponsor {

    /**
     * Fetch sponsor associated to the specified program Id
     **/
    fun fetchByProgramId(programId: String, callback: LiveLikeCallback<List<SponsorModel>>)
}
