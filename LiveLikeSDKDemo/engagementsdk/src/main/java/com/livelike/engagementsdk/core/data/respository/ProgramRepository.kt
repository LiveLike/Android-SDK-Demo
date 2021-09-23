package com.livelike.engagementsdk.core.data.respository

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.TEMPLATE_PROGRAM_ID
import com.livelike.engagementsdk.core.data.models.Program
import com.livelike.engagementsdk.core.data.models.ProgramModel
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.models.toProgram
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that handles program and program-user data.
 * Program is an event in CMS App.
 */
internal class ProgramRepository(
    val programId: String,
    private val userRepository: UserRepository
) : BaseRepository() {

    internal var program: Program? = null
    internal var programUrlTemplate: String? = null

    internal val rewardType: RewardsType by lazy {
        RewardsType.valueOf(
            program?.rewardsType?.toUpperCase() ?: "none"
        )
    }
    /**
     *  user points and other gamification stuff under this program.
     */
    val programGamificationProfileStream: Stream<ProgramGamificationProfile> =
        SubscriptionManager()

    /**
     * responsible for fetching program resource
     * @param programDetailUrlTemplate (received in engagement configuration resource)
     */
    suspend fun getProgramData(programDetailUrlTemplate: String): Result<ProgramModel> {
        var results: Result<ProgramModel>? = null
        results = dataClient.remoteCall<ProgramModel>(
            programDetailUrlTemplate.replace(
                TEMPLATE_PROGRAM_ID,
                programId
            ),
            RequestType.GET,
            accessToken = userRepository.userAccessToken
        )
        if (results is Result.Success) {
            this@ProgramRepository.program = results.data.toProgram()
        }
        return results
    }

    suspend fun fetchProgramRank() {
        program?.let { program ->
            if (program.rewardsType.equals(RewardsType.NONE.key)) {
                logError { "Should not call if Gamification is disabled" }
                return
            }

            val result = dataClient.remoteCall<ProgramGamificationProfile>(
                program.rankUrl,
                RequestType.GET,
                accessToken = userRepository.userAccessToken
            )
            if (result is Result.Success) {
                withContext(Dispatchers.Main) {
                    val programGamification = result.data
                    logDebug { "points update : ${programGamification.points}, rank update: ${programGamification.rank}" }
                    programGamificationProfileStream.onNext(programGamification)
                }
            }
        }
    }
}
