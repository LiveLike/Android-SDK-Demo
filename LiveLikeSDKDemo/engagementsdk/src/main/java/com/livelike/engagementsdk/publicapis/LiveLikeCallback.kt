package com.livelike.engagementsdk.publicapis

import com.livelike.engagementsdk.core.services.network.Result

abstract class LiveLikeCallback<T : Any> {

    abstract fun onResponse(result: T?, error: String?)

    internal fun processResult(result: Result<T>) {
        if (result is Result.Success) {
            onResponse(result.data, null)
        } else if (result is Result.Error) {
            onResponse(null, result.exception.message ?: "Error in fetching data")
        }
    }
}

abstract class ErrorDelegate {

    /**
     Called when the given object has failed to setup properly
     Upon receiving this call, the `sdk` should be considered invalid and unuseable.
     If caused by some transient failure like a poor network, a new `EngagementSDK` should
     be created.
     */
    abstract fun onError(error: String)
}
