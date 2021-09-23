package com.livelike.engagementsdk.core.utils

import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.exceptionhelpers.safeCodeBlockCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

internal class SubscriptionManager<T>(private val emitOnSubscribe: Boolean = true) :
    Stream<T> {
    override fun latest(): T? {
        return currentData
    }

    private val observerMap = ConcurrentHashMap<Any, (T?) -> Unit>()
    var currentData: T? = null
        private set

    override fun onNext(data1: T?) {
        // TODO add debug log with class name appended
        logDebug { "subscription Manger: ${observerMap.size},data:$data1" }
        // Important change if not working properly revert back
        currentData = data1
        safeCodeBlockCall({
            observerMap.forEach {
                it.value.invoke(data1)
            }
        })
    }

    override fun subscribe(key: Any, observer: (T?) -> Unit) {
        observerMap[key] = observer
        if (emitOnSubscribe) observer.invoke(currentData)
    }

    override fun unsubscribe(key: Any) {
        if (observerMap.containsKey(key))
            observerMap.remove(key)
    }

    override fun clear() {
        currentData = null
        onNext(null)
        observerMap.clear()
    }
}

/**
 * Applies the given function on the same thread to each value emitted by source stream and returns stream, which emits resulting values.
 */
internal fun <X, Y> Stream<X>.map(applyTransformation: (x: X) -> Y): Stream<Y> {

    val out = SubscriptionManager<Y>()
    this.subscribe(out.hashCode()) {
        it?.let {
            out.onNext(applyTransformation(it))
        }
    }

    return out
}

/**
 * combine the latest from 2 streams only once, so the stream out will be single RX
 */
internal fun <X, Y> Stream<X>.combineLatestOnce(
    other: Stream<Y>,
    hashCode: Int? = null
): Stream<Pair<X, Y>> {
    val pairedStream: Stream<Pair<X, Y>> =
        SubscriptionManager()
    val combinedHashCode = "${other.hashCode()}$hashCode"
    this.subscribe(combinedHashCode) {
        it?.let { x ->
            this.unsubscribe(combinedHashCode)
            other.subscribe(this.hashCode()) { y ->
                y?.let {
                    other.unsubscribe(this.hashCode())
                    pairedStream.onNext(Pair(x, y))
                }
            }
        }
    }
    return pairedStream
}

internal fun <T> SubscriptionManager<T>.debounce(duration: Long = 300L): SubscriptionManager<T> =
    SubscriptionManager<T>()
        .let { mgr ->
            val source = this
            val handler = Handler(Looper.getMainLooper())

            fun runnable(): Runnable {
                return Runnable {
                    mgr.onNext(source.currentData)
                }
            }

            source.subscribe(source::class.java.simpleName) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(runnable(), duration)
            }

            return mgr
        }

internal fun <T> Stream<T>.toFlow(): Flow<T?> {
    return flow {
        while (this@toFlow.latest() == null) {
            delay(1000)
        }
        emit(this@toFlow.latest())
    }
}
