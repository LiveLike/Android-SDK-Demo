package com.livelike.engagementsdk.gamification

import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.TEMPLATE_PROFILE_ID
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LLPaginatedResult
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.toFlow
import com.livelike.engagementsdk.core.utils.validateUuid
import com.livelike.engagementsdk.gamification.models.Badge
import com.livelike.engagementsdk.gamification.models.BadgeProgress
import com.livelike.engagementsdk.gamification.models.ProfileBadge
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class Badges internal constructor(
    private val applicationResourceStream: Stream<EngagementSDK.SdkConfiguration>,
    private val dataClient: EngagementDataClientImpl,
    private val sdkScope: CoroutineScope
) {

    private var profileBadgesResultMap = mutableMapOf<String, LLPaginatedResult<ProfileBadge>>()

    /**
     * fetch all the badges associated to provided profile id in pages
     * to fetch next page function need to be called again with LiveLikePagination.NEXT and for first call as LiveLikePagination.FIRST
     **/
    fun getProfileBadges(
        profileId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<ProfileBadge>>
    ) {

        if (!validateUuid(profileId)) {
            liveLikeCallback.onResponse(null, "Invalid Profile ID")
            return
        }

        val result = profileBadgesResultMap[profileId]

        var fetchUrl: String? = null

        sdkScope.launch {
            if (result == null || liveLikePagination == LiveLikePagination.FIRST) {
                applicationResourceStream.toFlow().collect { applicatoionResource ->
                    applicatoionResource?.let {
                        dataClient.remoteCall<LiveLikeUser>(
                            it.profileDetailUrlTemplate.replace(
                                TEMPLATE_PROFILE_ID, profileId
                            ),
                            RequestType.GET, null, null
                        ).run {
                            if (this is Result.Success) {
                                fetchUrl = this.data.badgesUrl
                            }
                        }
                    }
                }
            } else {
                fetchUrl = result.getPaginationUrl(liveLikePagination)
            }

            if (fetchUrl == null) {
                liveLikeCallback.onResponse(null, "No more data")
            } else {
                dataClient.remoteCall<LLPaginatedResult<ProfileBadge>>(
                    fetchUrl ?: "",
                    RequestType.GET,
                    null,
                    null
                ).run {
                    if (this is Result.Success) {
                        profileBadgesResultMap[profileId] = this.data
                    }
                    liveLikeCallback.processResult(this)
                }
            }
        }
    }

    private var lastApplicationBadgePage: LLPaginatedResult<Badge>? = null

    /**
     * fetch all the badges associated to the client id passed at initialization of sdk
     * to fetch next page function need to be called again with LiveLikePagination.NEXT and for first call as LiveLikePagination.FIRST
     **/
    fun getApplicationBadges(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<Badge>>
    ) {
        var fetchUrl: String? = null
        sdkScope.launch {
            if (lastApplicationBadgePage == null || liveLikePagination == LiveLikePagination.FIRST) {
                applicationResourceStream.toFlow().collect { applicatoionResource ->
                    applicatoionResource?.let {
                        fetchUrl = it.badgesUrl
                    }
                }
            } else {
                fetchUrl = lastApplicationBadgePage?.getPaginationUrl(liveLikePagination)
            }

            if (fetchUrl == null) {
                liveLikeCallback.onResponse(null, "No more data")
            } else {
                dataClient.remoteCall<LLPaginatedResult<Badge>>(
                    fetchUrl ?: "",
                    RequestType.GET,
                    null,
                    null
                ).run {
                    if (this is Result.Success) {
                        lastApplicationBadgePage = this.data
                    }
                    liveLikeCallback.processResult(this)
                }
            }
        }
    }

    /**
     * fetch all the profile badges progressions for the passed badge Ids
     * @param profileId : id of the profile for which progressions to be looked
     * @param badgeIds : list of all badge-ids, it has a hard limit checkout rest api documentation for latest limit
     **/
    fun getProfileBadgeProgress(
        profileId: String,
        badgeIds: List<String>,
        liveLikeCallback: LiveLikeCallback<List<BadgeProgress>>
    ) {

        if (!validateUuid(profileId)) {
            liveLikeCallback.onResponse(null, "Invalid Profile ID")
            return
        }

        sdkScope.launch {
            applicationResourceStream.toFlow().collect { applicatoionResource ->
                applicatoionResource?.let {
                    dataClient.remoteCall<LiveLikeUser>(
                        it.profileDetailUrlTemplate.replace(
                            TEMPLATE_PROFILE_ID, profileId
                        ),
                        RequestType.GET, null, null
                    ).run {
                        if (this is Result.Success) {
                            val badgeProgressURL = this.data.badgeProgressUrl
                            val httpUrl = badgeProgressURL?.toHttpUrlOrNull()?.newBuilder()?.apply {
                                for (id in badgeIds) {
                                    addQueryParameter("badge_id", id)
                                }
                            }
                            dataClient.remoteCall<List<BadgeProgress>>(
                                httpUrl?.build()!!,
                                RequestType.GET,
                                null,
                                null
                            ).run {
                                liveLikeCallback.processResult(this)
                            }
                        }
                    }
                }
            }
        }
    }
}
