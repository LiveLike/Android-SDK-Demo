package com.livelike.engagementsdk.publicapis

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntry
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntryPaginationResult
import com.livelike.engagementsdk.core.data.models.LeaderboardClient
import com.livelike.engagementsdk.gamification.Badges
import com.livelike.engagementsdk.sponsorship.Sponsor
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate
import com.livelike.engagementsdk.widget.domain.UserProfileDelegate

interface IEngagement {

//    TODO add remaining public SDK functions in this interface
    /** The analytics services **/
    val analyticService: Stream<AnalyticsService>

    /**
     *  Returns access token associated with user to be used for future sdk initialization.
     *  This access token acts as a unique identifier for a user profile in LiveLike system.
     *  Null value means sdk initialization process not completed.
     */
    val userAccessToken: String?

    /**
     * Returns public user stream.
     */
    val userStream: Stream<LiveLikeUserApi>

    /* Set user profile delegate to intercept any user related updates like rewards */
    var userProfileDelegate: UserProfileDelegate?

    var leaderBoardDelegate: LeaderBoardDelegate?

    /** Override the default auto-generated chat nickname **/
    fun updateChatNickname(nickname: String)

    /** Override the default auto-generated chat userpic **/
    fun updateChatUserPic(url: String?)

    fun createChatRoom(
        title: String? = null,
        visibility: Visibility? = null,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    )

    fun updateChatRoom(
        chatRoomId: String,
        title: String? = null,
        visibility: Visibility? = null,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    )

    fun getChatRoom(id: String, liveLikeCallback: LiveLikeCallback<ChatRoomInfo>)

    fun addCurrentUserToChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    )

    fun getCurrentUserChatRoomList(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInfo>>
    )

    fun getMembersOfChatRoom(
        chatRoomId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<LiveLikeUser>>
    )

    fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<Boolean>
    )

    fun getLeaderBoardsForProgram(
        programId: String,
        liveLikeCallback: LiveLikeCallback<List<LeaderBoard>>
    )

    fun getLeaderBoardDetails(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoard>
    )

    fun getEntriesForLeaderBoard(
        leaderBoardId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntryPaginationResult>
    )

    fun getLeaderBoardEntryForProfile(
        leaderBoardId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    )

    fun getLeaderBoardEntryForCurrentUserProfile(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    )

    fun getLeaderboardClients(
        leaderBoardId: List<String>,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    )

    fun getChatUserMutedStatus(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatUserMuteStatus>
    )

    fun getCurrentUserDetails(liveLikeCallback: LiveLikeCallback<LiveLikeUserApi>)

    /**
     * Returns the sponsor client
     */
    fun sponsor(): Sponsor

    /**
     * Returns the Badges client
     */
    fun badges(): Badges

    fun close()
}
