package com.livelike.engagementsdk

import android.content.Context
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.ChatSession
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMemberListResponse
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.chat.data.remote.UserChatRoomListResponse
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.data.repository.ChatRoomRepository
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntry
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntryPaginationResult
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntryResult
import com.livelike.engagementsdk.core.data.models.LeaderBoardForClient
import com.livelike.engagementsdk.core.data.models.LeaderBoardResource
import com.livelike.engagementsdk.core.data.models.LeaderboardClient
import com.livelike.engagementsdk.core.data.models.LeaderboardPlacement
import com.livelike.engagementsdk.core.data.models.toLeadBoard
import com.livelike.engagementsdk.core.data.models.toReward
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.Queue
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.combineLatestOnce
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedAccessToken
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.setSharedAccessToken
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.gamification.Badges
import com.livelike.engagementsdk.publicapis.ChatUserMuteStatus
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeUserApi
import com.livelike.engagementsdk.sponsorship.Sponsor
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.respository.LocalPredictionWidgetVoteRepository
import com.livelike.engagementsdk.widget.data.respository.PredictionWidgetVoteRepository
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate
import com.livelike.engagementsdk.widget.domain.UserProfileDelegate
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import java.io.IOException

/**
 * Use this class to initialize the EngagementSDK. This is the entry point for SDK usage. This creates an instance of EngagementSDK.
 *
 * @param clientId Client's id
 * @param applicationContext The application context
 */
