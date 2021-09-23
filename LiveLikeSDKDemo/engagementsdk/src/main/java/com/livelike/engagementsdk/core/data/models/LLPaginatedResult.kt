package com.livelike.engagementsdk.core.data.models

import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination

class LLPaginatedResult<out T> {

    internal val previous: String? = null
    internal val next: String? = null
    val count: Int = 0
    val results: List<T>? = null

    fun hasNext(): Boolean {
        return next != null
    }

    fun hasPrev(): Boolean {
        return previous != null
    }

    internal fun getPaginationUrl(liveLikePagination: LiveLikePagination): String? {
        return when (liveLikePagination) {
            LiveLikePagination.NEXT -> this.next
            LiveLikePagination.PREVIOUS -> this.previous
            else -> this.next
        }
    }
}
