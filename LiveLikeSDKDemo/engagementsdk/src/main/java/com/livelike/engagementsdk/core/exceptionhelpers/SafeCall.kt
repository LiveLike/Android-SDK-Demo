package com.livelike.engagementsdk.core.exceptionhelpers

import com.livelike.engagementsdk.core.services.network.Result
import java.io.IOException

@Suppress("UNUSED_PARAMETER")
internal fun safeCodeBlockCall(call: () -> Unit, errorMessage: String? = null) {

    return try {
        call()
    } catch (ex: Throwable) {
        ex.cause?.printStackTrace() ?: ex.printStackTrace()
    }
}

/**
 * Wrap a suspending API [call] in try/catch. In case an exception is thrown, a [Result.Error] is
 * created based on the [errorMessage].
 */
internal suspend fun <T : Any> safeRemoteApiCall(
    call: suspend () -> Result<T>,
    errorMessage: String? = null
): Result<T> {
    return try {
        call()
    } catch (e: Exception) {
        e.printStackTrace()
        // An exception was thrown when calling the API so we're converting this to an IOException
        Result.Error(IOException(errorMessage ?: e.message, e))
    }
}
