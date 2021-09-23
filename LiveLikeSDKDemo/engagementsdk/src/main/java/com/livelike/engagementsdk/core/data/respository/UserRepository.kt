package com.livelike.engagementsdk.core.data.respository

import com.google.gson.JsonObject
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.data.models.RewardItem
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getNickename
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.setNickname
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.data.respository.WidgetRepository
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate
import com.livelike.engagementsdk.widget.domain.UserProfileDelegate
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Repository that handles user data. It knows what data sources need to be
 * triggered to get user and where to store the data.
 * In Typical frontend application we have local and remote data source, we will move towards that gradually[TODO].
 */
internal class UserRepository(private val clientId: String) : WidgetRepository() {

    /**
     *  User returned will be new if no access-token passed.
     *  Null latest value in stream means sdk initialization process not completed yet.
     */
    val currentUserStream: Stream<LiveLikeUser> =
        SubscriptionManager()

    val userAccessToken: String?
        get() = currentUserStream.latest()?.accessToken

    val lifetimePoints: Stream<Int> =
        SubscriptionManager()
    val rank: Stream<Int> =
        SubscriptionManager()
    private var profileUrl: String = ""

    var userProfileDelegate: UserProfileDelegate? = null

    var leaderBoardDelegate: LeaderBoardDelegate? = null

    val rewardItemMapCache: MutableMap<String, RewardItem> = mutableMapOf()

    /**
     * Create or init user according to passed access token.
     * If no access token new user profile will be created.
     * If invalid token passed then also new user created with error.
     */
    fun initUser(userAccessToken: String?, profileUrl: String) {
        this.profileUrl = profileUrl
        if (userAccessToken == null || userAccessToken.isEmpty()) {
            logError {
                "The EngagementSDK is creating a new User Profile because it was initialized without an existing Access Token.\n" +
                    "The created User Profile will be counted towards the Monthly Active Users (MAU) calculation.\n" +
                    "For more information: https://docs.livelike.com/docs/user-profiles"
            }
            dataClient.createUserData(profileUrl) {
                publishUser(it)
            }
        } else {
            dataClient.getUserData(profileUrl, accessToken = userAccessToken) {
                // TODO add Result class wrapper for network result instead of treating null as a case of invalid access token
                if (it == null) {
                    logError { "Network error or invalid access token" }
                    initUser(null, profileUrl)
                } else {
                    publishUser(it)
                }
            }
        }
    }

    private fun publishUser(it: LiveLikeUser) {
        val nickname =
            getNickename() // Checking again the saved nickname as it could have changed during the web request.
        if (nickname.isNotEmpty() && !it.nickname.equals(nickname)) {
            it.nickname = nickname
            GlobalScope.launch {
                patchNickNameOnRemote(it)
            }
        }
        currentUserStream.onNext(it)
    }

    suspend fun updateChatNickname(nickname: String) {
        setNickname(
            nickname
        )
        currentUserStream.latest()?.apply {
            this.nickname = nickname
            patchNickNameOnRemote(this)
            currentUserStream.onNext(this)
        }
    }

    private suspend fun patchNickNameOnRemote(liveLikeUser: LiveLikeUser) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("id", liveLikeUser.id)
        jsonObject.addProperty("nickname", liveLikeUser.nickname)
        dataClient.patchUser(profileUrl, jsonObject, userAccessToken ?: liveLikeUser.accessToken)
    }

    var rewardType = "none"

    suspend fun getGamificationReward(
        rewardUrl: String,
        analyticsService: AnalyticsService
    ): ProgramGamificationProfile? {
        if (rewardType == "none") {
            return null
        }
        val reward =
            widgetDataClient.rewardAsync(rewardUrl, analyticsService, accessToken = userAccessToken)
        lifetimePoints.onNext(reward?.points)
        rank.onNext(reward?.rank)
        return reward
    }

    fun setProfilePicUrl(url: String?) {
        currentUserStream.latest()?.apply {
            this.userPic = url
            currentUserStream.onNext(this)
        }
    }

    fun updateRewardItemCache(rewardItems: List<RewardItem>) {
        rewardItems.forEach {
            rewardItemMapCache[it.id] = it
        }
    }
}