class EngagementSDK(
    private val clientId: String,
    private val applicationContext: Context,
    private val errorDelegate: ErrorDelegate? = null,
    private val originURL: String? = null,
    private var accessTokenDelegate: AccessTokenDelegate? = null
) : IEngagement {

    private var userChatRoomListResponse: UserChatRoomListResponse? = null
    private var chatRoomMemberListMap: MutableMap<String, ChatRoomMemberListResponse> =
        mutableMapOf()
    internal var configurationStream: Stream<SdkConfiguration> =
        SubscriptionManager(true)
    private val dataClient =
        EngagementDataClientImpl()
    private val widgetDataClient = WidgetDataClientImpl()

    internal val userRepository =
        UserRepository(clientId)

    override var userProfileDelegate: UserProfileDelegate? = null
        set(value) {
            field = value
            userRepository.userProfileDelegate = value
        }

    override var leaderBoardDelegate: LeaderBoardDelegate? = null
        set(value) {
            field = value
            userRepository.leaderBoardDelegate = value
        }
    override var analyticService: Stream<AnalyticsService> =
        SubscriptionManager()

    private val job = SupervisorJob()

    // by default sdk calls will run on Default pool and further data layer calls will run o
    internal val sdkScope = CoroutineScope(Dispatchers.Default + job)

    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    // sdk config-user flow that can be collected by collect which is suspendably instead of using rx style combine on 2 seperate async results
    // TODO add util fun to convert streams to flow
    internal val configurationUserPairFlow = flow {
        while (configurationStream.latest() == null || userRepository.currentUserStream.latest() == null) {
            delay(1000)
        }
        emit(Pair(userRepository.currentUserStream.latest()!!, configurationStream.latest()!!))
    }

    /**
     * SDK Initialization logic.
     */
    init {
        AndroidThreeTen.init(applicationContext) // Initialize DateTime lib
        initLiveLikeSharedPrefs(
            applicationContext
        )
        if (accessTokenDelegate == null) {
            accessTokenDelegate = object : AccessTokenDelegate {
                override fun getAccessToken(): String? = getSharedAccessToken()

                override fun storeAccessToken(accessToken: String?) {
                    accessToken?.let { setSharedAccessToken(accessToken) }
                }
            }
        }
        userRepository.currentUserStream.subscribe(this.javaClass.simpleName) {
            it?.accessToken?.let { token ->
                userRepository.currentUserStream.unsubscribe(this.javaClass.simpleName)
                accessTokenDelegate!!.storeAccessToken(token)
            }
        }
        val url = originURL?.plus("/api/v1/applications/$clientId")
            ?: BuildConfig.CONFIG_URL.plus("applications/$clientId")
        dataClient.getEngagementSdkConfig(url) {
            if (it is Result.Success) {
                configurationStream.onNext(it.data)
                analyticService.onNext(
                    MixpanelAnalytics(
                        applicationContext,
                        it.data.mixpanelToken,
                        it.data.clientId
                    )
                )
                userRepository.initUser(accessTokenDelegate!!.getAccessToken(), it.data.profileUrl)
            } else {
                errorDelegate?.onError(
                    (it as Result.Error).exception.message
                        ?: "Some Error occurred, used sdk logger for more details"
                )
            }
        }
    }

    override val userStream: Stream<LiveLikeUserApi>
        get() = userRepository.currentUserStream.map {
            LiveLikeUserApi(it.nickname, it.accessToken, it.id, it.custom_data)
        }
    override val userAccessToken: String?
        get() = userRepository.userAccessToken

    override fun updateChatNickname(nickname: String) {
        sdkScope.launch {
            userRepository.updateChatNickname(nickname)
        }
    }

    override fun updateChatUserPic(url: String?) {
        sdkScope.launch {
            userRepository.setProfilePicUrl(url)
        }
    }

    override fun createChatRoom(
        title: String?,
        visibility: Visibility?,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    ) {
        createUpdateChatRoom(null, visibility, title, liveLikeCallback)
    }

    private fun createUpdateChatRoom(
        chatRoomId: String?,
        visibility: Visibility?,
        title: String?,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )

                    uiScope.launch {
                        val chatRoomResult = when (chatRoomId == null) {
                            true -> chatRepository.createChatRoom(
                                title, visibility, pair.second.createChatRoomUrl
                            )
                            else -> chatRepository.updateChatRoom(
                                title, visibility, chatRoomId, pair.second.chatRoomDetailUrlTemplate
                            )
                        }
                        if (chatRoomResult is Result.Success) {
                            liveLikeCallback.onResponse(
                                ChatRoomInfo(
                                    chatRoomResult.data.id,
                                    chatRoomResult.data.title,
                                    chatRoomResult.data.visibility,
                                    chatRoomResult.data.contentFilter,
                                    chatRoomResult.data.customData
                                ),
                                null
                            )
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun updateChatRoom(
        chatRoomId: String,
        title: String?,
        visibility: Visibility?,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    ) {
        createUpdateChatRoom(chatRoomId, visibility, title, liveLikeCallback)
    }

    override fun getChatRoom(id: String, liveLikeCallback: LiveLikeCallback<ChatRoomInfo>) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )

                    uiScope.launch {
                        val chatRoomResult = chatRepository.fetchChatRoom(
                            id, pair.second.chatRoomDetailUrlTemplate
                        )
                        if (chatRoomResult is Result.Success) {
                            liveLikeCallback.onResponse(
                                ChatRoomInfo(
                                    chatRoomResult.data.id,
                                    chatRoomResult.data.title,
                                    chatRoomResult.data.visibility,
                                    chatRoomResult.data.contentFilter,
                                    chatRoomResult.data.customData
                                ),
                                null
                            )
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun addCurrentUserToChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )
                    uiScope.launch {
                        val chatRoomResult = chatRepository.fetchChatRoom(
                            chatRoomId, pair.second.chatRoomDetailUrlTemplate
                        )
                        if (chatRoomResult is Result.Success) {
                            val currentUserChatRoomResult =
                                dataClient.remoteCall<ChatRoomMembership>(
                                    chatRoomResult.data.membershipsUrl,
                                    accessToken = pair.first.accessToken,
                                    requestType = RequestType.POST,
                                    requestBody = RequestBody.create(null, byteArrayOf())
                                )
                            if (currentUserChatRoomResult is Result.Success) {
                                liveLikeCallback.onResponse(
                                    currentUserChatRoomResult.data, null
                                )
                            } else if (currentUserChatRoomResult is Result.Error) {
                                liveLikeCallback.onResponse(
                                    null,
                                    currentUserChatRoomResult.exception.message
                                )
                            }
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun getCurrentUserChatRoomList(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInfo>>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) { it ->
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )
                    uiScope.launch {
                        val url = when (liveLikePagination) {
                            LiveLikePagination.FIRST -> pair.first.chat_room_memberships_url
                            LiveLikePagination.NEXT -> userChatRoomListResponse?.next
                            LiveLikePagination.PREVIOUS -> userChatRoomListResponse?.previous
                        }
                        if (url != null) {
                            val chatRoomResult = chatRepository.getCurrentUserChatRoomList(
                                url
                            )
                            if (chatRoomResult is Result.Success) {
                                userChatRoomListResponse = chatRoomResult.data
                                val list = userChatRoomListResponse!!.results?.map {
                                    ChatRoomInfo(
                                        it?.chatRoom?.id!!,
                                        it.chatRoom.title,
                                        it.chatRoom.visibility,
                                        it.chatRoom.contentFilter,
                                        it.chatRoom.customData
                                    )
                                }
                                liveLikeCallback.onResponse(list, null)
                            } else if (chatRoomResult is Result.Error) {
                                liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                            }
                        } else {
                            liveLikeCallback.onResponse(null, "No More data to load")
                        }
                    }
                }
            }
    }

    override fun getMembersOfChatRoom(
        chatRoomId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<LiveLikeUser>>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) { it ->
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )

                    uiScope.launch {
                        val chatRoomResult = chatRepository.fetchChatRoom(
                            chatRoomId, pair.second.chatRoomDetailUrlTemplate
                        )
                        if (chatRoomResult is Result.Success) {
                            val url = when (liveLikePagination) {
                                LiveLikePagination.FIRST -> chatRoomResult.data.membershipsUrl
                                LiveLikePagination.NEXT -> chatRoomMemberListMap[chatRoomId]?.next
                                LiveLikePagination.PREVIOUS -> chatRoomMemberListMap[chatRoomId]?.previous
                            }
                            if (url != null) {
                                val chatRoomMemberResult =
                                    dataClient.remoteCall<ChatRoomMemberListResponse>(
                                        url,
                                        accessToken = pair.first.accessToken,
                                        requestType = RequestType.GET
                                    )
                                if (chatRoomMemberResult is Result.Success) {
                                    chatRoomMemberListMap[chatRoomId] = chatRoomMemberResult.data
                                    val list = chatRoomMemberResult.data.results?.map {
                                        it?.profile!!
                                    }
                                    liveLikeCallback.onResponse(list, null)
                                } else if (chatRoomMemberResult is Result.Error) {
                                    liveLikeCallback.onResponse(
                                        null,
                                        chatRoomMemberResult.exception.message
                                    )
                                }
                            } else {
                                liveLikeCallback.onResponse(null, "No More data to load")
                            }
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<Boolean>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )
                    uiScope.launch {
                        val chatRoomResult = chatRepository.deleteCurrentUserFromChatRoom(
                            chatRoomId, pair.second.chatRoomDetailUrlTemplate
                        )
                        liveLikeCallback.onResponse(
                            true, null
                        )
                    }
                }
            }
    }

    override fun getLeaderBoardsForProgram(
        programId: String,
        liveLikeCallback: LiveLikeCallback<List<LeaderBoard>>
    ) {
        configurationStream.subscribe(this) { configuration ->
            configuration?.let {
                configurationStream.unsubscribe(this)
                dataClient.getProgramData(
                    configuration.programDetailUrlTemplate.replace(
                        TEMPLATE_PROGRAM_ID,
                        programId
                    )
                ) { program, error ->
                    when {
                        program?.leaderboards != null -> {
                            liveLikeCallback.onResponse(
                                program.leaderboards.map {
                                    LeaderBoard(
                                        it.id,
                                        it.name,
                                        it.rewardItem.toReward()
                                    )
                                },
                                null
                            )
                        }
                        error != null -> {
                            liveLikeCallback.onResponse(null, error)
                        }
                        else -> {
                            liveLikeCallback.onResponse(null, "Unable to fetch LeaderBoards")
                        }
                    }
                }
            }
        }
    }

    override fun getLeaderBoardDetails(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoard>
    ) {
        configurationStream.subscribe(this) {
            it?.let {
                configurationStream.unsubscribe(this)
                uiScope.launch {
                    val url = "${
                    it.leaderboardDetailUrlTemplate?.replace(
                        TEMPLATE_LEADER_BOARD_ID,
                        leaderBoardId
                    )
                    }"
                    val result = dataClient.remoteCall<LeaderBoardResource>(
                        url,
                        requestType = RequestType.GET,
                        accessToken = null
                    )
                    if (result is Result.Success) {
                        liveLikeCallback.onResponse(
                            result.data.toLeadBoard(),
                            null
                        )
                        // leaderBoardDelegate?.leaderBoard(result.data.toLeadBoard(),result.data)
                    } else if (result is Result.Error) {
                        liveLikeCallback.onResponse(null, result.exception.message)
                    }
                }
            }
        }
    }

    override fun getLeaderboardClients(
        leaderBoardId: List<String>,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    ) {
        val leaderBoardClientList = mutableListOf<LeaderboardClient>()
        configurationStream.subscribe(this) {
            it?.let {
                userRepository.currentUserStream.subscribe(this) { user ->
                    userRepository.currentUserStream.unsubscribe(this)
                    configurationStream.unsubscribe(this)
                    CoroutineScope(Dispatchers.IO).launch {

                        val job = ArrayList<Job>()
                        for (i in 0 until leaderBoardId.size.toInt()) {
                            job.add(
                                launch {
                                    val url = "${
                                    it.leaderboardDetailUrlTemplate?.replace(
                                        TEMPLATE_LEADER_BOARD_ID,
                                        leaderBoardId.get(i)
                                    )
                                    }"
                                    val result = dataClient.remoteCall<LeaderBoardResource>(
                                        url,
                                        requestType = RequestType.GET,
                                        accessToken = null
                                    )
                                    if (result is Result.Success) {
                                        user?.let { user ->
                                            val result2 =
                                                getLeaderBoardEntry(it, result.data.id, user.id)
                                            if (result2 is Result.Success) {
                                                leaderBoardDelegate?.leaderBoard(
                                                    LeaderBoardForClient(
                                                        result.data.id,
                                                        result.data.name,
                                                        result.data.rewardItem
                                                    ),
                                                    LeaderboardPlacement(
                                                        result2.data.rank,
                                                        result2.data.percentile_rank.toString(),
                                                        result2.data.score
                                                    )
                                                )
//                                            leaderBoardClientList.add(LeaderboardClient(result.data.id,result.data.name,result.data.rewardItem,LeaderboardPlacement(result2.data.rank
//                                            ,result2.data.percentile_rank.toString(),result2.data.score),leaderBoardDelegate!!))
                                                liveLikeCallback.onResponse(
                                                    LeaderboardClient(
                                                        result.data.id,
                                                        result.data.name,
                                                        result.data.rewardItem,
                                                        LeaderboardPlacement(
                                                            result2.data.rank,
                                                            result2.data.percentile_rank.toString(),
                                                            result2.data.score
                                                        ),
                                                        leaderBoardDelegate!!
                                                    ),
                                                    null
                                                )
                                            } else if (result2 is Result.Error) {
                                                leaderBoardDelegate?.leaderBoard(
                                                    LeaderBoardForClient(
                                                        result.data.id,
                                                        result.data.name,
                                                        result.data.rewardItem
                                                    ),
                                                    LeaderboardPlacement(0, " ", 0)
                                                )
                                            }
//
                                        }
                                    } else if (result is Result.Error) {
                                        liveLikeCallback.onResponse(null, result.exception.message)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun getChatUserMutedStatus(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatUserMuteStatus>
    ) {
        sdkScope.launch {
            getChatRoom(chatRoomId).collect {
                if (it is Result.Success) {
                    configurationUserPairFlow.collect { pair ->
                        val url = it.data.mutedStatusUrlTemplate ?: ""
                        liveLikeCallback.processResult(
                            ChatRoomRepository.getUserRoomMuteStatus(
                                url.replace(TEMPLATE_PROFILE_ID, pair.first.id)
                            )
                        )
                    }
                } else if (it is Result.Error) {
                    liveLikeCallback.processResult(it)
                }
            }
        }
    }

    override fun getCurrentUserDetails(liveLikeCallback: LiveLikeCallback<LiveLikeUserApi>) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) { pair ->
                pair?.let { _ ->
                    dataClient.getUserData(pair.second.profileUrl, pair.first.accessToken) {
                        if (it == null) {
                            liveLikeCallback.onResponse(
                                null,
                                "Network error or invalid access token"
                            )
                        } else {
                            liveLikeCallback.onResponse(
                                LiveLikeUserApi(
                                    it.nickname,
                                    it.accessToken,
                                    it.id,
                                    it.custom_data
                                ),
                                null
                            )
                        }
                    }
                }
            }
    }

    override fun sponsor(): Sponsor {
        return Sponsor(this)
    }

    override fun badges(): Badges {
        return Badges(configurationStream, dataClient, sdkScope)
    }

    /**
     * Closing all the services , stream and clear the variable
     * TODO: all stream close,instance clear
     */
    override fun close() {
        analyticService.latest()?.destroy()
        analyticService.clear()
    }

    internal suspend fun getChatRoom(chatRoomId: String): Flow<Result<ChatRoom>> {
        return flow {
            configurationUserPairFlow.collect {
                it.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )

                    val chatRoomResult = chatRepository.fetchChatRoom(
                        chatRoomId, pair.second.chatRoomDetailUrlTemplate
                    )
                    emit(chatRoomResult)
                }
            }
        }
    }

    private var leaderBoardEntryResult: HashMap<String, LeaderBoardEntryResult> = hashMapOf()
    private val leaderBoardEntryPaginationQueue =
        Queue<Pair<LiveLikePagination, Pair<String, LiveLikeCallback<LeaderBoardEntryPaginationResult>>>>()
    private var isQueueProcess = false

    override fun getEntriesForLeaderBoard(
        leaderBoardId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntryPaginationResult>
    ) {
        leaderBoardEntryPaginationQueue.enqueue(
            Pair(
                liveLikePagination,
                Pair(leaderBoardId, liveLikeCallback)
            )
        )
        if (!isQueueProcess) {
            val pair = leaderBoardEntryPaginationQueue.dequeue()
            if (pair != null)
                getEntries(pair)
        }
    }

    private fun getEntries(pair: Pair<LiveLikePagination, Pair<String, LiveLikeCallback<LeaderBoardEntryPaginationResult>>>) {
        isQueueProcess = true
        configurationStream.subscribe(this) { sdkConfiguration ->
            sdkConfiguration?.let {
                configurationStream.unsubscribe(this)
                uiScope.launch {
                    val leaderBoardId = pair.second.first
                    val liveLikeCallback = pair.second.second
                    val entriesUrl = when (pair.first) {
                        LiveLikePagination.FIRST -> {
                            val url = "${
                            it.leaderboardDetailUrlTemplate?.replace(
                                TEMPLATE_LEADER_BOARD_ID,
                                leaderBoardId
                            )
                            }"
                            val result = dataClient.remoteCall<LeaderBoardResource>(
                                url,
                                requestType = RequestType.GET,
                                accessToken = null
                            )
                            var defaultUrl = ""
                            if (result is Result.Success) {
                                defaultUrl = result.data.entries_url
                            } else if (result is Result.Error) {
                                defaultUrl = ""
                                liveLikeCallback.onResponse(null, result.exception.message)
                            }
                            defaultUrl
                        }
                        LiveLikePagination.NEXT -> leaderBoardEntryResult[leaderBoardId]?.next
                        LiveLikePagination.PREVIOUS -> leaderBoardEntryResult[leaderBoardId]?.previous
                    }
                    if (entriesUrl != null && entriesUrl.isNotEmpty()) {
                        val listResult = dataClient.remoteCall<LeaderBoardEntryResult>(
                            entriesUrl,
                            requestType = RequestType.GET,
                            accessToken = null
                        )
                        if (listResult is Result.Success) {
                            leaderBoardEntryResult[leaderBoardId] = listResult.data
                            liveLikeCallback.onResponse(
                                leaderBoardEntryResult[leaderBoardId]?.let {
                                    LeaderBoardEntryPaginationResult(
                                        it.count ?: 0,
                                        it.previous != null,
                                        it.next != null,
                                        it.results
                                    )
                                },
                                null
                            )
                        } else if (listResult is Result.Error) {
                            liveLikeCallback.onResponse(
                                null,
                                listResult.exception.message
                            )
                        }
                        isQueueProcess = false
                        val dequeuePair = leaderBoardEntryPaginationQueue.dequeue()
                        if (dequeuePair != null)
                            getEntries(dequeuePair)
                    } else if (entriesUrl == null || entriesUrl.isEmpty()) {
                        liveLikeCallback.onResponse(null, "No More data to load")
                        isQueueProcess = false
                        val dequeuePair = leaderBoardEntryPaginationQueue.dequeue()
                        if (dequeuePair != null)
                            getEntries(dequeuePair)
                    }
                }
            }
        }
    }

    override fun getLeaderBoardEntryForProfile(
        leaderBoardId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        configurationStream.subscribe(this) {
            it?.let {
                configurationStream.unsubscribe(this)
                uiScope.launch {
                    val url = "${
                    it.leaderboardDetailUrlTemplate?.replace(
                        TEMPLATE_LEADER_BOARD_ID,
                        leaderBoardId
                    )
                    }"
                    val result = dataClient.remoteCall<LeaderBoardResource>(
                        url,
                        requestType = RequestType.GET,
                        accessToken = null
                    )
                    if (result is Result.Success) {
                        val profileResult = dataClient.remoteCall<LeaderBoardEntry>(
                            result.data.entry_detail_url_template.replace(
                                TEMPLATE_PROFILE_ID,
                                profileId
                            ),
                            requestType = RequestType.GET,
                            accessToken = null
                        )
                        if (profileResult is Result.Success) {
                            liveLikeCallback.onResponse(profileResult.data, null)
                            // leaderBoardDelegate?.leaderBoard(profileResul)
                        } else if (profileResult is Result.Error) {
                            liveLikeCallback.onResponse(null, profileResult.exception.message)
                        }
                    } else if (result is Result.Error) {
                        liveLikeCallback.onResponse(null, result.exception.message)
                    }
                }
            }
        }

        getLeaderBoardDetails(
            leaderBoardId,
            object : LiveLikeCallback<LeaderBoard>() {
                override fun onResponse(result: LeaderBoard?, error: String?) {
                    result?.let {
                        uiScope.launch {
                        }
                    }
                    error?.let {
                        liveLikeCallback.onResponse(null, error)
                    }
                }
            }
        )
    }

    internal suspend fun getLeaderBoardEntry(
        sdkConfig: SdkConfiguration,
        leaderBoardId: String,
        profileId: String
    ): Result<LeaderBoardEntry> {
        val url = "${
        sdkConfig.leaderboardDetailUrlTemplate?.replace(
            TEMPLATE_LEADER_BOARD_ID,
            leaderBoardId
        )
        }"
        val result = dataClient.remoteCall<LeaderBoardResource>(
            url,
            requestType = RequestType.GET,
            accessToken = null
        )
        return when (result) {
            is Result.Success -> {
                dataClient.remoteCall(
                    result.data.entry_detail_url_template.replace(
                        TEMPLATE_PROFILE_ID,
                        profileId
                    ),
                    requestType = RequestType.GET,
                    accessToken = null
                )
            }
            is Result.Error -> {
                Result.Error(result.exception)
            }
        }
    }

    override fun getLeaderBoardEntryForCurrentUserProfile(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        userRepository.currentUserStream.subscribe(this) {
            it?.let { user ->
                userRepository.currentUserStream.unsubscribe(this)
                getLeaderBoardEntryForProfile(leaderBoardId, user.id, liveLikeCallback)
            }
        }
    }

    fun fetchWidgetDetails(
        widgetId: String,
        widgetKind: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeWidget>
    ) {
        uiScope.launch {
            try {
                val jsonObject = widgetDataClient.getWidgetDataFromIdAndKind(widgetId, widgetKind)
                val widget = gson.fromJson(jsonObject, LiveLikeWidget::class.java)
                liveLikeCallback.onResponse(
                    widget,
                    null
                )
            } catch (e: JsonParseException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            } catch (e: IOException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            }
        }
    }

    /**
     *  Creates a content session without sync.
     *  @param programId Backend generated unique identifier for current program
     */
    fun createContentSession(
        programId: String,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeContentSession {
        return ContentSession(
            clientId,
            configurationStream,
            userRepository,
            applicationContext,
            programId,
            analyticService,
            errorDelegate
        ) { EpochTime(0) }
    }

    /**
     * Use to retrieve the current timecode from the videoplayer to enable Spoiler-Free Sync.
     *
     */
    interface TimecodeGetter {
        fun getTimecode(): EpochTime
    }

    /**
     *  Creates a content session with sync.
     *  @param programId Backend generated identifier for current program
     *  @param timecodeGetter returns the video timecode
     */
    fun createContentSession(
        programId: String,
        timecodeGetter: TimecodeGetter,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeContentSession {
        return ContentSession(
            clientId,
            configurationStream,
            userRepository,
            applicationContext,
            programId,
            analyticService,
            errorDelegate
        ) { timecodeGetter.getTimecode() }.apply {
            this.engagementSDK = this@EngagementSDK
        }
    }

    /**
     *  Creates a chat session.
     *  @param programId Backend generated identifier for current program
     *  @param timecodeGetter returns the video timecode
     */
    fun createChatSession(
        timecodeGetter: TimecodeGetter,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeChatSession {
        return ChatSession(
            configurationStream,
            userRepository,
            applicationContext,
            false,
            analyticService,
            errorDelegate
        ) { timecodeGetter.getTimecode() }
    }

    internal data class SdkConfiguration(
        val url: String,
        val name: String?,
        @SerializedName("client_id")
        val clientId: String,
        @SerializedName("media_url")
        val mediaUrl: String,
        @SerializedName("pubnub_subscribe_key")
        val pubNubKey: String,
        @SerializedName("pubnub_publish_key")
        val pubnubPublishKey: String?,
        @SerializedName("sendbird_app_id")
        val sendBirdAppId: String,
        @SerializedName("sendbird_api_endpoint")
        val sendBirdEndpoint: String,
        @SerializedName("programs_url")
        val programsUrl: String,
        @SerializedName("sessions_url")
        val sessionsUrl: String,
        @SerializedName("sticker_packs_url")
        val stickerPackUrl: String,
        @SerializedName("reaction_packs_url")
        val reactionPacksUrl: String,
        @SerializedName("mixpanel_token")
        val mixpanelToken: String,
        @SerializedName("analytics_properties")
        val analyticsProps: Map<String, String>,
        @SerializedName("chat_room_detail_url_template")
        val chatRoomDetailUrlTemplate: String,
        @SerializedName("create_chat_room_url")
        val createChatRoomUrl: String,
        @SerializedName("profile_url")
        val profileUrl: String,
        @SerializedName("profile_detail_url_template")
        val profileDetailUrlTemplate: String,
        @SerializedName("program_detail_url_template")
        val programDetailUrlTemplate: String,
        @SerializedName("pubnub_origin")
        val pubnubOrigin: String? = null,
        @SerializedName("leaderboard_detail_url_template")
        val leaderboardDetailUrlTemplate: String? = null,
        @SerializedName("pubnub_heartbeat_interval")
        val pubnubHeartbeatInterval: Int,
        @SerializedName("pubnub_presence_timeout")
        val pubnubPresenceTimeout: Int,
        @SerializedName("badges_url")
        val badgesUrl: String
    )

    companion object {
        @JvmStatic
        var enableDebug: Boolean = false

        @JvmStatic
        var predictionWidgetVoteRepository: PredictionWidgetVoteRepository =
            LocalPredictionWidgetVoteRepository()
    }
}
